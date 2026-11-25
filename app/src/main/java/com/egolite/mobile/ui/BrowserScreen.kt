package com.egolite.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.input.pointer.pointerInput
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.BrowserScripts
import org.json.JSONObject
import com.egolite.mobile.browser.TranslationManager
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.SpaceType
import com.egolite.mobile.model.TranslationPopupState
import com.egolite.mobile.tunnel.EgoForegroundService
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen() {
    val context = LocalContext.current
    val spaces by SpaceManager.spaces.collectAsState()
    val activeSpaceId by SpaceManager.activeSpaceId.collectAsState()
    val prefs by SpaceManager.preferences.collectAsState()
    val bookmarks by SpaceManager.bookmarks.collectAsState()
    val bookmarkFolders by SpaceManager.bookmarkFolders.collectAsState()
    val recentSearches by SpaceManager.recentSearches.collectAsState()
    val history by SpaceManager.history.collectAsState()
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()
    val adBlockState by AdBlockEngine.state.collectAsState()
    val fullscreenVideo by BrowserActivityBridge.fullscreenVideo.collectAsState()
    val longPressTarget by BrowserActivityBridge.longPressTarget.collectAsState()
    val recentlyClosed by SpaceManager.recentlyClosedTabs.collectAsState()

    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId } ?: spaces.first()
    val activeTab = activeSpace.activeTab
    val isAnyAiWorking = spaces.any { it.type == SpaceType.AI && it.aiStatus == AiSpaceStatus.WORKING }
    val isMcpDisconnected = runtimeState.connectionState != McpConnectionState.CONNECTED

    var addressInput by remember(activeTab.id, activeTab.url) { mutableStateOf(activeTab.url) }
    var spaceBarExpanded by remember { mutableStateOf(false) }
    var showMcpDrawer by remember { mutableStateOf(false) }
    var showAllTabsDialog by remember { mutableStateOf(false) }
    var showChromeMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showOmniboxOverlay by remember { mutableStateOf(false) }
    var showBookmarkManager by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<BookmarkItem?>(null) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var readerMarkdown by remember { mutableStateOf<String?>(null) }
    var translateState by remember { mutableStateOf(TranslationPopupState()) }
    var findQuery by remember { mutableStateOf("") }
    var pendingCrashLog by remember { mutableStateOf(com.egolite.mobile.CrashLogger.readLastCrash(context)) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val activeIconColor = MaterialTheme.colorScheme.onSurface
    val mutedIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    val currentHost = remember(activeTab.url) { AdBlockEngine.extractHost(activeTab.url) }
    val pageBlockedCount = adBlockState.perTabCounts[activeTab.id] ?: 0
    val isSiteAdBlockOn = AdBlockEngine.isEnabledForHost(currentHost)

    fun triggerTranslation(src: String, dst: String) {
        if (activeTab.url == NEW_TAB_URL) {
            Toast.makeText(context, "번역할 웹페이지가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        translateState = translateState.copy(
            visible = true,
            isTranslating = true,
            sourceLang = src,
            targetLang = dst,
            statusText = "페이지 번역 중..."
        )
        scope.launch {
            val (ok, detected) = TranslationManager.translatePageInSpace(
                spaceId = activeSpace.id,
                sourceLang = src,
                targetLang = dst,
                engine = prefs.translationEngine,
                onProgress = { msg -> translateState = translateState.copy(statusText = msg) }
            )
            translateState = translateState.copy(
                isTranslating = false,
                isTranslated = ok,
                statusText = if (ok) "페이지 번역됨" else detected,
                detectedLangLabel = if (ok && src == "auto") "감지됨(${TranslationManager.labelFor(detected)})" else "감지된 언어"
            )
        }
    }

    LaunchedEffect(activeSpace.id, activeTab.id) {
        translateState = translateState.copy(
            visible = false,
            isTranslating = false,
            isTranslated = false,
            statusText = "",
            detectedLangLabel = "감지된 언어"
        )
    }

    LaunchedEffect(activeTab.id, activeTab.url, activeTab.isLoading) {
        addressInput = activeTab.url
        if (!activeTab.isLoading && activeTab.url != NEW_TAB_URL && currentHost.isNotBlank() && currentHost in prefs.autoTranslateHosts) {
            triggerTranslation(translateState.sourceLang, translateState.targetLang)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1행: 최상단 — 알약 캡슐(Personal/AI) + 현재 공간의 크롬형 탭 목록만 배치
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillSpaceCapsule(
                    activeSpace = activeSpace,
                    isAnyAiWorking = isAnyAiWorking,
                    onToggleExpand = { spaceBarExpanded = !spaceBarExpanded }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeSpace.tabs.forEach { tab ->
                        val isSelected = tab.id == activeSpace.activeTabId
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .widthIn(min = 110.dp, max = 210.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                .background(
                                    if (prefs.darkModeEnabled) {
                                        if (isSelected) DarkTabSelected else DarkTabUnselected
                                    } else {
                                        if (isSelected) LightTabSelected else LightTabUnselected
                                    }
                                )
                                .clickable { SpaceManager.selectTab(activeSpace.id, tab.id) }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tab.isIncognito) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "시크릿 탭",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = tab.title.ifBlank { tab.url },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = activeIconColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "탭 닫기",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { SpaceManager.closeTab(activeSpace.id, tab.id) },
                                tint = mutedIconColor
                            )
                        }
                    }

                    IconButton(
                        onClick = { SpaceManager.createTab(activeSpace.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "새 탭",
                            tint = activeIconColor
                        )
                    }
                }
            }

            // 2행: 주소창 + 조건부 붉은 MCP 끊김 배지 + 광고차단 · 번역 · 다크모드 · 크롬형 모든탭보기[N] · 크롬형 메뉴(⋮)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canBackOrClose = activeTab.canGoBack || activeSpace.tabs.size > 1
                IconButton(
                    onClick = { SpaceManager.handleBackOrCloseTab(activeSpace.id) },
                    enabled = canBackOrClose,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = if (canBackOrClose) activeIconColor else mutedIconColor.copy(alpha = 0.45f)
                    )
                }
                IconButton(
                    onClick = { SpaceManager.goForward(activeSpace.id) },
                    enabled = activeTab.canGoForward,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "앞으로",
                        tint = if (activeTab.canGoForward) activeIconColor else mutedIconColor.copy(alpha = 0.45f)
                    )
                }
                IconButton(
                    onClick = { SpaceManager.reload(activeSpace.id) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = activeIconColor)
                }

                // 주소창 (터치 시 모바일 크롬과 동일한 Omnibox 오버레이 표시)
                val existingBookmark = bookmarks.firstOrNull { it.url == activeTab.url }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .pointerInput(activeSpace.id) {
                            var dragSum = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragSum > 70f) SpaceManager.switchTabByOffset(activeSpace.id, -1)
                                    else if (dragSum < -70f) SpaceManager.switchTabByOffset(activeSpace.id, 1)
                                    dragSum = 0f
                                },
                                onHorizontalDrag = { _, amount -> dragSum += amount }
                            )
                        }
                        .clickable { showOmniboxOverlay = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (activeTab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Lock,
                        contentDescription = if (activeTab.isIncognito) "시크릿 모드" else null,
                        modifier = Modifier.size(15.dp),
                        tint = if (activeTab.isIncognito) MaterialTheme.colorScheme.primary else mutedIconColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isNewTab = activeTab.url == NEW_TAB_URL
                    Text(
                        text = if (isNewTab) "Google 검색 또는 URL 입력" else activeTab.url,
                        fontSize = 14.sp,
                        color = if (isNewTab) mutedIconColor else activeIconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (existingBookmark != null) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "북마크 추가/수정",
                        tint = if (existingBookmark != null) MaterialTheme.colorScheme.primary else mutedIconColor,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                if (existingBookmark != null) {
                                    editingBookmark = existingBookmark
                                } else {
                                    showAddBookmarkDialog = true
                                }
                            }
                    )
                }

                // 조건부 경고 배지: MCP 연결 끊김일 때만 주소창과 광고차단 버튼 사이에 붉은색으로 표시
                if (isMcpDisconnected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(EgoDangerRed.copy(alpha = 0.12f))
                            .border(1.dp, EgoDangerRed.copy(alpha = 0.45f), CircleShape)
                            .clickable { showMcpDrawer = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = "MCP 연결 끊김",
                            modifier = Modifier.size(15.dp),
                            tint = EgoDangerRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MCP 끊김",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EgoDangerRed
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 광고 차단 (짧게: 현재 사이트 ON/OFF + 새로고침, 길게: uBlock 스타일 상세 팝업)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                if (currentHost.isNotBlank() && adBlockState.globalEnabled) {
                                    AdBlockEngine.toggleWhitelistForHost(currentHost)
                                } else {
                                    val next = !adBlockState.globalEnabled
                                    AdBlockEngine.setGlobalEnabled(next)
                                    SpaceManager.updatePreferences { it.copy(adBlockEnabled = next) }
                                }
                                SpaceManager.reload(activeSpace.id)
                            },
                            onLongClick = { showAdBlockDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "광고 차단 (길게 눌러 상세 설정)",
                        tint = if (isSiteAdBlockOn) MaterialTheme.colorScheme.primary else mutedIconColor
                    )
                    if (isSiteAdBlockOn && pageBlockedCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 2.dp)
                                .clip(CircleShape)
                                .background(EgoDangerRed)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pageBlockedCount.coerceAtMost(99).toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }

                // 크롬 스타일 번역 버튼 (클릭 시 팝업 표시 + 즉시 번역)
                IconButton(
                    onClick = {
                        if (translateState.visible) {
                            translateState = translateState.copy(visible = false)
                        } else {
                            triggerTranslation(translateState.sourceLang, translateState.targetLang)
                        }
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "페이지 번역",
                        tint = if (translateState.isTranslated) MaterialTheme.colorScheme.primary else activeIconColor
                    )
                }

                // 주/야간 다크모드 토글 (주간: 달 아이콘, 야간: 해 아이콘으로 명확히 구분)
                IconButton(
                    onClick = { SpaceManager.updatePreferences { it.copy(darkModeEnabled = !it.darkModeEnabled) } },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (prefs.darkModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "다크모드 전환",
                        tint = if (prefs.darkModeEnabled) MaterialTheme.colorScheme.primary else activeIconColor
                    )
                }

                // 다크모드 우측: 크롬형 모든 탭 보기 버튼 ([N] 사각 카운터)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { showAllTabsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(2.dp, activeIconColor, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeSpace.tabs.size.coerceAtMost(99).toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeIconColor
                        )
                    }
                }

                // 우측 끝: 크롬형 브라우저 메뉴 (최상단: MCP 설정 메뉴)
                Box {
                    IconButton(
                        onClick = { showChromeMenu = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "브라우저 메뉴",
                            tint = activeIconColor
                        )
                    }

                    DropdownMenu(
                        expanded = showChromeMenu,
                        onDismissRequest = { showChromeMenu = false }
                    ) {
                        // 최상단: MCP 설정 메뉴
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isMcpDisconnected) "MCP · 터널 설정 (끊김)" else "MCP · 터널 설정 (연결됨)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMcpDisconnected) EgoDangerRed else EgoEmerald
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = if (isMcpDisconnected) EgoDangerRed else EgoEmerald
                                )
                            },
                            onClick = {
                                showChromeMenu = false
                                showMcpDrawer = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("새 탭") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                SpaceManager.createTab(activeSpace.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("새 시크릿 탭") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                SpaceManager.createTab(activeSpace.id, incognito = true)
                            }
                        )
                        if (recentlyClosed.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("닫은 탭 다시 열기") },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                                onClick = {
                                    showChromeMenu = false
                                    SpaceManager.reopenLastClosedTab(activeSpace.id)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("새 AI Space") },
                            leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = EgoEmerald) },
                            onClick = {
                                showChromeMenu = false
                                SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("북마크 바 표시") },
                            leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                            trailingIcon = {
                                if (prefs.showBookmarkBar) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showChromeMenu = false
                                SpaceManager.updatePreferences { it.copy(showBookmarkBar = !it.showBookmarkBar) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("북마크 관리") },
                            leadingIcon = { Icon(Icons.Default.StarBorder, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                showBookmarkManager = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("방문 기록") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                showHistoryDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("읽기 모드 (본문만 보기)") },
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                scope.launch {
                                    runCatching {
                                        val raw = SpaceManager.evaluateJsInExactSpace(activeSpace.id, BrowserScripts.MARKDOWN_EXTRACT_JS)
                                        val md = JSONObject(raw).optString("markdown", "")
                                        readerMarkdown = md.ifBlank { "본문을 추출할 수 없는 페이지입니다." }
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("인쇄 / PDF 저장") },
                            leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                SpaceManager.printActiveTab(context, activeSpace.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("페이지에서 찾기") },
                            leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                showFindBar = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("공유") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                                    putExtra(Intent.EXTRA_TEXT, activeTab.url)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "링크 공유"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("링크 복사") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("URL", activeTab.url))
                                Toast.makeText(context, "링크 복사됨", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("데스크톱 사이트") },
                            leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                            trailingIcon = {
                                if (prefs.desktopModeEnabled) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showChromeMenu = false
                                SpaceManager.updatePreferences { it.copy(desktopModeEnabled = !it.desktopModeEnabled) }
                                SpaceManager.reload(activeSpace.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("방문 기록 · 캐시 삭제") },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                SpaceManager.clearHistoryAndCache()
                                Toast.makeText(context, "방문 기록 및 캐시 삭제됨", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("설정") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showChromeMenu = false
                                showMcpDrawer = true
                            }
                        )
                    }
                }
            }

            // 페이지에서 찾기 바 (활성화 시)
            if (showFindBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it
                            SpaceManager.findInPage(activeSpace.id, it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = activeIconColor),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (findQuery.isEmpty()) Text("페이지 내 검색어 입력...", fontSize = 14.sp, color = mutedIconColor)
                            inner()
                        }
                    )
                    IconButton(onClick = { SpaceManager.findNextInPage(activeSpace.id, false) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "이전", tint = activeIconColor)
                    }
                    IconButton(onClick = { SpaceManager.findNextInPage(activeSpace.id, true) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "다음", tint = activeIconColor)
                    }
                    IconButton(onClick = {
                        findQuery = ""
                        SpaceManager.findInPage(activeSpace.id, "")
                        showFindBar = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = activeIconColor)
                    }
                }
            }

            // 3행 (선택): 크롬 스타일 주소창 하단 북마크 바 + 우측 "모든 북마크" 버튼
            if (prefs.showBookmarkBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bookmarks.forEach { bm ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .combinedClickable(
                                        onClick = { SpaceManager.navigate(activeSpace.id, bm.url) },
                                        onLongClick = { editingBookmark = bm }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = bm.title,
                                    fontSize = 12.sp,
                                    color = activeIconColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBookmarkManager = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = mutedIconColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("모든 북마크", fontSize = 12.sp, color = activeIconColor)
                    }
                }
            }

            if (activeTab.isLoading && activeTab.progress in 1..99) {
                LinearProgressIndicator(
                    progress = { activeTab.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = if (activeSpace.type == SpaceType.AI) EgoEmerald else MaterialTheme.colorScheme.primary
                )
            }

            // AI Space Takeover 제어권 바
            if (activeSpace.type == SpaceType.AI &&
                (activeSpace.aiStatus == AiSpaceStatus.WORKING || activeSpace.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED)
            ) {
                val isPaused = activeSpace.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EgoEmerald.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPaused) "직접 조작 모드 (AI 일시정지됨)" else "AI 작업 중: ${activeSpace.lastActionSummary}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EgoEmerald
                    )
                    TextButton(onClick = { SpaceManager.toggleTakeover(activeSpace.id) }) {
                        Text(
                            text = if (isPaused) "AI 제어 재개" else "직접 조작 (Takeover)",
                            color = EgoEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 웹 캔버스 (AI Space일 때 2dp 에메랄드 테두리 + 안정적 풀스크린 WebView 호스트)
            val canvasBorder = if (activeSpace.type == SpaceType.AI) {
                Modifier.border(2.dp, EgoEmerald, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .then(canvasBorder)
            ) {
                WebViewHostContainer(
                    spaces = spaces,
                    activeSpace = activeSpace
                )
                if (activeTab.url == NEW_TAB_URL) {
                    ChromeNewTabPage(
                        bookmarks = bookmarks,
                        onOpenOmnibox = { showOmniboxOverlay = true },
                        onNavigate = { url -> SpaceManager.navigate(activeSpace.id, url) },
                        onEditBookmark = { bm -> editingBookmark = bm },
                        onAddBookmark = { showAddBookmarkDialog = true }
                    )
                }
            }
        }

        // 알약 캡슐 터치 시 바로 아래(주소창 줄 위)로 확장되는 슬라이드 Space 전환 바
        Box(
            modifier = Modifier
                .padding(top = 46.dp)
                .fillMaxWidth()
        ) {
            ExpandedSpaceSlideBar(
                visible = spaceBarExpanded,
                spaces = spaces,
                activeSpaceId = activeSpaceId,
                canAddAiSpace = spaces.count { it.type == SpaceType.AI } < prefs.maxAiSpaces,
                onSelectSpace = { id ->
                    SpaceManager.selectSpace(id)
                    spaceBarExpanded = false
                },
                onCreateAiSpace = {
                    SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
                },
                onCloseAiSpace = { id -> SpaceManager.closeAiSpace(id) }
            )
        }

        // 주소창 아래 크롬 스타일 플로팅 번역 팝업
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            ChromeStyleTranslatePopup(
                state = translateState,
                engine = prefs.translationEngine,
                currentHost = currentHost,
                alwaysTranslateSite = currentHost in prefs.autoTranslateHosts,
                onChangeSourceLang = { newSrc -> triggerTranslation(newSrc, translateState.targetLang) },
                onChangeTargetLang = { newDst -> triggerTranslation(translateState.sourceLang, newDst) },
                onToggleAlwaysTranslate = { SpaceManager.toggleAutoTranslateHost(currentHost) },
                onUndoRestore = {
                    scope.launch {
                        TranslationManager.restoreOriginalInSpace(activeSpace.id)
                        translateState = translateState.copy(isTranslated = false, visible = false)
                    }
                },
                onDismiss = { translateState = translateState.copy(visible = false) }
            )
        }

        // 모바일 크롬 스타일 주소창(Omnibox) 오버레이
        if (showOmniboxOverlay) {
            ChromeOmniboxOverlay(
                currentTitle = activeTab.title,
                currentUrl = activeTab.url,
                bookmarks = bookmarks,
                recentSearches = recentSearches,
                onNavigate = { target -> SpaceManager.navigate(activeSpace.id, target) },
                onDismiss = { showOmniboxOverlay = false }
            )
        }

        // 북마크 추가 / 수정 다이얼로그
        if (showAddBookmarkDialog || editingBookmark != null) {
            BookmarkEditDialog(
                existing = editingBookmark,
                initialTitle = if (activeTab.url == NEW_TAB_URL) "" else activeTab.title,
                initialUrl = if (activeTab.url == NEW_TAB_URL) "" else activeTab.url,
                folders = bookmarkFolders,
                onSave = { id, title, url, folder -> SpaceManager.addOrUpdateBookmark(id, title, url, folder) },
                onDelete = { id -> SpaceManager.removeBookmark(id) },
                onAddFolder = { name -> SpaceManager.addBookmarkFolder(name) },
                onDismiss = {
                    showAddBookmarkDialog = false
                    editingBookmark = null
                }
            )
        }

        // 모든 북마크 관리자 다이얼로그
        if (showBookmarkManager) {
            BookmarkManagerDialog(
                bookmarks = bookmarks,
                folders = bookmarkFolders,
                onOpenUrl = { url -> SpaceManager.navigate(activeSpace.id, url) },
                onEditBookmark = { bm ->
                    showBookmarkManager = false
                    editingBookmark = bm
                },
                onDeleteBookmark = { id -> SpaceManager.removeBookmark(id) },
                onAddFolder = { name -> SpaceManager.addBookmarkFolder(name) },
                onDismiss = { showBookmarkManager = false }
            )
        }

        // 방패 길게 누르기: uBlock 스타일 광고차단 상세 팝업
        if (showAdBlockDialog) {
            AdBlockControlDialog(
                currentHost = currentHost,
                activeTabId = activeTab.id,
                onReloadPage = { SpaceManager.reload(activeSpace.id) },
                onDismiss = { showAdBlockDialog = false }
            )
        }

        // 링크 · 이미지 길게 누르기 컨텍스트 메뉴
        longPressTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { BrowserActivityBridge.dismissLongPressMenu() },
                title = { Text(target.linkUrl ?: target.imageUrl ?: "항목 작업", maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!target.linkUrl.isNullOrBlank()) {
                            TextButton(onClick = {
                                SpaceManager.createTab(activeSpace.id, initialUrl = target.linkUrl, selectImmediately = true)
                                BrowserActivityBridge.dismissLongPressMenu()
                            }) { Text("새 탭에서 열기") }
                            TextButton(onClick = {
                                SpaceManager.createTab(activeSpace.id, initialUrl = target.linkUrl, selectImmediately = false)
                                BrowserActivityBridge.dismissLongPressMenu()
                            }) { Text("백그라운드 탭으로 열기") }
                            TextButton(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Link", target.linkUrl))
                                Toast.makeText(context, "링크 복사됨", Toast.LENGTH_SHORT).show()
                                BrowserActivityBridge.dismissLongPressMenu()
                            }) { Text("링크 주소 복사") }
                        }
                        if (!target.imageUrl.isNullOrBlank()) {
                            TextButton(onClick = {
                                BrowserActivityBridge.enqueueDownload(context, target.imageUrl)
                                BrowserActivityBridge.dismissLongPressMenu()
                            }) { Text("이미지 다운로드") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { BrowserActivityBridge.dismissLongPressMenu() }) { Text("닫기") }
                }
            )
        }

        // 읽기 모드 (Reader Mode) 오버레이
        readerMarkdown?.let { md ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("읽기 모드", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { readerMarkdown = null }) {
                            Icon(Icons.Default.Close, contentDescription = "닫기")
                        }
                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = md,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }

        // 동영상 전체화면 (YouTube 등) 오버레이
        fullscreenVideo.view?.let { videoView ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            (videoView.parent as? ViewGroup)?.removeView(videoView)
                            addView(
                                videoView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    }
                )
            }
        }

        // 우측 MCP / 터널 / 브라우저 설정 드로어
        if (showMcpDrawer) {
            McpSettingsDrawer(
                onDismiss = { showMcpDrawer = false }
            )
        }

        // 크롬형 모든 탭 보기 다이얼로그 (현재 공간 + 다른 Space 탭 전환)
        if (showAllTabsDialog) {
            AlertDialog(
                onDismissRequest = { showAllTabsDialog = false },
                title = { Text("${activeSpace.name} 탭 (${activeSpace.tabs.size})") },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeSpace.tabs, key = { it.id }) { tab ->
                            val selected = tab.id == activeSpace.activeTabId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SpaceManager.selectTab(activeSpace.id, tab.id)
                                        showAllTabsDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (tab.isIncognito) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = "시크릿 탭",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tab.title.ifBlank { tab.url },
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tab.url,
                                            fontSize = 11.sp,
                                            color = mutedIconColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { SpaceManager.closeTab(activeSpace.id, tab.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = mutedIconColor)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        SpaceManager.createTab(activeSpace.id)
                        showAllTabsDialog = false
                    }) {
                        Text("+ 새 탭")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            SpaceManager.closeAllTabsInSpace(activeSpace.id)
                            showAllTabsDialog = false
                        }) {
                            Text("모두 닫기", color = EgoDangerRed)
                        }
                        TextButton(onClick = { showAllTabsDialog = false }) {
                            Text("닫기")
                        }
                    }
                }
            )
        }

        // 방문 기록 다이얼로그
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = { Text("방문 기록") },
                text = {
                    if (history.isEmpty()) {
                        Text("방문 기록이 없습니다.", color = mutedIconColor)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(history, key = { it.id }) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            SpaceManager.navigate(activeSpace.id, item.url)
                                            showHistoryDialog = false
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text(item.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.url, fontSize = 11.sp, color = mutedIconColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistoryDialog = false }) { Text("닫기") }
                }
            )
        }

        // 이전 실행 크래시 로그 팝업 (튕김 발생 시 다음 실행에서 즉시 확인·복사 가능)
        pendingCrashLog?.let { crashText ->
            AlertDialog(
                onDismissRequest = {
                    com.egolite.mobile.CrashLogger.clearLastCrash(context)
                    pendingCrashLog = null
                },
                title = { Text("이전 종료 크래시 로그") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = crashText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("CrashLog", crashText))
                        Toast.makeText(context, "크래시 로그 복사됨", Toast.LENGTH_SHORT).show()
                        com.egolite.mobile.CrashLogger.clearLastCrash(context)
                        pendingCrashLog = null
                    }) {
                        Text("로그 복사")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        com.egolite.mobile.CrashLogger.clearLastCrash(context)
                        pendingCrashLog = null
                    }) {
                        Text("닫기")
                    }
                }
            )
        }
    }
}

