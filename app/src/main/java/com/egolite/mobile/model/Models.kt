package com.egolite.mobile.model

import kotlinx.serialization.Serializable
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

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "새 탭",
    val url: String = "https://www.google.com",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
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
    val url: String
)

data class BrowserPreferences(
    val adBlockEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val desktopModeEnabled: Boolean = true,
    val autoTranslateEnabled: Boolean = false,
    val showBookmarkBar: Boolean = false,
    val maxAiSpaces: Int = 5,
    val translateApiKey: String = "",
    val firecrawlApiKey: String = ""
)

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
