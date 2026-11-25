package com.egolite.mobile.browser

import android.net.Uri
import com.egolite.mobile.model.TranslationEngine
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LangOption(val code: String, val label: String)

object TranslationManager {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        engine { requestTimeout = 15_000 }
    }

    val sourceLanguages = listOf(
        LangOption("auto", "감지된 언어"),
        LangOption("en", "영어"),
        LangOption("ja", "일본어"),
        LangOption("zh-CN", "중국어"),
        LangOption("ko", "한국어"),
        LangOption("de", "독일어"),
        LangOption("fr", "프랑스어"),
        LangOption("es", "스페인어")
    )

    val targetLanguages = listOf(
        LangOption("ko", "한국어"),
        LangOption("en", "영어"),
        LangOption("ja", "일본어"),
        LangOption("zh-CN", "중국어"),
        LangOption("de", "독일어"),
        LangOption("fr", "프랑스어"),
        LangOption("es", "스페인어")
    )

    fun labelFor(code: String): String {
        val norm = if (code.startsWith("zh")) "zh-CN" else code.lowercase()
        return sourceLanguages.firstOrNull { it.code.equals(norm, ignoreCase = true) }?.label ?: code.uppercase()
    }

    suspend fun translatePageInSpace(
        spaceId: String,
        sourceLang: String,
        targetLang: String,
        engine: TranslationEngine,
        onProgress: (String) -> Unit = {}
    ): Pair<Boolean, String> {
        val rawNodes = SpaceManager.evaluateJsInExactSpace(spaceId, BrowserScripts.COLLECT_TRANSLATABLE_NODES_JS)
        val arr = runCatching { JSONArray(rawNodes) }.getOrNull() ?: return false to "번역할 텍스트를 찾지 못했습니다"
        if (arr.length() == 0) return false to "번역할 텍스트가 없습니다"

        val items = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            obj.getInt("i") to obj.getString("t")
        }
        val originals = items.map { it.second }

        val (translated, detectedCode) = when (engine) {
            TranslationEngine.ONLINE_GOOGLE -> {
                onProgress("Google 서버로 번역 중...")
                translateOnlineChunked(originals, sourceLang, targetLang)
            }
            TranslationEngine.OFFLINE_MLKIT -> {
                onProgress("온디바이스 오프라인 모델 준비·번역 중...")
                translateOfflineMlKit(originals, sourceLang, targetLang)
            }
        }

        val outArr = JSONArray()
        for (idx in items.indices) {
            val newText = translated.getOrNull(idx) ?: originals[idx]
            outArr.put(JSONObject().put("i", items[idx].first).put("t", newText))
        }
        SpaceManager.evaluateJsInExactSpace(spaceId, BrowserScripts.applyTranslationsJs(outArr.toString()))
        return true to detectedCode
    }

    suspend fun restoreOriginalInSpace(spaceId: String) {
        runCatching {
            SpaceManager.evaluateJsInExactSpace(spaceId, BrowserScripts.RESTORE_ORIGINAL_TEXT_JS)
        }
    }

    private suspend fun translateOnlineChunked(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): Pair<List<String>, String> = coroutineScope {
        val chunks = texts.chunked(14)
        var detected = sourceLang
        val results = chunks.map { chunk ->
            async {
                val (res, det) = translateSingleOnlineChunk(chunk, sourceLang, targetLang)
                if (det.isNotBlank() && detected == "auto") detected = det
                res
            }
        }.awaitAll().flatten()
        results to detected
    }

    private suspend fun translateSingleOnlineChunk(
        chunk: List<String>,
        sourceLang: String,
        targetLang: String
    ): Pair<List<String>, String> {
        val sep = "\n@@@\n"
        val joined = chunk.joinToString(sep)
        return runCatching {
            val sl = Uri.encode(sourceLang.ifBlank { "auto" })
            val tl = Uri.encode(targetLang.ifBlank { "ko" })
            val q = Uri.encode(joined)
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$q"
            val body = httpClient.get(url).bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray
            val sentences = root[0].jsonArray
            val detected = root.getOrNull(2)?.jsonPrimitive?.contentOrNull ?: sourceLang
            val full = buildString {
                sentences.forEach { s ->
                    append(s.jsonArray[0].jsonPrimitive.contentOrNull.orEmpty())
                }
            }
            val split = full.split("@@@").map { it.trim() }
            if (split.size == chunk.size) split to detected else chunk to detected
        }.getOrElse { chunk to sourceLang }
    }

    private suspend fun translateOfflineMlKit(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): Pair<List<String>, String> {
        val detectedSource = if (sourceLang == "auto") {
            val sample = texts.take(5).joinToString(" ").take(300)
            detectLanguageMlKit(sample) ?: "en"
        } else {
            sourceLang
        }

        val mlSrc = toMlKitLang(detectedSource) ?: TranslateLanguage.ENGLISH
        val mlDst = toMlKitLang(targetLang) ?: TranslateLanguage.KOREAN
        if (mlSrc == mlDst) return texts to detectedSource

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlSrc)
            .setTargetLanguage(mlDst)
            .build()
        val translator = Translation.getClient(options)
        return try {
            val conditions = DownloadConditions.Builder().build()
            suspendCancellableCoroutine<Unit> { cont ->
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val translated = texts.map { line ->
                runCatching {
                    suspendCancellableCoroutine<String> { cont ->
                        translator.translate(line)
                            .addOnSuccessListener { cont.resume(it) }
                            .addOnFailureListener { cont.resume(line) }
                    }
                }.getOrDefault(line)
            }
            translated to detectedSource
        } catch (_: Exception) {
            texts to detectedSource
        } finally {
            translator.close()
        }
    }

    private suspend fun detectLanguageMlKit(sample: String): String? {
        val identifier = LanguageIdentification.getClient()
        return try {
            suspendCancellableCoroutine { cont ->
                identifier.identifyLanguage(sample)
                    .addOnSuccessListener { code ->
                        cont.resume(if (code == "und") null else code)
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        } finally {
            identifier.close()
        }
    }

    private fun toMlKitLang(code: String): String? {
        val norm = code.lowercase().substringBefore("-")
        return when (norm) {
            "ko" -> TranslateLanguage.KOREAN
            "en" -> TranslateLanguage.ENGLISH
            "ja" -> TranslateLanguage.JAPANESE
            "zh" -> TranslateLanguage.CHINESE
            "de" -> TranslateLanguage.GERMAN
            "fr" -> TranslateLanguage.FRENCH
            "es" -> TranslateLanguage.SPANISH
            else -> TranslateLanguage.fromLanguageTag(norm)
        }
    }
}
