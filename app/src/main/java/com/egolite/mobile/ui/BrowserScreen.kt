package com.egolite.mobile.ui

import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalView
import android.view.RoundedCorner
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.toArgb
import com.egolite.mobile.model.TabWidthMode
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.delay
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
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.key
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import android.view.ViewConfiguration
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import com.egolite.mobile.model.CardVanishStyle
import com.egolite.mobile.model.ProgressBarStyle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
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
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.egolite.mobile.account.GoogleAccountManager
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.DownloadCenter
import com.egolite.mobile.browser.ExtensionManager
import com.egolite.mobile.browser.InstalledExtension
import com.egolite.mobile.browser.FaviconCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawOutline
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
    var dragStartTabIndex by remember { mutableIntStateOf(-1) }
    var dragTargetTabIndex by remember { mutableIntStateOf(-1) }
    var draggedTabWidthPx by remember { mutableFloatStateOf(160f) }
    var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
    var hoveredDropSpaceId by remember { mutableStateOf<String?>(null) }
    var hoveredNewSpaceDrop by remember { mutableStateOf(false) }
    var dwellProgress by remember { mutableFloatStateOf(0f) }
    // 발열/리컴포지션 최적화: 좌표 맵은 UI 렌더링에 직접 읽히지 않으므로 일반 HashMap으로 유지
    val tabBoundsMap = remember { HashMap<String, Rect>() }
    val spaceBoundsMap = remember { HashMap<String, Rect>() }
    var newSpaceDropBounds by remember { mutableStateOf<Rect?>(null) }
    var personalCapsuleBounds by remember { mutableStateOf<Rect?>(null) }
    var rootBoxBounds by remember { mutableStateOf<Rect?>(null) }
    var contentAspectRatio by remember { mutableFloatStateOf(1.35f) }
    // statusBarTopPx는 아래 density 선언 이후 초기화 (rootBoxTopPx 우선 사용)
    var returningSpaceId by remember { mutableStateOf<String?>(null) }
    var showMcpDrawer by remember { mutableStateOf(false) }
    var showChromeSettings by remember { mutableStateOf(false) }
    var omniTabDragStep by remember { mutableStateOf<Int?>(null) }
    val activeTabIdxNow = activeSpace.tabs.indexOfFirst { it.id == activeTab.id }.coerceAtLeast(0)
    val installedExtensions by ExtensionManager.extensions.collectAsState()
    var activeExtPopup by remember { mutableStateOf<InstalledExtension?>(null) }
    var managingExt by remember { mutableStateOf<InstalledExtension?>(null) }
    var showDownloadsScreen by remember { mutableStateOf(false) }
    val downloadBanner by DownloadCenter.banner.collectAsState()
    val closedTabUndo by SpaceManager.closedTabUndoBanner.collectAsState()
    val duplicateDownloadPrompt by DownloadCenter.duplicatePrompt.collectAsState()
    var settingsInitialCategory by remember { mutableStateOf(SettingsCategory.ACCOUNT) }
    val googleAccount by GoogleAccountManager.state.collectAsState()
    val completedDoneIds by SpaceManager.completedDoneSpaceIds.collectAsState()
    val googleAvatar by GoogleAccountManager.avatarBitmap.collectAsState()
    var showAllTabsDialog by remember { mutableStateOf(false) }
    var showChromeMenu by remember { mutableStateOf(false) }
    var isQuickMenuEditMode by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showTranslateSettingsDialog by remember { mutableStateOf(false) }
    var mcpAnchorRect by remember { mutableStateOf<Rect?>(null) }
    var adBlockAnchorRect by remember { mutableStateOf<Rect?>(null) }
    var translateAnchorRect by remember { mutableStateOf<Rect?>(null) }
    val adBlockPulseAnim = remember { Animatable(1f) }
    val adBlockWaveAnim = remember { Animatable(0f) }
    val translateFlipAnim = remember { Animatable(0f) }
    val translateScaleAnim = remember { Animatable(1f) }
    var globalDustBursts by remember { mutableStateOf<List<DustVanishRequest>>(emptyList()) }
    var vanishingTopTabIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var collapsingTopTabIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var nextDustBurstId by remember { mutableStateOf(1000L) }
    var showOmniboxOverlay by remember { mutableStateOf(false) }
    var showBookmarkManager by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<BookmarkItem?>(null) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var readerMarkdown by remember { mutableStateOf<String?>(null) }
    var previewPageUrl by remember { mutableStateOf<String?>(null) }
    var hoveredSpaceIndex by remember { mutableStateOf(-1) }
    var hoveredQuickMenuKey by remember { mutableStateOf<String?>(null) }
    val quickMenuTileBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootBoxLeftPx by remember { mutableStateOf(0f) }
    var rootBoxTopPx by remember { mutableStateOf(0f) }

    fun emitDustBurst(
        rootRect: Rect?,
        accent: Color,
        densityFactor: Float = 0.85f,
        touchRoot: Offset? = null,
        capturedBitmap: android.graphics.Bitmap? = null
    ) {
        if (prefs.cardVanishStyle == CardVanishStyle.OFF || rootRect == null) return
        if (rootRect.width <= 8f || rootRect.height <= 8f) return
        val rawLocal = rootRect.translate(-rootBoxLeftPx, -rootBoxTopPx)
        // 보정: 진입 모핑(scale) 중 측정된 축소 rect 대신 캡처된 실제 비트맵 1:1 픽셀 크기 우선 적용
        val localRect = if (capturedBitmap != null && capturedBitmap.width > 16 && capturedBitmap.height > 16) {
            val bw = capturedBitmap.width.toFloat()
            val bh = capturedBitmap.height.toFloat()
            if (rawLocal.width < bw * 0.92f || rawLocal.height < bh * 0.92f) {
                Rect(
                    left = rawLocal.left,
                    top = rawLocal.top,
                    right = rawLocal.left + bw,
                    bottom = rawLocal.top + bh
                )
            } else rawLocal
        } else rawLocal
        val localOriginX = touchRoot?.let { it.x - rootBoxLeftPx } ?: -1f
        val localOriginY = touchRoot?.let { it.y - rootBoxTopPx } ?: -1f
        val id = nextDustBurstId++
        val count = (prefs.vanishParticleCount * densityFactor).toInt().coerceIn(400, 14000)
        globalDustBursts = globalDustBursts + DustVanishRequest(
            id = id,
            rect = localRect,
            accentColor = accent,
            thumbnail = capturedBitmap,
            style = prefs.cardVanishStyle,
            particleCount = count,
            durationMs = prefs.vanishDurationMs.coerceIn(520, 1600),
            originX = localOriginX,
            originY = localOriginY,
            maxFps = prefs.vanishMaxFps,
            direction = prefs.vanishDirection,
            edgeGlow = prefs.vanishEdgeGlow,
            curlStrength = prefs.vanishCurlStrength,
            shockwaveStrength = prefs.vanishShockwaveStrength,
            airDrag = prefs.vanishAirDrag,
            additiveGlow = prefs.vanishAdditiveGlow,
            zPerspective = prefs.vanishZPerspective,
            sizeAdaptiveDuration = prefs.vanishSizeAdaptiveDuration,
            sandRibbonDensity = prefs.sandRibbonDensity,
            sandSilverRatio = prefs.sandSilverRatio,
            sandWindStrength = prefs.sandWindStrength,
            sandSwirlStrength = prefs.sandSwirlStrength
        )
    }

    fun emitDustSnapshot(snap: VanishSnapshot, accent: Color, densityFactor: Float = 0.85f) {
        emitDustBurst(snap.rect, accent, densityFactor, snap.touchRoot, snap.bitmap)
    }
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
    val statusBarTopPx = with(density) { 28.dp.toPx() }

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
            progress = 0.06f,
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
                onProgress = { msg -> translateState = translateState.copy(statusText = msg) },
                onProgressFraction = { frac -> translateState = translateState.copy(progress = frac) }
            )
            translateState = translateState.copy(
                isTranslating = false,
                isTranslated = ok,
                progress = if (ok) 1f else 0f,
                statusText = if (ok) "번역됨" else detected,
                detectedLangLabel = if (ok && src == "auto" && detected.isNotBlank() && detected != "auto") {
                    TranslationManager.labelFor(detected)
                } else {
                    "자동 감지"
                }
            )
        }
    }

    val chromeMenuItems = buildBrowserChromeMenuItems(
        context = context,
        scope = scope,
        activeSpace = activeSpace,
        activeTab = activeTab,
        prefs = prefs,
        isMcpDisconnected = isMcpDisconnected,
        isWebPage = isWebPage,
        isEffectiveDark = isEffectiveDark,
        currentHost = currentHost,
        recentlyClosedEmpty = recentlyClosed.isEmpty(),
        onShowMcpDrawer = { showMcpDrawer = true },
        onShowAdBlockDialog = { showAdBlockDialog = true },
        onShowChromeSettings = { showChromeSettings = true },
        onShowBookmarkManager = { showBookmarkManager = true },
        onShowFindBar = { showFindBar = true },
        onTriggerTranslation = { src, dst -> triggerTranslation(src, dst) },
        onSetReaderMarkdown = { md -> readerMarkdown = md }
    )

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
                if (rootBoxBounds != b) {
                    rootBoxBounds = b
                    rootBoxLeftPx = b.left
                    rootBoxTopPx = b.top
                }
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
        val buddySlotWidth = buddySlotWidthDp(spaces)
        val tabStartPadding by animateDpAsState(
            targetValue = if (showAiMonitorTray) 0.dp else (buddySlotWidth + 4.dp),
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 240f),
            label = "tabStripLeftSlide"
        )

        // 메인 뷰포트 양방향 컨테이너 모프 전환:
        // WebView Surface의 graphicsLayer 제약을 우회하기 위해 떠나는 화면의 비트맵 스냅샷을 순수 Compose Image로 모프 축소
        var swapOutgoingBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val outScale = remember { Animatable(1f) }
        val outAlpha = remember { Animatable(0f) }
        val outOffsetX = remember { Animatable(0f) }
        val outOffsetY = remember { Animatable(0f) }
        val outCorner = remember { Animatable(16f) }

        val mainSwapScale = remember { Animatable(1f) }
        val mainSwapBlur = remember { Animatable(0f) }
        val mainSwapAlpha = remember { Animatable(1f) }
        val mainSwapOffsetX = remember { Animatable(0f) }
        val mainSwapOffsetY = remember { Animatable(0f) }
        val mainSwapCorner = remember { Animatable(16f) }

        fun triggerSpaceSwap(targetId: String) {
            if (targetId == activeSpaceId) return
            val fromLeft = activeMonitorPos == MonitorPosition.LEFT
            val shiftPx = with(density) { 120.dp.toPx() }
            val oldTabId = activeSpace.activeTab.id
            swapOutgoingBitmap = SpaceManager.captureTabThumbnail(oldTabId)
            returningSpaceId = activeSpaceId

            // 새 공간으로 즉시 전환하면서 떠나는 스냅샷은 모니터로 빨려들어가고, 새 공간은 모니터에서 펼쳐짐
            SpaceManager.selectSpace(targetId)
            scope.launch {
                outScale.snapTo(1f)
                outAlpha.snapTo(1f)
                outOffsetX.snapTo(0f)
                outOffsetY.snapTo(0f)
                outCorner.snapTo(16f)

                mainSwapScale.snapTo(0.72f)
                mainSwapBlur.snapTo(12f)
                mainSwapAlpha.snapTo(0.25f)
                mainSwapCorner.snapTo(36f)
                if (fromLeft) {
                    mainSwapOffsetX.snapTo(-shiftPx * 0.7f)
                    mainSwapOffsetY.snapTo(0f)
                } else {
                    mainSwapOffsetY.snapTo(-shiftPx * 0.7f)
                    mainSwapOffsetX.snapTo(0f)
                }

                // 떠나는 스냅샷 → 모니터 방향으로 모프 축소 (320ms)
                launch { outScale.animateTo(0.46f, animationSpec = EgoMotion.morphSpring()) }
                launch { outCorner.animateTo(40f, animationSpec = tween(260)) }
                launch { outAlpha.animateTo(0f, animationSpec = tween(280)) }
                if (fromLeft) {
                    launch { outOffsetX.animateTo(-shiftPx * 1.4f, animationSpec = EgoMotion.morphSpring()) }
                } else {
                    launch { outOffsetY.animateTo(-shiftPx * 1.4f, animationSpec = EgoMotion.morphSpring()) }
                }

                // 들어오는 새 공간 → 모니터에서 탄성 있게 모프 확장 (~360ms)
                launch { mainSwapScale.animateTo(1f, animationSpec = EgoMotion.morphSpring()) }
                launch { mainSwapOffsetX.animateTo(0f, animationSpec = EgoMotion.morphSpring()) }
                launch { mainSwapOffsetY.animateTo(0f, animationSpec = EgoMotion.morphSpring()) }
                launch { mainSwapCorner.animateTo(16f, animationSpec = EgoMotion.morphSpring()) }
                launch { mainSwapBlur.animateTo(0f, animationSpec = tween(360)) }
                launch { mainSwapAlpha.animateTo(1f, animationSpec = tween(280)) }

                kotlinx.coroutines.delay(460L)
                swapOutgoingBitmap = null
                returningSpaceId = null
            }
        }

        val ringColor = MaterialTheme.colorScheme.primary

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT 모드(가로 기본): 좌측 사이드바 AI 라이브 모니터 (상단 페이드 그라데이션 + 링 광효과)
            if (activeMonitorPos == MonitorPosition.LEFT) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .clipToBounds()
                ) {
                    AiLiveMonitorTray(
                        visible = showAiMonitorTray,
                        position = MonitorPosition.LEFT,
                        spaces = spaces,
                        activeSpaceId = activeSpaceId,
                        returningSpaceId = returningSpaceId,
                        connectedAiName = connectedAiName,
                        canAddAiSpace = canAddAiSpace,
                        contentAspectRatio = contentAspectRatio,
                        isTabDragging = draggingTabId != null,
                        hoveredDropSpaceId = hoveredDropSpaceId,
                        isHoveredNewSpaceDrop = hoveredNewSpaceDrop,
                        onRegisterSpaceBounds = { id, rect -> spaceBoundsMap[id] = rect },
                        onRegisterNewSpaceBounds = { rect -> newSpaceDropBounds = rect },
                        onSelectSpace = { id -> triggerSpaceSwap(id) },
                        onCloseSpace = { id -> SpaceManager.closeAiSpace(id) },
                        onMoveSpace = { fromId, toId -> SpaceManager.moveSpaceById(fromId, toId) },
                        onCreateAiSpace = { SpaceManager.createAiSpace() },
                        onGlobalDustVanish = { snap, accent -> emitDustSnapshot(snap, accent, 1.0f) },
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
                            contentAspectRatio = contentAspectRatio,
                            isTabDragging = draggingTabId != null,
                            hoveredDropSpaceId = hoveredDropSpaceId,
                            isHoveredNewSpaceDrop = hoveredNewSpaceDrop,
                            onRegisterSpaceBounds = { id, rect -> spaceBoundsMap[id] = rect },
                            onRegisterNewSpaceBounds = { rect -> newSpaceDropBounds = rect },
                            onSelectSpace = { id -> triggerSpaceSwap(id) },
                            onCloseSpace = { id -> SpaceManager.closeAiSpace(id) },
                            onMoveSpace = { fromId, toId -> SpaceManager.moveSpaceById(fromId, toId) },
                            onCreateAiSpace = { SpaceManager.createAiSpace() },
                            onGlobalDustVanish = { snap, accent -> emitDustSnapshot(snap, accent, 1.0f) },
                            onCollapse = { monitorAutoOpenedByDrag = false; showAiMonitorTray = false }
                        )
                    }

                    // 1행: 현재 공간의 크롬형 탭 목록 (탭 버튼만 빛 위 앞 레이어에 표시)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(start = (10.dp + tabStartPadding).coerceAtLeast(0.dp), end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var pressedTabIndex by remember(activeSpace.id) { mutableIntStateOf(-1) }
                        val tabStripScrollState = rememberScrollState()
                        var tabStripViewportWidthPx by remember { mutableFloatStateOf(0f) }
                        val tabLocalXMap = remember(activeSpace.id) { mutableStateMapOf<String, Pair<Float, Float>>() }
                        val omniTargetTabIdx = omniTabDragStep?.let { step ->
                            (activeTabIdxNow + step).coerceIn(0, (activeSpace.tabs.size - 1).coerceAtLeast(0))
                        }
                        LaunchedEffect(omniTargetTabIdx, activeSpace.id) {
                            val tIdx = omniTargetTabIdx ?: return@LaunchedEffect
                            val tTab = activeSpace.tabs.getOrNull(tIdx) ?: return@LaunchedEffect
                            val pair = tabLocalXMap[tTab.id]
                            if (pair != null && tabStripViewportWidthPx > 40f) {
                                val desired = (pair.first + pair.second * 0.5f - tabStripViewportWidthPx * 0.5f)
                                    .roundToInt()
                                    .coerceIn(0, tabStripScrollState.maxValue)
                                tabStripScrollState.animateScrollTo(desired)
                            }
                        }
                        var justLaunchedTabId by remember(activeSpace.id) { mutableStateOf<String?>(null) }
                        var tabImpactSeq by remember(activeSpace.id) { mutableIntStateOf(0) }
                        var tabImpactOriginIdx by remember(activeSpace.id) { mutableIntStateOf(-1) }
                        val tabElastic = rememberElasticOverscroll(isVertical = false, maxPullPx = 76f)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords -> tabStripViewportWidthPx = coords.size.width.toFloat() }
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                    drawContent()
                                    val scrollVal = tabStripScrollState.value.toFloat()
                                    if (scrollVal > 1f) {
                                        val fadeW = 30.dp.toPx().coerceAtMost(size.width * 0.35f)
                                        val strength = (scrollVal / fadeW).coerceIn(0f, 1f)
                                        if (fadeW > 1f && strength > 0.01f) {
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    0.0f to Color.Black.copy(alpha = 1f - strength),
                                                    1.0f to Color.Black,
                                                    startX = 0f,
                                                    endX = fadeW
                                                ),
                                                topLeft = Offset.Zero,
                                                size = Size(fadeW, size.height),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                    }
                                }
                                .then(tabElastic.containerModifier)
                                .horizontalScroll(tabStripScrollState)
                                .animateContentSize(animationSpec = EgoMotion.intSizeSpring),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp + tabElastic.extraSpacingDp)
                        ) {
                            val totalTabs = activeSpace.tabs.size
                            activeSpace.tabs.forEachIndexed { idx, tab ->
                                key(tab.id) {
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
                                val isOmniDragActive = omniTargetTabIdx != null
                                val isOmniOpticalTarget = omniTargetTabIdx == idx
                                val tabScale by animateFloatAsState(
                                    targetValue = when {
                                        isPressed -> 0.95f
                                        isOmniOpticalTarget -> 1.02f
                                        isOmniDragActive -> 0.99f
                                        else -> 1f
                                    },
                                    animationSpec = EgoMotion.bouncySpring(),
                                    label = "tabPressScale"
                                )
                                val tabOpticalAlpha by animateFloatAsState(
                                    targetValue = if (isOmniDragActive && !isOmniOpticalTarget) 0.66f else 1f,
                                    animationSpec = tween(150),
                                    label = "tabOpticalAlpha"
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
                                val chromeColorsTab = LocalEgoChromeColors.current
                                val tabBg by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        chromeColorsTab.selectedTabBg
                                    } else {
                                        chromeColorsTab.unselectedTabBg
                                    },
                                    animationSpec = tween(180),
                                    label = "tabBgAnim"
                                )
                                val tabFg = if (isSelected) MaterialTheme.colorScheme.onSurface else activeIconColor.copy(alpha = 0.76f)
                                val safeStartR = startRadius.coerceAtLeast(0.dp)
                                val safeEndR = endRadius.coerceAtLeast(0.dp)
                                val tabShape = RoundedCornerShape(
                                    topStart = safeStartR,
                                    bottomStart = safeStartR,
                                    topEnd = safeEndR,
                                    bottomEnd = safeEndR
                                )

                                val isBeingDragged = draggingTabId == tab.id
                                val isReorderingInBar = draggingTabId != null &&
                                    hoveredDropSpaceId == null &&
                                    !hoveredNewSpaceDrop &&
                                    dragStartTabIndex >= 0 &&
                                    dragTargetTabIndex >= 0
                                val rawDragShiftPx = when {
                                    !isReorderingInBar -> 0f
                                    isBeingDragged -> (dragTargetTabIndex - dragStartTabIndex) * draggedTabWidthPx
                                    dragStartTabIndex < dragTargetTabIndex && idx in (dragStartTabIndex + 1)..dragTargetTabIndex -> -draggedTabWidthPx
                                    dragStartTabIndex > dragTargetTabIndex && idx in dragTargetTabIndex until dragStartTabIndex -> draggedTabWidthPx
                                    else -> 0f
                                }
                                val animatedDragShiftPx by animateFloatAsState(
                                    targetValue = rawDragShiftPx,
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
                                    label = "tabDragShift_${tab.id}"
                                )
                                val slotBorderMod = if (isBeingDragged && isReorderingInBar) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                                        shape = tabShape
                                    )
                                } else Modifier

                                val tabCaptureLayer = rememberGraphicsLayer()
                                val isCollapsingTab = tab.id in collapsingTopTabIds
                                val tabWidthFactor by animateFloatAsState(
                                    targetValue = if (isCollapsingTab) 0f else 1f,
                                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 340f),
                                    label = "tabCollapse_${tab.id}"
                                )
                                val isBulletNewTab = tab.id == justLaunchedTabId
                                val bulletOffsetX = remember(tab.id) { Animatable(if (isBulletNewTab) 78f else 0f) }
                                val bulletScaleX = remember(tab.id) { Animatable(if (isBulletNewTab) 0.72f else 1f) }
                                val bulletScaleY = remember(tab.id) { Animatable(if (isBulletNewTab) 1.08f else 1f) }
                                val dominoKickX = remember(tab.id) { Animatable(0f) }
                                LaunchedEffect(isBulletNewTab) {
                                    if (isBulletNewTab) {
                                        launch { bulletOffsetX.animateTo(0f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f)) }
                                        launch { bulletScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.64f, stiffness = 620f)) }
                                        launch { bulletScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.64f, stiffness = 620f)) }
                                    }
                                }
                                LaunchedEffect(tabImpactSeq) {
                                    if (tabImpactSeq > 0 && !isBulletNewTab && tabImpactOriginIdx >= 0 && idx <= tabImpactOriginIdx) {
                                        val dist = tabImpactOriginIdx - idx
                                        if (dist in 0..5) {
                                            delay(55L + dist * 26L)
                                            val impulse = (-11.5f * Math.pow(0.68, dist.toDouble())).toFloat()
                                            dominoKickX.animateTo(impulse, animationSpec = tween(55))
                                            dominoKickX.animateTo(0f, animationSpec = spring(dampingRatio = 0.48f, stiffness = 420f))
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            val w = (placeable.width * tabWidthFactor).toInt().coerceAtLeast(0)
                                            layout(w, placeable.height) {
                                                if (w > 0) placeable.placeRelative(0, 0)
                                            }
                                        }
                                        .padding(horizontal = extraMargin.coerceAtLeast(0.dp))
                                        .onGloballyPositioned { coords ->
                                            if (draggingTabId == null) {
                                                tabBoundsMap[tab.id] = coords.boundsInRoot()
                                            }
                                        }
                                        .drawWithContent {
                                            tabCaptureLayer.record { this@drawWithContent.drawContent() }
                                            drawLayer(tabCaptureLayer)
                                        }
                                        .graphicsLayer {
                                            scaleX = tabScale * bulletScaleX.value
                                            scaleY = tabScale * bulletScaleY.value
                                            translationX = neighborNudge.toPx() + animatedDragShiftPx + (bulletOffsetX.value + dominoKickX.value).dp.toPx()
                                            alpha = when { tab.id in vanishingTopTabIds -> 0f; isBeingDragged -> 0.36f; else -> 1f }
                                        }
                                        .height(34.dp)
                                        .then(
                                            if (prefs.tabWidthMode == TabWidthMode.MANUAL) {
                                                Modifier.width(prefs.manualTabWidthDp.coerceIn(110, 240).dp)
                                            } else {
                                                Modifier.widthIn(min = 68.dp, max = 212.dp)
                                            }
                                        )
                                        .clip(tabShape)
                                        .background(
                                            if (isBeingDragged && isReorderingInBar) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            else tabBg
                                        )
                                        .harmonyRimGlow(
                                            enabled = prefs.rimGlowEnabled,
                                            shape = tabShape,
                                            glowColor = MaterialTheme.colorScheme.primary
                                        )
                                        .drawWithContent {
                                            drawContent()
                                            if (prefs.rimGlowEnabled && draggingTabId != null && !isBeingDragged) {
                                                val tRect = tabBoundsMap[tab.id]
                                                if (tRect != null) {
                                                    val localPtr = dragPointerRoot - tRect.topLeft
                                                    val dx = when {
                                                        dragPointerRoot.x < tRect.left -> tRect.left - dragPointerRoot.x
                                                        dragPointerRoot.x > tRect.right -> dragPointerRoot.x - tRect.right
                                                        else -> 0f
                                                    }
                                                    val dy = when {
                                                        dragPointerRoot.y < tRect.top -> tRect.top - dragPointerRoot.y
                                                        dragPointerRoot.y > tRect.bottom -> dragPointerRoot.y - tRect.bottom
                                                        else -> 0f
                                                    }
                                                    val dist = kotlin.math.hypot(dx, dy)
                                                    val prox = (1f - dist / 210f).coerceIn(0f, 1f)
                                                    if (prox > 0.01f) {
                                                        val eff = prefs.rimGlowIntensity.coerceIn(0.5f, 2.5f)
                                                        val outline = tabShape.createOutline(size, layoutDirection, this)
                                                        val rad = (size.maxDimension * 0.72f).coerceIn(120f, 320f)
                                                        drawOutline(
                                                            outline = outline,
                                                            brush = Brush.radialGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = (0.14f * eff * prox).coerceAtMost(0.38f)),
                                                                    ringColor.copy(alpha = (0.24f * eff * prox).coerceAtMost(0.52f)),
                                                                    Color.Transparent
                                                                ),
                                                                center = localPtr,
                                                                radius = rad
                                                            ),
                                                            style = Fill
                                                        )
                                                        drawOutline(
                                                            outline = outline,
                                                            brush = Brush.radialGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = (0.88f * eff * prox).coerceAtMost(1f)),
                                                                    ringColor.copy(alpha = (0.55f * eff * prox).coerceAtMost(0.92f)),
                                                                    Color.Transparent
                                                                ),
                                                                center = localPtr,
                                                                radius = rad * 0.9f
                                                            ),
                                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        .onGloballyPositioned { coords ->
                                            val pos = coords.positionInParent()
                                            tabLocalXMap[tab.id] = pos.x to coords.size.width.toFloat()
                                        }
                                        .graphicsLayer { alpha = tabOpticalAlpha }
                                        .then(
                                            if (isOmniOpticalTarget) {
                                                Modifier
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = 0.11f),
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                            )
                                                        ),
                                                        tabShape
                                                    )
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                                                        tabShape
                                                    )
                                            } else slotBorderMod
                                        )
                                        .pointerInput(tab.id) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    awaitFirstDown(requireUnconsumed = false)
                                                    pressedTabIndex = activeSpace.tabs.indexOfFirst { it.id == tab.id }
                                                    waitForUpOrCancellation()
                                                    pressedTabIndex = -1
                                                }
                                            }
                                        }
                                        .pointerInput(tab.id, activeSpace.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { startOffset ->
                                                    val rect = tabBoundsMap[tab.id]
                                                    val origin = rect?.topLeft ?: Offset.Zero
                                                    val startIdx = activeSpace.tabs.indexOfFirst { it.id == tab.id }
                                                    draggingTabId = tab.id
                                                    dragStartTabIndex = startIdx
                                                    dragTargetTabIndex = startIdx
                                                    draggedTabWidthPx = ((rect?.width ?: 150f) + with(density) { 4.dp.toPx() })
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

                                                    // 드래그 중에는 리스트를 직접 변경하지 않고 목표 슬롯 인덱스만 계산 -> 제스처 끊김 완전 방지 + 인접 탭 비켜주기
                                                    if (hoveredDropSpaceId == null && !hoveredNewSpaceDrop) {
                                                        val currentTabs = activeSpace.tabs
                                                        val startRect = tabBoundsMap[curId]
                                                        val inBarY = startRect == null || (ptr.y >= startRect.top - 72f && ptr.y <= startRect.bottom + 72f)
                                                        if (inBarY && currentTabs.isNotEmpty()) {
                                                            var newIdx = dragStartTabIndex.coerceIn(0, currentTabs.lastIndex)
                                                            val centers = currentTabs.mapIndexed { i, t ->
                                                                val r = tabBoundsMap[t.id]
                                                                i to ((r?.left ?: 0f) + (r?.width ?: 140f) / 2f)
                                                            }
                                                            newIdx = centers.count { (_, cx) -> ptr.x > cx }.coerceIn(0, currentTabs.lastIndex)
                                                            if (newIdx != dragTargetTabIndex) {
                                                                dragTargetTabIndex = newIdx
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val curId = draggingTabId
                                                    val fromIdx = dragStartTabIndex
                                                    val toIdx = dragTargetTabIndex
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
                                                            if (runtimeState.connectionState == McpConnectionState.CONNECTED) {
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
                                                            if (runtimeState.connectionState == McpConnectionState.CONNECTED) {
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

                                                    if (!didDrop && curId != null && fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                                                        val targetTab = activeSpace.tabs.getOrNull(toIdx)
                                                        if (targetTab != null && targetTab.id != curId) {
                                                            SpaceManager.moveTabById(activeSpace.id, curId, targetTab.id)
                                                        }
                                                    }
                                                    draggingTabId = null
                                                    dragStartTabIndex = -1
                                                    dragTargetTabIndex = -1
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
                                                    dragStartTabIndex = -1
                                                    dragTargetTabIndex = -1
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
                                        EgoIcon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = "시크릿 탭",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        , symbolSize = 15.dp)
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
                                    if (!tab.isIncognito && !(tab.isPlayingAudio || tab.isMuted)) {
                                        val isInternalNew = tab.url == NEW_TAB_URL || tab.url.isBlank()
                                        val isInternalDl = tab.url == "ego://downloads"
                                        val isInternalHist = tab.url == "ego://history"
                                        if (isInternalNew || isInternalDl || isInternalHist) {
                                            val sysIcon = when {
                                                isInternalDl -> Icons.Default.Download
                                                isInternalHist -> Icons.Default.History
                                                else -> Icons.Default.Home
                                            }
                                            EgoIcon(
                                                imageVector = sysIcon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else mutedIconColor,
                                                modifier = Modifier.size(15.dp),
                                                symbolSize = 15.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        } else {
                                            var tabFavicon by remember(tab.url) {
                                                mutableStateOf(FaviconCache.getMemoryCached(tab.url))
                                            }
                                            LaunchedEffect(tab.url) {
                                                if (tabFavicon == null) {
                                                    tabFavicon = FaviconCache.loadFavicon(context, tab.url)
                                                }
                                            }
                                            val favBmp = tabFavicon
                                            if (favBmp != null && !favBmp.isRecycled) {
                                                Image(
                                                    bitmap = favBmp.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                )
                                            } else {
                                                EgoIcon(
                                                    imageVector = Icons.Default.Public,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else mutedIconColor,
                                                    modifier = Modifier.size(15.dp),
                                                    symbolSize = 15.dp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                    }
                                    val displayTabTitle = tab.title.ifBlank { tab.url }
                                    val isDynamic = prefs.tabWidthMode == TabWidthMode.DYNAMIC
                                    Box(
                                        modifier = Modifier
                                            .then(if (isDynamic) Modifier.weight(1f, fill = false) else Modifier.weight(1f))
                                            .clipToBounds(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = displayTabTitle,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = tabFg,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
                                        )
                                        if (displayTabTitle.length > 10) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .width(22.dp)
                                                    .height(22.dp)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            0.0f to Color.Transparent,
                                                            1.0f to tabBg
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    EgoIcon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "탭 닫기",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                if (prefs.cardVanishStyle == CardVanishStyle.OFF || tab.id in vanishingTopTabIds) {
                                                    SpaceManager.closeTab(activeSpace.id, tab.id)
                                                } else {
                                                    val tbRect = tabBoundsMap[tab.id]
                                                    val waitMs = when (prefs.cardVanishStyle) {
                                                        CardVanishStyle.HARMONY_DUST, CardVanishStyle.HARMONY_GPU_SHADER, CardVanishStyle.HARMONY_7_SAND_WAVE -> (prefs.vanishDurationMs * 0.45f).toLong().coerceIn(260L, 620L)
                                                        CardVanishStyle.LIGHT_PARTICLES -> 300L
                                                        CardVanishStyle.SCALE_FADE -> 180L
                                                        CardVanishStyle.OFF -> 0L
                                                    }
                                                    scope.launch {
                                                        val capturedBmp = runCatching { tabCaptureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                                                        vanishingTopTabIds = vanishingTopTabIds + tab.id
                                                        emitDustBurst(
                                                            rootRect = tbRect,
                                                            accent = tabBg,
                                                            densityFactor = 0.42f,
                                                            touchRoot = tbRect?.let { Offset(it.right - 20f, it.center.y) },
                                                            capturedBitmap = capturedBmp
                                                        )
                                                        // 1단계: 40% 침식 구간 동안 자리 유지 -> 2단계: 과감쇠 스프링으로 가로 폭 0dp 수축 후 제거
                                                        kotlinx.coroutines.delay(waitMs)
                                                        collapsingTopTabIds = collapsingTopTabIds + tab.id
                                                        kotlinx.coroutines.delay(240L)
                                                        SpaceManager.closeTab(activeSpace.id, tab.id)
                                                        collapsingTopTabIds = collapsingTopTabIds - tab.id
                                                        vanishingTopTabIds = vanishingTopTabIds - tab.id
                                                    }
                                                }
                                            },
                                        tint = if (isSelected) tabFg.copy(alpha = 0.78f) else mutedIconColor,
                                        symbolSize = 15.dp
                                    )
                                }
                                }
                            }
                        }

                        // 새 탭(+) 버튼: 클릭 반동 + 새 탭 총알 발사 + 기존 탭 도미노 충격파
                        Spacer(modifier = Modifier.width(4.dp))
                        BallisticNewTabButton(
                            tint = activeIconColor,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                val prevLastIdx = activeSpace.tabs.lastIndex
                                val createdTab = SpaceManager.createTab(activeSpace.id)
                                justLaunchedTabId = createdTab.id
                                tabImpactOriginIdx = prevLastIdx
                                tabImpactSeq += 1
                                scope.launch {
                                    delay(36L)
                                    tabStripScrollState.animateScrollTo(tabStripScrollState.maxValue)
                                    delay(420L)
                                    if (justLaunchedTabId == createdTab.id) {
                                        justLaunchedTabId = null
                                    }
                                }
                            }
                        )
                    }
                }
            }

            var tbB12TargetPx by remember { mutableFloatStateOf(0f) }
            var tbB23TargetPx by remember { mutableFloatStateOf(0f) }
            var g1NaturalWidthPx by remember { mutableFloatStateOf(0f) }
            var g3NaturalWidthPx by remember { mutableFloatStateOf(0f) }
            val tbB12Px by animateFloatAsState(
                targetValue = tbB12TargetPx,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
                label = "toolbarB12"
            )
            val tbB23Px by animateFloatAsState(
                targetValue = tbB23TargetPx,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
                label = "toolbarB23"
            )

            // 2행: Material 3 Expressive Connected 3-그룹 툴바 (바깥 22dp · 인접 6dp, 3dp 간격, 선택 탭과 동일 색상)
            val toolbarChrome = LocalEgoChromeColors.current
            val m3GroupBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
            val m3LeftGroupShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
            val m3CenterGroupShape = RoundedCornerShape(6.dp)
            val m3RightGroupShape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(toolbarChrome.backdropBg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 그룹 1: (홈 · 뒤로 · 앞으로 · 새로고침) — 좌측 끝 고정, 우측 경계만 고무줄 연동 (0-갭)
                val g1WidthMod = if (prefs.toolbarElasticityEnabled && g1NaturalWidthPx > 20f && kotlin.math.abs(tbB12Px) > 0.4f) {
                    val wPx = (g1NaturalWidthPx + tbB12Px).coerceIn(g1NaturalWidthPx * 0.66f, g1NaturalWidthPx + with(density) { 46.dp.toPx() })
                    Modifier.width(with(density) { wPx.toDp() })
                } else Modifier
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .then(g1WidthMod)
                        .onGloballyPositioned { coords ->
                            if (kotlin.math.abs(tbB12Px) < 0.4f && coords.size.width > 0) {
                                g1NaturalWidthPx = coords.size.width.toFloat()
                            }
                        }
                        .clip(m3LeftGroupShape)
                        .background(toolbarChrome.groupSurface)
                        .harmonyRimGlow(enabled = prefs.rimGlowEnabled, shape = m3LeftGroupShape, glowColor = MaterialTheme.colorScheme.primary)
                        .border(1.dp, m3GroupBorder, m3LeftGroupShape)
                        .pointerInput(prefs.toolbarElasticityEnabled) {
                            if (!prefs.toolbarElasticityEnabled) return@pointerInput
                            var dragSum = 0f
                            val maxPullPx = with(density) { 42.dp.toPx() }
                            detectHorizontalDragGestures(
                                onDragEnd = { dragSum = 0f; tbB12TargetPx = 0f },
                                onDragCancel = { dragSum = 0f; tbB12TargetPx = 0f },
                                onHorizontalDrag = { _, amount ->
                                    dragSum += amount
                                    tbB12TargetPx = if (dragSum > 0f) {
                                        maxPullPx * (1f - kotlin.math.exp(-dragSum / (maxPullPx * 2.1f)))
                                    } else 0f
                                    tbB23TargetPx = 0f
                                }
                            )
                        }
                        .padding(horizontal = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LeftNavToolbarButtons(
                        showHomeButton = prefs.showHomeButton,
                        canBackOrClose = activeTab.canGoBack || activeSpace.tabs.size > 1 || activeTab.url != NEW_TAB_URL,
                        canGoForward = activeTab.canGoForward,
                        activeIconColor = activeIconColor,
                        mutedIconColor = mutedIconColor,
                        onGoHome = { SpaceManager.goHome(activeSpace.id) },
                        onBackOrClose = { SpaceManager.handleBackOrCloseTab(activeSpace.id) },
                        onGoForward = { SpaceManager.goForward(activeSpace.id) },
                        onReload = { SpaceManager.reload(activeSpace.id) }
                    )
                }

                // 그룹 2: (주소창) — 별도 Composable로 분리 (JVM 메서드 크기 최적화)
                val existingBookmark = bookmarks.firstOrNull { it.url == activeTab.url }
                OmniboxCenterCapsule(
                    activeTab = activeTab,
                    activeSpaceId = activeSpace.id,
                    isAiSpace = activeSpace.type == SpaceType.AI,
                    isEffectiveDark = isEffectiveDark,
                    showOmniboxOverlay = showOmniboxOverlay || showAiCommandPopup,
                    prefs = prefs,
                    existingBookmark = existingBookmark,
                    groupSurface = toolbarChrome.groupSurface,
                    groupBorder = m3GroupBorder,
                    activeIconColor = activeIconColor,
                    mutedIconColor = mutedIconColor,
                    rootBoxLeftPx = rootBoxLeftPx,
                    rootBoxTopPx = rootBoxTopPx,
                    tabCount = activeSpace.tabs.size,
                    activeTabIndex = activeTabIdxNow,
                    onElasticPullChanged = { pullPx ->
                        when {
                            pullPx < -0.2f -> { tbB12TargetPx = pullPx; tbB23TargetPx = 0f }
                            pullPx > 0.2f -> { tbB12TargetPx = 0f; tbB23TargetPx = pullPx }
                            else -> { tbB12TargetPx = 0f; tbB23TargetPx = 0f }
                        }
                    },
                    onTabDragOffsetChanged = { omniTabDragStep = it },
                    onBoundsMeasured = { l, t, w ->
                        addressBarLeftPx = l
                        addressBarTopPx = t
                        addressBarWidthPx = w
                    },
                    onSwitchTabByOffset = { off -> SpaceManager.switchTabByOffset(activeSpace.id, off) },
                    onOpenOmnibox = { showOmniboxOverlay = true },
                    onClickBookmark = {
                        if (existingBookmark != null) editingBookmark = existingBookmark
                        else showAddBookmarkDialog = true
                    },
                    showAiCommand = prefs.showAiCommandButton && runtimeState.connectionState == McpConnectionState.CONNECTED,
                    aiCommandExpanded = showAiCommandPopup,
                    aiPendingCount = mcpPendingCount,
                    onClickAiCommand = {
                        aiPopupSpaceId = null
                        aiPopupTabId = null
                        if (!showAiCommandPopup) showOmniboxOverlay = false
                        showAiCommandPopup = !showAiCommandPopup
                    }
                )

                // 그룹 3: (MCP · AI · 광고차단 · 번역 · 다크모드 · 프로필 · 전체탭 · 메뉴) — 우측 끝 고정, 좌측 경계만 고무줄 연동 (0-갭)
                val g3WidthMod = if (prefs.toolbarElasticityEnabled && g3NaturalWidthPx > 20f && kotlin.math.abs(tbB23Px) > 0.4f) {
                    val wPx = (g3NaturalWidthPx - tbB23Px).coerceIn(g3NaturalWidthPx * 0.72f, g3NaturalWidthPx + with(density) { 46.dp.toPx() })
                    Modifier.width(with(density) { wPx.toDp() })
                } else Modifier
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .then(g3WidthMod)
                        .onGloballyPositioned { coords ->
                            if (kotlin.math.abs(tbB23Px) < 0.4f && coords.size.width > 0) {
                                g3NaturalWidthPx = coords.size.width.toFloat()
                            }
                        }
                        .clip(m3RightGroupShape)
                        .background(toolbarChrome.groupSurface)
                        .harmonyRimGlow(enabled = prefs.rimGlowEnabled, shape = m3RightGroupShape, glowColor = MaterialTheme.colorScheme.primary)
                        .border(1.dp, m3GroupBorder, m3RightGroupShape)
                        .pointerInput(prefs.toolbarElasticityEnabled) {
                            if (!prefs.toolbarElasticityEnabled) return@pointerInput
                            var dragSum = 0f
                            val maxPullPx = with(density) { 42.dp.toPx() }
                            detectHorizontalDragGestures(
                                onDragEnd = { dragSum = 0f; tbB23TargetPx = 0f },
                                onDragCancel = { dragSum = 0f; tbB23TargetPx = 0f },
                                onHorizontalDrag = { _, amount ->
                                    dragSum += amount
                                    tbB23TargetPx = if (dragSum < 0f) {
                                        -maxPullPx * (1f - kotlin.math.exp(-kotlin.math.abs(dragSum) / (maxPullPx * 2.1f)))
                                    } else 0f
                                    tbB12TargetPx = 0f
                                }
                            )
                        }
                        .padding(horizontal = 3.dp)
                        .then(
                            if (isQuickMenuEditMode) {
                                Modifier.pointerInput("real_toolbar_edit_drag") {
                                    val downThresholdPx = 44.dp.toPx()
                                    val stepXPx = 36.dp.toPx()
                                    var activeTbKey: String? = null
                                    var accumX = 0f
                                    var accumY = 0f
                                    val tbToQsMap = mapOf(
                                        "MCP" to "mcp", "AI" to "aispace", "DARKMODE" to "dark",
                                        "NEW_TAB" to "newtab", "SHARE" to "share", "FIND" to "find",
                                        "DESKTOP" to "desktop", "READER" to "reader"
                                    )
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { startPos ->
                                            val p = SpaceManager.preferences.value
                                            val base = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
                                            val ord = p.toolbarButtonOrder.filter { it in base } + base.filter { it !in p.toolbarButtonOrder }
                                            val enabled = ord.filter { k ->
                                                when (k) {
                                                    "MCP" -> p.showMcpBadgeButton
                                                    "ADBLOCK" -> p.showAdBlockButton
                                                    "TRANSLATE" -> p.showTranslateButton
                                                    "DARKMODE" -> p.showDarkModeButton
                                                    "NEW_TAB" -> p.showNewTabToolbarButton
                                                    "SHARE" -> p.showShareButton
                                                    "FIND" -> p.showFindInPageButton
                                                    "DESKTOP" -> p.showDesktopSiteButton
                                                    "READER" -> p.showReaderModeButton
                                                    "PROFILE" -> p.showProfileAvatarButton
                                                    else -> false
                                                }
                                            }
                                            if (enabled.isNotEmpty() && size.width > 0) {
                                                val slotW = size.width.toFloat() / enabled.size.toFloat()
                                                val idx = (startPos.x / slotW).toInt().coerceIn(0, enabled.lastIndex)
                                                activeTbKey = enabled[idx]
                                                accumX = 0f
                                                accumY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onDragEnd = {
                                            val k = activeTbKey
                                            activeTbKey = null
                                            // 아래쪽 빠른메뉴 방향으로 드래그하면 실제 툴바에서 빼고 빠른메뉴로 이동!
                                            if (k != null && accumY > downThresholdPx) {
                                                val qsKey = tbToQsMap[k]
                                                if (qsKey != null) {
                                                    SpaceManager.updatePreferences { p ->
                                                        val curQs = p.quickMenuLayout.toMutableList()
                                                        curQs.removeAll { it == "hide:$qsKey" || it.startsWith("$qsKey:") }
                                                        curQs.add(1.coerceAtMost(curQs.size), "$qsKey:1")
                                                        val updated = p.copy(quickMenuLayout = curQs)
                                                        when (k) {
                                                            "MCP" -> updated.copy(showMcpBadgeButton = false)
                                                            "DARKMODE" -> updated.copy(showDarkModeButton = false)
                                                            "NEW_TAB" -> updated.copy(showNewTabToolbarButton = false)
                                                            "SHARE" -> updated.copy(showShareButton = false)
                                                            "FIND" -> updated.copy(showFindInPageButton = false)
                                                            "DESKTOP" -> updated.copy(showDesktopSiteButton = false)
                                                            "READER" -> updated.copy(showReaderModeButton = false)
                                                            else -> updated
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onDragCancel = { activeTbKey = null },
                                        onDrag = { change, dragAmount ->
                                            val k = activeTbKey ?: return@detectDragGesturesAfterLongPress
                                            change.consume()
                                            accumX += dragAmount.x
                                            accumY += dragAmount.y
                                            val dir = when {
                                                accumX >= stepXPx -> 1
                                                accumX <= -stepXPx -> -1
                                                else -> 0
                                            }
                                            if (dir != 0) {
                                                accumX = 0f
                                                val p = SpaceManager.preferences.value
                                                val base = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
                                                val full = (p.toolbarButtonOrder.filter { it in base } + base.filter { it !in p.toolbarButtonOrder }).toMutableList()
                                                val fi = full.indexOf(k)
                                                val ti = (fi + dir).coerceIn(0, full.lastIndex)
                                                if (fi >= 0 && ti != fi) {
                                                    full.removeAt(fi)
                                                    full.add(ti, k)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    SpaceManager.updatePreferences { it.copy(toolbarButtonOrder = full) }
                                                }
                                            }
                                        }
                                    )
                                }
                            } else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                // 사용자 커스텀 순서(prefs.toolbarButtonOrder)에 따라 주소창 우측 버튼 동적 배치
                val defaultTbOrder = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
                val effectiveTbOrder = remember(prefs.toolbarButtonOrder) {
                    val known = prefs.toolbarButtonOrder.filter { it in defaultTbOrder }
                    known + defaultTbOrder.filter { it !in known }
                }
                effectiveTbOrder.forEach { tbKey ->
                    when (tbKey) {
                        "MCP" -> {
                            if (prefs.showMcpBadgeButton) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .onGloballyPositioned { coords -> mcpAnchorRect = coords.boundsInRoot() }
                                        .pressBounceScale(0.86f)
                                        .clip(CircleShape)
                                        .clickable { showMcpDrawer = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    McpHubStatusIcon(
                                        disconnected = isMcpDisconnected,
                                        contentDescription = if (isMcpDisconnected) "MCP 연결 끊김 (탭하여 설정)" else "MCP 연결됨 (탭하여 설정)",
                                        tint = if (isMcpDisconnected) mutedIconColor else MaterialTheme.colorScheme.primary,
                                        cutoutColor = toolbarChrome.groupSurface,
                                        symbolSize = 20.dp
                                    )
                                }
                            }
                        }
                        "AI" -> { /* MCP 연결 시 주소창(Omnibox) 우측에 통합 표시 */ }
                        "ADBLOCK" -> {
                            if (prefs.showAdBlockButton) {
                                val shieldWaveColor = MaterialTheme.colorScheme.primary
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .onGloballyPositioned { coords -> adBlockAnchorRect = coords.boundsInRoot() }
                                        .drawBehind {
                                            val w = adBlockWaveAnim.value
                                            if (w > 0.01f && w < 0.99f) {
                                                drawCircle(
                                                    color = shieldWaveColor.copy(alpha = (1f - w) * 0.65f),
                                                    radius = size.minDimension * (0.28f + 0.36f * w),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = (2.4.dp.toPx() * (1f - w * 0.6f)))
                                                )
                                            }
                                        }
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                scope.launch {
                                                    adBlockPulseAnim.snapTo(0.76f)
                                                    adBlockPulseAnim.animateTo(1.22f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 620f))
                                                    adBlockPulseAnim.animateTo(1f, animationSpec = EgoMotion.bouncySpring())
                                                }
                                                scope.launch {
                                                    adBlockWaveAnim.snapTo(0f)
                                                    adBlockWaveAnim.animateTo(1f, animationSpec = tween(420))
                                                    adBlockWaveAnim.snapTo(0f)
                                                }
                                                if (currentHost.isNotBlank() && adBlockState.globalEnabled) {
                                                    AdBlockEngine.toggleWhitelistForHost(currentHost)
                                                } else {
                                                    val next = !adBlockState.globalEnabled
                                                    AdBlockEngine.setGlobalEnabled(next)
                                                    SpaceManager.updatePreferences { it.copy(adBlockEnabled = next) }
                                                }
                                                SpaceManager.reload(activeSpace.id)
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showAdBlockDialog = true
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "광고 차단 (길게 눌러 상세 설정)",
                                        tint = if (isSiteAdBlockOn) MaterialTheme.colorScheme.primary else mutedIconColor,
                                        filled = isSiteAdBlockOn,
                                        modifier = Modifier
                                            .drawBehind {
                                                val w = adBlockWaveAnim.value
                                                if (w > 0.01f && w < 0.99f) {
                                                    val c = if (isSiteAdBlockOn) EgoEmerald else Color(0xFF94A3B8)
                                                    drawCircle(
                                                        color = c.copy(alpha = (1f - w) * 0.55f),
                                                        radius = size.minDimension * (0.45f + 0.65f * w),
                                                        style = Stroke(width = (2.4f * (1f - w)).dp.toPx())
                                                    )
                                                    val w2 = ((w - 0.18f) / 0.82f).coerceIn(0f, 1f)
                                                    if (w2 > 0.01f) {
                                                        drawCircle(
                                                            color = Color.White.copy(alpha = (1f - w2) * 0.42f),
                                                            radius = size.minDimension * (0.35f + 0.55f * w2),
                                                            style = Stroke(width = (1.5f * (1f - w2)).dp.toPx())
                                                        )
                                                    }
                                                }
                                            }
                                            .graphicsLayer {
                                                scaleX = adBlockPulseAnim.value
                                                scaleY = adBlockPulseAnim.value
                                                rotationY = kotlin.math.sin(adBlockWaveAnim.value * Math.PI * 2.0).toFloat() * 24f
                                                cameraDistance = 12f * density.density
                                            },
                                        symbolSize = 20.dp
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
                                            RollingCounterText(
                                                count = pageBlockedCount.coerceAtMost(99),
                                                textStyle = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = androidx.compose.ui.graphics.Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "TRANSLATE" -> {
                            if (prefs.showTranslateButton) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .onGloballyPositioned { coords -> translateAnchorRect = coords.boundsInRoot() }
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                scope.launch {
                                                    translateFlipAnim.snapTo(0f)
                                                    translateFlipAnim.animateTo(360f, animationSpec = spring(dampingRatio = 0.68f, stiffness = 320f))
                                                }
                                                scope.launch {
                                                    translateScaleAnim.snapTo(0.80f)
                                                    translateScaleAnim.animateTo(1.18f, animationSpec = spring(dampingRatio = 0.50f, stiffness = 560f))
                                                    translateScaleAnim.animateTo(1f, animationSpec = EgoMotion.bouncySpring())
                                                }
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
                                    val trOrbitColor = MaterialTheme.colorScheme.primary
                                    EgoIcon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = "페이지 번역 (길게 눌러 번역 설정)",
                                        tint = if (translateState.isTranslated || translateState.isTranslating) trOrbitColor else activeIconColor,
                                        modifier = Modifier
                                            .drawBehind {
                                                if (translateState.isTranslating) {
                                                    val p = translateState.progress.coerceIn(0.08f, 1f)
                                                    drawCircle(
                                                        color = trOrbitColor.copy(alpha = 0.25f),
                                                        radius = size.minDimension * 0.72f,
                                                        style = Stroke(width = 1.6.dp.toPx())
                                                    )
                                                    drawArc(
                                                        color = trOrbitColor,
                                                        startAngle = -90f,
                                                        sweepAngle = 360f * p,
                                                        useCenter = false,
                                                        style = Stroke(width = 2.0.dp.toPx())
                                                    )
                                                }
                                            }
                                            .graphicsLayer {
                                                rotationY = translateFlipAnim.value
                                                scaleX = translateScaleAnim.value
                                                scaleY = translateScaleAnim.value
                                                cameraDistance = 12f * density.density
                                            },
                                        symbolSize = 20.dp
                                    )
                                }
                            }
                        }
                        "DARKMODE" -> {
                            if (prefs.showDarkModeButton) {
                                IconButton(
                                    onClick = { val next = !isEffectiveDark; SpaceManager.updatePreferences { it.copy(darkModeEnabled = next, themeMode = if (next) ThemeMode.DARK else ThemeMode.LIGHT) } },
                                    modifier = Modifier.size(38.dp).pressBounceScale()
                                ) {
                                    val themeSpinDeg by animateFloatAsState(
                                        targetValue = if (isEffectiveDark) 180f else 0f,
                                        animationSpec = spring(dampingRatio = 0.70f, stiffness = 260f),
                                        label = "themeIconRotation"
                                    )
                                    AnimatedContent(
                                        targetState = isEffectiveDark,
                                        transitionSpec = {
                                            (scaleIn(EgoMotion.bouncySpring(), initialScale = 0.55f) + fadeIn(tween(160)))
                                                .togetherWith(scaleOut(tween(120), targetScale = 0.55f) + fadeOut(tween(110)))
                                        },
                                        modifier = Modifier.graphicsLayer { rotationZ = themeSpinDeg },
                                        label = "themeIconMorph"
                                    ) { darkNow ->
                                        EgoIcon(
                                            imageVector = if (darkNow) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "다크모드 전환",
                                            tint = if (darkNow) MaterialTheme.colorScheme.primary else activeIconColor,
                                            filled = darkNow,
                                            modifier = Modifier.graphicsLayer { rotationZ = if (darkNow) -180f else 0f }
                                        )
                                    }
                                }
                            }
                        }
                        "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER" -> {
                            RightExtraToolbarButton(
                                tbKey = tbKey,
                                prefs = prefs,
                                isWebPage = isWebPage,
                                showFindBar = showFindBar,
                                activeIconColor = activeIconColor,
                                mutedIconColor = mutedIconColor,
                                activeTabTitle = activeTab.title,
                                activeTabUrl = activeTab.url,
                                onCreateTab = { SpaceManager.createTab(activeSpace.id, selectImmediately = true) },
                                onToggleFindBar = { showFindBar = !showFindBar },
                                onToggleDesktop = { SpaceManager.updatePreferences { it.copy(desktopModeEnabled = !it.desktopModeEnabled) } },
                                onOpenReader = {
                                    scope.launch {
                                        runCatching {
                                            val raw = SpaceManager.evaluateJsInExactSpace(activeSpace.id, BrowserScripts.MARKDOWN_EXTRACT_JS)
                                            val md = JSONObject(raw).optString("markdown", "")
                                            readerMarkdown = md.ifBlank { "본문을 추출할 수 없는 페이지입니다." }
                                        }
                                    }
                                }
                            )
                        }
                        "PROFILE" -> {
                            if (prefs.showProfileAvatarButton) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .pressBounceScale()
                                        .clip(CircleShape)
                                        .clickable {
                                            settingsInitialCategory = SettingsCategory.ACCOUNT
                                            showChromeSettings = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (googleAccount.isSignedIn && googleAvatar != null) {
                                        Image(
                                            bitmap = googleAvatar!!.asImageBitmap(),
                                            contentDescription = "Google 프로필",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (googleAccount.isSignedIn) googleAccount.displayName.take(1).uppercase().ifBlank { "G" } else "G",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 다크모드 우측: 크롬형 모든 탭 보기 버튼 ([N] 사각 카운터)
                ExtensionToolbarButtons(
                    extensions = installedExtensions,
                    activeIconColor = activeIconColor,
                    onTapExtension = { ext ->
                        if (ext.popupPath != null) {
                            activeExtPopup = ext
                        } else {
                            val wv = SpaceManager.getActiveWebView(activeSpace.id)
                            if (wv != null) {
                                ExtensionManager.injectMatchingExtensions(wv, activeTab.url, "end", forceExtId = ext.id)
                                Toast.makeText(context, "${ext.name} 실행됨", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "실행할 웹페이지가 없습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLongPressExtension = { ext -> managingExt = ext }
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .pressBounceScale()
                        .clip(CircleShape)
                        .clickable { showAllTabsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(2.dp, activeIconColor, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        RollingCounterText(
                            count = activeSpace.tabs.size.coerceAtMost(99),
                            textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = activeIconColor
                        )
                    }
                }

                BrowserMenuDragButton(
                    showChromeMenu = showChromeMenu,
                    chromeMenuItems = chromeMenuItems,
                    tileBounds = quickMenuTileBounds,
                    activeIconColor = activeIconColor,
                    onSetShowMenu = { showChromeMenu = it },
                    onSetHoveredKey = { hoveredQuickMenuKey = it }
                )
                }
            }

            BrowserFindInPageBar(
                visible = showFindBar,
                findQuery = findQuery,
                activeIconColor = activeIconColor,
                mutedIconColor = mutedIconColor,
                onQueryChange = {
                    findQuery = it
                    SpaceManager.findInPage(activeSpace.id, it)
                },
                onFindPrev = { SpaceManager.findNextInPage(activeSpace.id, false) },
                onFindNext = { SpaceManager.findNextInPage(activeSpace.id, true) },
                onClose = {
                    findQuery = ""
                    SpaceManager.findInPage(activeSpace.id, "")
                    showFindBar = false
                }
            )

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
                    onOpenAllBookmarks = { SpaceManager.openBookmarksTab(activeSpace.id) }
                )
            }

            if (prefs.progressBarStyle == ProgressBarStyle.MATERIAL || !prefs.progressShowInOmnibox) {
                IsolatedTabProgressBar(
                    isLoading = activeTab.isLoading,
                    progress = activeTab.progress,
                    isAiSpace = activeSpace.type == SpaceType.AI
                )
            }

            BrowserAiTakeoverBar(
                activeSpace = activeSpace,
                onToggleTakeover = { SpaceManager.toggleTakeover(activeSpace.id) }
            )

            val activeSpaceAccent = parseSpaceColor(activeSpace.accentColorHex)
            val hostView = LocalView.current
            val (deviceBottomLeftDp, deviceBottomRightDp) = remember(hostView, density) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val insets = hostView.rootWindowInsets
                    val blPx = insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
                    val brPx = insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
                    val blDp = if (blPx > 0) ((blPx / density.density) - 3f).coerceIn(10f, 28f) else 16f
                    val brDp = if (brPx > 0) ((brPx / density.density) - 3f).coerceIn(10f, 28f) else 16f
                    blDp.dp to brDp.dp
                } else {
                    16.dp to 16.dp
                }
            }
            val topCanvasCornerDp = mainSwapCorner.value.coerceAtLeast(14f).dp
            val canvasShape = RoundedCornerShape(
                topStart = topCanvasCornerDp,
                topEnd = topCanvasCornerDp,
                bottomStart = if (mainSwapCorner.value > 20f) mainSwapCorner.value.dp else deviceBottomLeftDp,
                bottomEnd = if (mainSwapCorner.value > 20f) mainSwapCorner.value.dp else deviceBottomRightDp
            )
            // 웹 캔버스 (AI Space일 때 상·하단 4모서리 모두 둥근 2dp 공간 색상 테두리 + 모프 전환)
            val canvasBorder = if (activeSpace.type == SpaceType.AI) {
                Modifier
                    .padding(start = 3.dp, end = 3.dp, bottom = 4.dp)
                    .border(2.dp, activeSpaceAccent, canvasShape)
            } else Modifier

            val mainBlurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainSwapBlur.value > 0.3f) {
                Modifier.blur(mainSwapBlur.value.dp)
            } else Modifier
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { sz ->
                        if (sz.width > 0 && sz.height > 0) {
                            val r = (sz.width.toFloat() / sz.height.toFloat()).coerceIn(0.48f, 2.0f)
                            if (kotlin.math.abs(contentAspectRatio - r) > 0.02f) {
                                contentAspectRatio = r
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = mainSwapScale.value
                        scaleY = mainSwapScale.value
                        translationX = mainSwapOffsetX.value
                        translationY = mainSwapOffsetY.value
                        alpha = mainSwapAlpha.value
                    }
                    .then(mainBlurMod)
                    .then(canvasBorder)
                    .clip(canvasShape)
            ) {
                WebViewHostContainer(
                    spaces = spaces,
                    activeSpace = activeSpace
                )
                val outBmp = swapOutgoingBitmap
                if (outBmp != null && outAlpha.value > 0.02f) {
                    Image(
                        bitmap = outBmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = outScale.value
                                scaleY = outScale.value
                                translationX = outOffsetX.value
                                translationY = outOffsetY.value
                                alpha = outAlpha.value
                            }
                            .clip(RoundedCornerShape(outCorner.value.coerceAtLeast(12f).dp))
                    )
                }
                val tabPageDustColor = MaterialTheme.colorScheme.surfaceContainerHigh
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeTab.url == DOWNLOADS_URL,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(120))
                ) {
                    ChromeDownloadsScreen(
                        embeddedInTab = true,
                        onOpenSettings = {
                            settingsInitialCategory = SettingsCategory.DOWNLOADS
                            showChromeSettings = true
                        },
                        onDustVanish = { snap -> emitDustSnapshot(snap, tabPageDustColor, 0.72f) },
                        onDismiss = { SpaceManager.navigate(activeSpace.id, NEW_TAB_URL) }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = activeTab.url == HISTORY_URL,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(120))
                ) {
                    ChromeHistoryScreen(
                        history = history,
                        onNavigate = { url -> SpaceManager.navigate(activeSpace.id, url) },
                        onRemoveItem = { id -> SpaceManager.removeHistoryItem(id) },
                        onClearAll = { SpaceManager.clearHistory() },
                        onDustVanish = { snap -> emitDustSnapshot(snap, tabPageDustColor, 0.62f) }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = activeTab.url == BOOKMARKS_URL,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(120))
                ) {
                    ChromeBookmarksScreen(
                        bookmarks = bookmarks,
                        folders = bookmarkFolders,
                        onNavigate = { url -> SpaceManager.navigate(activeSpace.id, url) },
                        onEditBookmark = { bm -> editingBookmark = bm },
                        onDeleteBookmark = { id -> SpaceManager.removeBookmark(id) },
                        onMoveBookmark = { id, delta -> SpaceManager.moveBookmark(id, delta) },
                        onDismiss = {}
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = activeTab.url == NEW_TAB_URL,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(140))
                ) {
                    val ntpDustColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ChromeNewTabPage(
                        bookmarks = bookmarks,
                        onOpenOmnibox = { showOmniboxOverlay = true },
                        onNavigate = { url -> SpaceManager.navigate(activeSpace.id, url) },
                        onEditBookmark = { bm -> editingBookmark = bm },
                        onAddBookmark = { showAddBookmarkDialog = true },
                        onOpenInNewTab = { url -> SpaceManager.createTab(activeSpace.id, initialUrl = url, selectImmediately = false) },
                        onOpenInIncognito = { url -> SpaceManager.createTab(activeSpace.id, initialUrl = url, incognito = true, selectImmediately = true) },
                        onDeleteBookmark = { bm -> SpaceManager.removeBookmark(bm.id) },
                        onDustVanish = { snap -> emitDustSnapshot(snap, ntpDustColor, 0.90f) }
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

        // 최상단 좌측 고정 알약 캡슐 (설정·전체 탭·리더 전체화면에서는 숨김)
        if (!showChromeSettings && !showDownloadsScreen && !showAllTabsDialog && readerMarkdown == null && fullscreenVideo.view == null) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 6.dp)
                .zIndex(15f)
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInRoot()
                    if (personalCapsuleBounds != b) personalCapsuleBounds = b
                }
        ) {
            PillSpaceCapsule(
                activeSpace = activeSpace,
                spaces = spaces,
                isAnyAiWorking = workingSpace != null,
                workingAccentColor = workingAccent,
                spaceCount = spaces.size,
                canAddAiSpace = canAddAiSpace,
                isExpanded = spaceBarExpanded,
                isMonitorExpanded = showAiMonitorTray,
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
                        SpaceManager.createAiSpace()
                    }
                }
            )
        }
        // 단일 통합 M3 완료 미니 말풍선 (여러 공간 완료 시에도 말풍선은 항상 1개만 깔끔하게 합쳐서 표시!)
        UnifiedAiDoneSpeechBubble(
            completedIds = completedDoneIds,
            spaces = spaces,
            onSelectSpace = { sid ->
                SpaceManager.dismissCompletedDoneBanner()
                SpaceManager.selectSpace(sid)
            },
            onAutoDismiss = { SpaceManager.dismissCompletedDoneBanner() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 10.dp, top = 44.dp)
                .zIndex(52f)
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

        BrowserFloatingDraggedTabOverlay(
            floatingTab = draggingTabId?.let { tid -> activeSpace.tabs.firstOrNull { it.id == tid } },
            rootBoxBounds = rootBoxBounds,
            dragPointerRoot = dragPointerRoot,
            draggedTabWidthPx = draggedTabWidthPx,
            spaces = spaces,
            hoveredDropSpaceId = hoveredDropSpaceId,
            hoveredNewSpaceDrop = hoveredNewSpaceDrop,
            prefs = prefs
        )

        BrowserRecentDropPromptChip(
            recentDropPromptChip = recentDropPromptChip,
            showAiCommandPopup = showAiCommandPopup,
            connectionState = runtimeState.connectionState,
            onClickChip = { info ->
                aiPopupSpaceId = info.first
                aiPopupTabId = info.second
                recentDropPromptChip = null
                showAiCommandPopup = true
            }
        )

        // 주소창(ChromeOmniboxOverlay)과 1:1 위치·모핑이 일치하는 전용 AI 명령 입력 팝업
        AnimatedVisibility(
            visible = showAiCommandPopup,
            enter = fadeIn(tween(70)),
            exit = fadeOut(tween(130)),
            modifier = Modifier.zIndex(48f)
        ) {
            AiCommandOverlay(
                spaces = spaces,
                activeSpaceId = activeSpaceId,
                preselectedSpaceId = aiPopupSpaceId,
                preselectedTabId = aiPopupTabId,
                connectedClients = mcpConnectedClients,
                defaultAiName = connectedAiName,
                recentCommands = mcpRecentCommands,
                anchorLeftPx = addressBarLeftPx,
                anchorTopPx = addressBarTopPx,
                anchorWidthPx = addressBarWidthPx,
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

        val popupDustColor = MaterialTheme.colorScheme.surfaceContainerHigh

        // 상단 탭 바 직접 은은한 광감 하이라이트 및 자동 스크롤로 대체 완료

        // 번역·다운로드·닫은 탭 되돌리기 배너는 하단 통합 상단 스택(Column)에서 겹침 없이 렌더링

        // 모바일 크롬 스타일 주소창(Omnibox) 오버레이 (주소창에서 펼쳐지는 스프링 모프)
        AnimatedVisibility(
            visible = showOmniboxOverlay,
            enter = fadeIn(tween(70)),
            exit = fadeOut(tween(130))
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

        // 번역 버튼 길게 누르기: 카드 자체에서 우상단 모프 구동 (닫기 시 먼지 소멸)
        AnimatedVisibility(
            visible = showTranslateSettingsDialog,
            enter = fadeIn(tween(90)),
            exit = if (prefs.cardVanishStyle != CardVanishStyle.OFF) ExitTransition.None else fadeOut(tween(110))
        ) {
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
                anchorRect = translateAnchorRect,
                onDismiss = { snap -> emitDustSnapshot(snap, popupDustColor, 0.90f); showTranslateSettingsDialog = false }
            )
        }

        // 방패 길게 누르기: 카드 자체에서 우상단 모프 구동 (닫기 시 먼지 소멸)
        AnimatedVisibility(
            visible = showAdBlockDialog,
            enter = fadeIn(tween(90)),
            exit = if (prefs.cardVanishStyle != CardVanishStyle.OFF) ExitTransition.None else fadeOut(tween(110))
        ) {
            AdBlockControlDialog(
                currentHost = currentHost,
                activeTabId = activeTab.id,
                onReloadPage = { SpaceManager.reload(activeSpace.id) },
                anchorRect = adBlockAnchorRect,
                onDismiss = { snap -> emitDustSnapshot(snap, popupDustColor, 0.90f); showAdBlockDialog = false }
            )
        }

        // 링크 · 이미지 길게 누르기 M3 Expressive 1+2 하이브리드 컨텍스트 메뉴 (썸네일 + 스프링 등장 + 파티클 퇴장)
        longPressTarget?.let { target ->
            ChromeLongPressContextMenu(
                target = target,
                activeSpaceId = activeSpace.id,
                darkTheme = isEffectiveDark,
                onPreviewPage = { url -> previewPageUrl = url },
                onDismiss = { BrowserActivityBridge.dismissLongPressMenu() },
                onDustDismiss = { snap ->
                    emitDustSnapshot(snap, popupDustColor, 0.82f)
                    BrowserActivityBridge.dismissLongPressMenu()
                }
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

        BrowserReaderModeOverlay(
            readerMarkdown = readerMarkdown,
            onDismiss = { readerMarkdown = null }
        )

        BrowserFullscreenVideoOverlay(
            videoView = fullscreenVideo.view,
            prefs = prefs
        )

        // 우상단 3열 퀵세팅 커스텀 브라우저 메뉴 (Drag-to-Open / Drag-to-Select 완전 지원)
        QuickSettingsBrowserMenuOverlay(
            visible = showChromeMenu,
            items = chromeMenuItems,
            savedLayout = prefs.quickMenuLayout,
            hoveredKey = hoveredQuickMenuKey,
            topPadding = 98.dp + monitorTopOffset,
            onTileBoundsUpdated = { k, r -> quickMenuTileBounds[k] = r },
            onClearTileBounds = { quickMenuTileBounds.clear() },
            onSaveLayout = { newLayout ->
                SpaceManager.updatePreferences { it.copy(quickMenuLayout = newLayout) }
            },
            onSetZoomPercent = { pct ->
                SpaceManager.updatePreferences { it.copy(textZoom = pct.coerceIn(50, 150)) }
            },
            onEditModeChanged = { isQuickMenuEditMode = it },
            onDismiss = {
                showChromeMenu = false
                hoveredQuickMenuKey = null
            }
        )

        // MCP 버튼을 덮으며 카드 자체에서 우상단 모프 구동 (닫기 시 먼지 소멸)
        AnimatedVisibility(
            visible = showMcpDrawer,
            enter = fadeIn(tween(90)),
            exit = if (prefs.cardVanishStyle != CardVanishStyle.OFF) ExitTransition.None else fadeOut(tween(110))
        ) {
            McpSettingsDrawer(
                anchorRect = mcpAnchorRect,
                onDismiss = { snap ->
                    emitDustSnapshot(snap, popupDustColor, 0.90f)
                    showMcpDrawer = false
                }
            )
        }

        // 크롬 태블릿 1:1 2-패널 전체 화면 설정 페이지
        BackHandler(enabled = showMcpDrawer || showOmniboxOverlay || showAiCommandPopup || showAllTabsDialog || readerMarkdown != null) {
            when {
                showMcpDrawer -> showMcpDrawer = false
                showAiCommandPopup -> showAiCommandPopup = false
                showOmniboxOverlay -> showOmniboxOverlay = false
                showAllTabsDialog -> showAllTabsDialog = false
                readerMarkdown != null -> readerMarkdown = null
            }
        }

        // 통합 상단 배너 스택 (다운로드 · 자동번역 · 닫은 탭 되돌리기 — 동시 표시 시 겹침 없이 세로 자동 정렬)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 98.dp + monitorTopOffset)
                .zIndex(46f)
                .animateContentSize(animationSpec = EgoMotion.intSizeSpring),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChromeDownloadStatusBanner(
                banner = downloadBanner,
                onOpenDetails = { SpaceManager.openDownloadsTab(activeSpace.id) },
                onOpenFile = { rec -> DownloadCenter.openFile(context, rec.contentUri, rec.mimeType, rec.fileName, forceChooser = true) },
                onDismiss = { DownloadCenter.dismissBanner() },
                onDustDismiss = { snap ->
                    emitDustSnapshot(snap, popupDustColor, 0.72f)
                    DownloadCenter.dismissBanner()
                }
            )
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
                        translateState = translateState.copy(isTranslated = false, visible = true, statusText = "원문 표시 중")
                    }
                },
                onReapplyTranslate = {
                    scope.launch {
                        val applied = TranslationManager.reapplyCachedTranslationInSpace(activeSpace.id)
                        if (applied) {
                            translateState = translateState.copy(isTranslated = true, visible = true, statusText = "번역됨")
                        } else {
                            triggerTranslation(translateState.sourceLang, translateState.targetLang)
                        }
                    }
                },
                onSwipeDismiss = {
                    translateState = translateState.copy(visible = false)
                },
                onDismiss = { snap ->
                    emitDustSnapshot(snap, popupDustColor, 0.55f)
                    translateState = translateState.copy(visible = false)
                }
            )
            UndoClosedTabBanner(
                entry = closedTabUndo,
                onUndo = { SpaceManager.reopenUndoClosedTab() },
                onSwipeDismiss = { SpaceManager.dismissClosedTabUndoBanner() },
                onDustDismiss = { snap ->
                    emitDustSnapshot(snap, popupDustColor, 0.68f)
                    SpaceManager.dismissClosedTabUndoBanner()
                }
            )
        }

        // 상단 탭 · MCP · 광고차단 · 번역 설정 · 번역 캡슐 · 다운로드 알림 통합 먼지 소멸 오버레이
        if (globalDustBursts.isNotEmpty()) {
            ParticleDisintegrationOverlay(
                bursts = globalDustBursts,
                onBurstFinished = { id ->
                    globalDustBursts = globalDustBursts.filterNot { it.id == id }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(120f)
            )
        }

        BrowserFullScreenFeatureOverlays(
            context = context,
            spaces = spaces,
            activeSpace = activeSpace,
            activeTab = activeTab,
            isEffectiveDark = isEffectiveDark,
            canAddAiSpace = canAddAiSpace,
            managingExt = managingExt,
            activeExtPopup = activeExtPopup,
            duplicateDownloadPrompt = duplicateDownloadPrompt,
            showDownloadsScreen = showDownloadsScreen,
            showChromeSettings = showChromeSettings,
            settingsInitialCategory = settingsInitialCategory,
            showAllTabsDialog = showAllTabsDialog,
            onUpdateManagingExt = { managingExt = it },
            onDismissExtPopup = { activeExtPopup = null },
            onDismissDownloadsScreen = { showDownloadsScreen = false },
            onOpenMcpDrawer = { showMcpDrawer = true },
            onDismissChromeSettings = { showChromeSettings = false },
            onOpenChromeSettings = { showChromeSettings = true },
            onDismissAllTabsDialog = { showAllTabsDialog = false }
        )

        BrowserHistoryAlertDialog(
            visible = showHistoryDialog,
            history = history,
            mutedIconColor = mutedIconColor,
            onNavigate = { url -> SpaceManager.navigate(activeSpace.id, url) },
            onDismiss = { showHistoryDialog = false }
        )

        BrowserCrashLogDialog(
            crashText = pendingCrashLog,
            onClear = { pendingCrashLog = null }
        )
    }
}

private class M3PullRefreshFrameLayout(context: Context) : FrameLayout(context) {
    var canChildScrollUp: () -> Boolean = { false }
    var onPullProgress: (Float) -> Unit = {}
    var onRefreshTrigger: () -> Unit = {}

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val triggerDistPx = 110f * context.resources.displayMetrics.density
    private var startX = 0f
    private var startY = 0f
    private var isPulling = false
    private var currentProgress = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isPulling = false
                currentProgress = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                if (ev.pointerCount > 1) return false
                val dy = ev.y - startY
                val dx = abs(ev.x - startX)
                if (dy > touchSlop * 1.4f && dy > dx * 1.6f && !canChildScrollUp()) {
                    isPulling = true
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPulling = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isPulling) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dy = (ev.y - startY).coerceAtLeast(0f)
                val damped = (dy / (triggerDistPx * 1.45f)).coerceIn(0f, 1.35f)
                currentProgress = damped
                onPullProgress(damped)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val shouldRefresh = ev.actionMasked == MotionEvent.ACTION_UP && currentProgress >= 0.78f
                isPulling = false
                currentProgress = 0f
                onPullProgress(0f)
                if (shouldRefresh) onRefreshTrigger()
                return true
            }
        }
        return super.onTouchEvent(ev)
    }
}

@Composable
private fun M3ExpressiveLoadingIndicator(
    pullProgress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val visible = isRefreshing || pullProgress > 0.04f
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val shapeColor = MaterialTheme.colorScheme.onPrimaryContainer

    val morphAnim = if (isRefreshing) {
        val inf = rememberInfiniteTransition(label = "m3ExpressiveMorph")
        val phase by inf.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "m3ShapePhase"
        )
        val spin by inf.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "m3ShapeSpin"
        )
        phase to spin
    } else {
        (pullProgress * 1.2f) to (pullProgress * 180f)
    }

    val targetOffsetY = if (isRefreshing) 18f else (pullProgress.coerceIn(0f, 1.25f) * 58f - 36f)
    val animatedOffsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 360f),
        label = "m3RefreshY"
    )
    val targetScale = if (isRefreshing) 1f else (0.52f + pullProgress.coerceIn(0f, 1f) * 0.48f)
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 400f),
        label = "m3RefreshScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.6f, animationSpec = tween(150)),
        exit = fadeOut(tween(240)) + scaleOut(targetScale = 0.35f, animationSpec = tween(240)) + slideOutVertically(tween(240)) { -it / 2 },
        modifier = modifier
    ) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 5.dp,
        modifier = Modifier
            .graphicsLayer {
                translationY = animatedOffsetY * density
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = if (isRefreshing) 1f else (pullProgress * 2.2f).coerceIn(0f, 1f)
            }
            .size(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseR = size.minDimension * 0.40f
                val phase = morphAnim.first
                val spinRad = (morphAnim.second * PI / 180.0).toFloat()

                // Material 3 Expressive 4-shape parametric morph:
                // 0: 10-petal Scallop Cookie (Morphe/Google M3E signature)
                // 1: 4-petal Soft Clover
                // 2: 8-petal Soft Starburst
                // 3: Rounded Squircle
                fun radiusFor(shapeIdx: Int, theta: Float): Float {
                    return when (shapeIdx % 4) {
                        0 -> baseR * (1.0f + 0.145f * cos(10f * theta))
                        1 -> baseR * (0.94f + 0.21f * cos(4f * theta))
                        2 -> baseR * (0.96f + 0.17f * cos(8f * theta))
                        else -> {
                            val c = abs(cos(theta))
                            val s = abs(sin(theta))
                            val norm = (c * c * c + s * s * s)
                            val sq = if (norm > 0.001f) 1f / kotlin.math.cbrt(norm.toDouble()).toFloat() else 1f
                            baseR * 0.86f * sq.coerceAtMost(1.18f)
                        }
                    }
                }

                val idxA = phase.toInt() % 4
                val idxB = (idxA + 1) % 4
                val rawT = phase - phase.toInt()
                val easedT = rawT * rawT * (3f - 2f * rawT)

                val steps = 96
                val path = Path()
                for (i in 0 until steps) {
                    val theta = (2.0 * PI * i / steps).toFloat()
                    val rA = radiusFor(idxA, theta)
                    val rB = radiusFor(idxB, theta)
                    val r = rA + (rB - rA) * easedT
                    val angle = theta + spinRad
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path = path, color = shapeColor)
            }
        }
    }
    }
}

