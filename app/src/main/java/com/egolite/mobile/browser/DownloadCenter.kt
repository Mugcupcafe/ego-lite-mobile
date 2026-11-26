package com.egolite.mobile.browser

import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.util.Locale

enum class DownloadStatus {
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadRecord(
    val id: String,
    val systemDownloadId: Long = -1L,
    val fileName: String,
    val sourceUrl: String,
    val sourceHost: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val contentUri: String = "",
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val timestampMs: Long = System.currentTimeMillis()
)

data class DownloadBannerState(
    val visible: Boolean = false,
    val isCompleted: Boolean = false,
    val activeCount: Int = 1,
    val record: DownloadRecord? = null
)

data class DuplicateDownloadPrompt(
    val fileName: String,
    val existingSizeBytes: Long,
    val existingUri: String,
    val mimeType: String,
    val onConfirmRedownload: (finalFileName: String) -> Unit
)

object DownloadCenter {
    private const val PREF_NAME = "ego_downloads_v1"
    private const val KEY_RECORDS = "records_json"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _records = MutableStateFlow<List<DownloadRecord>>(emptyList())
    val records: StateFlow<List<DownloadRecord>> = _records.asStateFlow()

    private val _banner = MutableStateFlow(DownloadBannerState())
    val banner: StateFlow<DownloadBannerState> = _banner.asStateFlow()

    private val _duplicatePrompt = MutableStateFlow<DuplicateDownloadPrompt?>(null)
    val duplicatePrompt: StateFlow<DuplicateDownloadPrompt?> = _duplicatePrompt.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadRecords()
    }

