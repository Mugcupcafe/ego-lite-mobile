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
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.ByteArrayOutputStream

object EgoWebViewFactory {

    private const val DESKTOP_CHROME_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    private const val MOBILE_CHROME_UA =
        "Mozilla/5.0 (Linux; Android 16; YLP-W00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        context: Context,
        adBlockProvider: () -> Boolean,
        darkModeProvider: () -> Boolean,
        desktopModeProvider: () -> Boolean,
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
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowFileAccess = false
            allowContentAccess = true
            userAgentString = if (desktopModeProvider()) DESKTOP_CHROME_UA else MOBILE_CHROME_UA
        }

        applyDarkMode(webView, darkModeProvider())

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (adBlockProvider() && AdBlockEngine.shouldBlock(request?.url?.toString())) {
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
                    return false
                }
                // 한국 쇼핑몰·간편인증(PASS, 카카오, 토스, 카드사) 커스텀 스킴 및 intent:// 처리
                return handleExternalScheme(context, url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!url.isNullOrBlank()) {
                    onPageStarted(url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null) {
                    if (adBlockProvider()) {
                        view.evaluateJavascript(AdBlockEngine.COSMETIC_FILTER_JS, null)
                    }
                    onPageFinished(
                        url ?: view.url.orEmpty(),
                        view.title?.ifBlank { url.orEmpty() } ?: "새 탭",
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
        }

        return webView
    }

    fun applyDarkMode(webView: WebView, enabled: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, enabled)
        }
    }

    fun applyDesktopMode(webView: WebView, desktop: Boolean) {
        webView.settings.userAgentString = if (desktop) DESKTOP_CHROME_UA else MOBILE_CHROME_UA
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
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