@Composable
private fun WebViewHostContainer(
    spaces: List<BrowserSpace>,
    activeSpace: BrowserSpace
) {
    val activeTab = activeSpace.activeTab
    var pullProgress by remember { mutableFloatStateOf(0f) }
    var isExpressiveRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isExpressiveRefreshing, activeTab.isLoading) {
        if (isExpressiveRefreshing) {
            delay(850L)
            if (!activeTab.isLoading) {
                isExpressiveRefreshing = false
            } else {
                delay(1400L)
                isExpressiveRefreshing = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            M3PullRefreshFrameLayout(ctx).apply {
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

            swipe.canChildScrollUp = {
                activeWv.scrollY > 0 ||
                    activeWv.canScrollVertically(-1) ||
                    com.egolite.mobile.browser.EgoWebViewFactory.isInnerScrolled(activeTab.id)
            }
            swipe.onPullProgress = { p -> pullProgress = p }
            swipe.onRefreshTrigger = {
                isExpressiveRefreshing = true
                activeWv.reload()
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

            // 발열 최적화: 현재 활성 탭, 오디오 재생 탭, 작업 중인 AI 스페이스만 깨워두고 나머지 숨김 WebView는 onPause()
            val keepAwakeWebViews = buildSet {
                add(activeWv)
                spaces.forEach { sp ->
                    val isAiBusy = sp.aiStatus == AiSpaceStatus.WORKING || sp.aiStatus == AiSpaceStatus.THINKING
                    sp.tabs.forEach { t ->
                        val keep = (isAiBusy && t.id == sp.activeTabId) || (t.isPlayingAudio && !t.isMuted)
                        if (keep) {
                            SpaceManager.getExistingWebView(t.id)?.let { add(it) }
                        }
                    }
                }
            }

            var needsFront = false
            for (i in 0 until host.childCount) {
                val child = host.getChildAt(i)
                val wv = child as? android.webkit.WebView
                when {
                    child === activeWv -> {
                        if (child.visibility != View.VISIBLE || child.alpha != 1f) {
                            child.alpha = 1f
                            child.visibility = View.VISIBLE
                            needsFront = true
                        }
                        wv?.onResume()
                    }
                    child in bgAiWebViews -> {
                        if (child.visibility != View.INVISIBLE || child.alpha != 0f) {
                            child.alpha = 0f
                            child.visibility = View.INVISIBLE
                        }
                        if (wv != null) {
                            if (wv in keepAwakeWebViews) wv.onResume() else wv.onPause()
                        }
                    }
                    else -> {
                        if (child.visibility != View.GONE || child.alpha != 0f) {
                            child.alpha = 0f
                            child.visibility = View.GONE
                        }
                        if (wv != null) {
                            if (wv in keepAwakeWebViews) wv.onResume() else wv.onPause()
                        }
                    }
                }
            }
            if (needsFront || host.getChildAt(host.childCount - 1) !== activeWv) {
                activeWv.bringToFront()
                activeWv.requestLayout()
                host.requestLayout()
            }
        }
    )
    M3ExpressiveLoadingIndicator(
        pullProgress = pullProgress,
        isRefreshing = isExpressiveRefreshing,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(20f)
    )
    }
}


@Composable
private fun IsolatedTabProgressBar(
    isLoading: Boolean,
    progress: Int,
    isAiSpace: Boolean
) {
    if (isLoading && progress in 1..99) {
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = if (isAiSpace) EgoEmerald else MaterialTheme.colorScheme.primary
        )
    }
}


