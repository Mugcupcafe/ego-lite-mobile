package com.egolite.mobile.browser

import com.egolite.mobile.model.PersistedAiSpace
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.WebView
import com.egolite.mobile.model.AI_SPACE_COLOR_PRESETS
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.BrowserPreferences
import com.egolite.mobile.model.ThemeMode
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.BrowserTab
import com.egolite.mobile.model.PersistedTab
import com.egolite.mobile.model.SpaceType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONTokener
import java.util.concurrent.ConcurrentHashMap

object SpaceManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonCodec = Json { ignoreUnknownKeys = true }
    private val webViews = ConcurrentHashMap<String, WebView>()
    private lateinit var appContext: Context

    private val _preferences = MutableStateFlow(BrowserPreferences())
    val preferences: StateFlow<BrowserPreferences> = _preferences.asStateFlow()

    private val _bookmarkFolders = MutableStateFlow(listOf("북마크 바", "모바일 북마크", "AI"))
    val bookmarkFolders: StateFlow<List<String>> = _bookmarkFolders.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    private val _recentlyClosedTabs = MutableStateFlow<List<PersistedTab>>(emptyList())
    val recentlyClosedTabs: StateFlow<List<PersistedTab>> = _recentlyClosedTabs.asStateFlow()
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _bookmarks = MutableStateFlow(
        listOf(
            BookmarkItem(title = "Google", url = "https://www.google.com", folder = "북마크 바"),
            BookmarkItem(title = "NAVER", url = "https://www.naver.com", folder = "북마크 바"),
            BookmarkItem(title = "알구몬", url = "https://www.algumon.com/n/deal", folder = "북마크 바"),
            BookmarkItem(title = "GitHub", url = "https://github.com", folder = "모바일 북마크")
        )
    )
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val history: StateFlow<List<BookmarkItem>> = _history.asStateFlow()

    private val initialPersonalTab = BrowserTab(title = "새 탭", url = "ego://newtab")

    // 최초 실행 시 기본 AI 스페이스 없이 Personal만으로 시작 (저장된 AI 스페이스가 있으면 loadPersistedData에서 복원)
    private val _spaces = MutableStateFlow(
        listOf(
            BrowserSpace(
                id = "personal",
                name = "Personal",
                type = SpaceType.PERSONAL,
                tabs = listOf(initialPersonalTab),
                activeTabId = initialPersonalTab.id
            )
        )
    )
    val spaces: StateFlow<List<BrowserSpace>> = _spaces.asStateFlow()

    private val _activeSpaceId = MutableStateFlow("personal")
    val activeSpaceId: StateFlow<String> = _activeSpaceId.asStateFlow()

    private val _connectedAiClientName = MutableStateFlow("Gemini")
    val connectedAiClientName: StateFlow<String> = _connectedAiClientName.asStateFlow()

    fun updateConnectedAiClient(rawClientName: String?) {
        val low = rawClientName?.trim()?.lowercase().orEmpty()
        if (low.isBlank()) return
        val normalized = when {
            "gemini" in low -> "Gemini"
            "claude" in low || "anthropic" in low -> "Claude"
            "muse" in low -> "Muse"
            "openai" in low || "chatgpt" in low || "codex" in low -> "OpenAI"
            "grok" in low || "xai" in low -> "Grok"
            "deepseek" in low -> "DeepSeek"
            "cursor" in low -> "Cursor"
            else -> rawClientName?.trim()?.take(16).orEmpty()
        }
        _connectedAiClientName.value = normalized
    }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val aiIdleJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    val isAnyAiWorking: Boolean
        get() = _spaces.value.any {
            it.type == SpaceType.AI && (it.aiStatus == AiSpaceStatus.WORKING || it.aiStatus == AiSpaceStatus.THINKING)
        }

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            AdBlockEngine.init(appContext)
            com.egolite.mobile.tunnel.EgoForegroundService.init(appContext)
            loadPersistedData()
        }
    }

    private fun loadPersistedData() {
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.getString("browser_prefs_json", null)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<BrowserPreferences>(raw) }.getOrNull()?.let { loaded ->
                _preferences.value = loaded
            }
        }
        sp.getString("bookmarks_json", null)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<List<BookmarkItem>>(raw) }.getOrNull()?.let {
                if (it.isNotEmpty()) _bookmarks.value = it
            }
        }
        sp.getString("folders_json", null)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<List<String>>(raw) }.getOrNull()?.let {
                if (it.isNotEmpty()) _bookmarkFolders.value = it
            }
        }
        sp.getString("searches_json", null)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<List<String>>(raw) }.getOrNull()?.let {
                _recentSearches.value = it
            }
        }
        sp.getString("personal_tabs_json", null)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<List<PersistedTab>>(raw) }.getOrNull()?.let { saved ->
                if (saved.isNotEmpty()) {
                    val restored = saved.map { BrowserTab(title = it.title, url = it.url) }
                    _spaces.update { list ->
                        list.map { space ->
                            if (space.id == "personal") space.copy(tabs = restored, activeTabId = restored.last().id)
                            else space
                        }
                    }
                }
            }
        }
    }

    private fun savePreferences() {
        if (!::appContext.isInitialized) return
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("browser_prefs_json", jsonCodec.encodeToString(_preferences.value)).apply()

        sp.getString("ai_spaces_json", null)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<List<PersistedAiSpace>>(raw) }.getOrNull()?.let { savedList ->
                val retentionDays = _preferences.value.aiSpaceRetentionDays
                val now = System.currentTimeMillis()
                val maxAgeMs = if (retentionDays <= 0) Long.MAX_VALUE else retentionDays.toLong() * 86_400_000L
                val validSpaces = savedList.filter { saved ->
                    retentionDays <= 0 || (now - saved.lastUpdatedAtMs) <= maxAgeMs
                }.map { saved ->
                    val restoredTabs = saved.tabs.ifEmpty {
                        listOf(PersistedTab(saved.name, "https://www.google.com"))
                    }.map { BrowserTab(title = it.title, url = it.url) }
                    val safeIdx = saved.activeTabIndex.coerceIn(0, restoredTabs.lastIndex)
                    BrowserSpace(
                        id = saved.id,
                        name = saved.name,
                        type = SpaceType.AI,
                        tabs = restoredTabs,
                        activeTabId = restoredTabs[safeIdx].id,
                        accentColorHex = saved.accentColorHex
                    )
                }
                if (validSpaces.isNotEmpty()) {
                    _spaces.update { list ->
                        val personalOnly = list.filter { it.type == SpaceType.PERSONAL }
                        personalOnly + validSpaces
                    }
                }
                if (validSpaces.size != savedList.size) {
                    saveAiSpaces()
                }
            }
        }
    }

    private fun savePersonalTabs() {
        if (!::appContext.isInitialized) return
        val personal = _spaces.value.firstOrNull { it.id == "personal" } ?: return
        val toSave = personal.tabs.filterNot { it.isIncognito }.map { PersistedTab(it.title, it.url) }
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("personal_tabs_json", jsonCodec.encodeToString(toSave)).apply()
    }

    private fun saveAiSpaces() {
        if (!::appContext.isInitialized) return
        val now = System.currentTimeMillis()
        val aiList = _spaces.value
            .filter { it.type == SpaceType.AI }
            .map { sp ->
                val tabs = sp.tabs.filterNot { it.isIncognito }.map { PersistedTab(it.title, it.url) }
                val activeIdx = sp.tabs.indexOfFirst { it.id == sp.activeTabId }.coerceAtLeast(0)
                PersistedAiSpace(
                    id = sp.id,
                    name = sp.name,
                    accentColorHex = sp.accentColorHex,
                    tabs = tabs,
                    activeTabIndex = activeIdx,
                    lastUpdatedAtMs = now
                )
            }
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("ai_spaces_json", jsonCodec.encodeToString(aiList)).apply()
    }

    private fun saveBookmarks() {
        if (!::appContext.isInitialized) return
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("bookmarks_json", jsonCodec.encodeToString(_bookmarks.value))
            .putString("folders_json", jsonCodec.encodeToString(_bookmarkFolders.value))
            .apply()
    }

    private fun saveSearches() {
        if (!::appContext.isInitialized) return
        val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("searches_json", jsonCodec.encodeToString(_recentSearches.value)).apply()
    }

    fun getActiveSpace(): BrowserSpace {
        return _spaces.value.firstOrNull { it.id == _activeSpaceId.value } ?: _spaces.value.first()
    }

    fun setTabAudioPlaying(tabId: String, playing: Boolean) {
        _spaces.update { list ->
            list.map { sp ->
                if (sp.tabs.any { it.id == tabId && it.isPlayingAudio != playing }) {
                    sp.copy(tabs = sp.tabs.map { t -> if (t.id == tabId) t.copy(isPlayingAudio = playing) else t })
                } else sp
            }
        }
    }

    fun toggleTabMute(spaceId: String, tabId: String) {
        var nextMuted = false
        _spaces.update { list ->
            list.map { sp ->
                if (sp.id == spaceId) {
                    sp.copy(tabs = sp.tabs.map { t ->
                        if (t.id == tabId) {
                            nextMuted = !t.isMuted
                            t.copy(isMuted = nextMuted)
                        } else t
                    })
                } else sp
            }
        }
        val wv = webViews[tabId]
        wv?.post {
            val js = "(function(){window.__egoTabMuted=$nextMuted;document.querySelectorAll('video,audio').forEach(function(m){try{m.muted=$nextMuted;}catch(e){}});})();"
            wv.evaluateJavascript(js, null)
        }
    }

    fun moveSpaceById(fromSpaceId: String, toSpaceId: String) {
        if (fromSpaceId == toSpaceId) return
        _spaces.update { list ->
            val fromIdx = list.indexOfFirst { it.id == fromSpaceId }
            val toIdx = list.indexOfFirst { it.id == toSpaceId }
            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                val mutable = list.toMutableList()
                val moved = mutable.removeAt(fromIdx)
                mutable.add(toIdx, moved)
                mutable
            } else list
        }
    }

    fun selectSpace(spaceId: String) {
        if (_spaces.value.any { it.id == spaceId }) {
            _activeSpaceId.value = spaceId
        }
    }

    private fun nextGenericAiSpaceNumber(): Int {
        val existingNames = _spaces.value.map { it.name }.toSet()
        var n = 1
        while ("AI Space $n" in existingNames) n++
        return n
    }

    private fun resolveUniqueAiClientName(baseName: String, excludeSpaceId: String): String {
        val cleanBase = baseName.trim().ifBlank { "AI" }
        val existingNames = _spaces.value
            .filter { it.id != excludeSpaceId }
            .map { it.name }
            .toSet()
        if (cleanBase !in existingNames) return cleanBase
        var n = 2
        while ("$cleanBase $n" in existingNames) n++
        return "$cleanBase $n"
    }

    fun createAiSpace(): BrowserSpace? {
        val currentAiCount = _spaces.value.count { it.type == SpaceType.AI }
        if (currentAiCount >= _preferences.value.maxAiSpaces) return null
        val nextIndex = nextGenericAiSpaceNumber()
        val newTab = BrowserTab(title = "AI Space $nextIndex", url = "https://www.google.com")
        val colorHex = AI_SPACE_COLOR_PRESETS[(nextIndex - 1) % AI_SPACE_COLOR_PRESETS.size]
        val newSpace = BrowserSpace(
            id = "ai-$nextIndex-${System.currentTimeMillis() % 1000}",
            name = "AI Space $nextIndex",
            type = SpaceType.AI,
            tabs = listOf(newTab),
            activeTabId = newTab.id,
            accentColorHex = colorHex
        )
        _spaces.update { it + newSpace }
        saveAiSpaces()
        return newSpace
    }

    /**
     * 드래그 드롭으로 기존 AI 스페이스에 탭 복제 생성
     * - 대기 중(IDLE / DONE): 새 탭을 즉시 활성 탭으로 전환
     * - 작업 중(WORKING / THINKING / TAKEOVER_PAUSED): 백그라운드 탭으로 조용히 추가
     */
    fun cloneTabToSpace(sourceUrl: String, sourceTitle: String, targetSpaceId: String): Pair<BrowserTab, Boolean>? {
        val targetSpace = _spaces.value.firstOrNull { it.id == targetSpaceId } ?: return null
        val activateNow = targetSpace.aiStatus == AiSpaceStatus.IDLE || targetSpace.aiStatus == AiSpaceStatus.DONE
        val safeUrl = sourceUrl.ifBlank { "ego://newtab" }
        val newTab = BrowserTab(
            title = sourceTitle.ifBlank { safeUrl },
            url = safeUrl,
            parentTabId = targetSpace.activeTabId
        )
        _spaces.update { list ->
            list.map { sp ->
                if (sp.id == targetSpaceId) {
                    sp.copy(
                        tabs = sp.tabs + newTab,
                        activeTabId = if (activateNow) newTab.id else sp.activeTabId
                    )
                } else sp
            }
        }
        if (targetSpaceId == "personal") savePersonalTabs() else saveAiSpaces()
        return Pair(newTab, activateNow)
    }

    /**
     * 드래그 드롭으로 맨 끝 빈 공간에 새 AI 스페이스 생성 + 드롭한 페이지를 첫 페이지로 열기
     */
    fun createAiSpaceWithClonedTab(sourceUrl: String, sourceTitle: String): BrowserSpace? {
        val currentAiCount = _spaces.value.count { it.type == SpaceType.AI }
        if (currentAiCount >= _preferences.value.maxAiSpaces) return null
        val nextIndex = nextGenericAiSpaceNumber()
        val safeUrl = sourceUrl.ifBlank { "https://www.google.com" }
        val firstTab = BrowserTab(
            title = sourceTitle.ifBlank { safeUrl },
            url = safeUrl
        )
        val colorHex = AI_SPACE_COLOR_PRESETS[(nextIndex - 1) % AI_SPACE_COLOR_PRESETS.size]
        val newSpace = BrowserSpace(
            id = "ai-$nextIndex-${System.currentTimeMillis() % 1000}",
            name = "AI Space $nextIndex",
            type = SpaceType.AI,
            tabs = listOf(firstTab),
            activeTabId = firstTab.id,
            accentColorHex = colorHex
        )
        _spaces.update { it + newSpace }
        saveAiSpaces()
        return newSpace
    }

    fun closeAiSpace(spaceId: String) {
        val space = _spaces.value.firstOrNull { it.id == spaceId && it.type == SpaceType.AI } ?: return
        space.tabs.forEach { destroyWebView(it.id) }
        _spaces.update { list -> list.filterNot { it.id == spaceId } }
        if (_activeSpaceId.value == spaceId) {
            _activeSpaceId.value = "personal"
        }
        saveAiSpaces()
    }

    fun createTab(spaceId: String, initialUrl: String = "ego://newtab", incognito: Boolean = false, selectImmediately: Boolean = true): BrowserTab {
        val currentActive = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId
        val tabTitle = when { initialUrl == "ego://downloads" -> "다운로드"; incognito -> "시크릿 탭"; else -> "새 탭" }
        val tab = BrowserTab(title = tabTitle, url = initialUrl, parentTabId = currentActive, isIncognito = incognito)
        _spaces.update { list ->
            list.map { space ->
                if (space.id == spaceId) {
                    space.copy(tabs = space.tabs + tab, activeTabId = if (selectImmediately) tab.id else space.activeTabId)
                } else space
            }
        }
        if (spaceId == "personal") savePersonalTabs() else saveAiSpaces()
        return tab
    }

    fun reopenLastClosedTab(spaceId: String): Boolean {
        val last = _recentlyClosedTabs.value.firstOrNull() ?: return false
        _recentlyClosedTabs.update { it.drop(1) }
        createTab(spaceId, initialUrl = last.url)
        return true
    }

    fun moveTabById(spaceId: String, fromTabId: String, toTabId: String) {
        if (fromTabId == toTabId) return
        _spaces.update { list ->
            list.map { space ->
                if (space.id == spaceId) {
                    val fromIdx = space.tabs.indexOfFirst { it.id == fromTabId }
                    val toIdx = space.tabs.indexOfFirst { it.id == toTabId }
                    if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                        val mutable = space.tabs.toMutableList()
                        val moved = mutable.removeAt(fromIdx)
                        mutable.add(toIdx, moved)
                        space.copy(tabs = mutable)
                    } else space
                } else space
            }
        }
        if (spaceId == "personal") savePersonalTabs()
    }

    fun switchTabByOffset(spaceId: String, delta: Int) {
        val space = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        if (space.tabs.size <= 1) return
        val idx = space.tabs.indexOfFirst { it.id == space.activeTabId }.coerceAtLeast(0)
        val nextIdx = (idx + delta + space.tabs.size) % space.tabs.size
        selectTab(spaceId, space.tabs[nextIdx].id)
    }

    fun printActiveTab(context: Context, spaceId: String) {
        val space = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        val tab = space.activeTab
        mainHandler.post {
            val wv = webViews[tab.id] ?: return@post
            BrowserActivityBridge.printWebViewToPdf(context, wv, tab.title)
        }
    }

    fun importBookmarks(items: List<BookmarkItem>): Int {
        if (items.isEmpty()) return 0
        val existingUrls = _bookmarks.value.map { it.url }.toSet()
        val fresh = items.filter { it.url !in existingUrls }
        if (fresh.isEmpty()) return 0
        if ("가져온 북마크" !in _bookmarkFolders.value) {
            _bookmarkFolders.update { it + "가져온 북마크" }
        }
        _bookmarks.update { it + fresh }
        saveBookmarks()
        return fresh.size
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
        val closing = targetSpace.tabs.firstOrNull { it.id == tabId }
        if (closing != null && !closing.isIncognito && closing.url.startsWith("http")) {
            _recentlyClosedTabs.update { (listOf(PersistedTab(closing.title, closing.url)) + it).take(15) }
        }
        destroyWebView(tabId)
        _spaces.update { list ->
            list.map { space ->
                if (space.id == spaceId) {
                    val remaining = space.tabs.filterNot { it.id == tabId }
                    val closedTab = space.tabs.firstOrNull { it.id == tabId }
                    val newActive = if (space.activeTabId == tabId) {
                        val parent = closedTab?.parentTabId
                        if (parent != null && remaining.any { it.id == parent }) parent else remaining.last().id
                    } else space.activeTabId
                    space.copy(tabs = remaining, activeTabId = newActive)
                } else space
            }
        }
        if (spaceId == "personal") savePersonalTabs()
    }

    fun getExistingWebView(tabId: String): WebView? = webViews[tabId]

    fun getOrCreateWebView(spaceId: String, tab: BrowserTab): WebView {
        return webViews.getOrPut(tab.id) {
            EgoWebViewFactory.createWebView(
                context = appContext,
                tabId = tab.id,
                adBlockProvider = { _preferences.value.adBlockEnabled },
                darkModeProvider = { shouldDarkenWebsites() },
                desktopModeProvider = { _preferences.value.desktopModeEnabled },
                textZoomProvider = { _preferences.value.textZoom },
                webFontProvider = { _preferences.value.webFontMode },
                onPageStarted = { url ->
                    AdBlockEngine.resetTabCount(tab.id)
                    updateTab(spaceId, tab.id) { it.copy(url = url, isLoading = true) }
                },
                onPageFinished = { url, title, canBack, canForward ->
                    if (url.startsWith("http") && !tab.isIncognito) {
                        _history.update { prev ->
                            (listOf(BookmarkItem(title = title.ifBlank { url }.take(40), url = url)) + prev.filterNot { it.url == url }).take(80)
                        }
                    }
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
                    if (spaceId == "personal") savePersonalTabs()
                },
                onProgressChanged = { progress ->
                    updateTab(spaceId, tab.id) { it.copy(progress = progress, isLoading = progress < 100) }
                },
                onCreatePopup = { resultMsg ->
                    handlePopupWindow(spaceId, resultMsg)
                },
                onCloseWindow = {
                    mainHandler.post { closeTab(spaceId, tab.id) }
                },
                onRenderProcessCrashed = {
                    recoverCrashedWebView(spaceId, tab.id)
                }
            ).apply {
                if (tab.url.startsWith("http://") || tab.url.startsWith("https://")) {
                    loadUrl(tab.url)
                }
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
        val trimmed = rawInput.trim()
        val isIncog = _spaces.value.firstOrNull { it.id == spaceId }?.activeTab?.isIncognito == true
        if (!isIncog && trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.contains(".")) {
            _recentSearches.update { prev -> (listOf(trimmed) + prev.filterNot { it == trimmed }).take(25) }
            saveSearches()
        }
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

    fun openDownloadsTab(spaceId: String) {
        val sp = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        val existing = sp.tabs.firstOrNull { it.url == "ego://downloads" }
        if (existing != null) {
            selectTab(spaceId, existing.id)
        } else {
            createTab(spaceId, initialUrl = "ego://downloads", selectImmediately = true)
        }
    }

    fun getActiveWebView(spaceId: String): WebView? {
        val tabId = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId ?: return null
        return webViews[tabId]
    }

    fun reload(spaceId: String) {
        val tabId = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId ?: return
        mainHandler.post { webViews[tabId]?.reload() }
    }

    fun openExternalUrl(url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        selectSpace("personal")
        val tab = createTab("personal", initialUrl = url, selectImmediately = true)
        BrowserActivityBridge.notifyExternalUrlOpened()
        mainHandler.post {
            val wv = getOrCreateWebView("personal", tab)
            wv.onResume()
            wv.resumeTimers()
            if (wv.url.isNullOrBlank() || wv.url == "about:blank") {
                wv.loadUrl(url)
            }
            wv.requestLayout()
        }
    }

    /**
     * 앱이 백그라운드로 이동할 때 오디오 재생 탭·작업 중인 AI 스페이스를 제외한 모든 WebView 절전
     */
    fun pauseIdleWebViewsForBackground() {
        val keepIds = buildSet {
            _spaces.value.forEach { sp ->
                val isBusy = sp.aiStatus == AiSpaceStatus.WORKING || sp.aiStatus == AiSpaceStatus.THINKING
                sp.tabs.forEach { t ->
                    if ((isBusy && t.id == sp.activeTabId) || (t.isPlayingAudio && !t.isMuted)) {
                        add(t.id)
                    }
                }
            }
        }
        mainHandler.post {
            webViews.forEach { (tabId, wv) ->
                if (tabId !in keepIds) {
                    runCatching { wv.onPause() }
                }
            }
        }
    }

    fun resumeActiveWebView() {
        val activeTabId = getActiveSpace().activeTabId
        mainHandler.post {
            webViews[activeTabId]?.let { wv ->
                wv.onResume()
                wv.resumeTimers()
                wv.requestLayout()
            }
        }
    }

    fun goHome(spaceId: String) {
        val home = _preferences.value.homepageUrl.ifBlank { "ego://newtab" }
        val space = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        val tab = space.activeTab
        if (home == "ego://newtab") {
            updateTab(spaceId, tab.id) {
                it.copy(title = "새 탭", url = "ego://newtab", isLoading = false, progress = 100)
            }
            if (spaceId == "personal") savePersonalTabs()
        } else {
            navigate(spaceId, home)
        }
    }

    fun findInPage(spaceId: String, query: String) {
        val tabId = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId ?: return
        mainHandler.post {
            val wv = webViews[tabId] ?: return@post
            if (query.isBlank()) wv.clearMatches() else wv.findAllAsync(query)
        }
    }

    fun findNextInPage(spaceId: String, forward: Boolean) {
        val tabId = _spaces.value.firstOrNull { it.id == spaceId }?.activeTabId ?: return
        mainHandler.post { webViews[tabId]?.findNext(forward) }
    }

    fun clearHistoryAndCache() {
        _history.value = emptyList()
        mainHandler.post {
            webViews.values.forEach { it.clearCache(true) }
        }
    }

    fun closeAllTabsInSpace(spaceId: String) {
        val targetSpace = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        targetSpace.tabs.forEach { destroyWebView(it.id) }
        val freshTab = BrowserTab(title = "새 탭", url = "ego://newtab")
        _spaces.update { list ->
            list.map { if (it.id == spaceId) it.copy(tabs = listOf(freshTab), activeTabId = freshTab.id) else it }
        }
    }

    fun updatePreferences(transform: (BrowserPreferences) -> BrowserPreferences) {
        val updated = transform(_preferences.value)
        _preferences.value = updated
        savePreferences()
        mainHandler.post {
            webViews.values.forEach { wv ->
                EgoWebViewFactory.applyDarkMode(wv, shouldDarkenWebsites(updated))
                EgoWebViewFactory.applyDesktopMode(wv, updated.desktopModeEnabled)
                EgoWebViewFactory.applyTextZoom(wv, updated.textZoom)
                EgoWebViewFactory.applyWebFont(wv, updated.webFontMode, updated.customFontPath)
                EgoWebViewFactory.applySitePreferences(wv, updated)
            }
        }
    }

    fun addToReadingList(title: String, url: String) {
        addOrUpdateBookmark(id = null, title = title.ifBlank { url }, url = url, folder = "읽기 목록")
    }

    fun handleBackOrCloseTab(spaceId: String): Boolean {
        val space = _spaces.value.firstOrNull { it.id == spaceId } ?: return false
        val tab = space.activeTab
        val wv = webViews[tab.id]
        // 1) Check LIVE WebView back stack (fixes SPA pushState & stale canGoBack state)
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
            updateTab(spaceId, tab.id) {
                it.copy(canGoBack = wv.canGoBack(), canGoForward = wv.canGoForward())
            }
            return true
        }
        // 2) If multiple tabs are open, close current tab and return to previous/parent tab
        if (space.tabs.size > 1) {
            closeTab(spaceId, tab.id)
            return true
        }
        // 3) If on the last tab and NOT on Home/NewTab page yet, return to Home page first!
        if (tab.url != "ego://newtab") {
            updateTab(spaceId, tab.id) {
                it.copy(title = "새 탭", url = "ego://newtab", isLoading = false, canGoBack = false, canGoForward = false)
            }
            if (spaceId == "personal") savePersonalTabs()
            return true
        }
        // 4) Already on single Home page -> let MainActivity handle double-back-to-exit Toast
        return false
    }

    fun addOrUpdateBookmark(id: String?, title: String, url: String, folder: String) {
        val cleanTitle = title.ifBlank { url }.take(40)
        val cleanFolder = folder.ifBlank { "모바일 북마크" }
        if (cleanFolder !in _bookmarkFolders.value) {
            _bookmarkFolders.update { it + cleanFolder }
        }
        _bookmarks.update { list ->
            if (id != null && list.any { it.id == id }) {
                list.map { if (it.id == id) it.copy(title = cleanTitle, url = url, folder = cleanFolder) else it }
            } else {
                list + BookmarkItem(title = cleanTitle, url = url, folder = cleanFolder)
            }
        }
        saveBookmarks()
    }

    fun addBookmarkFolder(name: String) {
        val clean = name.trim()
        if (clean.isBlank() || clean in _bookmarkFolders.value) return
        _bookmarkFolders.update { it + clean }
        saveBookmarks()
    }

    fun removeBookmark(id: String) {
        _bookmarks.update { list -> list.filterNot { it.id == id } }
        saveBookmarks()
    }

    fun moveBookmark(id: String, delta: Int) {
        val current = _bookmarks.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return
        val folder = current[idx].folder
        // 같은 폴더 내 인접 항목과 위치 교환
        val sameFolderIndices = current.indices.filter { current[it].folder == folder }
        val posInFolder = sameFolderIndices.indexOf(idx)
        val targetPosInFolder = posInFolder + delta
        if (targetPosInFolder !in sameFolderIndices.indices) return
        val targetGlobalIdx = sameFolderIndices[targetPosInFolder]
        val item = current[idx]
        current[idx] = current[targetGlobalIdx]
        current[targetGlobalIdx] = item
        _bookmarks.value = current
        saveBookmarks()
    }

    fun removeRecentSearch(query: String) {
        _recentSearches.update { list -> list.filterNot { it == query } }
        saveSearches()
    }

    fun setAiStatus(spaceId: String, status: AiSpaceStatus, summary: String = "") {
        if (status == AiSpaceStatus.WORKING || status == AiSpaceStatus.TAKEOVER_PAUSED || status == AiSpaceStatus.IDLE) {
            aiIdleJobs.remove(spaceId)?.cancel()
        }
        val genericPattern = Regex("^AI Space \\d+$")
        _spaces.update { list ->
            list.map { sp ->
                if (sp.id == spaceId) {
                    val shouldAutoRename = sp.type == SpaceType.AI &&
                        (status == AiSpaceStatus.WORKING || status == AiSpaceStatus.THINKING) &&
                        genericPattern.matches(sp.name)
                    val resolvedName = if (shouldAutoRename) {
                        resolveUniqueAiClientName(_connectedAiClientName.value, sp.id)
                    } else sp.name
                    sp.copy(name = resolvedName, aiStatus = status, lastActionSummary = summary)
                } else sp
            }
        }
    }

    /**
     * MCP 툴 실행 직후 바로 끄지 않고 25초간 '다음 행동을 생각하고 있어요...' 유지 후
     * 새 호출이 없으면 '작업 완료'(3초) -> IDLE(자동 숨김)로 자연스럽게 전환
     */
    fun enterAiThinkingMode(spaceId: String) {
        aiIdleJobs.remove(spaceId)?.cancel()
        val current = _spaces.value.firstOrNull { it.id == spaceId } ?: return
        if (current.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED) return
        setAiStatus(spaceId, AiSpaceStatus.THINKING, "다음 행동을 생각하고 있어요...")
        aiIdleJobs[spaceId] = managerScope.launch {
            delay(25_000L)
            val afterThink = _spaces.value.firstOrNull { it.id == spaceId } ?: return@launch
            if (afterThink.aiStatus == AiSpaceStatus.THINKING) {
                setAiStatus(spaceId, AiSpaceStatus.DONE, "작업 완료")
                delay(3_000L)
                val afterDone = _spaces.value.firstOrNull { it.id == spaceId } ?: return@launch
                if (afterDone.aiStatus == AiSpaceStatus.DONE) {
                    setAiStatus(spaceId, AiSpaceStatus.IDLE, "")
                }
            }
        }
    }

    fun setSpaceAccentColor(spaceId: String, hex: String) {
        _spaces.update { list ->
            list.map {
                if (it.id == spaceId) it.copy(accentColorHex = hex) else it
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

    suspend fun evaluateJsInExactSpace(spaceId: String, js: String, timeoutMs: Long = 15000L): String {
        val space = _spaces.value.firstOrNull { it.id == spaceId } ?: getActiveSpace()
        val tab = space.activeTab
        val deferred = CompletableDeferred<String>()
        mainHandler.post {
            val wv = getOrCreateWebView(space.id, tab)
            wv.evaluateJavascript(js) { raw ->
                deferred.complete(decodeEvaluateJsResult(raw))
            }
        }
        return withTimeout(timeoutMs) { deferred.await() }
    }

    fun toggleAutoTranslateHost(host: String) {
        val clean = host.lowercase().trim()
        if (clean.isBlank()) return
        updatePreferences { prefs ->
            val next = if (clean in prefs.autoTranslateHosts) prefs.autoTranslateHosts - clean else prefs.autoTranslateHosts + clean
            prefs.copy(autoTranslateHosts = next)
        }
    }

    fun togglePinnedTranslateLang(code: String) {
        val clean = code.trim()
        if (clean.isBlank() || clean == "auto") return
        updatePreferences { prefs ->
            val next = if (clean in prefs.pinnedTranslateLangs) {
                prefs.pinnedTranslateLangs - clean
            } else {
                listOf(clean) + prefs.pinnedTranslateLangs
            }
            prefs.copy(pinnedTranslateLangs = next)
        }
    }

    fun captureTabThumbnail(tabId: String): android.graphics.Bitmap? {
        val wv = webViews[tabId] ?: return null
        return EgoWebViewFactory.captureThumbnailBitmap(wv)
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

    suspend fun navigateAndWait(spaceId: String, url: String, waitMs: Long = 4200L) {
        val space = resolveAiSpace(spaceId)
        navigate(space.id, url)
        delay(220L)
        val deadline = System.currentTimeMillis() + waitMs
        while (System.currentTimeMillis() < deadline) {
            val curTab = _spaces.value.firstOrNull { it.id == space.id }?.activeTab
            if (curTab != null && !curTab.isLoading && curTab.progress >= 90) {
                delay(120L)
                return
            }
            delay(100L)
        }
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

    private fun recoverCrashedWebView(spaceId: String, tabId: String) {
        mainHandler.post {
            webViews.remove(tabId)?.let { dead ->
                runCatching {
                    (dead.parent as? android.view.ViewGroup)?.removeView(dead)
                    dead.destroy()
                }
            }
            val space = _spaces.value.firstOrNull { it.id == spaceId } ?: return@post
            val tab = space.tabs.firstOrNull { it.id == tabId } ?: return@post
            // 강제 리컴포지션 유도 및 새 WebView로 자동 복구
            updateTab(spaceId, tabId) { it.copy(isLoading = true, progress = 10) }
            if (tab.url.startsWith("http://") || tab.url.startsWith("https://")) {
                getOrCreateWebView(spaceId, tab).loadUrl(tab.url)
            }
        }
    }

    private fun destroyWebView(tabId: String) {
        mainHandler.post {
            webViews.remove(tabId)?.apply {
                runCatching {
                    (parent as? android.view.ViewGroup)?.removeView(this)
                    onPause()
                    stopLoading()
                    loadUrl("about:blank")
                    clearHistory()
                    removeAllViews()
                    destroy()
                }
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
        return "${_preferences.value.searchEngine.searchUrlPrefix}${android.net.Uri.encode(trimmed)}"
    }

    fun isSystemDarkMode(): Boolean {
        val ctx = appContext ?: return false
        val mask = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mask == Configuration.UI_MODE_NIGHT_YES
    }

    fun isEffectiveDarkMode(prefs: BrowserPreferences = _preferences.value): Boolean {
        return when (prefs.themeMode) {
            ThemeMode.SYSTEM -> isSystemDarkMode()
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    }

    fun shouldDarkenWebsites(prefs: BrowserPreferences = _preferences.value): Boolean {
        return isEffectiveDarkMode(prefs) && prefs.darkenWebsitesWhenPossible
    }

    fun syncWebViewsDarkMode(isDark: Boolean) {
        val shouldDarken = isDark && _preferences.value.darkenWebsitesWhenPossible
        mainHandler.post {
            webViews.values.forEach { wv ->
                EgoWebViewFactory.applyDarkMode(wv, shouldDarken)
            }
        }
    }
}
