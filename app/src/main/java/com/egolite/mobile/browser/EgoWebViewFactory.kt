package com.egolite.mobile.browser

import android.webkit.URLUtil
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
import android.webkit.WebResourceError
import android.webkit.RenderProcessGoneDetail
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import java.util.concurrent.ConcurrentHashMap
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.egolite.mobile.model.LongPressTarget
import com.egolite.mobile.model.WebFontMode
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

object EgoWebViewFactory {

    private val innerScrolledTabs = ConcurrentHashMap<String, Boolean>()

    fun isInnerScrolled(tabId: String): Boolean = innerScrolledTabs[tabId] == true

    fun setInnerScrolled(tabId: String, scrolled: Boolean) {
        innerScrolledTabs[tabId] = scrolled
    }

    private const val INNER_SCROLL_GUARD_JS = """
        (function() {
            if (window.__egoScrollGuardInstalled) return;
            window.__egoScrollGuardInstalled = true;
            var lastScrollCheckMs = 0;
            var lastScrolledSent = null;
            function checkInnerScroll(target, force) {
                var now = Date.now();
                if (!force && now - lastScrollCheckMs < 100) return;
                lastScrollCheckMs = now;
                var scrolled = (window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0) > 2;
                var el = target && target.nodeType === 1 ? target : null;
                while (!scrolled && el && el !== document && el !== document.documentElement) {
                    if (el.scrollTop > 2 && el.scrollHeight > el.clientHeight + 2) {
                        scrolled = true;
                        break;
                    }
                    el = el.parentElement;
                }
                if (scrolled === lastScrolledSent) return;
                lastScrolledSent = scrolled;
                try {
                    if (window.EgoScrollBridge && window.EgoScrollBridge.setInnerScrolled) {
                        window.EgoScrollBridge.setInnerScrolled(scrolled);
                    }
                } catch (e) {}
            }
            document.addEventListener('touchstart', function(e) { checkInnerScroll(e.target, true); }, { passive: true, capture: true });
            document.addEventListener('scroll', function(e) { checkInnerScroll(e.target, false); }, { passive: true, capture: true });
            function checkMedia() {
                try {
                    var list = document.querySelectorAll('video, audio');
                    var playing = false;
                    for (var i = 0; i < list.length; i++) {
                        var m = list[i];
                        if (window.__egoTabMuted === true && !m.muted) { m.muted = true; }
                        if (!m.paused && !m.ended && m.readyState > 1) { playing = true; }
                    }
                    if (window.EgoScrollBridge && window.EgoScrollBridge.setMediaPlaying) {
                        window.EgoScrollBridge.setMediaPlaying(playing);
                    }
                } catch (e) {}
            }
            document.addEventListener('play', checkMedia, true);
            document.addEventListener('playing', checkMedia, true);
            document.addEventListener('pause', checkMedia, true);
            document.addEventListener('ended', checkMedia, true);
            document.addEventListener('volumechange', checkMedia, true);
            document.addEventListener('visibilitychange', checkMedia, false);
            setTimeout(checkMedia, 1200);
        })();
    """

    private fun buildRealMobileChromeUa(context: Context): String {
        val raw = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrDefault("")
        if (raw.isBlank()) return MOBILE_CHROME_UA
        // Strip only "; wv" so Chrome version, Android version & Client Hints match 100% (passes Cloudflare Turnstile)
        return raw.replace("; wv)", ")").replace("Version/4.0 ", "")
    }

    private fun buildRealDesktopChromeUa(context: Context): String {
        val raw = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrDefault("")
        val ver = Regex("""Chrome/([0-9.]+)""").find(raw)?.groupValues?.getOrNull(1) ?: "135.0.0.0"
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$ver Safari/537.36"
    }

