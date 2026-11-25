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
    THINKING,
    TAKEOVER_PAUSED,
    DONE
}

val AI_SPACE_COLOR_PRESETS: List<String> = listOf(
    "#10B981", // 에메랄드
    "#38BDF8", // 스카이블루
    "#A855F7", // 바이올렛
    "#F59E0B", // 앰버
    "#F43F5E", // 로즈
    "#06B6D4"  // 시안
)

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

enum class ThemeMode(val label: String) {
    SYSTEM("시스템 기본값"),
    LIGHT("라이트"),
    DARK("다크")
}

enum class SearchEngine(
    val label: String,
    val domain: String,
    val searchUrlPrefix: String,
    val isPrimary: Boolean = true
) {
    GOOGLE("Google", "google.com", "https://www.google.com/search?q=", true),
    BING("Microsoft Bing", "bing.com", "https://www.bing.com/search?q=", true),
    DUCKDUCKGO("DuckDuckGo", "duckduckgo.com", "https://duckduckgo.com/?q=", true),
    ECOSIA("Ecosia", "ecosia.org", "https://www.ecosia.org/search?q=", true),
    BRAVE("Brave", "search.brave.com", "https://search.brave.com/search?q=", true),
    NAVER("m.naver.com", "m.naver.com", "https://search.naver.com/search.naver?query=", false),
    DAUM("daum.net", "daum.net", "https://search.daum.net/search?q=", false)
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
    val lastActionSummary: String = "",
    val accentColorHex: String = "#10B981"
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
    SYSTEM_FONT("기기 시스템 폰트 적용"),
    CLEAN_GOTHIC("깔끔한 고딕 (Sans-serif)"),
    SERIF("명조 (Serif)"),
    CUSTOM_TTF("커스텀 폰트 (.ttf/.otf)")
}

data class LongPressTarget(
    val linkUrl: String? = null,
    val linkText: String? = null,
    val imageUrl: String? = null,
    val touchX: Float = -1f,
    val touchY: Float = -1f
)

@Serializable
data class BrowserPreferences(
    val adBlockEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val desktopModeEnabled: Boolean = true,
    val showBookmarkBar: Boolean = false,
    val showHomeButton: Boolean = true,
    val homepageEnabled: Boolean = true,
    val homepageUrl: String = "ego://newtab",
    val maxAiSpaces: Int = 5,
    val inactiveTabDays: Int = 21,
    val translationEngine: TranslationEngine = TranslationEngine.ONLINE_GOOGLE,
    val offerTranslate: Boolean = true,
    val defaultTranslateTarget: String = "ko",
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val textZoom: Int = 100,
    val showZoomInMenu: Boolean = false,
    val forceEnableZoom: Boolean = true,
    val simplifiedViewPrompt: Boolean = false,
    val webFontMode: WebFontMode = WebFontMode.SITE_DEFAULT,
    val customFontPath: String = "",
    val customFontName: String = "",
    val allowLocation: Boolean = true,
    val allowCamera: Boolean = true,
    val allowMic: Boolean = true,
    val allowPopups: Boolean = true,
    val allowJavaScript: Boolean = true,
    val allowThirdPartyCookies: Boolean = true,
    val askDownloadLocation: Boolean = false,
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

@Serializable
data class ServerRuntimeState(
    val connectionState: McpConnectionState = McpConnectionState.DISCONNECTED,
    val publicHttpsUrl: String = "",
    val localUrl: String = "http://127.0.0.1:8787/mcp",
    val statusMessage: String = "MCP 서버 꺼짐",
    val recentLogs: List<String> = emptyList()
)
