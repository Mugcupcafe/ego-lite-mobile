package com.egolite.mobile.ui
import com.egolite.mobile.browser.SpaceManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.Animatable
import com.egolite.mobile.model.ProgressBarStyle
import com.egolite.mobile.browser.ClosedTabUndoEntry
import androidx.compose.material.icons.filled.Restore
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.egolite.mobile.CrashLogger
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.BrowserPreferences
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.BrowserTab
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.SpaceType
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * BrowserScreen() JVM 64KB 메서드 크기 제한을 영구 회피하기 위해
 * 메뉴 드래그 버튼 · 찾기 바 · AI 제어권 바 · 플로팅 탭 · 오버레이/다이얼로그를 분리한 파일.
 */
@Composable
fun BrowserMenuDragButton(
    showChromeMenu: Boolean,
    chromeMenuItems: List<ChromeMenuItemSpec>,
    tileBounds: Map<String, Rect>,
    activeIconColor: Color,
    onSetShowMenu: (Boolean) -> Unit,
    onSetHoveredKey: (String?) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val tapSlopPx = with(density) { 12.dp.toPx() }
    val openThresholdPx = with(density) { 20.dp.toPx() }
    val closeThresholdPx = with(density) { 8.dp.toPx() }
    val latestShowMenu by rememberUpdatedState(showChromeMenu)
    val latestMenuItems by rememberUpdatedState(chromeMenuItems)
    val latestBounds by rememberUpdatedState(tileBounds)
    var buttonRootPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(38.dp)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) buttonRootPos = coords.positionInRoot()
            }
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val wasAlreadyOpen = latestShowMenu
                    var menuOpenNow = wasAlreadyOpen
                    var dragged = false
                    var lastHoverKey: String? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val itemsNow = latestMenuItems
                        if (!change.pressed) {
                            if (dragged) {
                                if (menuOpenNow && lastHoverKey != null) {
                                    val chosen = itemsNow.firstOrNull { it.key == lastHoverKey }
                                    onSetHoveredKey(null)
                                    if (chosen != null && chosen.zoomPercent == null) {
                                        onSetShowMenu(false)
                                        chosen.onClick()
                                    }
                                } else {
                                    onSetShowMenu(false)
                                    onSetHoveredKey(null)
                                }
                            } else {
                                // 단순 탭: 누른 순간이 아니라 뗄 때 메뉴 토글 (모프 애니메이션 재생)
                                onSetShowMenu(!wasAlreadyOpen)
                                onSetHoveredKey(null)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            break
                        }
                        val dy = change.position.y - down.position.y
                        val dx = change.position.x - down.position.x
                        if (!dragged && (abs(dy) > tapSlopPx || abs(dx) > tapSlopPx)) {
                            dragged = true
                        }
                        // 아래로 내리면 모프 열기 · 다시 위로 올리면 역모프 닫기 · 다시 내리면 재오픈 (히스테리시스 적용)
                        if (!menuOpenNow && dy >= openThresholdPx) {
                            menuOpenNow = true
                            dragged = true
                            onSetShowMenu(true)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else if (menuOpenNow && dragged && dy <= closeThresholdPx) {
                            menuOpenNow = false
                            lastHoverKey = null
                            onSetHoveredKey(null)
                            onSetShowMenu(false)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        if (dragged && menuOpenNow) {
                            val rootPt = buttonRootPos + change.position
                            val hitKey = latestBounds.entries.firstOrNull { (_, r) ->
                                rootPt.x in r.left..r.right && rootPt.y in r.top..r.bottom
                            }?.key
                            if (hitKey != lastHoverKey) {
                                lastHoverKey = hitKey
                                onSetHoveredKey(hitKey)
                                if (hitKey != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        EgoIcon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "브라우저 메뉴",
            tint = activeIconColor,
            symbolSize = 20.dp
        )
    }
}

@Composable
fun BrowserFindInPageBar(
    visible: Boolean,
    findQuery: String,
    activeIconColor: Color,
    mutedIconColor: Color,
    onQueryChange: (String) -> Unit,
    onFindPrev: () -> Unit,
    onFindNext: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
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
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = activeIconColor),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (findQuery.isEmpty()) Text("페이지 내 검색어 입력...", fontSize = 14.sp, color = mutedIconColor)
                    inner()
                }
            )
            IconButton(onClick = onFindPrev) {
                EgoIcon(Icons.Default.KeyboardArrowUp, contentDescription = "이전", tint = activeIconColor)
            }
            IconButton(onClick = onFindNext) {
                EgoIcon(Icons.Default.KeyboardArrowDown, contentDescription = "다음", tint = activeIconColor)
            }
            IconButton(onClick = onClose) {
                EgoIcon(Icons.Default.Close, contentDescription = "닫기", tint = activeIconColor)
            }
        }
    }
}

