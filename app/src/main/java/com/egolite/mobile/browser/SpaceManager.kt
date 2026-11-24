package com.egolite.mobile.browser

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.WebView
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.BrowserPreferences
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.BrowserTab
import com.egolite.mobile.model.SpaceType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import org.json.JSONTokener
import java.util.concurrent.ConcurrentHashMap

object SpaceManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webViews = ConcurrentHashMap<String, WebView>()
    private lateinit var appContext: Context

    private val _preferences = MutableStateFlow(BrowserPreferences())
    val preferences: StateFlow<BrowserPreferences> = _preferences.asStateFlow()

    private val _bookmarks = MutableStateFlow(
        listOf(
            BookmarkItem(title = "Google", url = "https://www.google.com"),
            BookmarkItem(title = "Naver", url = "https://www.naver.com"),
            BookmarkItem(title = "GitHub", url = "https://github.com")
        )
    )
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val initialPersonalTab = BrowserTab(title = "Google", url = "https://www.google.com")
    private val initialAiTab = BrowserTab(title = "AI Space 1", url = "https://www.google.com")

    private val _spaces = MutableStateFlow(
        listOf(
            BrowserSpace(
                id = "personal",
                name = "Personal",
                type = SpaceType.PERSONAL,
                tabs = listOf(initialPersonalTab),
                activeTabId = initialPersonalTab.id
            ),
            BrowserSpace(
                id = "ai-1",
                name = "AI Space 1",
                type = SpaceType.AI,
                tabs = listOf(initialAiTab),
                activeTabId = initialAiTab.id
            )
        )
    )
    val spaces: StateFlow<List<BrowserSpace>> = _spaces.asStateFlow()

    private val _activeSpaceId = MutableStateFlow("personal")
    val activeSpaceId: StateFlow<String> = _activeSpaceId.asStateFlow()

    val isAnyAiWorking: Boolean
        get() = _spaces.value.any { it.type == SpaceType.AI && it.aiStatus == AiSpaceStatus.WORKING }

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    fun getActiveSpace(): BrowserSpace {
        return _spaces.value.firstOrNull { it.id == _activeSpaceId.value } ?: _spaces.value.first()
    }

    fun selectSpace(spaceId: String) {
        if (_spaces.value.any { it.id == spaceId }) {
            _activeSpaceId.value = spaceId
        }
    }

    fun createAiSpace(): BrowserSpace? {
        val currentAiCount = _spaces.value.count { it.type == SpaceType.AI }
        if (currentAiCount >= _preferences.value.maxAiSpaces) return null
        val nextIndex = currentAiCount + 1
        val newTab = BrowserTab(title = "AI Space $nextIndex", url = "https://www.google.com")
        val newSpace = BrowserSpace(
            id = "ai-$nextIndex-${System.currentTimeMillis() % 1000}",
            name = "AI Space $nextIndex",
            type = SpaceType.AI,
            tabs = listOf(newTab),
            activeTabId = newTab.id
        )
        _spaces.update { it + newSpace }
        return newSpace
    }

    fun closeAiSpace(spaceId: String) {
        val space = _spaces.value.firstOrNull { it.id == spaceId && it.type == SpaceType.AI } ?: return
        space.tabs.forEach { destroyWebView(it.id) }
        _spaces.update { list -> list.filterNot { it.id == spaceId } }
        if (_activeSpaceId.value == spaceId) {
            _activeSpaceId.value = "personal"
        }
    }

    fun createTab(spaceId: String, initialUrl: String = "https://www.google.com"): BrowserTab {
        val tab = BrowserTab(title = "새 탭", url = initialUrl)
        _spaces.update { list ->
            list.map { space ->
                if (space.id == spaceId) {
                    space.copy(tabs = space.tabs + tab, activeTabId = tab.id)
                } else space
            }
        }
        return tab
    }

    fun selectTab(spaceId: String, tabId: String) {
        _spaces.update { list ->
            list.map { space ->
                if (space.id == spaceId && space.tabs.any { it.id == tabId }) {
                    space.copy(activeTabId = tabId)
                } else space
            }
        }
    }

    fun closeTab(spaceId: String, tabId: String) {
        val targetSpace = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        if (targetSpace.tabs.size <= 1) {
            navigate(spaceId, "https://www.google.com")
            return
        }
        destroyWebView(tabId)
        _spaces.update { list ->
            list.map { space ->
                if (space.id == spaceId) {
                    val remaining = space.tabs.filterNot { it.id == tabId }
                    val newActive = if (space.activeTabId == tabId) remaining.last().id else space.activeTabId
                    space.copy(tabs = remaining, activeTabId = newActive)
                } else space
            }
        }
    }

    fun getOrCreateWebView(spaceId: String, tab: BrowserTab): WebView {
        return webViews.getOrPut(tab.id) {
            EgoWebViewFactory.createWebView(
                context = appContext,
                adBlockProvider = { _preferences.value.adBlockEnabled },
                darkModeProvider = { _preferences.value.darkModeEnabled },
                desktopModeProvider = { _preferences.value.desktopModeEnabled },
                onPageStarted = { url ->
                    updateTab(spaceId, tab.id) { it.copy(url = url, isLoading = true) }
                },
                onPageFinished = { url, title, canBack, canForward ->
                    updateTab(spaceId, tab.id) {
                        it.copy(
                            url = url,
                            title = title.ifBlank { url },
                            isLoading = false,
                            progress = 100,
                            canGoBack = canBack,
                            canGoForward = canForward
                        )
                    }
                },
                onProgressChanged = { progress ->
                    updateTab(spaceId, tab.id) { it.copy(progress = progress, isLoading = progress < 100) }
                },
                onCreatePopup = { resultMsg ->
                    handlePopupWindow(spaceId, resultMsg)
                }
            ).apply {
                loadUrl(tab.url)
            }
        }
    }

    private fun handlePopupWindow(spaceId: String, resultMsg: Message) {
        val popupTab = createTab(spaceId, "about:blank")
        val popupWebView = getOrCreateWebView(spaceId, popupTab)
        val transport = resultMsg.obj as? WebView.WebViewTransport
        transport?.webView = popupWebView
        resultMsg.sendToTarget()
    }

    fun navigate(spaceId: String, rawInput: String) {
        val url = normalizeUrl(rawInput)
        val space = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        val activeTab = space.activeTab
        updateTab(spaceId, activeTab.id) { it.copy(url = url, isLoading = true) }
        mainHandler.post {
            getOrCreateWebView(spaceId, activeTab).loadUrl(url)
        }
    }

    fun goBack(spaceId: String) {
        val tabId = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId ?: return
        mainHandler.post { webViews[tabId]?.goBack() }
    }

    fun goForward(spaceId: String) {
        val tabId = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId ?: return
        mainHandler.post { webViews[tabId]?.goForward() }
    }

    fun reload(spaceId: String) {
        val tabId = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId ?: return
        mainHandler.post { webViews[tabId]?.reload() }
    }

    fun updatePreferences(transform: (BrowserPreferences) -> BrowserPreferences) {
        val updated = transform(_preferences.value)
        _preferences.value = updated
        mainHandler.post {
            webViews.values.forEach { wv ->
                EgoWebViewFactory.applyDarkMode(wv, updated.darkModeEnabled)
                EgoWebViewFactory.applyDesktopMode(wv, updated.desktopModeEnabled)
            }
        }
    }

    fun toggleBookmark(title: String, url: String) {
        val exists = _bookmarks.value.any { it.url == url }
        _bookmarks.update { list ->
            if (exists) list.filterNot { it.url == url }
            else list + BookmarkItem(title = title.ifBlank { url }.take(24), url = url)
        }
    }

    fun removeBookmark(id: String) {
        _bookmarks.update { list -> list.filterNot { it.id == id } }
    }

    fun setAiStatus(spaceId: String, status: AiSpaceStatus, summary: String = "") {
        _spaces.update { list ->
            list.map {
                if (it.id == spaceId) it.copy(aiStatus = status, lastActionSummary = summary) else it
            }
        }
    }

    fun toggleTakeover(spaceId: String) {
        val space = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        val next = if (space.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED) {
            AiSpaceStatus.IDLE
        } else {
            AiSpaceStatus.TAKEOVER_PAUSED
        }
        setAiStatus(spaceId, next, if (next == AiSpaceStatus.TAKEOVER_PAUSED) "사용자 직접 조작 중" else "")
    }

    fun resolveAiSpace(spaceId: String?): BrowserSpace {
        if (!spaceId.isNullOrBlank()) {
            _spaces.value.firstOrNull { it.id == spaceId && it.type == SpaceType.AI }?.let { return it }
        }
        return _spaces.value.firstOrNull { it.type == SpaceType.AI } ?: createAiSpace() ?: _spaces.value.first()
    }

    suspend fun evaluateJsInSpace(spaceId: String, js: String, timeoutMs: Long = 15000L): String {
        val space = resolveAiSpace(spaceId)
        val tab = space.activeTab
        val deferred = CompletableDeferred<String>()
        mainHandler.post {
            val wv = getOrCreateWebView(space.id, tab)
            wv.evaluateJavascript(js) { raw ->
                val unescaped = decodeEvaluateJsResult(raw)
                deferred.complete(unescaped)
            }
        }
        return withTimeout(timeoutMs) { deferred.await() }
    }

    suspend fun navigateAndWait(spaceId: String, url: String, waitMs: Long = 2200L) {
        val space = resolveAiSpace(spaceId)
        navigate(space.id, url)
        kotlinx.coroutines.delay(waitMs)
    }

    suspend fun captureScreenshotInSpace(spaceId: String): String {
        val space = resolveAiSpace(spaceId)
        val tab = space.activeTab
        val deferred = CompletableDeferred<String>()
        mainHandler.post {
            val wv = getOrCreateWebView(space.id, tab)
            deferred.complete(EgoWebViewFactory.captureScreenshotBase64(wv))
        }
        return withTimeout(10000L) { deferred.await() }
    }

    private fun decodeEvaluateJsResult(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return "{}"
        return runCatching {
            val parsed = JSONTokener(raw).nextValue()
            parsed?.toString() ?: raw
        }.getOrDefault(raw)
    }

    private fun updateTab(spaceId: String, tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _spaces.update { list ->
            list.map { space ->
                if (space.id == spaceId) {
                    space.copy(tabs = space.tabs.map { if (it.id == tabId) transform(it) else it })
                } else space
            }
        }
    }

    private fun destroyWebView(tabId: String) {
        mainHandler.post {
            webViews.remove(tabId)?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("about:")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }
        return "https://www.google.com/search?q=${android.net.Uri.encode(trimmed)}"
    }
}
