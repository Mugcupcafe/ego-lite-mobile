package com.egolite.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.DownloadManager
import android.provider.Settings
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import com.egolite.mobile.R
import com.egolite.mobile.model.ThemeMode
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.tween
import androidx.compose.ui.zIndex
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.egolite.mobile.model.MonitorPosition
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.BrowserScripts
import org.json.JSONObject
import com.egolite.mobile.browser.TranslationManager
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.mcp.McpCommandBridge
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
    val connectedAiName by SpaceManager.connectedAiClientName.collectAsState()
    val prefs by SpaceManager.preferences.collectAsState()
    val bookmarks by SpaceManager.bookmarks.collectAsState()
    val bookmarkFolders by SpaceManager.bookmarkFolders.collectAsState()
    val recentSearches by SpaceManager.recentSearches.collectAsState()
    val history by SpaceManager.history.collectAsState()
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()
    val adBlockState by AdBlockEngine.state.collectAsState()
    val fullscreenVideo by BrowserActivityBridge.fullscreenVideo.collectAsState()
    val isInPip by BrowserActivityBridge.isInPip.collectAsState()
    val longPressTarget by BrowserActivityBridge.longPressTarget.collectAsState()
    val externalOpenSeq by BrowserActivityBridge.externalOpenSeq.collectAsState()
    val recentlyClosed by SpaceManager.recentlyClosedTabs.collectAsState()

    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId } ?: spaces.first()
    val activeTab = activeSpace.activeTab
    val isAnyAiWorking = spaces.any { it.type == SpaceType.AI && it.aiStatus == AiSpaceStatus.WORKING }
    val isMcpDisconnected = runtimeState.connectionState != McpConnectionState.CONNECTED

    var addressInput by remember(activeTab.id, activeTab.url) { mutableStateOf(activeTab.url) }
    var spaceBarExpanded by remember { mutableStateOf(false) }
    var showAiMonitorTray by remember { mutableStateOf(false) }
    var showAiCommandPopup by remember { mutableStateOf(false) }
    var aiPopupSpaceId by remember { mutableStateOf<String?>(null) }
    var aiPopupTabId by remember { mutableStateOf<String?>(null) }
    var recentDropPromptChip by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val mcpConnectedClients by McpCommandBridge.connectedClients.collectAsState()
    val mcpRecentCommands by McpCommandBridge.recentCommands.collectAsState()
    val mcpPendingCount by McpCommandBridge.pendingCount.collectAsState()
    var monitorAutoOpenedByDrag by remember { mutableStateOf(false) }
    var draggingTabId by remember { mutableStateOf<String?>(null) }
    var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
    var hoveredDropSpaceId by remember { mutableStateOf<String?>(null) }
    var hoveredNewSpaceDrop by remember { mutableStateOf(false) }
    var dwellProgress by remember { mutableFloatStateOf(0f) }
    val tabBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val spaceBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var newSpaceDropBounds by remember { mutableStateOf<Rect?>(null) }
    var personalCapsuleBounds by remember { mutableStateOf<Rect?>(null) }
    var rootBoxBounds by remember { mutableStateOf<Rect?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat().coerceAtLeast(with(density) { 28.dp.toPx() })
    var returningSpaceId by remember { mutableStateOf<String?>(null) }
    var showMcpDrawer by remember { mutableStateOf(false) }
    var showChromeSettings by remember { mutableStateOf(false) }
    var showAllTabsDialog by remember { mutableStateOf(false) }
    var showChromeMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showTranslateSettingsDialog by remember { mutableStateOf(false) }
    var showOmniboxOverlay by remember { mutableStateOf(false) }
    var showBookmarkManager by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<BookmarkItem?>(null) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var readerMarkdown by remember { mutableStateOf<String?>(null) }
    var previewPageUrl by remember { mutableStateOf<String?>(null) }
    var hoveredSpaceIndex by remember { mutableStateOf(-1) }
    var hoveredMenuIndex by remember { mutableStateOf(-1) }
    var rootBoxLeftPx by remember { mutableStateOf(0f) }
    var rootBoxTopPx by remember { mutableStateOf(0f) }
    var addressBarLeftPx by remember { mutableStateOf(0f) }
    var addressBarTopPx by remember { mutableStateOf(0f) }
    var addressBarWidthPx by remember { mutableStateOf(0f) }
    var translateState by remember { mutableStateOf(TranslationPopupState()) }
    var findQuery by remember { mutableStateOf("") }
    var pendingCrashLog by remember { mutableStateOf(com.egolite.mobile.CrashLogger.readLastCrash(context)) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val systemDark = isSystemInDarkTheme()
    val isEffectiveDark = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val activeIconColor = MaterialTheme.colorScheme.onSurface
    val mutedIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    val currentHost = remember(activeTab.url) { AdBlockEngine.extractHost(activeTab.url) }
    val isWebPage = activeTab.url != NEW_TAB_URL && (activeTab.url.startsWith("http://") || activeTab.url.startsWith("https://"))
    val pageBlockedCount = adBlockState.perTabCounts[activeTab.id] ?: 0
    val isSiteAdBlockOn = AdBlockEngine.isEnabledForHost(currentHost)
    val canAddAiSpace = spaces.count { it.type == SpaceType.AI } < prefs.maxAiSpaces

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
                statusText = if (ok) "번역됨" else detected,
                detectedLangLabel = if (ok && src == "auto" && detected.isNotBlank() && detected != "auto") {
                    TranslationManager.labelFor(detected)
                } else {
                    "자동 감지"
                }
            )
        }
    }

    // 크롬 태블릿 1:1 문맥 인식 메뉴 (글자 크기 조절 포함)
    val chromeMenuItems = buildList {
        add(
            ChromeMenuItemSpec(
                title = if (isMcpDisconnected) "MCP · 터널 설정 (끊김)" else "MCP · 터널 설정 (연결됨)",
                icon = Icons.Default.Hub,
                accentColor = if (isMcpDisconnected) EgoDangerRed else EgoEmerald,
                bold = true,
                dividerAfter = true,
                onClick = { showMcpDrawer = true }
            )
        )
        add(ChromeMenuItemSpec("새 탭", Icons.Default.AddBox) { SpaceManager.createTab(activeSpace.id) })
        add(ChromeMenuItemSpec("새 창 (AI Space)", Icons.Default.SmartToy, accentColor = EgoEmerald) {
            SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
        })
        add(ChromeMenuItemSpec("새 시크릿 창", Icons.Default.VisibilityOff, dividerAfter = true) {
            SpaceManager.createTab(activeSpace.id, incognito = true)
        })
        add(ChromeMenuItemSpec("방문 기록", Icons.Default.History) { showHistoryDialog = true })
        add(ChromeMenuItemSpec("인터넷 사용 기록 삭제", Icons.Default.DeleteOutline) {
            SpaceManager.clearHistoryAndCache()
            Toast.makeText(context, "인터넷 사용 기록 및 캐시 삭제됨", Toast.LENGTH_SHORT).show()
        })
        add(ChromeMenuItemSpec("사이트 제어", Icons.Default.Tune, dividerAfter = true) {
            if (isWebPage) showAdBlockDialog = true else showChromeSettings = true
        })
        add(ChromeMenuItemSpec("다운로드", Icons.Default.DownloadDone) {
            runCatching {
                context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            }.onFailure {
                Toast.makeText(context, "다운로드 앱을 열 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        })
        add(ChromeMenuItemSpec("북마크", Icons.Default.StarBorder) { showBookmarkManager = true })
        add(ChromeMenuItemSpec("북마크 바 표시", Icons.Default.BookmarkBorder, checked = prefs.showBookmarkBar, isToggleCheckbox = true) {
            SpaceManager.updatePreferences { it.copy(showBookmarkBar = !it.showBookmarkBar) }
        })
        add(ChromeMenuItemSpec("최근 탭", Icons.Default.Devices, dividerAfter = true) {
            if (recentlyClosed.isNotEmpty()) {
                SpaceManager.reopenLastClosedTab(activeSpace.id)
            } else {
                Toast.makeText(context, "최근 닫은 탭이 없습니다", Toast.LENGTH_SHORT).show()
            }
        })
        // 글자 크기 (확대/축소) 인라인 조절 — 크롬 1:1 (- 100% +)
        add(
            ChromeMenuItemSpec(
                title = "확대/축소",
                icon = Icons.Default.ZoomIn,
                dividerAfter = true,
                zoomPercent = prefs.textZoom,
                onZoomOut = {
                    SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom - 10).coerceIn(75, 150)) }
                },
                onZoomReset = {
                    SpaceManager.updatePreferences { it.copy(textZoom = 100) }
                },
                onZoomIn = {
                    SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom + 10).coerceIn(75, 150)) }
                }
            )
        )
        if (isWebPage) {
            add(ChromeMenuItemSpec("공유...", Icons.Default.Share) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                    putExtra(Intent.EXTRA_TEXT, activeTab.url)
                }
                context.startActivity(Intent.createChooser(shareIntent, "링크 공유"))
            })
            add(ChromeMenuItemSpec("링크 복사", Icons.Default.ContentCopy) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("URL", activeTab.url))
                Toast.makeText(context, "링크 복사됨", Toast.LENGTH_SHORT).show()
            })
            add(ChromeMenuItemSpec("다운로드 페이지 (PDF)", Icons.Default.Download) {
                SpaceManager.printActiveTab(context, activeSpace.id)
            })
            add(ChromeMenuItemSpec("페이지에서 찾기", Icons.Default.FindInPage) { showFindBar = true })
            add(ChromeMenuItemSpec("번역...", Icons.Default.Translate) {
                triggerTranslation("auto", prefs.defaultTranslateTarget)
            })
            add(ChromeMenuItemSpec("읽기 모드 표시", Icons.Default.MenuBook) {
                scope.launch {
                    runCatching {
                        val raw = SpaceManager.evaluateJsInExactSpace(activeSpace.id, BrowserScripts.MARKDOWN_EXTRACT_JS)
                        val md = JSONObject(raw).optString("markdown", "")
                        readerMarkdown = md.ifBlank { "본문을 추출할 수 없는 페이지입니다." }
                    }
                }
            })
            add(ChromeMenuItemSpec("홈 화면에 바로가기 추가", Icons.Default.AddToHomeScreen, dividerAfter = true) {
                runCatching {
                    val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(activeTab.url)).apply {
                        setPackage(context.packageName)
                    }
                    val shortcut = ShortcutInfoCompat.Builder(context, "ego_sc_${activeTab.url.hashCode()}")
                        .setShortLabel(activeTab.title.ifBlank { currentHost.ifBlank { "웹 페이지" } }.take(24))
                        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                        .setIntent(launchIntent)
                        .build()
                    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                        Toast.makeText(context, "홈 화면 바로가기 추가 요청됨", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "런처가 바로가기 고정을 지원하지 않습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            add(ChromeMenuItemSpec("데스크톱 사이트", Icons.Default.DesktopWindows, checked = prefs.desktopModeEnabled, isToggleCheckbox = true) {
                SpaceManager.updatePreferences { it.copy(desktopModeEnabled = !it.desktopModeEnabled) }
                SpaceManager.reload(activeSpace.id)
            })
        }
        add(ChromeMenuItemSpec("어두운 테마", Icons.Default.Contrast, checked = isEffectiveDark, isToggleCheckbox = true, dividerAfter = true) {
            val nextDark = !isEffectiveDark
            SpaceManager.updatePreferences {
                it.copy(darkModeEnabled = nextDark, themeMode = if (nextDark) ThemeMode.DARK else ThemeMode.LIGHT)
            }
        })
        add(ChromeMenuItemSpec("설정", Icons.Default.Settings) { showChromeSettings = true })
        add(ChromeMenuItemSpec("ego(lite)를 기본값으로 설정", Icons.Default.Language) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }.onFailure {
                showChromeSettings = true
            }
        })
    }

    // 제미나이 등 외부 앱에서 링크를 열어 진입 시 열려 있던 오버레이를 즉시 닫아 웹화면 노출 보장
    LaunchedEffect(externalOpenSeq) {
        if (externalOpenSeq > 0L) {
            showChromeSettings = false
            showMcpDrawer = false
            showBookmarkManager = false
            showAllTabsDialog = false
            showOmniboxOverlay = false
            showChromeMenu = false
            spaceBarExpanded = false
            showHistoryDialog = false
            showAdBlockDialog = false
            showTranslateSettingsDialog = false
            readerMarkdown = null
            previewPageUrl = null
        }
    }

    LaunchedEffect(activeSpace.id, activeTab.id) {
        translateState = translateState.copy(
            visible = false,
            isTranslating = false,
            isTranslated = false,
            statusText = "",
            detectedLangLabel = "자동 감지"
        )
    }

    LaunchedEffect(activeTab.id, activeTab.url, activeTab.isLoading) {
        addressInput = activeTab.url
        if (!activeTab.isLoading && activeTab.url != NEW_TAB_URL && currentHost.isNotBlank() && currentHost in prefs.autoTranslateHosts) {
            kotlinx.coroutines.delay(350L)
            triggerTranslation(translateState.sourceLang, translateState.targetLang)
        }
    }

    if (isInPip && fullscreenVideo.view != null) {
        val pipVideoView = fullscreenVideo.view
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        ) {
            if (pipVideoView != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            (pipVideoView.parent as? ViewGroup)?.removeView(pipVideoView)
                            addView(
                                pipVideoView,
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
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .onGloballyPositioned { coords ->
                val b = coords.boundsInRoot()
                rootBoxBounds = b
                rootBoxLeftPx = b.left
                rootBoxTopPx = b.top
            }
    ) {
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val activeMonitorPos = if (isLandscape) prefs.monitorPosLandscape else prefs.monitorPosPortrait

        // B 방식 드래그 자동 열기: 세로(TOP)=상태바 60% 이상 0.5초 유지 / 가로(LEFT)=좌측·Personal 캡슐 0.45초 유지
        LaunchedEffect(draggingTabId, showAiMonitorTray, activeMonitorPos) {
            if (draggingTabId == null || showAiMonitorTray) {
                dwellProgress = 0f
                return@LaunchedEffect
            }
            val pillHeightPx = with(density) { 38.dp.toPx() }
            val leftEdgePx = with(density) { 42.dp.toPx() }
            val capsulePadPx = with(density) { 14.dp.toPx() }
            val targetMs = if (activeMonitorPos == MonitorPosition.TOP) 500L else 450L
            var elapsedMs = 0L
            while (draggingTabId != null && !showAiMonitorTray) {
                val ptr = dragPointerRoot
                val rootTop = rootBoxBounds?.top ?: 0f
                val rootLeft = rootBoxBounds?.left ?: 0f
                val inTrigger = if (activeMonitorPos == MonitorPosition.TOP) {
                    val pillTop = ptr.y - pillHeightPx / 2f
                    val statusBarBottom = if (rootTop > 1f) rootTop else statusBarTopPx
                    val overlapRatio = ((statusBarBottom - pillTop) / pillHeightPx).coerceIn(0f, 1f)
                    overlapRatio >= 0.60f
                } else {
                    val inCap = personalCapsuleBounds?.inflate(capsulePadPx)?.contains(ptr) == true
                    val inEdge = ptr.x <= rootLeft + leftEdgePx
                    inCap || inEdge
                }
                if (inTrigger) {
                    elapsedMs += 16L
                    dwellProgress = (elapsedMs.toFloat() / targetMs.toFloat()).coerceIn(0f, 1f)
                    if (elapsedMs >= targetMs) {
                        monitorAutoOpenedByDrag = true
                        showAiMonitorTray = true
                        dwellProgress = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        break
                    }
                } else {
                    elapsedMs = 0L
                    dwellProgress = 0f
                }
                delay(16L)
            }
        }
        val monitorTopOffset = if (showAiMonitorTray && activeMonitorPos == MonitorPosition.TOP) 322.dp else 0.dp
        val workingSpace = spaces.firstOrNull {
            it.type == SpaceType.AI && (it.aiStatus == AiSpaceStatus.WORKING || it.aiStatus == AiSpaceStatus.THINKING)
        }
        val workingAccent = parseSpaceColor(workingSpace?.accentColorHex ?: activeSpace.accentColorHex)
        val tabStartPadding by animateDpAsState(
            targetValue = if (showAiMonitorTray) 0.dp else 72.dp,
            animationSpec = EgoMotion.smoothSpring(),
            label = "tabStripLeftSlide"
        )

        // 메인 뷰포트 양방향 블러 모프 전환 상태 (스페이스 전환 시 부드러운 블러+스케일 진입)
        val mainSwapScale = remember { Animatable(1f) }
        val mainSwapBlur = remember { Animatable(0f) }
        val mainSwapAlpha = remember { Animatable(1f) }
        fun triggerSpaceSwap(targetId: String) {
            if (targetId == activeSpaceId) return
            returningSpaceId = activeSpaceId
            SpaceManager.selectSpace(targetId)
            scope.launch {
                mainSwapScale.snapTo(0.93f)
                mainSwapBlur.snapTo(11f)
                mainSwapAlpha.snapTo(0.32f)
                launch { mainSwapScale.animateTo(1f, animationSpec = EgoMotion.bouncySpring()) }
                launch { mainSwapBlur.animateTo(0f, animationSpec = tween(340)) }
                launch { mainSwapAlpha.animateTo(1f, animationSpec = tween(260)) }
                kotlinx.coroutines.delay(520L)
                returningSpaceId = null
            }
        }

        // 좌측 상단 캡슐 중심에서 시야 밖까지 느리게(2.38s) 퍼져 나가는 원형 링 광효과 상태
        val ringProgress = remember { Animatable(0f) }
        val ringAlpha = remember { Animatable(0f) }
        val ringColor = MaterialTheme.colorScheme.primary
        LaunchedEffect(showAiMonitorTray, activeMonitorPos) {
            if (showAiMonitorTray) {
                ringProgress.snapTo(0.05f)
                ringAlpha.snapTo(0.92f)
                launch {
                    ringProgress.animateTo(
                        targetValue = 2.45f,
                        animationSpec = tween(durationMillis = 2380, easing = CubicBezierEasing(0.18f, 0.65f, 0.35f, 1f))
                    )
                }
                launch {
                    ringAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 2380, easing = CubicBezierEasing(0.18f, 0.65f, 0.35f, 1f))
                    )
                }
            } else {
                ringAlpha.snapTo(0f)
                ringProgress.snapTo(0f)
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT 모드(가로 기본): 좌측 사이드바 AI 라이브 모니터 (상단 페이드 그라데이션 + 링 광효과)
            if (activeMonitorPos == MonitorPosition.LEFT) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .clipToBounds()
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val a = ringAlpha.value
                        val p = ringProgress.value
                        if (a > 0.01f && p > 0f) {
                            val origin = Offset(44.dp.toPx(), 24.dp.toPx())
                            val maxR = (size.height * p).coerceAtLeast(1f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.00f to Color.Transparent,
                                    0.24f to Color.Transparent,
                                    0.38f to ringColor.copy(alpha = a * 0.30f),
                                    0.48f to workingAccent.copy(alpha = a * 0.56f),
                                    0.58f to ringColor.copy(alpha = a * 0.24f),
                                    0.72f to Color.Transparent,
                                    1.00f to Color.Transparent,
                                    center = origin,
                                    radius = maxR
                                ),
                                radius = maxR,
                                center = origin
                            )
                        }
                    }
                    AiLiveMonitorTray(
                        visible = showAiMonitorTray,
                        position = MonitorPosition.LEFT,
                        spaces = spaces,
                        activeSpaceId = activeSpaceId,
                        returningSpaceId = returningSpaceId,
                        connectedAiName = connectedAiName,
                        canAddAiSpace = canAddAiSpace,
                        isTabDragging = draggingTabId != null,
                        hoveredDropSpaceId = hoveredDropSpaceId,
                        isHoveredNewSpaceDrop = hoveredNewSpaceDrop,
                        onRegisterSpaceBounds = { id, rect -> spaceBoundsMap[id] = rect },
                        onRegisterNewSpaceBounds = { rect -> newSpaceDropBounds = rect },
                        onSelectSpace = { id -> triggerSpaceSwap(id) },
                        onMoveSpace = { fromId, toId -> SpaceManager.moveSpaceById(fromId, toId) },
                        onCreateAiSpace = { SpaceManager.createAiSpace()?.let { triggerSpaceSwap(it.id) } },
                        onCollapse = { monitorAutoOpenedByDrag = false; showAiMonitorTray = false }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // 상단 통합 크롬 영역 (TOP 모드: 라이브 모니터 + 탭 바 배경색 통일, 구분선 제거, 광효과는 주소창 위 경계에서 엄격 클리핑)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .clipToBounds()
            ) {
                if (activeMonitorPos == MonitorPosition.TOP) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val a = ringAlpha.value
                        val p = ringProgress.value
                        if (a > 0.01f && p > 0f) {
                            val origin = Offset(44.dp.toPx(), 24.dp.toPx())
                            val maxR = (size.width * p).coerceAtLeast(1f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.00f to Color.Transparent,
                                    0.24f to Color.Transparent,
                                    0.38f to ringColor.copy(alpha = a * 0.30f),
                                    0.48f to workingAccent.copy(alpha = a * 0.56f),
                                    0.58f to ringColor.copy(alpha = a * 0.24f),
                                    0.72f to Color.Transparent,
                                    1.00f to Color.Transparent,
                                    center = origin,
                                    radius = maxR
                                ),
                                radius = maxR,
                                center = origin
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (activeMonitorPos == MonitorPosition.TOP) {
                        AiLiveMonitorTray(
                            visible = showAiMonitorTray,
                            position = MonitorPosition.TOP,
                            spaces = spaces,
                            activeSpaceId = activeSpaceId,
                            returningSpaceId = returningSpaceId,
                            connectedAiName = connectedAiName,
                            canAddAiSpace = canAddAiSpace,
                            isTabDragging = draggingTabId != null,
                            hoveredDropSpaceId = hoveredDropSpaceId,
                            isHoveredNewSpaceDrop = hoveredNewSpaceDrop,
                            onRegisterSpaceBounds = { id, rect -> spaceBoundsMap[id] = rect },
                            onRegisterNewSpaceBounds = { rect -> newSpaceDropBounds = rect },
                            onSelectSpace = { id -> triggerSpaceSwap(id) },
                            onMoveSpace = { fromId, toId -> SpaceManager.moveSpaceById(fromId, toId) },
                            onCreateAiSpace = { SpaceManager.createAiSpace()?.let { triggerSpaceSwap(it.id) } },
                            onCollapse = { monitorAutoOpenedByDrag = false; showAiMonitorTray = false }
                        )
                    }

                    // 1행: 현재 공간의 크롬형 탭 목록 (탭 버튼만 빛 위 앞 레이어에 표시)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(start = 10.dp + tabStartPadding, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var pressedTabIndex by remember(activeSpace.id) { mutableIntStateOf(-1) }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .animateContentSize(animationSpec = EgoMotion.intSizeSpring),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val totalTabs = activeSpace.tabs.size
                            activeSpace.tabs.forEachIndexed { idx, tab ->
                                val isSelected = tab.id == activeSpace.activeTabId
                                val isPressed = pressedTabIndex == idx
                                val isFirst = idx == 0
                                val isLast = idx == totalTabs - 1

                                // M3 Expressive Style 1 (Connected Pill Morph):
                                // 비활성 8dp(양 끝 18dp) <-> 눌림 5dp <-> 활성 완전 알약(18dp)
                                val startRadius by animateDpAsState(
                                    targetValue = when {
                                        isPressed -> 5.dp
                                        isSelected -> 18.dp
                                        isFirst -> 18.dp
                                        else -> 8.dp
                                    },
                                    animationSpec = EgoMotion.bouncySpring(),
                                    label = "tabStartRadius"
                                )
                                val endRadius by animateDpAsState(
                                    targetValue = when {
                                        isPressed -> 5.dp
                                        isSelected -> 18.dp
                                        isLast -> 18.dp
                                        else -> 8.dp
                                    },
                                    animationSpec = EgoMotion.bouncySpring(),
                                    label = "tabEndRadius"
                                )
                                val tabScale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.95f else 1f,
                                    animationSpec = EgoMotion.bouncySpring(),
                                    label = "tabPressScale"
                                )
                                val neighborNudge by animateDpAsState(
                                    targetValue = when {
                                        pressedTabIndex != -1 && idx == pressedTabIndex - 1 -> (-3).dp
                                        pressedTabIndex != -1 && idx == pressedTabIndex + 1 -> 3.dp
                                        else -> 0.dp
                                    },
                                    animationSpec = EgoMotion.bouncySpring(),
                                    label = "tabNeighborNudge"
                                )
                                val extraMargin by animateDpAsState(
                                    targetValue = if (isSelected) 3.dp else 0.dp,
                                    animationSpec = EgoMotion.smoothSpring(),
                                    label = "tabActiveMargin"
                                )
                                val tabBg by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    animationSpec = tween(180),
                                    label = "tabBgAnim"
                                )
                                val tabFg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else activeIconColor
                                val tabShape = RoundedCornerShape(
                                    topStart = startRadius,
                                    bottomStart = startRadius,
                                    topEnd = endRadius,
                                    bottomEnd = endRadius
                                )

                                val isBeingDragged = draggingTabId == tab.id
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = extraMargin)
                                        .onGloballyPositioned { coords ->
                                            tabBoundsMap[tab.id] = coords.boundsInRoot()
                                        }
                                        .graphicsLayer {
                                            scaleX = tabScale
                                            scaleY = tabScale
                                            translationX = neighborNudge.toPx()
                                            alpha = if (isBeingDragged) 0.28f else 1f
                                        }
                                        .height(36.dp)
                                        .widthIn(min = 112.dp, max = 212.dp)
                                        .clip(tabShape)
                                        .background(tabBg)
                                        .pointerInput(tab.id, idx) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    awaitFirstDown(requireUnconsumed = false)
                                                    pressedTabIndex = idx
                                                    waitForUpOrCancellation()
                                                    if (pressedTabIndex == idx) pressedTabIndex = -1
                                                }
                                            }
                                        }
                                        .pointerInput(tab.id, activeSpace.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { startOffset ->
                                                    val origin = tabBoundsMap[tab.id]?.topLeft ?: Offset.Zero
                                                    draggingTabId = tab.id
                                                    dragPointerRoot = origin + startOffset
                                                    hoveredDropSpaceId = null
                                                    hoveredNewSpaceDrop = false
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val curId = draggingTabId ?: return@detectDragGesturesAfterLongPress
                                                    val ptr = dragPointerRoot + dragAmount
                                                    dragPointerRoot = ptr

                                                    if (showAiMonitorTray) {
                                                        val hitSpace = spaceBoundsMap.entries.firstOrNull { (sid, rect) ->
                                                            sid != activeSpaceId && rect.contains(ptr)
                                                        }?.key
                                                        val hitNew = hitSpace == null &&
                                                            canAddAiSpace &&
                                                            newSpaceDropBounds?.inflate(14f)?.contains(ptr) == true
                                                        if (hitSpace != hoveredDropSpaceId || hitNew != hoveredNewSpaceDrop) {
                                                            hoveredDropSpaceId = hitSpace
                                                            hoveredNewSpaceDrop = hitNew
                                                            if (hitSpace != null || hitNew) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                        }
                                                    } else {
                                                        hoveredDropSpaceId = null
                                                        hoveredNewSpaceDrop = false
                                                    }

                                                    // 모니터 드롭 중이 아닐 때 탭바 좌우 순서 이동
                                                    if (hoveredDropSpaceId == null && !hoveredNewSpaceDrop) {
                                                        val targetTabId = tabBoundsMap.entries.firstOrNull { (tid, rect) ->
                                                            tid != curId &&
                                                                ptr.y >= rect.top - 48f &&
                                                                ptr.y <= rect.bottom + 48f &&
                                                                ptr.x >= rect.left &&
                                                                ptr.x <= rect.right
                                                        }?.key
                                                        if (targetTabId != null) {
                                                            SpaceManager.moveTabById(activeSpace.id, curId, targetTabId)
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val curId = draggingTabId
                                                    val dropSpaceId = hoveredDropSpaceId
                                                    val dropNew = hoveredNewSpaceDrop
                                                    val wasAutoOpened = monitorAutoOpenedByDrag
                                                    val draggedTab = activeSpace.tabs.firstOrNull { it.id == curId }
                                                    var didDrop = false

                                                    if (draggedTab != null && dropSpaceId != null) {
                                                        val res = SpaceManager.cloneTabToSpace(
                                                            sourceUrl = draggedTab.url,
                                                            sourceTitle = draggedTab.title,
                                                            targetSpaceId = dropSpaceId
                                                        )
                                                        if (res != null) {
                                                            didDrop = true
                                                            val spName = spaces.firstOrNull { it.id == dropSpaceId }?.name ?: "AI 스페이스"
                                                            val msg = if (res.second) {
                                                                "$spName(대기 중)에 활성 탭으로 복제됨"
                                                            } else {
                                                                "$spName(작업 중)에 백그라운드 탭으로 추가됨"
                                                            }
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            if (serverState.connectionState == McpConnectionState.CONNECTED) {
                                                                val dropInfo = Triple(dropSpaceId, res.first.id, res.first.title)
                                                                recentDropPromptChip = dropInfo
                                                                scope.launch {
                                                                    delay(3600L)
                                                                    if (recentDropPromptChip == dropInfo) recentDropPromptChip = null
                                                                }
                                                            }
                                                        }
                                                    } else if (draggedTab != null && dropNew) {
                                                        val created = SpaceManager.createAiSpaceWithClonedTab(
                                                            sourceUrl = draggedTab.url,
                                                            sourceTitle = draggedTab.title
                                                        )
                                                        if (created != null) {
                                                            didDrop = true
                                                            Toast.makeText(
                                                                context,
                                                                "${created.name}에 첫 페이지로 생성됨",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            if (serverState.connectionState == McpConnectionState.CONNECTED) {
                                                                val firstTab = created.tabs.first()
                                                                val dropInfo = Triple(created.id, firstTab.id, firstTab.title)
                                                                recentDropPromptChip = dropInfo
                                                                scope.launch {
                                                                    delay(3600L)
                                                                    if (recentDropPromptChip == dropInfo) recentDropPromptChip = null
                                                                }
                                                            }
                                                        }
                                                    }

                                                    draggingTabId = null
                                                    hoveredDropSpaceId = null
                                                    hoveredNewSpaceDrop = false
                                                    dwellProgress = 0f

                                                    if (wasAutoOpened) {
                                                        monitorAutoOpenedByDrag = false
                                                        scope.launch {
                                                            delay(if (didDrop) 420L else 100L)
                                                            if (draggingTabId == null) {
                                                                showAiMonitorTray = false
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    val wasAutoOpened = monitorAutoOpenedByDrag
                                                    draggingTabId = null
                                                    hoveredDropSpaceId = null
                                                    hoveredNewSpaceDrop = false
                                                    dwellProgress = 0f
                                                    if (wasAutoOpened) {
                                                        monitorAutoOpenedByDrag = false
                                                        showAiMonitorTray = false
                                                    }
                                                }
                                            )
                                        }
                                        .clickable { SpaceManager.selectTab(activeSpace.id, tab.id) }
                                        .padding(horizontal = if (isSelected) 14.dp else 11.dp),
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
                                    if (tab.isPlayingAudio || tab.isMuted) {
                                        TabEqualizerIcon(
                                            isPlaying = tab.isPlayingAudio,
                                            isMuted = tab.isMuted,
                                            activeColor = MaterialTheme.colorScheme.primary,
                                            mutedColor = if (isSelected) tabFg.copy(alpha = 0.65f) else mutedIconColor,
                                            cutoutBgColor = tabBg,
                                            morphStyle = prefs.equalizerMorphStyle,
                                            onClick = { SpaceManager.toggleTabMute(activeSpace.id, tab.id) }
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = tab.title.ifBlank { tab.url },
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = tabFg,
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
                                        tint = if (isSelected) tabFg.copy(alpha = 0.78f) else mutedIconColor
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
                }
            }

            // 2행: 홈 · 뒤로 · 앞으로 · 새로고침 + 주소창 + 조건부 붉은 MCP 끊김 배지 + 광고차단 · 번역 · 다크모드 · 모든탭보기[N] · 메뉴(⋮)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { SpaceManager.goHome(activeSpace.id) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "홈",
                        tint = activeIconColor
                    )
                }
                val canBackOrClose = activeTab.canGoBack || activeSpace.tabs.size > 1 || activeTab.url != NEW_TAB_URL
                IconButton(
                    onClick = { SpaceManager.handleBackOrCloseTab(activeSpace.id) },
                    enabled = canBackOrClose,
                    modifier = Modifier.size(40.dp)
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
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "앞으로",
                        tint = if (activeTab.canGoForward) activeIconColor else mutedIconColor.copy(alpha = 0.45f)
                    )
                }
                IconButton(
                    onClick = { SpaceManager.reload(activeSpace.id) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = activeIconColor)
                }

                // 주소창 (터치 시 실제 주소창과 정확히 같은 위치·길이로 Omnibox 오버레이 표시)
                val existingBookmark = bookmarks.firstOrNull { it.url == activeTab.url }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            addressBarLeftPx = (pos.x - rootBoxLeftPx).coerceAtLeast(0f)
                            addressBarTopPx = (pos.y - rootBoxTopPx).coerceAtLeast(0f)
                            addressBarWidthPx = coords.size.width.toFloat()
                        }
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

                // MCP 연결 시에만 주소창과 광고차단 사이에 아이콘 전용 AI 명령 버튼 표시
                AnimatedVisibility(
                    visible = serverState.connectionState == McpConnectionState.CONNECTED,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.78f, animationSpec = EgoMotion.bouncySpring()),
                    exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.78f, animationSpec = tween(140))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .pressBounceScale(0.88f)
                                .clip(CircleShape)
                                .background(
                                    if (showAiCommandPopup) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    aiPopupSpaceId = null
                                    aiPopupTabId = null
                                    showAiCommandPopup = !showAiCommandPopup
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI 명령 팝업 열기",
                                tint = if (showAiCommandPopup) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                            if (mcpPendingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 5.dp, end = 5.dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EgoEmerald)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 광고 차단 (짧게: 현재 사이트 ON/OFF + 새로고침, 길게: 크롬 확장 스타일 팝업)
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

                // 크롬 스타일 번역 버튼 (짧게: 즉시 번역/팝업 토글, 길게: 번역 설정 팝업)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                if (translateState.visible) {
                                    translateState = translateState.copy(visible = false)
                                } else {
                                    triggerTranslation(translateState.sourceLang, translateState.targetLang)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showTranslateSettingsDialog = true
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "페이지 번역 (길게 눌러 번역 설정)",
                        tint = if (translateState.isTranslated) MaterialTheme.colorScheme.primary else activeIconColor
                    )
                }

                // 주/야간 다크모드 토글 (아이콘 모프 전환 + Micro-press)
                IconButton(
                    onClick = { val next = !isEffectiveDark; SpaceManager.updatePreferences { it.copy(darkModeEnabled = next, themeMode = if (next) ThemeMode.DARK else ThemeMode.LIGHT) } },
                    modifier = Modifier.size(42.dp).pressBounceScale()
                ) {
                    AnimatedContent(
                        targetState = isEffectiveDark,
                        transitionSpec = {
                            (scaleIn(EgoMotion.bouncySpring(), initialScale = 0.6f) + fadeIn(tween(150)))
                                .togetherWith(scaleOut(tween(120), targetScale = 0.6f) + fadeOut(tween(110)))
                        },
                        label = "themeIconMorph"
                    ) { darkNow ->
                        Icon(
                            imageVector = if (darkNow) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "다크모드 전환",
                            tint = if (darkNow) MaterialTheme.colorScheme.primary else activeIconColor
                        )
                    }
                }

                // 다크모드 우측: 크롬형 모든 탭 보기 버튼 ([N] 사각 카운터)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .pressBounceScale()
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

                // 우측 끝: 크롬형 브라우저 메뉴 (Drag-to-Open & Drag-to-Select 완벽 지원 — pointerInput(Unit))
                val menuItemHeightPx = with(density) { 44.dp.toPx() }
                val menuDragStartPx = with(density) { 26.dp.toPx() }
                val latestShowMenu by rememberUpdatedState(showChromeMenu)
                val latestMenuItems by rememberUpdatedState(chromeMenuItems)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val wasAlreadyOpen = latestShowMenu
                                if (!wasAlreadyOpen) {
                                    showChromeMenu = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                var dragged = false
                                var lastHover = -1
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val itemsNow = latestMenuItems
                                    if (!change.pressed) {
                                        if (dragged && lastHover in itemsNow.indices) {
                                            val chosen = itemsNow[lastHover]
                                            hoveredMenuIndex = -1
                                            if (chosen.zoomPercent == null) {
                                                showChromeMenu = false
                                                chosen.onClick()
                                            }
                                        } else if (dragged || wasAlreadyOpen) {
                                            // 드래그 후 메뉴 밖에서 손을 떼면 선택 없이 자동으로 닫힘
                                            showChromeMenu = false
                                            hoveredMenuIndex = -1
                                        } else {
                                            hoveredMenuIndex = -1
                                        }
                                        break
                                    }
                                    val dy = change.position.y - down.position.y
                                    val dx = change.position.x - down.position.x
                                    val menuWidthPx = with(density) { 260.dp.toPx() }
                                    val totalMenuHeightPx = itemsNow.size * menuItemHeightPx
                                    if (kotlin.math.abs(dy) > menuDragStartPx * 0.5f || kotlin.math.abs(dx) > menuDragStartPx * 0.5f) {
                                        dragged = true
                                    }
                                    val inMenuBounds = dy > menuDragStartPx &&
                                        dy <= (menuDragStartPx + totalMenuHeightPx) &&
                                        dx >= -menuWidthPx &&
                                        dx <= with(density) { 42.dp.toPx() }
                                    if (inMenuBounds) {
                                        val idx = ((dy - menuDragStartPx) / menuItemHeightPx).toInt()
                                        if (idx in itemsNow.indices && idx != lastHover) {
                                            lastHover = idx
                                            hoveredMenuIndex = idx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else if (lastHover != -1) {
                                        lastHover = -1
                                        hoveredMenuIndex = -1
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "브라우저 메뉴",
                        tint = activeIconColor
                    )
                }
            }

            // 페이지에서 찾기 바 (활성화 시 스프링 펼침)
            AnimatedVisibility(
                visible = showFindBar,
                enter = fadeIn(tween(140)) + expandVertically(EgoMotion.intSizeSpring),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
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

            // 3행 (선택): 태블릿 크롬 1:1 북마크 바 (스프링 펼침/접힘)
            AnimatedVisibility(
                visible = prefs.showBookmarkBar,
                enter = fadeIn(tween(150)) + expandVertically(EgoMotion.intSizeSpring),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
                ChromeBookmarkBar(
                    bookmarks = bookmarks,
                    folders = bookmarkFolders,
                    onNavigate = { url -> SpaceManager.navigate(activeSpace.id, url) },
                    onOpenInNewTab = { url -> SpaceManager.createTab(activeSpace.id, initialUrl = url, selectImmediately = true) },
                    onEditBookmark = { bm -> editingBookmark = bm },
                    onMoveBookmark = { id, delta -> SpaceManager.moveBookmark(id, delta) },
                    onDeleteBookmark = { id -> SpaceManager.removeBookmark(id) },
                    onOpenAllBookmarks = { showBookmarkManager = true }
                )
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

            val activeSpaceAccent = parseSpaceColor(activeSpace.accentColorHex)
            // 웹 캔버스 (AI Space일 때 2dp 커스텀 공간 색상 테두리 + 안정적 풀스크린 WebView 호스트)
            val canvasBorder = if (activeSpace.type == SpaceType.AI) {
                Modifier.border(2.dp, activeSpaceAccent, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            } else Modifier

            val mainBlurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainSwapBlur.value > 0.3f) {
                Modifier.blur(mainSwapBlur.value.dp)
            } else Modifier
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = mainSwapScale.value
                        scaleY = mainSwapScale.value
                        alpha = mainSwapAlpha.value
                    }
                    .then(mainBlurMod)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .then(canvasBorder)
            ) {
                WebViewHostContainer(
                    spaces = spaces,
                    activeSpace = activeSpace
                )
                AnimatedVisibility(
                    visible = activeTab.url == NEW_TAB_URL,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(140))
                ) {
                    ChromeNewTabPage(
                        bookmarks = bookmarks,
                        onOpenOmnibox = { showOmniboxOverlay = true },
                        onNavigate = { url -> SpaceManager.navigate(activeSpace.id, url) },
                        onEditBookmark = { bm -> editingBookmark = bm },
                        onAddBookmark = { showAddBookmarkDialog = true }
                    )
                }
                // AI Space 하단 young-newt-20 MCP 동작 표기 캡슐 HUD (비작동 IDLE 시 자동 숨김)
                if (activeSpace.type == SpaceType.AI) {
                    McpStatusCapsuleHud(
                        visible = activeSpace.aiStatus != AiSpaceStatus.IDLE,
                        status = activeSpace.aiStatus,
                        message = activeSpace.lastActionSummary,
                        accentColor = activeSpaceAccent,
                        darkTheme = isEffectiveDark,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    )
                }
            }
            }
        }

        // 최상단 좌측 고정 알약 캡슐 (AI 라이브 모니터가 열려도 좌측 상단에 그대로 유지)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 7.dp)
                .zIndex(15f)
                .onGloballyPositioned { coords ->
                    personalCapsuleBounds = coords.boundsInRoot()
                }
        ) {
            PillSpaceCapsule(
                activeSpace = activeSpace,
                isAnyAiWorking = workingSpace != null,
                workingAccentColor = workingAccent,
                spaceCount = spaces.size,
                canAddAiSpace = canAddAiSpace,
                isExpanded = spaceBarExpanded,
                onSetExpanded = { spaceBarExpanded = it },
                onToggleAiMonitor = {
                    monitorAutoOpenedByDrag = false
                    showAiMonitorTray = !showAiMonitorTray
                },
                onHoverIndexChanged = { hoveredSpaceIndex = it },
                onCommitIndex = { idx ->
                    if (idx in spaces.indices) {
                        SpaceManager.selectSpace(spaces[idx].id)
                    } else if (canAddAiSpace && idx == spaces.size) {
                        SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
                    }
                }
            )
        }

        // 세로모드 상태바 60%+ 0.5초 / 가로모드 좌측 0.45초 자동 열기 진행 게이지
        if (dwellProgress > 0.01f && !showAiMonitorTray) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(dwellProgress)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .zIndex(42f)
            )
        }

        // 길게 눌러 띄운 플로팅 탭 알약 캡슐 오버레이
        val floatingTab = draggingTabId?.let { tid -> activeSpace.tabs.firstOrNull { it.id == tid } }
        if (floatingTab != null) {
            val rootOrigin = rootBoxBounds?.topLeft ?: Offset.Zero
            val localX = (dragPointerRoot.x - rootOrigin.x).roundToInt()
            val localY = (dragPointerRoot.y - rootOrigin.y).roundToInt()
            val inDropMode = hoveredDropSpaceId != null || hoveredNewSpaceDrop
            val badgeText = when {
                hoveredNewSpaceDrop -> "새 스페이스"
                hoveredDropSpaceId != null -> {
                    val targetSp = spaces.firstOrNull { it.id == hoveredDropSpaceId }
                    val isIdle = targetSp?.aiStatus == AiSpaceStatus.IDLE || targetSp?.aiStatus == AiSpaceStatus.DONE
                    if (isIdle) "활성 복제" else "백그라운드"
                }
                else -> null
            }
            val pillHalfW = with(density) { 86.dp.roundToPx() }
            val pillHalfH = with(density) { 19.dp.roundToPx() }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .offset { IntOffset(localX - pillHalfW, localY - pillHalfH) }
                    .height(38.dp)
                    .widthIn(min = 128.dp, max = 224.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .graphicsLayer {
                        val s = if (inDropMode) 0.96f else 1.06f
                        scaleX = s
                        scaleY = s
                    }
                    .zIndex(45f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = floatingTab.title.ifBlank { floatingTab.url },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 탭 드롭 직후 3.5초 미니 칩 (누르면 방금 넘긴 탭이 자동 고정된 AI 명령 팝업 오픈)
        AnimatedVisibility(
            visible = recentDropPromptChip != null && !showAiCommandPopup && serverState.connectionState == McpConnectionState.CONNECTED,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(160)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp, start = 16.dp, end = 16.dp)
                .zIndex(44f)
        ) {
            val info = recentDropPromptChip
            if (info != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .border(1.2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            aiPopupSpaceId = info.first
                            aiPopupTabId = info.second
                            recentDropPromptChip = null
                            showAiCommandPopup = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "방금 넘긴 '${info.third.take(16)}' · 지시 보내기",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 주소창 팝업 형식의 전용 AI 명령 입력 팝업
        Box(modifier = Modifier.fillMaxSize().zIndex(48f)) {
            AiCommandOverlay(
                visible = showAiCommandPopup,
                spaces = spaces,
                activeSpaceId = activeSpaceId,
                preselectedSpaceId = aiPopupSpaceId,
                preselectedTabId = aiPopupTabId,
                connectedClients = mcpConnectedClients,
                defaultAiName = connectedAiName,
                recentCommands = mcpRecentCommands,
                onDismiss = { showAiCommandPopup = false },
                onSubmitCommand = { cmd ->
                    val feedback = McpCommandBridge.enqueueCommand(cmd)
                    Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 알약 캡슐 터치 시 바로 아래로 확장되는 슬라이드 Space 전환 바
        Box(
            modifier = Modifier
                .padding(top = 46.dp)
                .fillMaxWidth()
                .zIndex(14f)
        ) {
            ExpandedSpaceSlideBar(
                visible = spaceBarExpanded,
                spaces = spaces,
                activeSpaceId = activeSpaceId,
                canAddAiSpace = canAddAiSpace,
                hoveredIndex = hoveredSpaceIndex,
                onSelectSpace = { id ->
                    SpaceManager.selectSpace(id)
                    spaceBarExpanded = false
                },
                onCreateAiSpace = {
                    SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
                },
                onCloseAiSpace = { id -> SpaceManager.closeAiSpace(id) },
                onChangeSpaceColor = { id, hex -> SpaceManager.setSpaceAccentColor(id, hex) }
            )
        }

        // 주소창 아래 크롬 스타일 슬림 알약 번역 팝업
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp + monitorTopOffset)
        ) {
            ChromeStyleTranslatePopup(
                state = translateState,
                engine = prefs.translationEngine,
                currentHost = currentHost,
                alwaysTranslateSite = currentHost in prefs.autoTranslateHosts,
                pinnedLangs = prefs.pinnedTranslateLangs,
                onChangeSourceLang = { newSrc -> triggerTranslation(newSrc, translateState.targetLang) },
                onChangeTargetLang = { newDst -> triggerTranslation(translateState.sourceLang, newDst) },
                onTogglePinLang = { code -> SpaceManager.togglePinnedTranslateLang(code) },
                onToggleAlwaysTranslate = {
                    val willEnable = currentHost !in prefs.autoTranslateHosts
                    SpaceManager.toggleAutoTranslateHost(currentHost)
                    Toast.makeText(
                        context,
                        if (willEnable) "$currentHost 항상 자동 번역 켜짐" else "$currentHost 자동 번역 해제됨",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onUndoRestore = {
                    scope.launch {
                        TranslationManager.restoreOriginalInSpace(activeSpace.id)
                        translateState = translateState.copy(isTranslated = false, visible = false)
                    }
                },
                onDismiss = { translateState = translateState.copy(visible = false) }
            )
        }

        // 모바일 크롬 스타일 주소창(Omnibox) 오버레이 (주소창에서 펼쳐지는 스프링 모프)
        AnimatedVisibility(
            visible = showOmniboxOverlay,
            enter = fadeIn(tween(130)) + scaleIn(
                animationSpec = EgoMotion.smoothSpring(),
                initialScale = 0.94f,
                transformOrigin = TransformOrigin(0.5f, 0f)
            ),
            exit = fadeOut(tween(110)) + scaleOut(
                animationSpec = tween(130),
                targetScale = 0.96f,
                transformOrigin = TransformOrigin(0.5f, 0f)
            )
        ) {
            ChromeOmniboxOverlay(
                currentTitle = activeTab.title,
                currentUrl = activeTab.url,
                bookmarks = bookmarks,
                recentSearches = recentSearches,
                anchorLeftPx = addressBarLeftPx,
                anchorTopPx = addressBarTopPx,
                anchorWidthPx = addressBarWidthPx,
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
                onMoveBookmark = { id, delta -> SpaceManager.moveBookmark(id, delta) },
                onDeleteBookmark = { id -> SpaceManager.removeBookmark(id) },
                onAddFolder = { name -> SpaceManager.addBookmarkFolder(name) },
                onDismiss = { showBookmarkManager = false }
            )
        }

        // 번역 버튼 길게 누르기: 번역 설정 팝업
        if (showTranslateSettingsDialog) {
            TranslateSettingsPopupDialog(
                engine = prefs.translationEngine,
                defaultTargetLang = prefs.defaultTranslateTarget,
                offerTranslate = prefs.offerTranslate,
                currentHost = currentHost,
                alwaysTranslateSite = currentHost.isNotBlank() && currentHost in prefs.autoTranslateHosts,
                onChangeEngine = { eng -> SpaceManager.updatePreferences { it.copy(translationEngine = eng) } },
                onChangeDefaultTarget = { lang ->
                    SpaceManager.updatePreferences { it.copy(defaultTranslateTarget = lang) }
                    translateState = translateState.copy(targetLang = lang)
                },
                onToggleOfferTranslate = { offer -> SpaceManager.updatePreferences { it.copy(offerTranslate = offer) } },
                onToggleAlwaysTranslateSite = {
                    if (currentHost.isNotBlank()) {
                        val willEnable = currentHost !in prefs.autoTranslateHosts
                        SpaceManager.toggleAutoTranslateHost(currentHost)
                        Toast.makeText(
                            context,
                            if (willEnable) "$currentHost 항상 자동 번역 켜짐" else "$currentHost 자동 번역 해제됨",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onDismiss = { showTranslateSettingsDialog = false }
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

        // 링크 · 이미지 길게 누르기 크롬 1:1 컨텍스트 메뉴 (주/야간 자동 대응)
        longPressTarget?.let { target ->
            ChromeLongPressContextMenu(
                target = target,
                activeSpaceId = activeSpace.id,
                darkTheme = isEffectiveDark,
                onPreviewPage = { url -> previewPageUrl = url },
                onDismiss = { BrowserActivityBridge.dismissLongPressMenu() }
            )
        }

        // 페이지 미리보기 하단 시트
        previewPageUrl?.let { url ->
            PagePreviewBottomSheet(
                url = url,
                onOpenInNewTab = { targetUrl ->
                    previewPageUrl = null
                    SpaceManager.createTab(activeSpace.id, initialUrl = targetUrl, selectImmediately = true)
                },
                onDismiss = { previewPageUrl = null }
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

        // 우상단 크롬형 Drag-to-Open / Drag-to-Select 브라우저 메뉴 오버레이
        DragToSelectChromeMenuOverlay(
            visible = showChromeMenu,
            items = chromeMenuItems,
            hoveredIndex = hoveredMenuIndex,
            topPadding = 98.dp + monitorTopOffset,
            onDismiss = {
                showChromeMenu = false
                hoveredMenuIndex = -1
            }
        )

        // 우측 MCP / 터널 전용 설정 드로어 (우측 스프링 슬라이드 인)
        AnimatedVisibility(
            visible = showMcpDrawer,
            enter = slideInHorizontally(EgoMotion.intOffsetSpring) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(tween(190)) { it } + fadeOut(tween(150))
        ) {
            McpSettingsDrawer(
                onDismiss = { showMcpDrawer = false }
            )
        }

        // 크롬 태블릿 1:1 2-패널 전체 화면 설정 페이지
        BackHandler(enabled = showMcpDrawer || showOmniboxOverlay || showAllTabsDialog || readerMarkdown != null) {
            when {
                showMcpDrawer -> showMcpDrawer = false
                showOmniboxOverlay -> showOmniboxOverlay = false
                showAllTabsDialog -> showAllTabsDialog = false
                readerMarkdown != null -> readerMarkdown = null
            }
        }

        AnimatedVisibility(
            visible = showChromeSettings,
            enter = slideInHorizontally(EgoMotion.intOffsetSpring) { it / 3 } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(190)) { it / 3 } + fadeOut(tween(160))
        ) {
            ChromeSettingsScreen(
                onOpenMcpDrawer = { showMcpDrawer = true },
                onDismiss = { showChromeSettings = false }
            )
        }

        // 크롬형 전체 화면 썸네일 카드 그리드 모든 탭 보기 (스프링 줌 모프 전환)
        AnimatedVisibility(
            visible = showAllTabsDialog,
            enter = scaleIn(animationSpec = EgoMotion.smoothSpring(), initialScale = 0.90f) + fadeIn(tween(180)),
            exit = scaleOut(animationSpec = tween(150), targetScale = 0.92f) + fadeOut(tween(140))
        ) {
            ChromeTabSwitcherOverlay(
                spaces = spaces,
                activeSpace = activeSpace,
                darkTheme = isEffectiveDark,
                canAddAiSpace = canAddAiSpace,
                onSelectSpaceAndTab = { targetSpaceId, tabId ->
                    SpaceManager.selectSpace(targetSpaceId)
                    SpaceManager.selectTab(targetSpaceId, tabId)
                    showAllTabsDialog = false
                },
                onCloseTab = { targetSpaceId, tabId ->
                    SpaceManager.closeTab(targetSpaceId, tabId)
                },
                onMoveTab = { targetSpaceId, fromId, toId ->
                    SpaceManager.moveTabById(targetSpaceId, fromId, toId)
                },
                onCreateTab = { targetSpaceId, incog ->
                    SpaceManager.selectSpace(targetSpaceId)
                    SpaceManager.createTab(targetSpaceId, incognito = incog)
                    showAllTabsDialog = false
                },
                onCreateAiSpace = {
                    SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
                },
                onCloseAiSpace = { targetSpaceId ->
                    SpaceManager.closeAiSpace(targetSpaceId)
                },
                onCloseAllTabs = { targetSpaceId ->
                    SpaceManager.closeAllTabsInSpace(targetSpaceId)
                    showAllTabsDialog = false
                },
                onClearBrowsingData = {
                    SpaceManager.clearHistoryAndCache()
                    Toast.makeText(context, "인터넷 사용 기록 및 캐시 삭제됨", Toast.LENGTH_SHORT).show()
                },
                onOpenSettings = {
                    showAllTabsDialog = false
                    showChromeSettings = true
                },
                onDismiss = { showAllTabsDialog = false }
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

            swipe.setOnChildScrollUpCallback { _, _ ->
                activeWv.scrollY > 0 ||
                    activeWv.canScrollVertically(-1) ||
                    com.egolite.mobile.browser.EgoWebViewFactory.isInnerScrolled(activeTab.id)
            }
            swipe.setOnRefreshListener {
                activeWv.reload()
                swipe.isRefreshing = false
            }

            val bgAiWebViews = spaces
                .filter { it.id != activeSpace.id }
                .mapNotNull { bgSpace -> SpaceManager.getExistingWebView(bgSpace.activeTab.id) }
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

