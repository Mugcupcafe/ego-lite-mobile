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
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
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
        engine { requestTimeout = 20_000 }
    }

    private const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 16; YLP-W00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    val targetLanguages: List<LangOption> = listOf(
        LangOption("ko", "한국어"),
        LangOption("en", "영어"),
        LangOption("ja", "일본어"),
        LangOption("zh-CN", "중국어(간체)"),
        LangOption("zh-TW", "중국어(번체)"),
        LangOption("es", "스페인어"),
        LangOption("fr", "프랑스어"),
        LangOption("de", "독일어"),
        LangOption("ru", "러시아어"),
        LangOption("pt", "포르투갈어"),
        LangOption("it", "이탈리아어"),
        LangOption("vi", "베트남어"),
        LangOption("th", "태국어"),
        LangOption("id", "인도네시아어"),
        LangOption("ar", "아랍어"),
        LangOption("hi", "힌디어"),
        LangOption("tr", "튀르키예어"),
        LangOption("nl", "네덜란드어"),
        LangOption("pl", "폴란드어"),
        LangOption("sv", "스웨덴어"),
        LangOption("no", "노르웨이어"),
        LangOption("da", "덴마크어"),
        LangOption("fi", "핀란드어"),
        LangOption("cs", "체코어"),
        LangOption("el", "그리스어"),
        LangOption("iw", "히브리어"),
        LangOption("hu", "헝가리어"),
        LangOption("ro", "루마니아어"),
        LangOption("uk", "우크라이나어"),
        LangOption("bg", "불가리아어"),
        LangOption("hr", "크로아티아어"),
        LangOption("sk", "슬로바키아어"),
        LangOption("sl", "슬로베니아어"),
        LangOption("sr", "세르비아어"),
        LangOption("lt", "리투아니아어"),
        LangOption("lv", "라트비아어"),
        LangOption("et", "에스토니아어"),
        LangOption("ms", "말레이어"),
        LangOption("tl", "필리핀어"),
        LangOption("bn", "벵골어"),
        LangOption("ta", "타밀어"),
        LangOption("te", "텔루구어"),
        LangOption("ur", "우르두어"),
        LangOption("fa", "페르시아어"),
        LangOption("sw", "스와힐리어"),
        LangOption("af", "아프리칸스어"),
        LangOption("ca", "카탈루냐어"),
        LangOption("is", "아이슬란드어"),
        LangOption("ga", "아일랜드어"),
        LangOption("cy", "웨일스어"),
        LangOption("mn", "몽골어"),
        LangOption("my", "미얀마어"),
        LangOption("km", "크메르어"),
        LangOption("lo", "라오어"),
        LangOption("ne", "네팔어"),
        LangOption("si", "싱할라어"),
        LangOption("kk", "카자흐어"),
        LangOption("uz", "우즈베크어")
    )

    val sourceLanguages: List<LangOption> = listOf(LangOption("auto", "자동 감지")) + targetLanguages

    fun labelFor(code: String): String {
        if (code.equals("auto", ignoreCase = true)) return "자동 감지"
        val norm = when {
            code.equals("zh-TW", ignoreCase = true) || code.equals("zh-Hant", ignoreCase = true) -> "zh-TW"
            code.startsWith("zh", ignoreCase = true) -> "zh-CN"
            code.equals("he", ignoreCase = true) -> "iw"
            else -> code.lowercase().substringBefore("-")
        }
        return targetLanguages.firstOrNull { it.code.equals(norm, ignoreCase = true) }?.label ?: code.uppercase()
    }

    suspend fun translatePageInSpace(
        spaceId: String,
        sourceLang: String,
        targetLang: String,
        engine: TranslationEngine,
        onProgress: (String) -> Unit = {}
    ): Pair<Boolean, String> {
        val rawNodes = SpaceManager.evaluateJsInExactSpace(spaceId, BrowserScripts.COLLECT_TRANSLATABLE_NODES_JS)
        val arr = runCatching { JSONArray(rawNodes) }.getOrNull() ?: return false to "번역할 텍스트 없음"
        if (arr.length() == 0) return false to "번역할 텍스트 없음"

        val items = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            obj.getInt("i") to obj.getString("t")
        }
        val originals = items.map { it.second }

        val (translated, detectedCode, successCount) = when (engine) {
            TranslationEngine.ONLINE_GOOGLE -> {
                onProgress("번역 중...")
                translateOnlineChunked(originals, sourceLang, targetLang)
            }
            TranslationEngine.OFFLINE_MLKIT -> {
                onProgress("오프라인 번역 중...")
                translateOfflineMlKit(originals, sourceLang, targetLang)
            }
        }

        if (successCount == 0) {
            return false to "번역 실패 (네트워크 확인)"
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

    private data class ChunkResult(
        val texts: List<String>,
        val detected: String,
        val okCount: Int
    )

    private suspend fun translateOnlineChunked(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): ChunkResult = coroutineScope {
        // 문자 수 예산 기반 소청크 분할 (최대 8문장, 900자 이하)
        val chunks = mutableListOf<List<String>>()
        var cur = mutableListOf<String>()
        var curChars = 0
        for (t in texts) {
            val clean = t.replace("\r", " ").replace("\n", " ").trim()
            if (cur.isNotEmpty() && (cur.size >= 8 || curChars + clean.length > 900)) {
                chunks.add(cur)
                cur = mutableListOf()
                curChars = 0
            }
            cur.add(clean)
            curChars += clean.length + 1
        }
        if (cur.isNotEmpty()) chunks.add(cur)

        var detected = sourceLang
        var totalOk = 0

        // 동시 4개씩 병렬 처리 (레이트 리밋 방지)
        val allTranslated = mutableListOf<String>()
        for (batch in chunks.chunked(4)) {
            val batchResults = batch.map { chunk ->
                async { translateOnlineBatchWithFallback(chunk, sourceLang, targetLang) }
            }.awaitAll()
            for (res in batchResults) {
                if (res.detected.isNotBlank() && res.detected != "auto" && detected == "auto") {
                    detected = res.detected
                }
                totalOk += res.okCount
                allTranslated.addAll(res.texts)
            }
        }
        ChunkResult(allTranslated, detected, totalOk)
    }

    private suspend fun translateOnlineBatchWithFallback(
        chunk: List<String>,
        sourceLang: String,
        targetLang: String
    ): ChunkResult {
        val joined = chunk.joinToString("\n")
        val direct = callGoogleTranslatePost(joined, sourceLang, targetLang)
        if (direct != null) {
            val (translatedFull, detected) = direct
            val lines = translatedFull.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size == chunk.size) {
                return ChunkResult(lines, detected, chunk.size)
            }
        }

        // 줄 수가 어긋나면 문장별 개별 번역으로 안전하게 폴백
        var detected = sourceLang
        var okCount = 0
        val fallbackList = chunk.map { single ->
            val res = callGoogleTranslatePost(single, sourceLang, targetLang)
            if (res != null) {
                if (res.second.isNotBlank() && res.second != "auto" && detected == "auto") {
                    detected = res.second
                }
                okCount++
                res.first.trim().ifBlank { single }
            } else {
                single
            }
        }
        return ChunkResult(fallbackList, detected, okCount)
    }

    private suspend fun callGoogleTranslatePost(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Pair<String, String>? {
        return runCatching {
            val sl = Uri.encode(sourceLang.ifBlank { "auto" })
            val tl = Uri.encode(targetLang.ifBlank { "ko" })
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t"
            val response = httpClient.post(url) {
                header(HttpHeaders.UserAgent, BROWSER_UA)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("q=${Uri.encode(text)}")
            }
            if (response.status.value !in 200..299) return null
            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray
            val sentences = root[0].jsonArray
            val detected = root.getOrNull(2)?.jsonPrimitive?.contentOrNull ?: sourceLang
            val full = buildString {
                sentences.forEach { s ->
                    append(s.jsonArray[0].jsonPrimitive.contentOrNull.orEmpty())
                }
            }
            if (full.isBlank()) null else full to detected
        }.getOrNull()
    }

    private suspend fun translateOfflineMlKit(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): ChunkResult {
        val detectedSource = if (sourceLang == "auto") {
            val sample = texts.take(5).joinToString(" ").take(300)
            detectLanguageMlKit(sample) ?: "en"
        } else {
            sourceLang
        }

        val mlSrc = toMlKitLang(detectedSource) ?: TranslateLanguage.ENGLISH
        val mlDst = toMlKitLang(targetLang) ?: TranslateLanguage.KOREAN
        if (mlSrc == mlDst) return ChunkResult(texts, detectedSource, texts.size)

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
            var ok = 0
            val translated = texts.map { line ->
                runCatching {
                    suspendCancellableCoroutine<String> { cont ->
                        translator.translate(line)
                            .addOnSuccessListener {
                                ok++
                                cont.resume(it)
                            }
                            .addOnFailureListener { cont.resume(line) }
                    }
                }.getOrDefault(line)
            }
            ChunkResult(translated, detectedSource, ok)
        } catch (_: Exception) {
            ChunkResult(texts, detectedSource, 0)
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
            "iw", "he" -> TranslateLanguage.HEBREW
            else -> TranslateLanguage.fromLanguageTag(norm)
        }
    }
}
