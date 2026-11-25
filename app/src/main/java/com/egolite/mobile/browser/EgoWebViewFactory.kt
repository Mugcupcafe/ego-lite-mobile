package com.egolite.mobile.browser

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Message
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.egolite.mobile.model.LongPressTarget
import java.io.ByteArrayOutputStream

object EgoWebViewFactory {

    private const val DESKTOP_CHROME_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    private const val APPLY_DARK_CSS_JS = """
        (function() {
            var existing = document.getElementById('ego-dark-mode-css');
            if (existing) return;
            var style = document.createElement('style');
            style.id = 'ego-dark-mode-css';
            style.textContent =
                'html { filter: invert(0.92) hue-rotate(180deg) !important; background-color: #121212 !important; } ' +
                'img, video, picture, canvas, iframe, [style*="background-image"] { filter: invert(1) hue-rotate(180deg) !important; }';
            (document.head || document.documentElement).appendChild(style);
        })();
    """

    private const val REMOVE_DARK_CSS_JS = """
        (function() {
            var existing = document.getElementById('ego-dark-mode-css');
            if (existing) existing.remove();
        })();
    """

    private const val MOBILE_CHROME_UA =
        "Mozilla/5.0 (Linux; Android 16; YLP-W00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        context: Context,
        tabId: String,
        adBlockProvider: () -> Boolean,
        darkModeProvider: () -> Boolean,
        desktopModeProvider: () -> Boolean,
        textZoomProvider: () -> Int,
        onPageStarted: (url: String) -> Unit,
        onPageFinished: (url: String, title: String, canBack: Boolean, canForward: Boolean) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onCreatePopup: (Message) -> Unit
    ): WebView {
        val webView = WebView(context)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            setGeolocationEnabled(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowFileAccess = false
            allowContentAccess = true
            textZoom = textZoomProvider()
            userAgentString = if (desktopModeProvider()) DESKTOP_CHROME_UA else MOBILE_CHROME_UA
        }

        // 파일 다운로드 (PDF, 이미지, APK 등)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            BrowserActivityBridge.enqueueDownload(context, url, userAgent, contentDisposition, mimeType)
        }

        // 링크 · 이미지 길게 누르기 컨텍스트 메뉴
        webView.setOnLongClickListener { view ->
            val wv = view as? WebView ?: return@setOnLongClickListener false
            val hit = wv.hitTestResult
            when (hit.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    BrowserActivityBridge.showLongPressMenu(LongPressTarget(linkUrl = hit.extra))
                    true
                }
                WebView.HitTestResult.IMAGE_TYPE -> {
                    BrowserActivityBridge.showLongPressMenu(LongPressTarget(imageUrl = hit.extra))
                    true
                }
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    val msg = wv.handler?.obtainMessage()
                    if (msg != null) {
                        wv.requestFocusNodeHref(msg)
                        val link = msg.data?.getString("url")
                        BrowserActivityBridge.showLongPressMenu(LongPressTarget(linkUrl = link ?: hit.extra, imageUrl = hit.extra))
                    } else {
                        BrowserActivityBridge.showLongPressMenu(LongPressTarget(linkUrl = hit.extra, imageUrl = hit.extra))
                    }
                    true
                }
                else -> false
            }
        }

        var currentPageHost = ""

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString()
                if (request?.isForMainFrame == true) {
                    currentPageHost = AdBlockEngine.extractHost(reqUrl)
                    return super.shouldInterceptRequest(view, request)
                }
                if (adBlockProvider() && AdBlockEngine.shouldBlock(reqUrl, currentPageHost, tabId)) {
                    return AdBlockEngine.emptyResponse()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:")) {
                    currentPageHost = AdBlockEngine.extractHost(url)
                    return false
                }
                return handleExternalScheme(context, url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!url.isNullOrBlank()) {
                    currentPageHost = AdBlockEngine.extractHost(url)
                    onPageStarted(url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null) {
                    val finalUrl = url ?: view.url.orEmpty()
                    currentPageHost = AdBlockEngine.extractHost(finalUrl)
                    runCatching {
                        if (adBlockProvider()) {
                            val js = AdBlockEngine.buildCosmeticFilterJs(currentPageHost)
                            if (js.isNotBlank()) view.evaluateJavascript(js, null)
                        }
                        applyDarkMode(view, darkModeProvider())
                    }
                    onPageFinished(
                        finalUrl,
                        view.title?.ifBlank { finalUrl } ?: "새 탭",
                        view.canGoBack(),
                        view.canGoForward()
                    )
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress)
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                if (resultMsg != null) {
                    onCreatePopup(resultMsg)
                    return true
                }
                return false
            }

            // 동영상 전체화면 (YouTube 등)
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    BrowserActivityBridge.enterFullscreen(view, callback)
                }
            }

            override fun onHideCustomView() {
                BrowserActivityBridge.exitFullscreen()
            }

            // 파일 첨부 (<input type="file">)
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                BrowserActivityBridge.filePathCallback?.onReceiveValue(null)
                BrowserActivityBridge.filePathCallback = filePathCallback
                val accept = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }?.toTypedArray()
                val mimeTypes = if (accept.isNullOrEmpty()) arrayOf("*/*") else accept
                val launcher = BrowserActivityBridge.launchFilePicker
                return if (launcher != null) {
                    launcher(mimeTypes)
                    true
                } else {
                    BrowserActivityBridge.filePathCallback?.onReceiveValue(null)
                    BrowserActivityBridge.filePathCallback = null
                    false
                }
            }

            // 웹 위치 권한 허용
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            // 웹 카메라 · 마이크 권한 허용
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        return webView
    }

    fun applyDarkMode(webView: WebView, enabled: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, enabled)
        }
        webView.evaluateJavascript(if (enabled) APPLY_DARK_CSS_JS else REMOVE_DARK_CSS_JS, null)
    }

    fun applyDesktopMode(webView: WebView, desktop: Boolean) {
        webView.settings.userAgentString = if (desktop) DESKTOP_CHROME_UA else MOBILE_CHROME_UA
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
    }

    fun applyTextZoom(webView: WebView, zoom: Int) {
        webView.settings.textZoom = zoom.coerceIn(75, 150)
    }

    fun getCurrentChromiumVersion(context: Context): String {
        val pkg = WebViewCompat.getCurrentWebViewPackage(context)
        return if (pkg != null) "${pkg.packageName} (${pkg.versionName})" else "System WebView"
    }

    fun captureScreenshotBase64(webView: WebView): String {
        val width = webView.width.coerceAtLeast(1080)
        val height = webView.height.coerceAtLeast(720)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 78, out)
        bitmap.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun handleExternalScheme(context: Context, url: String): Boolean {
        return try {
            val intent = if (url.startsWith("intent://")) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                val parsed = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val fallbackUrl = parsed.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrBlank()) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallbackIntent)
                } else {
                    val pkg = parsed.`package`
                    if (!pkg.isNullOrBlank()) {
                        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                        market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(market)
                    }
                }
            } catch (_: Exception) {
                // 무시
            }
            true
        } catch (_: Exception) {
            true
        }
    }
}
