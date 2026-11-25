package com.egolite.mobile.model

import kotlinx.serialization.Serializable
import java.util.Locale
import java.util.UUID

enum class SpaceType {
    PERSONAL,
    AI
}

enum class AiSpaceStatus {
    IDLE,
    WORKING,
    TAKEOVER_PAUSED,
    DONE
}

enum class TunnelProvider {
    CLOUDFLARE,
    NGROK
}

enum class McpConnectionState {
    DISCONNECTED,
    STARTING,
    CONNECTED,
    ERROR
}

enum class TranslationEngine {
    ONLINE_GOOGLE,
    OFFLINE_MLKIT
}

enum class SearchEngine(val label: String, val searchUrlPrefix: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    NAVER("NAVER", "https://search.naver.com/search.naver?query="),
    DAUM("Daum", "https://search.daum.net/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=")
}

@Serializable
data class PersistedTab(
    val title: String,
    val url: String
)

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "새 탭",
    val url: String = "ego://newtab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val parentTabId: String? = null,
    val isIncognito: Boolean = false
)

data class BrowserSpace(
    val id: String,
    val name: String,
    val type: SpaceType,
    val tabs: List<BrowserTab>,
    val activeTabId: String,
    val aiStatus: AiSpaceStatus = AiSpaceStatus.IDLE,
    val lastActionSummary: String = ""
) {
    val activeTab: BrowserTab
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
}

@Serializable
data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val folder: String = "모바일 북마크"
)

enum class WebFontMode(val label: String) {
    SITE_DEFAULT("사이트 기본 폰트"),
    SYSTEM_FONT("태블릿 시스템 폰트 강제"),
    CLEAN_GOTHIC("깔끔한 고딕 (Sans-serif)"),
    SERIF("명조 (Serif)")
}

data class LongPressTarget(
    val linkUrl: String? = null,
    val linkText: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class BrowserPreferences(
    val adBlockEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val desktopModeEnabled: Boolean = true,
    val showBookmarkBar: Boolean = false,
    val maxAiSpaces: Int = 5,
    val translationEngine: TranslationEngine = TranslationEngine.ONLINE_GOOGLE,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val textZoom: Int = 100,
    val webFontMode: WebFontMode = WebFontMode.SITE_DEFAULT,
    val homepageUrl: String = "ego://newtab",
    val autoTranslateHosts: Set<String> = emptySet(),
    val pinnedTranslateLangs: List<String> = listOf("ko", "en", "ja", "zh-CN"),
    val firecrawlApiKey: String = ""
)

data class TranslationPopupState(
    val visible: Boolean = false,
    val isTranslating: Boolean = false,
    val isTranslated: Boolean = false,
    val statusText: String = "",
    val sourceLang: String = "auto",
    val targetLang: String = Locale.getDefault().language.ifBlank { "ko" },
    val detectedLangLabel: String = "감지된 언어"
)

@Serializable
data class TunnelSettings(
    val provider: TunnelProvider = TunnelProvider.CLOUDFLARE,
    val localPort: Int = 8787,
    val cloudflareToken: String = "",
    val ngrokAuthToken: String = "",
    val ngrokDomain: String = "",
    val bearerToken: String = ""
)

data class ServerRuntimeState(
    val connectionState: McpConnectionState = McpConnectionState.DISCONNECTED,
    val publicHttpsUrl: String = "",
    val localUrl: String = "http://127.0.0.1:8787/mcp",
    val statusMessage: String = "MCP 서버 꺼짐",
    val recentLogs: List<String> = emptyList()
)