    private fun loadRecords() {
        val raw = prefs?.getString(KEY_RECORDS, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val list = ArrayList<DownloadRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    DownloadRecord(
                        id = o.optString("id"),
                        systemDownloadId = o.optLong("sysId", -1L),
                        fileName = o.optString("fileName"),
                        sourceUrl = o.optString("sourceUrl"),
                        sourceHost = o.optString("sourceHost"),
                        mimeType = o.optString("mimeType", "application/octet-stream"),
                        sizeBytes = o.optLong("sizeBytes", 0L),
                        contentUri = o.optString("contentUri"),
                        status = runCatching { DownloadStatus.valueOf(o.optString("status", "COMPLETED")) }
                            .getOrDefault(DownloadStatus.COMPLETED),
                        timestampMs = o.optLong("ts", System.currentTimeMillis())
                    )
                )
            }
            _records.value = list.sortedByDescending { it.timestampMs }
        }
    }

    private fun persistRecords(list: List<DownloadRecord>) {
        val trimmed = list.take(200)
        _records.value = trimmed
        val arr = JSONArray()
        trimmed.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("sysId", r.systemDownloadId)
                    put("fileName", r.fileName)
                    put("sourceUrl", r.sourceUrl)
                    put("sourceHost", r.sourceHost)
                    put("mimeType", r.mimeType)
                    put("sizeBytes", r.sizeBytes)
                    put("contentUri", r.contentUri)
                    put("status", r.status.name)
                    put("ts", r.timestampMs)
                }
            )
        }
        prefs?.edit()?.putString(KEY_RECORDS, arr.toString())?.apply()
    }

    fun resolveCleanFileName(
        url: String,
        contentDisposition: String?,
        mimeType: String?,
        suggestedDownloadAttr: String? = null
    ): String {
        var name: String? = suggestedDownloadAttr?.trim()?.takeIf { it.isNotBlank() }

        // 1) RFC 5987 filename*=UTF-8''... 우선 파싱 (한글·특수문자 깨짐 방지)
        if (name.isNullOrBlank() && !contentDisposition.isNullOrBlank()) {
            val rfc5987 = Regex("""filename\*\s*=\s*(?:UTF-8|utf-8)?''([^;\r\n]+)""")
                .find(contentDisposition)?.groupValues?.getOrNull(1)?.trim()
            if (!rfc5987.isNullOrBlank()) {
                name = runCatching { URLDecoder.decode(rfc5987, "UTF-8") }.getOrDefault(rfc5987)
            }
        }
        // 2) 일반 filename="..." 파싱
        if (name.isNullOrBlank() && !contentDisposition.isNullOrBlank()) {
            val plain = Regex("""filename\s*=\s*"?([^";\r\n]+)"?""")
                .find(contentDisposition)?.groupValues?.getOrNull(1)?.trim()
            if (!plain.isNullOrBlank()) {
                name = runCatching { URLDecoder.decode(plain, "UTF-8") }.getOrDefault(plain)
            }
        }
        // 3) URL 경로의 마지막 세그먼트 (blob/data 제외)
        if (name.isNullOrBlank() && !url.startsWith("blob:", true) && !url.startsWith("data:", true)) {
            val cleanPath = Uri.parse(url).lastPathSegment
            if (!cleanPath.isNullOrBlank() && cleanPath.length <= 120) {
                name = runCatching { URLDecoder.decode(cleanPath, "UTF-8") }.getOrDefault(cleanPath)
            }
        }
        // 4) 기본 추측 (blob/data UUID 방지)
        if (name.isNullOrBlank()) {
            if (url.startsWith("blob:", true) || url.startsWith("data:", true)) {
                val ext = extensionFromMime(mimeType) ?: "bin"
                name = "download_${System.currentTimeMillis() % 100000}.$ext"
            } else {
                name = URLUtil.guessFileName(url, contentDisposition, mimeType)
            }
        }

        var cleaned = name
            .map { ch -> if (ch in "\\/:*?\"<>|\r\n") '_' else ch }.joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "download_${System.currentTimeMillis() % 100000}" }

        // .bin 잘못 붙은 확장자 보정
        val extFromMime = extensionFromMime(mimeType)
        if (cleaned.endsWith(".bin", ignoreCase = true) && !extFromMime.isNullOrBlank() && extFromMime != "bin") {
            cleaned = cleaned.removeSuffix(".bin").removeSuffix(".BIN") + "." + extFromMime
        } else if (!cleaned.contains('.') && !extFromMime.isNullOrBlank()) {
            cleaned = "$cleaned.$extFromMime"
        }
        return cleaned
    }

    private fun extensionFromMime(mimeType: String?): String? {
        if (mimeType.isNullOrBlank() || mimeType == "application/octet-stream") return null
        val cleanMime = mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)
        return when (cleanMime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "application/pdf" -> "pdf"
            "application/zip", "application/x-zip-compressed" -> "zip"
            "application/vnd.android.package-archive" -> "apk"
            "text/plain" -> "txt"
            "text/csv" -> "csv"
            "text/html" -> "html"
            "application/json" -> "json"
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime)
        }
    }

    fun extractHost(url: String): String {
        if (url.startsWith("blob:", true)) {
            val inner = url.removePrefix("blob:")
            return runCatching { Uri.parse(inner).host.orEmpty() }.getOrDefault("")
        }
        if (url.startsWith("data:", true)) return "내부 데이터"
        return runCatching { Uri.parse(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.2fKB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2fMB", mb)
        return String.format(Locale.US, "%.2fGB", mb / 1024.0)
    }

    fun checkDuplicateOrProceed(
        context: Context,
        fileName: String,
        mimeType: String,
        onProceed: (finalFileName: String) -> Unit
    ) {
        val existing = findExistingFile(context, fileName)
        if (existing != null) {
            _duplicatePrompt.value = DuplicateDownloadPrompt(
                fileName = fileName,
                existingSizeBytes = existing.first,
                existingUri = existing.second,
                mimeType = mimeType,
                onConfirmRedownload = { nextName ->
                    _duplicatePrompt.value = null
                    onProceed(nextName)
                }
            )
        } else {
            onProceed(fileName)
        }
    }

    fun dismissDuplicatePrompt() {
        _duplicatePrompt.value = null
    }

    fun makeUniqueFileName(context: Context, original: String): String {
        val dot = original.lastIndexOf('.')
        val base = if (dot > 0) original.substring(0, dot) else original
        val ext = if (dot > 0) original.substring(dot) else ""
        var n = 1
        while (n <= 50) {
            val candidate = "$base ($n)$ext"
            if (findExistingFile(context, candidate) == null) return candidate
            n++
        }
        return "${base}_${System.currentTimeMillis() % 10000}$ext"
    }

    private fun findExistingFile(context: Context, fileName: String): Pair<Long, String>? {
        val fromHistory = _records.value.firstOrNull {
            it.fileName.equals(fileName, ignoreCase = true) && it.status == DownloadStatus.COMPLETED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.DISPLAY_NAME
                )
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf(fileName),
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val size = cursor.getLong(1)
                        val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id).toString()
                        return size to uri
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val f = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (f.exists()) return f.length() to Uri.fromFile(f).toString()
        }
        return fromHistory?.let { it.sizeBytes to it.contentUri }
    }

    fun registerDownloading(
        systemDownloadId: Long,
        fileName: String,
        sourceUrl: String,
        mimeType: String
    ): DownloadRecord {
        val rec = DownloadRecord(
            id = "dl_${System.nanoTime()}",
            systemDownloadId = systemDownloadId,
            fileName = fileName,
            sourceUrl = sourceUrl,
            sourceHost = extractHost(sourceUrl),
            mimeType = mimeType.ifBlank { "application/octet-stream" },
            status = DownloadStatus.DOWNLOADING,
            timestampMs = System.currentTimeMillis()
        )
        persistRecords(listOf(rec) + _records.value)
        val activeCnt = _records.value.count { it.status == DownloadStatus.DOWNLOADING }.coerceAtLeast(1)
        _banner.value = DownloadBannerState(
            visible = true,
            isCompleted = false,
            activeCount = activeCnt,
            record = rec
        )
        if (systemDownloadId > 0L) {
            watchSystemDownload(rec.id, systemDownloadId)
        }
        return rec
    }

    fun registerCompletedImmediate(
        fileName: String,
        sourceUrl: String,
        mimeType: String,
        sizeBytes: Long,
        contentUri: String
    ) {
        val rec = DownloadRecord(
            id = "dl_${System.nanoTime()}",
            fileName = fileName,
            sourceUrl = sourceUrl,
            sourceHost = extractHost(sourceUrl),
            mimeType = mimeType.ifBlank { "application/octet-stream" },
            sizeBytes = sizeBytes,
            contentUri = contentUri,
            status = DownloadStatus.COMPLETED,
            timestampMs = System.currentTimeMillis()
        )
        persistRecords(listOf(rec) + _records.value)
        showCompletedBanner(rec)
    }

    private fun watchSystemDownload(recordId: String, systemId: Long) {
        val ctx = appContext ?: return
        scope.launch {
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return@launch
            repeat(180) {
                delay(650L)
                val q = DownloadManager.Query().setFilterById(systemId)
                var done = false
                runCatching {
                    dm.query(q)?.use { c ->
                        if (c.moveToFirst()) {
                            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            val totalBytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val dlBytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val sizeNow = if (totalBytes > 0) totalBytes else dlBytes
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                val uriStr = dm.getUriForDownloadedFile(systemId)?.toString().orEmpty()
                                val updated = _records.value.map { r ->
                                    if (r.id == recordId) r.copy(
                                        status = DownloadStatus.COMPLETED,
                                        sizeBytes = sizeNow,
                                        contentUri = uriStr
                                    ) else r
                                }
                                persistRecords(updated)
                                updated.firstOrNull { it.id == recordId }?.let { showCompletedBanner(it) }
                                done = true
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                val updated = _records.value.map { r ->
                                    if (r.id == recordId) r.copy(status = DownloadStatus.FAILED) else r
                                }
                                persistRecords(updated)
                                _banner.value = DownloadBannerState(visible = false)
                                done = true
                            }
                        }
                    }
                }
                if (done) return@launch
            }
        }
    }

    private fun showCompletedBanner(record: DownloadRecord) {
        _banner.value = DownloadBannerState(
            visible = true,
            isCompleted = true,
            activeCount = 1,
            record = record
        )
        scope.launch {
            delay(5500L)
            if (_banner.value.record?.id == record.id && _banner.value.isCompleted) {
                _banner.value = DownloadBannerState(visible = false)
            }
        }
    }

    fun dismissBanner() {
        _banner.value = DownloadBannerState(visible = false)
    }

    fun deleteRecord(recordId: String) {
        persistRecords(_records.value.filterNot { it.id == recordId })
    }

    fun clearAllRecords() {
        persistRecords(emptyList())
    }

    fun openFile(context: Context, uriString: String, mimeType: String) {
        val uri = uriString.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        if (uri == null) {
            runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                .onFailure { Toast.makeText(context, "파일을 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show() }
        }
    }

    fun shareFile(context: Context, record: DownloadRecord) {
        val uri = record.contentUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) } ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = record.mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "파일 공유")) }
    }
}
