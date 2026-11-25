package com.egolite.mobile.browser

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream

@Serializable
private data class PersistedAdBlockConfig(
    val globalEnabled: Boolean = true,
    val cosmeticEnabled: Boolean = true,
    val whitelistedHosts: Set<String> = emptySet(),
    val customBlockedHosts: Set<String> = emptySet(),
    val customCssSelectors: Set<String> = emptySet(),
    val totalBlockedCount: Int = 0
)

data class AdBlockUiState(
    val globalEnabled: Boolean = true,
    val cosmeticEnabled: Boolean = true,
    val whitelistedHosts: Set<String> = emptySet(),
    val customBlockedHosts: Set<String> = emptySet(),
    val customCssSelectors: Set<String> = emptySet(),
    val perTabCounts: Map<String, Int> = emptyMap(),
    val totalBlockedCount: Int = 0,
    val recentBlockedHosts: List<String> = emptyList()
)

object AdBlockEngine {
    private val jsonCodec = Json { ignoreUnknownKeys = true }
    private lateinit var appContext: Context

    private val _state = MutableStateFlow(AdBlockUiState())
    val state: StateFlow<AdBlockUiState> = _state.asStateFlow()

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            loadConfig()
        }
    }

    private fun loadConfig() {
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.getString("adblock_config_json", null)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<PersistedAdBlockConfig>(raw) }.getOrNull()?.let { cfg ->
                _state.update {
                    it.copy(
                        globalEnabled = cfg.globalEnabled,
                        cosmeticEnabled = cfg.cosmeticEnabled,
                        whitelistedHosts = cfg.whitelistedHosts,
                        customBlockedHosts = cfg.customBlockedHosts,
                        customCssSelectors = cfg.customCssSelectors,
                        totalBlockedCount = cfg.totalBlockedCount
                    )
                }
            }
        }
    }

    private fun saveConfig() {
        if (!::appContext.isInitialized) return
        val st = _state.value
        val cfg = PersistedAdBlockConfig(
            globalEnabled = st.globalEnabled,
            cosmeticEnabled = st.cosmeticEnabled,
            whitelistedHosts = st.whitelistedHosts,
            customBlockedHosts = st.customBlockedHosts,
            customCssSelectors = st.customCssSelectors,
            totalBlockedCount = st.totalBlockedCount
        )
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("adblock_config_json", jsonCodec.encodeToString(cfg)).apply()
    }

    private val defaultBlockedHosts = setOf(
        // Global ad networks & trackers
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com", "tpc.googlesyndication.com",
        "analytics.google.com", "www.google-analytics.com", "googletagmanager.com",
        "connect.facebook.net", "ads.twitter.com", "static.ads-twitter.com",
        "criteo.com", "criteo.net", "taboola.com", "outbrain.com",
        "adnxs.com", "rubiconproject.com", "pubmatic.com", "openx.net",
        "casalemedia.com", "amazon-adsystem.com", "moatads.com", "scorecardresearch.com",
        "quantserve.com", "hotjar.com", "adsrvr.org", "adroll.com",
        "mgid.com", "revcontent.com", "zedo.com", "popads.net",
        "popcash.net", "propellerads.com", "exoclick.com", "adsterra.com",
        "smartadserver.com", "yieldmo.com", "sharethrough.com", "33across.com",
        "media.net", "bidswitch.net", "indexww.com", "lijit.com",
        // Korea (List-KR core) ad & tracker networks
        "ad.daum.net", "t1.daumcdn.net", "display.ad.daum.net", "track.tiara.kakao.com",
        "ads.kakao.com", "adcr.naver.com", "siape.veta.naver.com", "gfp.veta.naver.com",
        "wcs.naver.net", "ntm.pstatic.net", "ssl.pstatic.net",
        "ads-partners.coupang.com", "link.coupang.com", "coupa.ng",
        "adpopcorn.com", "igaworks.com", "adbrix.io", "buzzvil.com",
        "mobon.net", "mediacategory.com", "realclick.co.kr", "widerplanet.com",
        "dable.io", "api.dable.io", "interworksmedia.co.kr", "cauly.co.kr",
        "tnkfactory.com", "mezzo.co.kr", "adbay.co.kr", "admaru.com",
        "tenping.kr", "adpick.co.kr", "acecounter.com", "logger.co.kr"
    )

    private val blockedPathKeywords = listOf(
        "/pagead/", "/ads/js", "/adserver", "/banner_ad", "/popunder",
        "/advert/", "/adclick", "/adsense/", "/doubleclick/"
    )

    fun extractHost(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return runCatching { Uri.parse(url).host?.lowercase().orEmpty() }.getOrDefault("")
    }

    fun isEnabledForHost(pageHost: String): Boolean {
        val st = _state.value
        if (!st.globalEnabled) return false
        if (pageHost.isBlank()) return true
        return pageHost.lowercase() !in st.whitelistedHosts
    }

    fun setGlobalEnabled(enabled: Boolean) {
        _state.update { it.copy(globalEnabled = enabled) }
        saveConfig()
    }

    fun setCosmeticEnabled(enabled: Boolean) {
        _state.update { it.copy(cosmeticEnabled = enabled) }
        saveConfig()
    }

    fun toggleWhitelistForHost(host: String) {
        val clean = host.lowercase().trim()
        if (clean.isBlank()) return
        _state.update { st ->
            val next = if (clean in st.whitelistedHosts) st.whitelistedHosts - clean else st.whitelistedHosts + clean
            st.copy(whitelistedHosts = next)
        }
        saveConfig()
    }

    fun addCustomBlockedHost(host: String) {
        val clean = host.lowercase().trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
        if (clean.isBlank()) return
        _state.update { it.copy(customBlockedHosts = it.customBlockedHosts + clean) }
        saveConfig()
    }

    fun removeCustomBlockedHost(host: String) {
        _state.update { it.copy(customBlockedHosts = it.customBlockedHosts - host) }
        saveConfig()
    }

    fun addCustomCssSelector(selector: String) {
        val clean = selector.trim()
        if (clean.isBlank()) return
        _state.update { it.copy(customCssSelectors = it.customCssSelectors + clean) }
        saveConfig()
    }

    fun removeCustomCssSelector(selector: String) {
        _state.update { it.copy(customCssSelectors = it.customCssSelectors - selector) }
        saveConfig()
    }

    fun resetTabCount(tabId: String) {
        _state.update { st ->
            st.copy(perTabCounts = st.perTabCounts + (tabId to 0))
        }
    }

    private fun matchesBlockedDomain(host: String, customSet: Set<String>): Boolean {
        var cur = host
        while (true) {
            if (cur in defaultBlockedHosts || cur in customSet) return true
            val dot = cur.indexOf('.')
            if (dot == -1) return false
            cur = cur.substring(dot + 1)
        }
    }

    private val captchaAllowHosts = setOf(
        "challenges.cloudflare.com",
        "cloudflare.com",
        "cloudflareinsights.com",
        "hcaptcha.com",
        "recaptcha.net",
        "www.google.com",
        "gstatic.com"
    )

    fun shouldBlock(requestUrl: String?, pageHost: String, tabId: String): Boolean {
        if (requestUrl.isNullOrBlank()) return false
        if (!isEnabledForHost(pageHost)) return false

        val uri = runCatching { Uri.parse(requestUrl) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        // Never block the main page document host or human-verification (Cloudflare Turnstile / hCaptcha / reCAPTCHA)
        if (host == pageHost.lowercase()) return false
        if (captchaAllowHosts.any { host == it || host.endsWith(".$it") }) return false

        val st = _state.value
        val hostMatch = matchesBlockedDomain(host, st.customBlockedHosts)
        val path = uri.path?.lowercase().orEmpty()
        val pathMatch = !hostMatch && blockedPathKeywords.any { path.contains(it) }

        if (hostMatch || pathMatch) {
            _state.update { cur ->
                val tabCnt = (cur.perTabCounts[tabId] ?: 0) + 1
                val recent = (listOf(host) + cur.recentBlockedHosts.filterNot { it == host }).take(20)
                cur.copy(
                    perTabCounts = cur.perTabCounts + (tabId to tabCnt),
                    totalBlockedCount = cur.totalBlockedCount + 1,
                    recentBlockedHosts = recent
                )
            }
            return true
        }
        return false
    }

    fun emptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            204,
            "No Content",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun buildCosmeticFilterJs(pageHost: String): String {
        val st = _state.value
        if (!st.cosmeticEnabled || !isEnabledForHost(pageHost)) return ""
        val baseSelectors = listOf(
            "ins.adsbygoogle",
            "[id^=\"google_ads_iframe\"]",
            "[class*=\"ad-banner\"]",
            "[class*=\"ad_banner\"]",
            "[data-ad-slot]",
            "iframe[src*=\"doubleclick\"]",
            "iframe[src*=\"googlesyndication\"]",
            "iframe[src*=\"ads-partners.coupang.com\"]",
            "[class*=\"coupang-ad\"]",
            "[id*=\"taboola\"]",
            "[class*=\"dable\"]"
        ) + st.customCssSelectors.toList()
        val joined = baseSelectors.joinToString(",")
        val escaped = joined.replace("'", "\\'")
        return """
            (function() {
                var existing = document.getElementById('ego-adblock-css');
                if (existing) existing.remove();
                var style = document.createElement('style');
                style.id = 'ego-adblock-css';
                style.textContent = '$escaped { display: none !important; }';
                (document.head || document.documentElement).appendChild(style);
            })();
        """.trimIndent()
    }
}
