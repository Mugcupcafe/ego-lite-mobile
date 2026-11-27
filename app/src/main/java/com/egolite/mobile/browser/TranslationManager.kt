package com.egolite.mobile.browser

import com.egolite.mobile.model.TranslationEngine
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LangOption(val code: String, val label: String)

object TranslationManager {
    private val json = Json { ignoreUnknownKeys = true }

    private const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 16; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36"

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
        onProgress: (String) -> Unit = {},
        onProgressFraction: (Float) -> Unit = {}
    ): Pair<Boolean, String> {
        val rawNodes = SpaceManager.evaluateJsInExactSpace(spaceId, BrowserScripts.COLLECT_TRANSLATABLE_NODES_JS)
        val arr = runCatching { JSONArray(rawNodes) }.getOrNull() ?: return false to "번역할 텍스트 없음"
        if (arr.length() == 0) return false to "번역할 텍스트 없음"

        val items = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            obj.getInt("i") to obj.getString("t")
        }
        val originals = items.map { it.second }

        onProgressFraction(0.08f)
        var result = when (engine) {
            TranslationEngine.ONLINE_GOOGLE -> {
                onProgress("번역 중...")
                translateOnlineChunked(originals, sourceLang, targetLang, onProgressFraction)
            }
            TranslationEngine.OFFLINE_MLKIT -> {
                onProgress("오프라인 번역 중...")
                onProgressFraction(0.35f)
                val r = translateOfflineMlKit(originals, sourceLang, targetLang)
                onProgressFraction(0.92f)
                r
            }
        }

        // 온라인 번역이 일시적으로 차단/실패한 경우 오프라인 ML Kit으로 자동 폴백
        if (result.okCount == 0 && engine == TranslationEngine.ONLINE_GOOGLE) {
            onProgress("오프라인 엔진으로 전환 중...")
            val fallback = translateOfflineMlKit(originals, sourceLang, targetLang)
            if (fallback.okCount > 0) {
                result = fallback
            }
        }

        if (result.okCount == 0) {
            return false to "번역 실패 (네트워크 확인)"
        }

        val outArr = JSONArray()
        for (idx in items.indices) {
            val newText = result.texts.getOrNull(idx) ?: originals[idx]
            outArr.put(JSONObject().put("i", items[idx].first).put("t", newText))
        }
        onProgressFraction(0.96f)
        SpaceManager.evaluateJsInExactSpace(spaceId, BrowserScripts.applyTranslationsJs(outArr.toString()))
        onProgressFraction(1.0f)
        return true to result.detected
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
        targetLang: String,
        onProgressFraction: (Float) -> Unit = {}
    ): ChunkResult = coroutineScope {
        // GET URL 길이 및 레이트 리밋을 고려한 안전한 소청크 분할 (최대 6문장, 480자 이하)
        val chunks = mutableListOf<List<String>>()
        var cur = mutableListOf<String>()
        var curChars = 0
        for (t in texts) {
            val clean = t.replace("\r", " ").replace("\n", " ").trim()
            if (cur.isNotEmpty() && (cur.size >= 6 || curChars + clean.length > 480)) {
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

        // 동시 2개씩 병렬 처리 + 짧은 간격으로 429 레이트 리밋 방지
        val allTranslated = mutableListOf<String>()
        val batches = chunks.chunked(2)
        val totalBatches = batches.size.coerceAtLeast(1)
        for ((batchIdx, batch) in batches.withIndex()) {
            if (batchIdx > 0) delay(45L)
            val batchResults = batch.map { chunk ->
                async(Dispatchers.IO) { translateOnlineBatchWithFallback(chunk, sourceLang, targetLang) }
            }.awaitAll()
            for (res in batchResults) {
                if (res.detected.isNotBlank() && res.detected != "auto" && detected == "auto") {
                    detected = res.detected
                }
                totalOk += res.okCount
                allTranslated.addAll(res.texts)
            }
            val frac = 0.12f + 0.80f * ((batchIdx + 1).toFloat() / totalBatches.toFloat())
            onProgressFraction(frac.coerceIn(0.12f, 0.94f))
        }
        ChunkResult(allTranslated, detected, totalOk)
    }

    private suspend fun translateOnlineBatchWithFallback(
        chunk: List<String>,
        sourceLang: String,
        targetLang: String
    ): ChunkResult {
        val joined = chunk.joinToString("\n")
        val direct = callGoogleTranslateWithFallback(joined, sourceLang, targetLang)
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
            val res = callGoogleTranslateWithFallback(single, sourceLang, targetLang)
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

    private suspend fun callGoogleTranslateWithFallback(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val sl = sourceLang.ifBlank { "auto" }
        val tl = targetLang.ifBlank { "ko" }
        // 1순위: translate.googleapis.com GET (안드로이드 네이티브 HttpURLConnection)
        callGtxGet(text, sl, tl)
            // 2순위: Chrome 번역 확장 API (clients5.google.com)
            ?: callClients5Get(text, sl, tl)
            // 3순위: translate.googleapis.com POST
            ?: callGtxPost(text, sl, tl)
    }

    private fun callGtxGet(text: String, sl: String, tl: String): Pair<String, String>? {
        return runCatching {
            val encSl = URLEncoder.encode(sl, "UTF-8")
            val encTl = URLEncoder.encode(tl, "UTF-8")
            val encQ = URLEncoder.encode(text, "UTF-8")
            val endpoint = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$encSl&tl=$encTl&dt=t&q=$encQ"
            val body = httpGetText(endpoint) ?: return null
            parseGtxResponse(body, sl)
        }.getOrNull()
    }

    private fun callClients5Get(text: String, sl: String, tl: String): Pair<String, String>? {
        return runCatching {
            val encSl = URLEncoder.encode(sl, "UTF-8")
            val encTl = URLEncoder.encode(tl, "UTF-8")
            val encQ = URLEncoder.encode(text, "UTF-8")
            val endpoint = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=$encSl&tl=$encTl&q=$encQ"
            val body = httpGetText(endpoint) ?: return null
            val root = json.parseToJsonElement(body).jsonArray
            if (root.isEmpty()) return null
            val first = root[0]
            when (first) {
                is JsonArray -> {
                    val translated = first.getOrNull(0)?.jsonPrimitive?.contentOrNull.orEmpty()
                    val detected = first.getOrNull(1)?.jsonPrimitive?.contentOrNull ?: sl
                    if (translated.isBlank()) null else translated to detected
                }
                is JsonPrimitive -> {
                    val translated = first.contentOrNull.orEmpty()
                    if (translated.isBlank()) null else translated to sl
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun callGtxPost(text: String, sl: String, tl: String): Pair<String, String>? {
        return runCatching {
            val encSl = URLEncoder.encode(sl, "UTF-8")
            val encTl = URLEncoder.encode(tl, "UTF-8")
            val endpoint = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$encSl&tl=$encTl&dt=t"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("User-Agent", BROWSER_UA)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }
            try {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write("q=${URLEncoder.encode(text, "UTF-8")}")
                    writer.flush()
                }
                if (conn.responseCode !in 200..299) return null
                val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                parseGtxResponse(body, sl)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun httpGetText(endpoint: String): String? {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", BROWSER_UA)
            setRequestProperty("Accept", "application/json, text/plain, */*")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseGtxResponse(body: String, fallbackSource: String): Pair<String, String>? {
        val root = json.parseToJsonElement(body).jsonArray
        val sentences = root.getOrNull(0)?.jsonArray ?: return null
        val detected = root.getOrNull(2)?.jsonPrimitive?.contentOrNull ?: fallbackSource
        val full = buildString {
            sentences.forEach { s ->
                val part = runCatching { s.jsonArray[0].jsonPrimitive.contentOrNull.orEmpty() }.getOrDefault("")
                append(part)
            }
        }
        return if (full.isBlank()) null else full to detected
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