@Composable
private fun UnifiedAiDoneSpeechBubble(
    completedIds: List<String>,
    spaces: List<BrowserSpace>,
    onSelectSpace: (String) -> Unit,
    onAutoDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedSpaces = remember(completedIds, spaces) {
        completedIds.mapNotNull { cid -> spaces.firstOrNull { it.id == cid } }
    }
    LaunchedEffect(completedIds) {
        if (completedIds.isNotEmpty()) {
            delay(4500L)
            onAutoDismiss()
        }
    }
    AnimatedVisibility(
        visible = completedSpaces.isNotEmpty(),
        enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.72f, stiffness = 420f), initialScale = 0.86f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.88f),
        modifier = modifier
    ) {
        val latest = completedSpaces.lastOrNull() ?: return@AnimatedVisibility
        val label = if (completedSpaces.size == 1) {
            "${latest.name.replace("AI Space", "AI 공간")} 완료"
        } else {
            "${completedSpaces.first().name.replace("AI Space", "AI 공간")} 외 ${completedSpaces.size - 1}개 완료"
        }
        val bubbleBg = MaterialTheme.colorScheme.secondaryContainer
        val bubbleFg = MaterialTheme.colorScheme.onSecondaryContainer
        Column(horizontalAlignment = Alignment.Start) {
            // 작은 M3 말풍선 윗꼬리
            Canvas(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .size(width = 10.dp, height = 5.dp)
            ) {
                val tri = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.5f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(tri, color = bubbleBg)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bubbleBg,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .animateContentSize(spring(dampingRatio = 0.78f, stiffness = 360f))
                    .clickable { onSelectSpace(latest.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    completedSpaces.take(3).forEach { sp ->
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(parseSpaceColor(sp.accentColorHex))
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = bubbleFg,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
