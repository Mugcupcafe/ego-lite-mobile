package com.egolite.mobile.browser

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Message
import android.util.Base64
import android.view.MotionEvent
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
import com.egolite.mobile.model.WebFontMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

object EgoWebViewFactory {

    private fun buildRealMobileChromeUa(context: Context): String {
        val raw = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrDefault("")
        if (raw.isBlank()) return MOBILE_CHROME_UA
        // Strip only "; wv" so Chrome version, Android version & Client Hints match 100% (passes Cloudflare Turnstile)
        return raw.replace("; wv)", ")").replace("Version/4.0 ", "")
    }

    private fun buildRealDesktopChromeUa(context: Context): String {
        val raw = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrDefault("")
        val ver = Regex("""Chrome/([0-9.]+)""").find(raw)?.groupValues?.getOrNull(1) ?: "134.0.0.0"
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$ver Safari/537.36"
    }

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

    private const val GOOGLE_AUTH_UA =
        "Mozilla/5.0 (Android 16; Mobile; rv:136.0) Gecko/136.0 Firefox/136.0"

    private fun isGoogleAuthHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "accounts.google.com" || h.endsWith(".accounts.google.com") || h == "accounts.youtube.com"
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    fun createWebView(
        context: Context,
        tabId: String,
        adBlockProvider: () -> Boolean,
        darkModeProvider: () -> Boolean,
        desktopModeProvider: () -> Boolean,
        textZoomProvider: () -> Int,
        webFontProvider: () -> WebFontMode = { WebFontMode.SITE_DEFAULT },
        onPageStarted: (url: String) -> Unit,
        onPageFinished: (url: String, title: String, canBack: Boolean, canForward: Boolean) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onCreatePopup: (Message) -> Unit
    ): WebView {
        val webView = WebView(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false) }
        }
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
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
            userAgentString = if (desktopModeProvider()) buildRealDesktopChromeUa(context) else buildRealMobileChromeUa(context)
            // Google 로그인 WebView 탐지 차단: X-Requested-With 패키지명 헤더 제거
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                runCatching { WebSettingsCompat.setRequestedWithHeaderOriginAllowList(this, emptySet()) }
            }
        }

        // 파일 다운로드 (PDF, 이미지, APK 등)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            BrowserActivityBridge.enqueueDownload(context, url, userAgent, contentDisposition, mimeType)
        }

        // 롱프레스 팝업의 정확한 터치 위치 계산용 좌표 기록 (WebView 기본 스크롤/클릭 동작은 그대로 통과)
        var lastTouchLocalX = -1f
        var lastTouchLocalY = -1f
        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                lastTouchLocalX = event.x
                lastTouchLocalY = event.y
            }
            false
        }

        // 링크 · 이미지 길게 누르기 컨텍스트 메뉴 (크롬 1:1 — 터치 위치 기준 + 제목/URL/이미지 추출)
        webView.setOnLongClickListener { view ->
            val wv = view as? WebView ?: return@setOnLongClickListener false
            val hit = wv.hitTestResult
            val winLoc = IntArray(2)
            wv.getLocationInWindow(winLoc)
            val tx = if (lastTouchLocalX >= 0f) winLoc[0] + lastTouchLocalX else -1f
            val ty = if (lastTouchLocalY >= 0f) winLoc[1] + lastTouchLocalY else -1f
            when (hit.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    val msg = wv.handler?.obtainMessage()
                    if (msg != null) {
                        wv.requestFocusNodeHref(msg)
                        val linkTitle = msg.data?.getString("title")
                        val linkHref = msg.data?.getString("url") ?: hit.extra
                        BrowserActivityBridge.showLongPressMenu(
                            LongPressTarget(linkUrl = linkHref, linkText = linkTitle, touchX = tx, touchY = ty)
                        )
                    } else {
                        BrowserActivityBridge.showLongPressMenu(
                            LongPressTarget(linkUrl = hit.extra, touchX = tx, touchY = ty)
                        )
                    }
                    true
                }
                WebView.HitTestResult.IMAGE_TYPE -> {
                    BrowserActivityBridge.showLongPressMenu(
                        LongPressTarget(imageUrl = hit.extra, touchX = tx, touchY = ty)
                    )
                    true
                }
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    val msg = wv.handler?.obtainMessage()
                    if (msg != null) {
                        wv.requestFocusNodeHref(msg)
                        val link = msg.data?.getString("url")
                        val title = msg.data?.getString("title")
                        BrowserActivityBridge.showLongPressMenu(
                            LongPressTarget(linkUrl = link ?: hit.extra, linkText = title, imageUrl = hit.extra, touchX = tx, touchY = ty)
                        )
                    } else {
                        BrowserActivityBridge.showLongPressMenu(
                            LongPressTarget(linkUrl = hit.extra, imageUrl = hit.extra, touchX = tx, touchY = ty)
                        )
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
                val reqUrl = request?.url?.toString().orEmpty()
                // 1) Serve local user-picked custom .ttf/.otf font cleanly with CORS
                if (reqUrl.startsWith(CUSTOM_FONT_VIRTUAL_URL)) {
                    val path = SpaceManager.preferences.value.customFontPath
                    val file = if (path.isNotBlank()) File(path) else null
                    if (file != null && file.exists()) {
                        val mime = if (file.name.endsWith(".otf", true)) "font/otf" else "font/ttf"
                        return WebResourceResponse(
                            mime,
                            null,
                            200,
                            "OK",
                            mapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "no-cache"),
                            FileInputStream(file)
                        )
                    }
                }
                // 2) Block remote site webfonts when SYSTEM_FONT or CUSTOM_TTF is active (keep icon fonts intact)
                val fontMode = SpaceManager.preferences.value.webFontMode
                if (fontMode == WebFontMode.SYSTEM_FONT || fontMode == WebFontMode.CUSTOM_TTF) {
                    val lower = reqUrl.lowercase().substringBefore("?")
                    val isFontFile = lower.endsWith(".woff2") || lower.endsWith(".woff") || lower.endsWith(".ttf") || lower.endsWith(".otf")
                    val isIconFont = lower.contains("icon") || lower.contains("fontawesome") || lower.contains("fa-") || lower.contains("material") || lower.contains("symbol") || lower.contains("glyph")
                    if (isFontFile && !isIconFont) {
                        return WebResourceResponse("text/plain", "utf-8", 204, "No Content", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                    }
                }
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
                    val host = AdBlockEngine.extractHost(url)
                    currentPageHost = host
                    if (view != null && request.isForMainFrame) {
                        val desiredUa = if (isGoogleAuthHost(host)) {
                            GOOGLE_AUTH_UA
                        } else if (desktopModeProvider()) {
                            buildRealDesktopChromeUa(context)
                        } else {
                            buildRealMobileChromeUa(context)
                        }
                        if (view.settings.userAgentString != desiredUa) {
                            view.settings.userAgentString = desiredUa
                            view.loadUrl(url)
                            return true
                        }
                    }
                    return false
                }
                return handleExternalScheme(context, url)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (view != null) {
                    onPageFinished(
                        url ?: view.url.orEmpty(),
                        view.title?.ifBlank { url.orEmpty() } ?: "새 탭",
                        view.canGoBack(),
                        view.canGoForward()
                    )
                }
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
                        applyWebFont(view, webFontProvider())
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
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                FaviconCache.putIcon(context, view?.url, icon)
            }

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
                callback?.invoke(origin, SpaceManager.preferences.value.allowLocation, false)
            }

            // 웹 카메라 · 마이크 권한 허용
            override fun onPermissionRequest(request: PermissionRequest?) {
                val prefs = SpaceManager.preferences.value
                val allowed = request?.resources?.filter { res ->
                    when (res) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> prefs.allowCamera
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> prefs.allowMic
                        else -> true
                    }
                }?.toTypedArray() ?: emptyArray()
                if (allowed.isNotEmpty()) request?.grant(allowed) else request?.deny()
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
        webView.settings.userAgentString = if (desktop) buildRealDesktopChromeUa(webView.context) else buildRealMobileChromeUa(webView.context)
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
    }

    fun applyTextZoom(webView: WebView, zoom: Int) {
        webView.settings.textZoom = zoom.coerceIn(75, 150)
    }

    const val CUSTOM_FONT_VIRTUAL_URL = "https://ego-local-font.internal/custom.ttf"

    fun applyWebFont(webView: WebView, mode: WebFontMode, customFontPath: String = SpaceManager.preferences.value.customFontPath) {
        if (mode == WebFontMode.SITE_DEFAULT) {
            val removeJs = """
                (function() {
                    var el = document.getElementById('ego-web-font-css');
                    if (el) el.remove();
                })();
            """.trimIndent()
            webView.evaluateJavascript(removeJs, null)
            return
        }

        val hasCustomFile = mode == WebFontMode.CUSTOM_TTF && customFontPath.isNotBlank() && File(customFontPath).exists()
        val fontFaceRule = if (hasCustomFile) {
            val ver = File(customFontPath).lastModified()
            "@font-face { font-family: \"EgoCustomFont\"; src: url(\"$CUSTOM_FONT_VIRTUAL_URL?v=$ver\") format(\"truetype\"); font-weight: 100 900; font-display: swap; } "
        } else ""

        val cssFamily = when {
            hasCustomFile -> "\"EgoCustomFont\", sans-serif"
            mode == WebFontMode.SYSTEM_FONT -> "sans-serif"
            mode == WebFontMode.CLEAN_GOTHIC -> "\"Noto Sans KR\", \"Malgun Gothic\", sans-serif"
            mode == WebFontMode.SERIF -> "\"Noto Serif KR\", Georgia, serif"
            else -> "sans-serif"
        }

        val selector = "html, body, p, span, div, a, li, td, th, h1, h2, h3, h4, h5, h6, input, button, textarea, article, section"
        val excludeIcons = ":not([class*=\"icon\"]):not([class*=\"Icon\"]):not([class*=\"fa-\"]):not(.material-icons):not(.material-symbols-outlined)"
        val fullCss = fontFaceRule + selector.split(", ").joinToString(", ") { it + excludeIcons } + " { font-family: $cssFamily !important; }"

        val injectJs = """
            (function() {
                var el = document.getElementById('ego-web-font-css');
                if (!el) {
                    el = document.createElement('style');
                    el.id = 'ego-web-font-css';
                    (document.head || document.documentElement).appendChild(el);
                }
                el.textContent = "$fullCss";
            })();
        """.trimIndent()
        webView.evaluateJavascript(injectJs, null)
    }

    fun applySitePreferences(webView: WebView, prefs: com.egolite.mobile.model.BrowserPreferences) {
        webView.settings.javaScriptEnabled = prefs.allowJavaScript
        webView.settings.javaScriptCanOpenWindowsAutomatically = prefs.allowPopups
        webView.settings.setSupportMultipleWindows(prefs.allowPopups)
        webView.settings.builtInZoomControls = true
        webView.settings.setSupportZoom(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, prefs.allowThirdPartyCookies)
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

    fun captureThumbnailBitmap(webView: WebView): Bitmap? {
        if (webView.width <= 0 || webView.height <= 0) return null
        return runCatching {
            val scale = 0.25f
            val tw = (webView.width * scale).toInt().coerceIn(160, 420)
            val th = (webView.height * scale).toInt().coerceIn(160, 520)
            val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.RGB_565)
            val canvas = Canvas(bmp)
            canvas.scale(tw.toFloat() / webView.width.toFloat(), th.toFloat() / webView.height.toFloat())
            webView.draw(canvas)
            bmp
        }.getOrNull()
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
