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

enum class MonitorPosition(val label: String) {
    TOP("상단"),
    LEFT("좌측")
}

enum class TabWidthMode(val label: String) {
    DYNAMIC("동적 조절 (이름 길이 맞춤)"),
    MANUAL("수동 조절 (고정 너비)")
}

enum class EqualizerMorphStyle(val label: String, val description: String) {
    DOTS_SLASH(
        "A안 · 3점 안착 + 컷아웃 사선",
        "3단 바가 3개의 둥근 점으로 탄성 수축하며 대각선 사선이 그려짐"
    ),
    FLAT_MERGE(
        "B안 · 중앙 플랫바 병합 + 사선",
        "3단 바가 중앙 수평선으로 납작하게 병합되며 대각선 사선이 겹쳐짐"
    ),
    PAUSE_MORPH(
        "C안 · 일시정지(II) 유기적 정렬",
        "사선 없이 가운데 바가 사라지고 양옆 바가 일시정지 형태로 정렬"
    )
}

enum class ThemeMode(val label: String) {
    SYSTEM("시스템 기본값"),
    LIGHT("밝게"),
    DARK("어둡게")
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

@Serializable
data class PersistedAiSpace(
    val id: String,
    val name: String,
    val accentColorHex: String = "#10B981",
    val tabs: List<PersistedTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val lastUpdatedAtMs: Long = System.currentTimeMillis()
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
    val isIncognito: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val isMuted: Boolean = false
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
enum class CardVanishStyle(val label: String, val description: String) {
    HARMONY_DUST("하모니OS 초미세 먼지 (기본값)", "카드 경계가 침식되며 초미세 먼지로 흩어지는 무손실 소멸"),
    LIGHT_PARTICLES("가벼운 입자 분산", "약 250개 입자로 빠르게 흩어지는 저부하 소멸 (560ms)"),
    SCALE_FADE("기본 축소 페이드", "먼지 효과 없이 깔끔하게 축소되며 사라짐"),
    OFF("즉시 삭제 (애니메이션 없음)", "지연 없이 즉시 닫기")
}

@Serializable
data class BrowserPreferences(
    val adBlockEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkenWebsitesWhenPossible: Boolean = true,
    val desktopModeEnabled: Boolean = true,
    val showBookmarkBar: Boolean = false,
    val showHomeButton: Boolean = true,
    val showMcpBadgeButton: Boolean = true,
    val showAiCommandButton: Boolean = true,
    val showAdBlockButton: Boolean = true,
    val showTranslateButton: Boolean = true,
    val showDarkModeButton: Boolean = true,
    val showProfileAvatarButton: Boolean = true,
    val showNewTabToolbarButton: Boolean = false,
    val showShareButton: Boolean = false,
    val showFindInPageButton: Boolean = false,
    val showDesktopSiteButton: Boolean = false,
    val showReaderModeButton: Boolean = false,
    val toolbarButtonOrder: List<String> = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE"),
    val homepageEnabled: Boolean = true,
    val homepageUrl: String = "ego://newtab",
    val maxAiSpaces: Int = 5,
    val monitorPosPortrait: MonitorPosition = MonitorPosition.TOP,
    val monitorPosLandscape: MonitorPosition = MonitorPosition.LEFT,
    val equalizerMorphStyle: EqualizerMorphStyle = EqualizerMorphStyle.DOTS_SLASH,
    val cardVanishStyle: CardVanishStyle = CardVanishStyle.HARMONY_DUST,
    val vanishParticleCount: Int = 1800,
    val vanishDurationMs: Int = 1300,
    val tabWidthMode: TabWidthMode = TabWidthMode.DYNAMIC,
    val manualTabWidthDp: Int = 180,
    val aiSpaceRetentionDays: Int = 7,
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