@Composable
fun BrowserAiTakeoverBar(
    activeSpace: BrowserSpace,
    onToggleTakeover: () -> Unit
) {
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
            TextButton(onClick = onToggleTakeover) {
                Text(
                    text = if (isPaused) "AI 제어 재개" else "직접 조작 (Takeover)",
                    color = EgoEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BrowserFloatingDraggedTabOverlay(
    floatingTab: BrowserTab?,
    rootBoxBounds: Rect?,
    dragPointerRoot: Offset,
    draggedTabWidthPx: Float,
    spaces: List<BrowserSpace>,
    hoveredDropSpaceId: String?,
    hoveredNewSpaceDrop: Boolean,
    prefs: BrowserPreferences
) {
    if (floatingTab == null) return
    val density = LocalDensity.current
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
    val exactTabWidthPx = (draggedTabWidthPx - with(density) { 4.dp.toPx() })
        .coerceIn(with(density) { 68.dp.toPx() }, with(density) { 240.dp.toPx() })
    val exactTabWidthDp = with(density) { exactTabWidthPx.toDp() }
    val pillHalfW = (exactTabWidthPx / 2f).roundToInt()
    val pillHalfH = with(density) { 18.dp.roundToPx() }
    val floatingGlowColor = MaterialTheme.colorScheme.primary
    val floatingEff = prefs.rimGlowIntensity.coerceIn(0.5f, 2.5f)

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 16.dp,
        modifier = Modifier
            .offset { IntOffset(localX - pillHalfW, localY - pillHalfH) }
            .height(36.dp)
            .width(if (badgeText != null) (exactTabWidthDp + 56.dp).coerceAtMost(268.dp) else exactTabWidthDp)
            .zIndex(45f)
            .drawBehind {
                if (prefs.rimGlowEnabled) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val auraR = (size.width * 0.68f).coerceIn(110f, 260f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (0.28f * floatingEff).coerceAtMost(0.55f)),
                                floatingGlowColor.copy(alpha = (0.42f * floatingEff).coerceAtMost(0.75f)),
                                floatingGlowColor.copy(alpha = (0.14f * floatingEff).coerceAtMost(0.30f)),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = auraR
                        ),
                        radius = auraR,
                        center = Offset(cx, cy)
                    )
                }
            }
            .border(
                width = 1.6.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (prefs.rimGlowEnabled) 0.85f else 0.35f),
                        floatingGlowColor,
                        Color.White.copy(alpha = if (prefs.rimGlowEnabled) 0.65f else 0.25f)
                    )
                ),
                shape = CircleShape
            )
            .graphicsLayer {
                val s = if (inDropMode) 0.96f else 1.06f
                scaleX = s
                scaleY = s
            }
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

@Composable
fun BoxScope.BrowserRecentDropPromptChip(
    recentDropPromptChip: Triple<String, String, String>?,
    showAiCommandPopup: Boolean,
    connectionState: McpConnectionState,
    onClickChip: (Triple<String, String, String>) -> Unit
) {
    AnimatedVisibility(
        visible = recentDropPromptChip != null && !showAiCommandPopup && connectionState == McpConnectionState.CONNECTED,
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
                    .clickable { onClickChip(info) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                        symbolSize = 16.dp
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
}

@Composable
fun BrowserReaderModeOverlay(
    readerMarkdown: String?,
    onDismiss: () -> Unit
) {
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
                    IconButton(onClick = onDismiss) {
                        EgoIcon(Icons.Default.Close, contentDescription = "닫기")
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
}

@Composable
fun BrowserFullscreenVideoOverlay(
    videoView: View?,
    prefs: BrowserPreferences
) {
    videoView?.let { v ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(55f)
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        (v.parent as? ViewGroup)?.removeView(v)
                        addView(
                            v,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
            FullscreenVideoVerticalHudOverlay(
                enabled = prefs.videoVerticalHudEnabled,
                lightIntensity = prefs.videoHudLightIntensity,
                particleDensity = prefs.videoHudParticleDensity,
                syncSystemBrightness = prefs.videoHudSyncSystemBrightness
            )
        }
    }
}

@Composable
fun BrowserHistoryAlertDialog(
    visible: Boolean,
    history: List<BookmarkItem>,
    mutedIconColor: Color,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
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
                                    onNavigate(item.url)
                                    onDismiss()
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
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

@Composable
fun BrowserCrashLogDialog(
    crashText: String?,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    crashText?.let { text ->
        AlertDialog(
            onDismissRequest = {
                CrashLogger.clearLastCrash(context)
                onClear()
            },
            title = { Text("이전 종료 크래시 로그") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = text,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("CrashLog", text))
                    Toast.makeText(context, "크래시 로그 복사됨", Toast.LENGTH_SHORT).show()
                    CrashLogger.clearLastCrash(context)
                    onClear()
                }) {
                    Text("로그 복사")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    CrashLogger.clearLastCrash(context)
                    onClear()
                }) {
                    Text("닫기")
                }
            }
        )
    }
}


/**
 * 닫은 탭 되돌리기 상단 알약 배너
 * - 4.5초 하모니 광감/파티클 카운트다운 프로그레스 바 -> 만료 시 자동 파티클 소멸
 * - 좌/우 스와이프 제거 시 파티클 없이 물리적으로 화면 밖으로 슬라이드 아웃
 */
