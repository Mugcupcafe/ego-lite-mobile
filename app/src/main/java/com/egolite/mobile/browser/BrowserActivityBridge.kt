package com.egolite.mobile.browser

import android.content.ContentValues
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import java.io.File
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.LongPressTarget
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class FullscreenVideoState(
    val view: View? = null,
    val callback: WebChromeClient.CustomViewCallback? = null
)

object BrowserActivityBridge {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        engine { requestTimeout = 5_000 }
    }

    var filePathCallback: ValueCallback<Array<Uri>>? = null
    var launchFilePicker: ((Array<String>) -> Unit)? = null
    var launchBookmarkImport: (() -> Unit)? = null
    var launchBookmarkExport: (() -> Unit)? = null
    var launchFontPicker: (() -> Unit)? = null

    private val _fullscreenVideo = MutableStateFlow(FullscreenVideoState())
    val fullscreenVideo: StateFlow<FullscreenVideoState> = _fullscreenVideo.asStateFlow()

    private val _isInPip = MutableStateFlow(false)
    val isInPip: StateFlow<Boolean> = _isInPip.asStateFlow()

    fun setInPictureInPictureMode(inPip: Boolean) {
        _isInPip.value = inPip
    }

    private val _longPressTarget = MutableStateFlow<LongPressTarget?>(null)
    val longPressTarget: StateFlow<LongPressTarget?> = _longPressTarget.asStateFlow()

    private val _externalOpenSeq = MutableStateFlow(0L)
    val externalOpenSeq: StateFlow<Long> = _externalOpenSeq.asStateFlow()

    fun notifyExternalUrlOpened() {
        _externalOpenSeq.value = _externalOpenSeq.value + 1L
    }

    fun enterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        _fullscreenVideo.value = FullscreenVideoState(view, callback)
    }

    fun exitFullscreen() {
        _fullscreenVideo.value.callback?.onCustomViewHidden()
        _fullscreenVideo.value = FullscreenVideoState()
    }

    fun showLongPressMenu(target: LongPressTarget) {
        _longPressTarget.value = target
    }

    fun dismissLongPressMenu() {
        _longPressTarget.value = null
    }

    fun enqueueDownload(
        context: Context,
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null
    ) {
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url))
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            request.setMimeType(mimeType)
            CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            request.setTitle(fileName)
            request.setDescription("Ego Lite Mobile 다운로드")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "다운로드 시작: $fileName", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "다운로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * blob: / data: URI(Codemagic, GitHub, ChatGPT 등)의 Base64 데이터를 Downloads 폴더에 직접 저장
     */
    fun saveBase64Download(
        context: Context,
        dataUrl: String,
        suggestedName: String,
        mimeType: String? = null
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        runCatching {
            val commaIdx = dataUrl.indexOf(',')
            val header = if (commaIdx > 0) dataUrl.substring(0, commaIdx) else ""
            val base64Body = if (commaIdx > 0) dataUrl.substring(commaIdx + 1) else dataUrl
            val detectedMime = mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
                ?: Regex("data:([^;]+);").find(header)?.groupValues?.getOrNull(1)
                ?: "application/octet-stream"
            val bytes = Base64.decode(base64Body, Base64.DEFAULT)
            val safeName = suggestedName
                .ifBlank { "download_${System.currentTimeMillis()}" }
                .replace(Regex("""[\\/:*?"<>|]"""), "_")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, detectedMime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("MediaStore 파일 생성 실패")
                resolver.openOutputStream(uri)?.use { out -> out.write(bytes) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                File(dir, safeName).writeBytes(bytes)
            }
            mainHandler.post {
                Toast.makeText(context, "다운로드 완료: $safeName", Toast.LENGTH_SHORT).show()
            }
        }.onFailure { err ->
            mainHandler.post {
                Toast.makeText(context, "Blob 다운로드 실패: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun printWebViewToPdf(context: Context, webView: WebView, title: String) {
        runCatching {
            val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = title.ifBlank { "EgoLitePage" }.take(40)
            val adapter = webView.createPrintDocumentAdapter(jobName)
            pm.print(jobName, adapter, PrintAttributes.Builder().build())
        }.onFailure {
            Toast.makeText(context, "인쇄/PDF 시작 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun fetchSearchSuggestions(query: String): List<String> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        return runCatching {
            val encoded = Uri.encode(q)
            val body = httpClient.get("https://suggestqueries.google.com/complete/search?client=firefox&hl=ko&q=$encoded").bodyAsText()
            val arr = json.parseToJsonElement(body).jsonArray
            arr[1].jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }.take(6)
        }.getOrDefault(emptyList())
    }

    fun parseNetscapeBookmarksHtml(html: String): List<BookmarkItem> {
        val regex = Regex("""<A\s+[^>]*HREF="([^"]+)"[^>]*>(.*?)</A>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return regex.findAll(html).mapNotNull { m ->
            val url = m.groupValues[1].trim()
            val title = m.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                BookmarkItem(title = title.ifBlank { url }.take(40), url = url, folder = "가져온 북마크")
            } else null
        }.toList()
    }

    fun exportNetscapeBookmarksHtml(bookmarks: List<BookmarkItem>): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        sb.appendLine("<TITLE>Ego Lite Mobile Bookmarks</TITLE>")
        sb.appendLine("<H1>Bookmarks</H1>")
        sb.appendLine("<DL><p>")
        bookmarks.groupBy { it.folder }.forEach { (folder, items) ->
            sb.appendLine("  <DT><H3>$folder</H3>")
            sb.appendLine("  <DL><p>")
            items.forEach { bm ->
                val safeTitle = bm.title.replace("<", "&lt;").replace(">", "&gt;")
                val safeUrl = bm.url.replace("\"", "&quot;")
                sb.appendLine("    <DT><A HREF=\"$safeUrl\">$safeTitle</A>")
            }
            sb.appendLine("  </DL><p>")
        }
        sb.appendLine("</DL><p>")
        return sb.toString()
    }
}