@Composable
private fun WebViewHostContainer(
    spaces: List<BrowserSpace>,
    activeSpace: BrowserSpace
) {
    val activeTab = activeSpace.activeTab
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val host = FrameLayout(ctx).apply {
                tag = "webview_host"
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            SwipeRefreshLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(host)
            }
        },
        update = { swipe ->
            val host = swipe.findViewWithTag<FrameLayout>("webview_host") ?: return@AndroidView
            val activeWv = SpaceManager.getOrCreateWebView(activeSpace.id, activeTab)

            swipe.setOnChildScrollUpCallback { _, _ -> activeWv.scrollY > 0 }
            swipe.setOnRefreshListener {
                activeWv.reload()
                swipe.isRefreshing = false
            }

            val bgAiWebViews = spaces
                .filter { it.type == SpaceType.AI && it.id != activeSpace.id }
                .map { bgSpace -> SpaceManager.getOrCreateWebView(bgSpace.id, bgSpace.activeTab) }
                .toSet()

            if (activeWv.parent !== host) {
                (activeWv.parent as? ViewGroup)?.removeView(activeWv)
                host.addView(
                    activeWv,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            } else if (activeWv.layoutParams?.width != ViewGroup.LayoutParams.MATCH_PARENT ||
                activeWv.layoutParams?.height != ViewGroup.LayoutParams.MATCH_PARENT
            ) {
                activeWv.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            bgAiWebViews.forEach { bgWv ->
                if (bgWv.parent !== host) {
                    (bgWv.parent as? ViewGroup)?.removeView(bgWv)
                    host.addView(
                        bgWv,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }

            for (i in 0 until host.childCount) {
                val child = host.getChildAt(i)
                when {
                    child === activeWv -> {
                        child.alpha = 1f
                        child.visibility = View.VISIBLE
                    }
                    child in bgAiWebViews -> {
                        child.alpha = 0f
                        child.visibility = View.INVISIBLE
                    }
                    else -> {
                        child.alpha = 0f
                        child.visibility = View.GONE
                    }
                }
            }
            activeWv.bringToFront()
            activeWv.requestLayout()
            host.requestLayout()
        }
    )
}