@Composable
fun UndoClosedTabBanner(
    entry: ClosedTabUndoEntry?,
    onUndo: () -> Unit,
    onSwipeDismiss: () -> Unit,
    onDustDismiss: (VanishSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by SpaceManager.preferences.collectAsState()
    var bannerRect by remember { mutableStateOf<Rect?>(null) }
    var bannerCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val captureLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var dismissedViaDust by remember(entry?.seq) { mutableStateOf(false) }

    AnimatedVisibility(
        visible = entry != null,
        enter = slideInVertically(EgoMotion.intOffsetSpring) { -it } + fadeIn(tween(180)),
        exit = if (dismissedViaDust) ExitTransition.None else (slideOutVertically(tween(160)) { -it } + fadeOut(tween(130))),
        modifier = modifier
    ) {
        val cur = entry ?: return@AnimatedVisibility
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val cardBg = if (isDark) Color(0xFF292C33) else Color(0xFFF3F6FC)
        val density = LocalDensity.current
        val dismissThresholdPx = with(density) { 92.dp.toPx() }
        val swipeX = remember(cur.seq) { Animatable(0f) }
        val countdownProg = remember(cur.seq) { Animatable(1f) }
        var isDragging by remember(cur.seq) { mutableStateOf(false) }

        fun triggerDustClose() {
            if (dismissedViaDust) return
            dismissedViaDust = true
            scope.launch {
                val bmp = runCatching { captureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                val liveRect = bannerCoords?.takeIf { it.isAttached }?.boundsInRoot() ?: bannerRect
                val origin = liveRect?.let { Offset(it.right - 22f, it.center.y) }
                onDustDismiss(VanishSnapshot(rect = liveRect, touchRoot = origin, bitmap = bmp))
            }
        }

        // 4.5초 자동 카운트다운 (드래그 중에는 일시정지, 완료 시 파티클 소멸)
        LaunchedEffect(cur.seq, isDragging) {
            if (!isDragging && countdownProg.value > 0.01f) {
                val remainingMs = (countdownProg.value * 4500f).toInt().coerceAtLeast(120)
                countdownProg.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = remainingMs, easing = LinearEasing)
                )
                if (!isDragging && countdownProg.value <= 0.01f) {
                    triggerDustClose()
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .padding(horizontal = 12.dp)
                .onGloballyPositioned { coords -> bannerCoords = coords; bannerRect = coords.boundsInRoot() }
                .drawWithContent {
                    captureLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(captureLayer)
                }
                .graphicsLayer {
                    translationX = swipeX.value
                    alpha = (1f - (kotlin.math.abs(swipeX.value) / (dismissThresholdPx * 2.2f))).coerceIn(0.15f, 1f)
                }
                .pointerInput(cur.seq) {
                    val vt = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            vt.resetTracking()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            vt.addPosition(change.uptimeMillis, change.position)
                            scope.launch { swipeX.snapTo(swipeX.value + dragAmount) }
                        },
                        onDragEnd = {
                            val vx = vt.calculateVelocity().x
                            scope.launch {
                                if (kotlin.math.abs(swipeX.value) >= dismissThresholdPx || kotlin.math.abs(vx) >= 950f) {
                                    val dir = if (kotlin.math.abs(vx) >= 950f) (if (vx >= 0f) 1f else -1f) else (if (swipeX.value >= 0f) 1f else -1f)
                                    swipeX.animateTo(
                                        targetValue = dir * dismissThresholdPx * 4.5f,
                                        animationSpec = EgoMotion.responsiveSpring(),
                                        initialVelocity = vx
                                    )
                                    onSwipeDismiss()
                                } else {
                                    isDragging = false
                                    swipeX.animateTo(0f, animationSpec = EgoMotion.responsiveSpring(), initialVelocity = vx)
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch { swipeX.animateTo(0f, animationSpec = EgoMotion.responsiveSpring()) }
                        }
                    )
                }
                .harmonyRimGlow(enabled = prefs.rimGlowEnabled, cornerRadius = 22.dp, glowColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // 하모니 광감 + 양방향 입자 카운트다운 진행바 (우측 끝 1.0 -> 좌측 끝 0.0 정밀 진행)
                HarmonyHorizontalProgressCanvas(
                    style = ProgressBarStyle.HARMONY_OPTICAL,
                    progress = countdownProg.value.coerceIn(0f, 1f),
                    isActive = countdownProg.value > 0.005f,
                    accentColor = MaterialTheme.colorScheme.primary,
                    lightIntensity = prefs.progressLightIntensity,
                    particleDensity = prefs.progressParticleDensity,
                    darkUseThemeColor = prefs.progressDarkUseThemeColor,
                    cornerRadiusDp = 22.dp,
                    isCountdown = true,
                    modifier = Modifier.matchParentSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                        symbolSize = 20.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "탭을 닫았습니다",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = cur.title,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onUndo() }
                    ) {
                        Text(
                            text = "되돌리기",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { triggerDustClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        EgoIcon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                            symbolSize = 16.dp
                        )
                    }
                }
            }
        }
    }
}
