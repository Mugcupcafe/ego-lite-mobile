package com.egolite.mobile.browser

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlockEngine {
    private val blockedHosts = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "ad.daum.net",
        "t1.daumcdn.net",
        "adcr.naver.com",
        "siape.veta.naver.com",
        "gfp.veta.naver.com",
        "ads.kakao.com",
        "analytics.google.com",
        "www.google-analytics.com",
        "connect.facebook.net",
        "ads.twitter.com",
        "static.ads-twitter.com",
        "criteo.com",
        "taboola.com",
        "outbrain.com",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "amazon-adsystem.com",
        "moatads.com",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com"
    )

    private val blockedPathKeywords = listOf(
        "/pagead/",
        "/ads/js",
        "/adserver",
        "/banner_ad",
        "/popunder"
    )

    fun shouldBlock(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (blockedHosts.any { host == it || host.endsWith(".$it") }) {
            return true
        }
        val path = uri.path?.lowercase().orEmpty()
        return blockedPathKeywords.any { path.contains(it) }
    }

    fun emptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    const val COSMETIC_FILTER_JS: String = """
        (function() {
            if (window.__egoAdblockApplied) return;
            window.__egoAdblockApplied = true;
            var style = document.createElement('style');
            style.id = 'ego-adblock-css';
            style.textContent = [
                'ins.adsbygoogle',
                '[id^="google_ads_iframe"]',
                '[class*="ad-banner"]',
                '[class*="ad_banner"]',
                '[data-ad-slot]',
                'iframe[src*="doubleclick"]',
                'iframe[src*="googlesyndication"]'
            ].join(',') + ' { display: none !important; }';
            (document.head || document.documentElement).appendChild(style);
        })();
    """
}