    /**
     * Cloudflare Turnstile 통과 핵심: User-Agent와 Sec-CH-UA Client Hints(Platform/Mobile/Brand)를 100% 일치시킴.
     * 데스크톱 UA(X11; Linux x86_64)인데 Sec-CH-UA-Platform이 Android로 나가면 봇으로 즉시 차단됨.
     */
    private fun syncUserAgentAndClientHints(webView: WebView, desktop: Boolean) {
        val ctx = webView.context
        val raw = runCatching { WebSettings.getDefaultUserAgent(ctx) }.getOrDefault("")
        val fullVer = Regex("""Chrome/([0-9.]+)""").find(raw)?.groupValues?.getOrNull(1) ?: "135.0.0.0"
        val majorVer = fullVer.substringBefore(".")
        webView.settings.userAgentString = if (desktop) buildRealDesktopChromeUa(ctx) else buildRealMobileChromeUa(ctx)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            runCatching {
                val brands = listOf(
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Chromium").setMajorVersion(majorVer).setFullVersion(fullVer).build(),
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Google Chrome").setMajorVersion(majorVer).setFullVersion(fullVer).build(),
                    UserAgentMetadata.BrandVersion.Builder()
                        .setBrand("Not-A.Brand").setMajorVersion("99").setFullVersion("99.0.0.0").build()
                )
                val meta = if (desktop) {
                    UserAgentMetadata.Builder()
                        .setBrandVersionList(brands)
                        .setFullVersion(fullVer)
                        .setPlatform("Linux")
                        .setPlatformVersion("6.6.0")
                        .setArchitecture("x86")
                        .setBitness(64)
                        .setMobile(false)
                        .setModel("")
                        .build()
                } else {
                    UserAgentMetadata.Builder()
                        .setBrandVersionList(brands)
                        .setFullVersion(fullVer)
                        .setPlatform("Android")
                        .setPlatformVersion(Build.VERSION.RELEASE ?: "16")
                        .setArchitecture("")
                        .setBitness(64)
                        .setMobile(true)
                        .setModel(Build.MODEL ?: "")
                        .build()
                }
                WebSettingsCompat.setUserAgentMetadata(webView.settings, meta)
            }
        }
    }

    private fun isCloudflareChallengeUrl(url: String): Boolean {
        val low = url.lowercase()
        return low.contains("challenges.cloudflare.com") || low.contains("__cf_chl") || low.contains("/cdn-cgi/challenge")
    }

    private const val DESKTOP_CHROME_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    private const val APPLY_DARK_CSS_JS = """
        (function() {
            if (document.querySelector('iframe[src*="challenges.cloudflare.com"], .cf-turnstile, #challenge-running, #cf-wrapper')) return;
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
        onCreatePopup: (Message) -> Unit,
        onCloseWindow: () -> Unit = {},
        onRenderProcessCrashed: () -> Unit = {}
    ): WebView {
        val webView = WebView(context)
        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun setInnerScrolled(scrolled: Boolean) {
                    innerScrolledTabs[tabId] = scrolled
                }

                @JavascriptInterface
                fun setMediaPlaying(playing: Boolean) {
                    SpaceManager.setTabAudioPlaying(tabId, playing)
                }
            },
            "EgoScrollBridge"
        )
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
            // UA와 Client Hints는 아래 syncUserAgentAndClientHints에서 일괄 동기화
            // Google 로그인 WebView 탐지 차단: X-Requested-With 패키지명 헤더 제거
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                runCatching { WebSettingsCompat.setRequestedWithHeaderOriginAllowList(this, emptySet()) }
            }
        }

        syncUserAgentAndClientHints(webView, desktopModeProvider())

        // blob: / data: 다운로드 전용 JS 브리지 등록
        webView.addJavascriptInterface(
            object {
                @android.webkit.JavascriptInterface
                fun onBlobDownloaded(dataUrl: String, fileName: String, mimeType: String) {
                    BrowserActivityBridge.saveBase64Download(context, dataUrl, fileName, mimeType)
                }
            },
            "__egoBlobDownload"
        )

        // 파일 다운로드 (HTTP/HTTPS + blob:/data: URI 모두 지원)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            when {
                url.startsWith("data:", ignoreCase = true) -> {
                    BrowserActivityBridge.saveBase64Download(context, url, guessedName, mimeType)
                }
                url.startsWith("blob:", ignoreCase = true) -> {
                    val safeUrlJs = JSONObject.quote(url)
                    val safeNameJs = JSONObject.quote(guessedName)
                    val safeMimeJs = JSONObject.quote(mimeType ?: "application/octet-stream")
                    val js = """
                        (function() {
                            fetch($safeUrlJs)
                                .then(function(r) { return r.blob(); })
                                .then(function(b) {
                                    var reader = new FileReader();
                                    reader.onloadend = function() {
                                        if (window.__egoBlobDownload && reader.result) {
                                            window.__egoBlobDownload.onBlobDownloaded(String(reader.result), $safeNameJs, b.type || $safeMimeJs);
                                        }
                                    };
                                    reader.readAsDataURL(b);
                                })
                                .catch(function(e) { console.error('ego blob dl error', e); });
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(js, null)
                }
                else -> {
                    BrowserActivityBridge.enqueueDownload(context, url, userAgent, contentDisposition, mimeType)
                }
            }
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
                    val isGet = request.method.isNullOrBlank() || request.method.equals("GET", ignoreCase = true)
                    if (view != null && request.isForMainFrame && isGet) {
                        if (isGoogleAuthHost(host)) {
                            if (view.settings.userAgentString != GOOGLE_AUTH_UA) {
                                view.settings.userAgentString = GOOGLE_AUTH_UA
                                view.loadUrl(url)
                                return true
                            }
                        } else {
                            val expectedUa = if (desktopModeProvider()) buildRealDesktopChromeUa(context) else buildRealMobileChromeUa(context)
                            if (view.settings.userAgentString != expectedUa) {
                                syncUserAgentAndClientHints(view, desktopModeProvider())
                                view.loadUrl(url)
                                return true
                            }
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
                innerScrolledTabs[tabId] = false
                SpaceManager.setTabAudioPlaying(tabId, false)
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
                        CookieManager.getInstance().flush()
                        if (!isCloudflareChallengeUrl(finalUrl)) {
                            if (adBlockProvider()) {
                                val js = AdBlockEngine.buildCosmeticFilterJs(currentPageHost)
                                if (js.isNotBlank()) {
                                    val guardedJs = "if (!document.querySelector('iframe[src*=\"challenges.cloudflare.com\"], .cf-turnstile, #challenge-running')) { $js }"
                                    view.evaluateJavascript(guardedJs, null)
                                }
                            }
                            applyDarkMode(view, darkModeProvider())
                            applyWebFont(view, webFontProvider())
                            view.evaluateJavascript(INNER_SCROLL_GUARD_JS, null)
                        }
                    }
                    onPageFinished(
                        finalUrl,
                        view.title?.ifBlank { finalUrl } ?: "새 탭",
                        view.canGoBack(),
                        view.canGoForward()
                    )
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (view == null || request?.isForMainFrame != true) return
                val code = error?.errorCode ?: return
                // 사용자 취소(ERR_ABORTED = -1)나 외부 앱 스킴(-10)은 정상 전환이므로 무시
                if (code == WebViewClient.ERROR_UNSUPPORTED_SCHEME || code == -1) return
                val failedUrl = request.url?.toString().orEmpty()
                if (failedUrl.isBlank() || failedUrl.startsWith("about:")) return
                val desc = error.description?.toString().orEmpty().ifBlank { "네트워크 연결을 확인해 주세요" }
                val isDark = darkModeProvider()
                val bg = if (isDark) "#0F172A" else "#F8FAFC"
                val card = if (isDark) "#1E293B" else "#FFFFFF"
                val fg = if (isDark) "#F1F5F9" else "#0F172A"
                val sub = if (isDark) "#94A3B8" else "#64748B"
                val safeUrl = failedUrl.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;").replace("'", "&#39;")
                val safeDesc = desc.replace("&", "&amp;").replace("<", "&lt;")
                val html = """
                    <!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                    <style>
                    body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:$bg;color:$fg;font-family:sans-serif;padding:24px;box-sizing:border-box;}
                    .c{max-width:420px;width:100%;background:$card;border-radius:18px;padding:24px;box-shadow:0 8px 28px rgba(0,0,0,.15);}
                    h2{margin:0 0 8px;font-size:18px;}
                    p{margin:0 0 14px;font-size:13px;color:$sub;word-break:break-all;line-height:1.5;}
                    button{background:#10B981;color:#fff;border:0;border-radius:999px;padding:10px 20px;font-size:14px;font-weight:600;cursor:pointer;}
                    </style></head><body><div class="c">
                    <h2>페이지를 열 수 없습니다</h2>
                    <p>$safeUrl<br/><br/>원인: $safeDesc</p>
                    <button onclick="location.href='$safeUrl'">다시 시도</button>
                    </div></body></html>
                """.trimIndent()
                view.loadDataWithBaseURL(failedUrl, html, "text/html", "UTF-8", failedUrl)
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                // 렌더러 프로세스 비정상 종료 시 앱 전체 강제종료 방지 및 탭 자동 복구
                if (view != null) {
                    (view.parent as? ViewGroup)?.removeView(view)
                }
                onRenderProcessCrashed()
                return true
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

            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                onCloseWindow()
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
            // 크로미움 네이티브 Algorithmic Darkening 단독 사용 (CSS invert 중첩으로 인한 이중 반전·회색 뭉개짐 방지)
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, enabled)
            webView.evaluateJavascript(REMOVE_DARK_CSS_JS, null)
        } else {
            webView.evaluateJavascript(if (enabled) APPLY_DARK_CSS_JS else REMOVE_DARK_CSS_JS, null)
        }
    }

    fun applyDesktopMode(webView: WebView, desktop: Boolean) {
        syncUserAgentAndClientHints(webView, desktop)
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
    }

    fun applyTextZoom(webView: WebView, zoom: Int) {
        webView.settings.textZoom = zoom.coerceIn(75, 150)
    }

    const val CUSTOM_FONT_VIRTUAL_URL = "https://ego-local-font.internal/custom.ttf"

    private var cachedCustomFontKey: String = ""
    private var cachedCustomFontBase64: String = ""

    private fun loadCustomFontBase64(path: String): Pair<Long, String>? {
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        val ver = file.lastModified()
        val key = "$path:$ver"
        if (cachedCustomFontKey == key && cachedCustomFontBase64.isNotBlank()) {
            return ver to cachedCustomFontBase64
        }
        return runCatching {
            val bytes = file.readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            cachedCustomFontKey = key
            cachedCustomFontBase64 = b64
            ver to b64
        }.getOrNull()
    }

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

        val customFile = if (mode == WebFontMode.CUSTOM_TTF && customFontPath.isNotBlank()) File(customFontPath) else null
        val hasCustomFile = customFile != null && customFile.exists() && customFile.isFile
        val isLargeCustomFont = hasCustomFile && (customFile!!.length() > 3L * 1024L * 1024L)
        val customData = if (hasCustomFile && !isLargeCustomFont) {
            loadCustomFontBase64(customFontPath)
        } else null

        val cssFamily = when {
            hasCustomFile -> "'EgoCustomFont', system-ui, sans-serif"
            mode == WebFontMode.SYSTEM_FONT -> "system-ui, -apple-system, sans-serif"
            mode == WebFontMode.CLEAN_GOTHIC -> "'Noto Sans KR', 'Malgun Gothic', sans-serif"
            mode == WebFontMode.SERIF -> "'Noto Serif KR', Georgia, serif"
            else -> "system-ui, sans-serif"
        }

        val selector = "html, body, p, span, div, a, li, td, th, h1, h2, h3, h4, h5, h6, input, button, textarea, article, section, strong, b, em, label, dd, dt"
        val excludeIcons = ":not([class*='icon']):not([class*='Icon']):not([class*='fa-']):not(.material-icons):not(.material-symbols-outlined)"
        val fullCss = selector.split(", ").joinToString(", ") { it + excludeIcons } + " { font-family: $cssFamily !important; }"
        val quotedCss = JSONObject.quote(fullCss)

        val fontFaceLoaderJs = when {
            customData != null -> {
                val (ver, b64) = customData
                """
                    try {
                        if (!window.__egoCustomFontLoaded || window.__egoCustomFontVer !== $ver) {
                            var b64 = "$b64";
                            var bin = atob(b64);
                            var bytes = new Uint8Array(bin.length);
                            for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
                            var ff = new FontFace('EgoCustomFont', bytes.buffer, { weight: '100 900' });
                            ff.load().then(function(loaded) {
                                document.fonts.add(loaded);
                                window.__egoCustomFontLoaded = true;
                                window.__egoCustomFontVer = $ver;
                            }).catch(function() {});
                        }
                    } catch (e) {}
                """.trimIndent()
            }
            hasCustomFile && isLargeCustomFont -> {
                val ver = customFile!!.lastModified()
                """
                    try {
                        if (!window.__egoCustomFontLoaded || window.__egoCustomFontVer !== $ver) {
                            fetch('$CUSTOM_FONT_VIRTUAL_URL?v=$ver')
                                .then(function(r) { return r.arrayBuffer(); })
                                .then(function(buf) { return new FontFace('EgoCustomFont', buf, { weight: '100 900' }).load(); })
                                .then(function(loaded) {
                                    document.fonts.add(loaded);
                                    window.__egoCustomFontLoaded = true;
                                    window.__egoCustomFontVer = $ver;
                                }).catch(function() {});
                        }
                    } catch (e) {}
                """.trimIndent()
            }
            else -> ""
        }

        val injectJs = """
            (function() {
                if (document.querySelector('iframe[src*="challenges.cloudflare.com"], .cf-turnstile, #challenge-running, #cf-wrapper')) return;
                $fontFaceLoaderJs
                var el = document.getElementById('ego-web-font-css');
                if (!el) {
                    el = document.createElement('style');
                    el.id = 'ego-web-font-css';
                    (document.head || document.documentElement).appendChild(el);
                }
                el.textContent = $quotedCss;
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
            canvas.save()
            canvas.scale(tw.toFloat() / webView.width.toFloat(), th.toFloat() / webView.height.toFloat())
            // 스크롤된 페이지(scrollY > 0)도 캔버스 원점(0,0)에 정확히 맞춰 캡처되도록 오프셋 보정
            canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())
            webView.draw(canvas)
            canvas.restore()
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
