package com.egolite.mobile.nativebridge

object EgoRustBridge {
    @Volatile
    var isLoaded: Boolean = false
        private set

    @Volatile
    var engineVersion: String = "Kotlin 폴백 모드"
        private set

    init {
        isLoaded = runCatching {
            System.loadLibrary("ego_core")
            engineVersion = nativeEngineVersion()
            true
        }.getOrDefault(false)
    }

    private external fun nativeEngineVersion(): String
    private external fun nativeSyncRules(domainsJoined: String, keywordsJoined: String): Int
    private external fun nativeShouldBlock(host: String, path: String): Boolean
    private external fun nativeStripHtmlToText(html: String): String

    fun syncAdBlockRules(domains: Collection<String>, keywords: Collection<String>): Int {
        if (!isLoaded) return 0
        return runCatching {
            nativeSyncRules(domains.joinToString("\n"), keywords.joinToString("\n"))
        }.getOrDefault(0)
    }

    fun shouldBlockNative(host: String, path: String): Boolean {
        if (!isLoaded) return false
        return runCatching { nativeShouldBlock(host, path) }.getOrDefault(false)
    }

    fun stripHtmlToText(html: String): String {
        if (!isLoaded) return html
        return runCatching { nativeStripHtmlToText(html) }.getOrDefault(html)
    }
}
