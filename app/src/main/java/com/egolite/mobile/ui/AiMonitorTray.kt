package com.egolite.mobile.ui

import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.Canvas
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.runtime.mutableIntStateOf
import com.egolite.mobile.model.CardVanishStyle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.random.Random
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.MonitorPosition
import com.egolite.mobile.model.SpaceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 반응형 AI 라이브 모니터 트레이 (세로=상단 TOP / 가로=좌측 LEFT, 설정에서 커스텀 가능)
 * - 좌측(LEFT) 모드: 스크롤 카드가 상단 캡슐·접기 버튼 아래로 올라갈 때 딱딱하게 잘리지 않고
 *   상단 페이드 그라데이션(배경색 → 투명) 속으로 부드럽게 사라지도록 처리
 * - 고정 순서 유지 + 양방향 블러 모프 + 길게 눌러 카드(Personal/AI Space) 순서 이동 지원
 */
@Composable
fun AiLiveMonitorTray(
    visible: Boolean,
    position: MonitorPosition,
    spaces: List<BrowserSpace>,
    activeSpaceId: String,
    returningSpaceId: String?,
    connectedAiName: String,
    canAddAiSpace: Boolean,
    contentAspectRatio: Float = 1.35f,
    isTabDragging: Boolean = false,
    hoveredDropSpaceId: String? = null,
    isHoveredNewSpaceDrop: Boolean = false,
    onRegisterSpaceBounds: (String, Rect) -> Unit = { _, _ -> },
    onRegisterNewSpaceBounds: (Rect?) -> Unit = {},
    onSelectSpace: (String) -> Unit,
    onCloseSpace: (String) -> Unit = {},
    onMoveSpace: (fromId: String, toId: String) -> Unit,
    onCreateAiSpace: () -> Unit,
    onGlobalDustVanish: ((VanishSnapshot, Color) -> Unit)? = null,
    onCollapse: () -> Unit
) {
    DisposableEffect(isTabDragging, visible) {
        onDispose {
            if (!isTabDragging || !visible) {
                onRegisterNewSpaceBounds(null)
            }
        }
    }
    val monitoredSpaces = remember(spaces, activeSpaceId) {
        spaces.filter { it.id != activeSpaceId }
    }
    var thumbnails by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 발열 최적화: 열릴 때 1회 캡처 후, 실제 작업 중(WORKING/THINKING)인 스페이스만 2.5초 주기로 갱신
    LaunchedEffect(visible, monitoredSpaces.map { "${it.id}:${it.activeTab.id}:${it.activeTab.url}:${it.aiStatus}:${it.lastActionSummary}" }) {
        if (!visible) return@LaunchedEffect
        val initial = mutableMapOf<String, Bitmap>()
        monitoredSpaces.forEach { sp ->
            val bmp = SpaceManager.captureTabThumbnail(sp.activeTab.id)
            if (bmp != null) {
                initial[sp.id] = bmp
            } else {
                thumbnails[sp.id]?.let { initial[sp.id] = it }
            }
        }
        thumbnails = initial

        val hasActiveWork = monitoredSpaces.any {
            it.aiStatus == AiSpaceStatus.WORKING || it.aiStatus == AiSpaceStatus.THINKING
        }
        if (!hasActiveWork) return@LaunchedEffect

        while (true) {
            delay(2500L)
            val updated = thumbnails.toMutableMap()
            var changed = false
            monitoredSpaces.forEach { sp ->
                if (sp.aiStatus == AiSpaceStatus.WORKING || sp.aiStatus == AiSpaceStatus.THINKING) {
                    val bmp = SpaceManager.captureTabThumbnail(sp.activeTab.id)
                    if (bmp != null) {
                        updated[sp.id] = bmp
                        changed = true
                    }
                }
            }
            if (changed) thumbnails = updated
        }
    }

    val prefs by SpaceManager.preferences.collectAsState()
    val activeVanishBursts = remember { mutableStateListOf<DustVanishRequest>() }
    val vanishingSpaceIds = remember { mutableStateListOf<String>() }
    val cardLocalRects = remember { mutableMapOf<String, Rect>() }
    val cardRootRects = remember { mutableMapOf<String, Rect>() }
    val cardCaptureLayers = remember { mutableMapOf<String, GraphicsLayer>() }
    val vanishScope = rememberCoroutineScope()

    fun triggerDustVanishClose(space: BrowserSpace, dropOffset: Offset = Offset.Zero) {
        if (space.id in vanishingSpaceIds) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val style = prefs.cardVanishStyle
        val baseRect = cardLocalRects[space.id]
        val baseRootRect = cardRootRects[space.id]
        if (style == CardVanishStyle.OFF || (baseRect == null && baseRootRect == null)) {
            onCloseSpace(space.id)
            return
        }
        val isDraggedDrop = dropOffset != Offset.Zero
        val spaceAccent = parseSpaceColor(space.accentColorHex)

        vanishScope.launch {
            // 초록 둥근 테두리 + 웹 미리보기 + 하단 푸터까지 카드 실픽셀 그대로 캡처!
            // 드래그 중 원본 카드가 축소/투명 상태일 수 있으므로 썸네일 폴백 + 안전 캡처 보장
            val capturedCardBmp = (if (isDraggedDrop) thumbnails[space.id] else null)
                ?: runCatching { cardCaptureLayers[space.id]?.toImageBitmap()?.asAndroidBitmap() }.getOrNull()
                ?: thumbnails[space.id]

            vanishingSpaceIds.add(space.id)

            if (onGlobalDustVanish != null && baseRootRect != null && baseRootRect.width > 10f && baseRootRect.height > 10f) {
                // 화면 최상위 전역 오버레이로 방출 -> 좌측 패널 경계를 넘어 웹뷰 위까지 자유롭게 흩날림!
                val finalRoot = baseRootRect
                val touchRoot = if (isDraggedDrop) finalRoot.center else Offset(finalRoot.right - 24f, finalRoot.top + 24f)
                onGlobalDustVanish(
                    VanishSnapshot(rect = finalRoot, touchRoot = touchRoot, bitmap = capturedCardBmp),
                    spaceAccent
                )
            } else if (baseRect != null) {
                val finalRect = if (isDraggedDrop) baseRect.translate(dropOffset.x, dropOffset.y) else baseRect
                val originX = if (isDraggedDrop) finalRect.center.x else (finalRect.right - 24f)
                val originY = if (isDraggedDrop) finalRect.center.y else (finalRect.top + 24f)
                activeVanishBursts.add(
                    DustVanishRequest(
                        id = System.nanoTime(),
                        rect = finalRect,
                        accentColor = spaceAccent,
                        thumbnail = capturedCardBmp,
                        style = style,
                        particleCount = prefs.vanishParticleCount,
                        durationMs = prefs.vanishDurationMs,
                        originX = originX,
                        originY = originY,
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
                )
            }

            val holdMs = if (isDraggedDrop) 50L else when (style) {
                CardVanishStyle.HARMONY_DUST, CardVanishStyle.HARMONY_GPU_SHADER, CardVanishStyle.HARMONY_7_SAND_WAVE ->
                    (prefs.vanishDurationMs * 0.42f).toLong().coerceIn(220L, 560L)
                CardVanishStyle.LIGHT_PARTICLES -> 240L
                CardVanishStyle.SCALE_FADE -> 160L
                CardVanishStyle.OFF -> 0L
            }
            delay(holdMs)
            onCloseSpace(space.id)
            vanishingSpaceIds.remove(space.id)
        }
    }

    fun finishVanishBurst(burstId: Long) {
        activeVanishBursts.removeAll { it.id == burstId }
    }

    if (position == MonitorPosition.LEFT) {
        // ============================================================
        // 좌측 사이드바 모드 (가로모드 기본): 상단 페이드 그라데이션 오버레이 적용
        // ============================================================
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)) + expandHorizontally(animationSpec = EgoMotion.intSizeSpring),
            exit = fadeOut(tween(180)) + shrinkHorizontally(animationSpec = tween(220))
        ) {
            val listState = rememberLazyListState()
            var draggingId by remember { mutableStateOf<String?>(null) }
            var dragStartIdx by remember { mutableIntStateOf(-1) }
            var dragTargetIdx by remember { mutableIntStateOf(-1) }
            var dragOffsetX by remember { mutableFloatStateOf(0f) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var draggedStepPx by remember { mutableFloatStateOf(220f) }
            var isHoveringDelete by remember { mutableStateOf(false) }
            var trayHeightPx by remember { mutableFloatStateOf(800f) }
            val latestSpaces by rememberUpdatedState(monitoredSpaces)
            val spacingPx = with(density) { 14.dp.toPx() }
            val topPadPx = with(density) { 56.dp.toPx() }
            val deleteSwipeThresholdPx = with(density) { 68.dp.toPx() }
            val bgColor = MaterialTheme.colorScheme.background
            val leftElastic = rememberElasticOverscroll(isVertical = true, maxPullPx = 88f)
            val draggingSpaceIsAi = draggingId != null && monitoredSpaces.firstOrNull { it.id == draggingId }?.type == SpaceType.AI

            Box(
                modifier = Modifier
                    .width(244.dp)
                    .fillMaxHeight()
                    .background(Color.Transparent)
                    .onGloballyPositioned { coords -> trayHeightPx = coords.size.height.toFloat() }
            ) {
                // 전체 높이 스크롤 리스트 (드래그 중 리스트 변경 없이 인접 카드만 부드럽게 비켜주기 -> 튕김 완전 제거)
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 56.dp, bottom = 56.dp, start = 13.dp, end = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp + leftElastic.extraSpacingDp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(leftElastic.containerModifier)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    val curList = latestSpaces
                                    if (curList.isEmpty()) return@detectDragGesturesAfterLongPress
                                    val adjY = startOffset.y - topPadPx
                                    val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        item.key is String && !(item.key as String).startsWith("__") &&
                                            adjY >= item.offset && adjY <= item.offset + item.size
                                    }
                                    val key = hit?.key as? String
                                    val idx = curList.indexOfFirst { it.id == key }
                                    if (key != null && idx >= 0) {
                                        draggingId = key
                                        dragStartIdx = idx
                                        dragTargetIdx = idx
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        draggedStepPx = (hit.size.toFloat() + spacingPx)
                                        isHoveringDelete = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val curId = draggingId ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += dragAmount.y
                                    val curList = latestSpaces
                                    val draggedSpace = curList.firstOrNull { it.id == curId }
                                    val canDelete = draggedSpace?.type == SpaceType.AI
                                    val ptrY = change.position.y
                                    val hitDelete = canDelete && (ptrY >= trayHeightPx - with(density) { 84.dp.toPx() } || dragOffsetX <= -deleteSwipeThresholdPx)
                                    if (hitDelete != isHoveringDelete) {
                                        isHoveringDelete = hitDelete
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    if (!hitDelete && curList.size > 1 && dragStartIdx >= 0) {
                                        val deltaSlots = kotlin.math.round(dragOffsetY / draggedStepPx.coerceAtLeast(80f)).toInt()
                                        val nextIdx = (dragStartIdx + deltaSlots).coerceIn(0, curList.lastIndex)
                                        if (nextIdx != dragTargetIdx) {
                                            dragTargetIdx = nextIdx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val curId = draggingId
                                    val fromIdx = dragStartIdx
                                    val toIdx = dragTargetIdx
                                    val delNow = isHoveringDelete
                                    val curList = latestSpaces
                                    val draggedSpace = curList.firstOrNull { it.id == curId }
                                    if (delNow && draggedSpace != null && draggedSpace.type == SpaceType.AI) {
                                        triggerDustVanishClose(draggedSpace, Offset(dragOffsetX * 0.45f, dragOffsetY))
                                    } else if (curId != null && fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                                        val targetSpace = curList.getOrNull(toIdx)
                                        if (targetSpace != null && targetSpace.id != curId) {
                                            onMoveSpace(curId, targetSpace.id)
                                        }
                                    }
                                    draggingId = null
                                    dragStartIdx = -1
                                    dragTargetIdx = -1
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    isHoveringDelete = false
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragStartIdx = -1
                                    dragTargetIdx = -1
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    isHoveringDelete = false
                                }
                            )
                        }
                ) {
                    if (monitoredSpaces.isEmpty() && !isTabDragging) {
                        item(key = "__empty_ai_left__") {
                            EmptyAiMonitorCard(isLeftSidebar = true, onCreateAiSpace = onCreateAiSpace)
                        }
                    }
                    itemsIndexed(monitoredSpaces, key = { _, sp -> sp.id }) { idx, space ->
                        val isDragging = draggingId == space.id
                        val rawNeighborShift = when {
                            draggingId == null || isHoveringDelete || dragStartIdx < 0 || dragTargetIdx < 0 || isDragging -> 0f
                            dragStartIdx < dragTargetIdx && idx in (dragStartIdx + 1)..dragTargetIdx -> -draggedStepPx
                            dragStartIdx > dragTargetIdx && idx in dragTargetIdx until dragStartIdx -> draggedStepPx
                            else -> 0f
                        }
                        val neighborShiftY by animateFloatAsState(
                            targetValue = rawNeighborShift,
                            animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
                            label = "leftMonitorShift_${space.id}"
                        )
                        MonitorSpaceCard(
                            space = space,
                            index = idx,
                            isLeftSidebar = true,
                            contentAspectRatio = contentAspectRatio,
                            trayVisible = visible,
                            isReturning = space.id == returningSpaceId,
                            isDragging = isDragging,
                            isDeleteHovered = isDragging && isHoveringDelete,
                            isDropHovered = hoveredDropSpaceId == space.id,
                            dragTranslationX = if (isDragging) dragOffsetX * 0.45f else 0f,
                            dragTranslationY = if (isDragging) dragOffsetY else neighborShiftY,
                            connectedAiName = connectedAiName,
                            thumbnail = thumbnails[space.id],
                            modifier = Modifier
                                .then(if (draggingId == null) Modifier.animateItem(placementSpec = spring(dampingRatio = 0.88f, stiffness = 360f)) else Modifier)
                                .graphicsLayer { alpha = if (space.id in vanishingSpaceIds) 0f else 1f }
                                .let { baseMod ->
                                    val cardLayer = rememberGraphicsLayer()
                                    cardCaptureLayers[space.id] = cardLayer
                                    baseMod.drawWithContent {
                                        cardLayer.record { this@drawWithContent.drawContent() }
                                        drawLayer(cardLayer)
                                    }
                                }
                                .onGloballyPositioned { coords ->
                                    val rBounds = coords.boundsInRoot()
                                    onRegisterSpaceBounds(space.id, rBounds)
                                    cardRootRects[space.id] = rBounds
                                    val pos = coords.positionInParent()
                                    cardLocalRects[space.id] = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
                                },
                            onSelect = { onSelectSpace(space.id) },
                            onClose = if (space.type == SpaceType.AI) ({ triggerDustVanishClose(space) }) else null
                        )
                    }
                    if (isTabDragging && canAddAiSpace) {
                        item(key = "__new_ai_space_drop_left__") {
                            val fullPane = monitoredSpaces.isEmpty()
                            NewAiSpaceDropZoneCard(
                                isLeftSidebar = true,
                                isHovered = isHoveredNewSpaceDrop,
                                isFullPane = fullPane,
                                modifier = Modifier
                                    .then(if (fullPane) Modifier.fillParentMaxHeight(0.88f) else Modifier)
                                    .animateItem(placementSpec = spring(dampingRatio = 0.88f, stiffness = 360f))
                                    .onGloballyPositioned { coords ->
                                        onRegisterNewSpaceBounds(coords.boundsInRoot())
                                    }
                            )
                        }
                    }
                }

                // 상단 그라데이션 페이드 마스크 (캡슐 + 접기 버튼 뒤로 카드가 위로 올라갈수록 점점 투명하게 사라짐)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(78.dp)
                        .background(
                            Brush.verticalGradient(
                                0.00f to bgColor,
                                0.56f to bgColor.copy(alpha = 0.90f),
                                0.80f to bgColor.copy(alpha = 0.45f),
                                1.00f to Color.Transparent
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (canAddAiSpace) {
                            IconButton(
                                onClick = onCreateAiSpace,
                                modifier = Modifier.size(32.dp).pressBounceScale()
                            ) {
                                EgoIcon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "새 AI 공간",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                , symbolSize = 18.dp)
                            }
                        }
                        IconButton(
                            onClick = onCollapse,
                            modifier = Modifier.size(32.dp).pressBounceScale()
                        ) {
                            EgoIcon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "AI 모니터 접기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            , symbolSize = 20.dp)
                        }
                    }
                }
                MonitorDeleteDropZone(
                    visible = draggingSpaceIsAi,
                    isHovered = isHoveringDelete,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp, start = 14.dp, end = 14.dp)
                        .zIndex(30f)
                )

                if (activeVanishBursts.isNotEmpty()) {
                    ParticleDisintegrationOverlay(
                        bursts = activeVanishBursts,
                        onBurstFinished = { bid -> finishVanishBurst(bid) }
                    )
                }
            }
        }
    } else {
        // ============================================================
        // 상단 트레이 모드 (세로모드 기본): 구분선 없음, 탭 바와 배경 통일
        // ============================================================
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)) + expandVertically(animationSpec = EgoMotion.intSizeSpring),
            exit = fadeOut(tween(180)) + shrinkVertically(animationSpec = tween(220))
        ) {
            val rowState = rememberLazyListState()
            var draggingId by remember { mutableStateOf<String?>(null) }
            var dragStartIdx by remember { mutableIntStateOf(-1) }
            var dragTargetIdx by remember { mutableIntStateOf(-1) }
            var dragOffsetX by remember { mutableFloatStateOf(0f) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var draggedStepPx by remember { mutableFloatStateOf(200f) }
            var isHoveringDelete by remember { mutableStateOf(false) }
            val latestSpaces by rememberUpdatedState(monitoredSpaces)
            val spacingPx = with(density) { 14.dp.toPx() }
            val startPadPx = with(density) { 14.dp.toPx() }
            val deletePullDownPx = with(density) { 64.dp.toPx() }
            val draggingSpaceIsAi = draggingId != null && monitoredSpaces.firstOrNull { it.id == draggingId }?.type == SpaceType.AI
            val topElastic = rememberElasticOverscroll(isVertical = false, maxPullPx = 88f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(322.dp)
                    .background(Color.Transparent)
            ) {
                // 상단 컨트롤 헤더
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(72.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canAddAiSpace) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .pressBounceScale()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { onCreateAiSpace() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "AI Space 추가",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    , symbolSize = 14.dp)
                                    Text(
                                        text = "새 AI 공간",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = onCollapse,
                            modifier = Modifier.size(32.dp).pressBounceScale()
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "AI 모니터 접기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            , symbolSize = 20.dp)
                        }
                    }
                }

                // 가로 스크롤 카드 행 (드래그 중 리스트 변경 없이 인접 카드만 부드럽게 비켜주기 + 아래로 끌어 삭제)
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp + topElastic.extraSpacingDp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(topElastic.containerModifier)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    val curList = latestSpaces
                                    if (curList.isEmpty()) return@detectDragGesturesAfterLongPress
                                    val adjX = startOffset.x - startPadPx
                                    val hit = rowState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        item.key is String && !(item.key as String).startsWith("__") &&
                                            adjX >= item.offset && adjX <= item.offset + item.size
                                    }
                                    val key = hit?.key as? String
                                    val idx = curList.indexOfFirst { it.id == key }
                                    if (key != null && idx >= 0) {
                                        draggingId = key
                                        dragStartIdx = idx
                                        dragTargetIdx = idx
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        draggedStepPx = (hit.size.toFloat() + spacingPx)
                                        isHoveringDelete = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val curId = draggingId ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += dragAmount.y
                                    val curList = latestSpaces
                                    val draggedSpace = curList.firstOrNull { it.id == curId }
                                    val canDelete = draggedSpace?.type == SpaceType.AI
                                    val hitDelete = canDelete && dragOffsetY >= deletePullDownPx
                                    if (hitDelete != isHoveringDelete) {
                                        isHoveringDelete = hitDelete
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    if (!hitDelete && curList.size > 1 && dragStartIdx >= 0) {
                                        val deltaSlots = kotlin.math.round(dragOffsetX / draggedStepPx.coerceAtLeast(80f)).toInt()
                                        val nextIdx = (dragStartIdx + deltaSlots).coerceIn(0, curList.lastIndex)
                                        if (nextIdx != dragTargetIdx) {
                                            dragTargetIdx = nextIdx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val curId = draggingId
                                    val fromIdx = dragStartIdx
                                    val toIdx = dragTargetIdx
                                    val delNow = isHoveringDelete
                                    val curList = latestSpaces
                                    val draggedSpace = curList.firstOrNull { it.id == curId }
                                    if (delNow && draggedSpace != null && draggedSpace.type == SpaceType.AI) {
                                        triggerDustVanishClose(draggedSpace, Offset(dragOffsetX, dragOffsetY * 0.55f))
                                    } else if (curId != null && fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                                        val targetSpace = curList.getOrNull(toIdx)
                                        if (targetSpace != null && targetSpace.id != curId) {
                                            onMoveSpace(curId, targetSpace.id)
                                        }
                                    }
                                    draggingId = null
                                    dragStartIdx = -1
                                    dragTargetIdx = -1
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    isHoveringDelete = false
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragStartIdx = -1
                                    dragTargetIdx = -1
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    isHoveringDelete = false
                                }
                            )
                        }
                ) {
                    if (monitoredSpaces.isEmpty() && !isTabDragging) {
                        item(key = "__empty_ai_top__") {
                            EmptyAiMonitorCard(isLeftSidebar = false, onCreateAiSpace = onCreateAiSpace)
                        }
                    }
                    itemsIndexed(monitoredSpaces, key = { _, sp -> sp.id }) { idx, space ->
                        val isDragging = draggingId == space.id
                        val rawNeighborShift = when {
                            draggingId == null || isHoveringDelete || dragStartIdx < 0 || dragTargetIdx < 0 || isDragging -> 0f
                            dragStartIdx < dragTargetIdx && idx in (dragStartIdx + 1)..dragTargetIdx -> -draggedStepPx
                            dragStartIdx > dragTargetIdx && idx in dragTargetIdx until dragStartIdx -> draggedStepPx
                            else -> 0f
                        }
                        val neighborShiftX by animateFloatAsState(
                            targetValue = rawNeighborShift,
                            animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
                            label = "topMonitorShift_${space.id}"
                        )
                        MonitorSpaceCard(
                            space = space,
                            index = idx,
                            isLeftSidebar = false,
                            contentAspectRatio = contentAspectRatio,
                            trayVisible = visible,
                            isReturning = space.id == returningSpaceId,
                            isDragging = isDragging,
                            isDeleteHovered = isDragging && isHoveringDelete,
                            isDropHovered = hoveredDropSpaceId == space.id,
                            dragTranslationX = if (isDragging) dragOffsetX else neighborShiftX,
                            dragTranslationY = if (isDragging) dragOffsetY * 0.55f else 0f,
                            connectedAiName = connectedAiName,
                            thumbnail = thumbnails[space.id],
                            modifier = Modifier
                                .then(if (draggingId == null) Modifier.animateItem(placementSpec = spring(dampingRatio = 0.88f, stiffness = 360f)) else Modifier)
                                .graphicsLayer { alpha = if (space.id in vanishingSpaceIds) 0f else 1f }
                                .let { baseMod ->
                                    val cardLayer = rememberGraphicsLayer()
                                    cardCaptureLayers[space.id] = cardLayer
                                    baseMod.drawWithContent {
                                        cardLayer.record { this@drawWithContent.drawContent() }
                                        drawLayer(cardLayer)
                                    }
                                }
                                .onGloballyPositioned { coords ->
                                    val rBounds = coords.boundsInRoot()
                                    onRegisterSpaceBounds(space.id, rBounds)
                                    cardRootRects[space.id] = rBounds
                                    val pos = coords.positionInParent()
                                    cardLocalRects[space.id] = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
                                },
                            onSelect = { onSelectSpace(space.id) },
                            onClose = if (space.type == SpaceType.AI) ({ triggerDustVanishClose(space) }) else null
                        )
                    }
                    if (isTabDragging && canAddAiSpace) {
                        item(key = "__new_ai_space_drop_top__") {
                            val fullPane = monitoredSpaces.isEmpty()
                            NewAiSpaceDropZoneCard(
                                isLeftSidebar = false,
                                isHovered = isHoveredNewSpaceDrop,
                                isFullPane = fullPane,
                                modifier = Modifier
                                    .then(if (fullPane) Modifier.fillParentMaxWidth(0.92f) else Modifier)
                                    .animateItem(placementSpec = spring(dampingRatio = 0.88f, stiffness = 360f))
                                    .onGloballyPositioned { coords ->
                                        onRegisterNewSpaceBounds(coords.boundsInRoot())
                                    }
                            )
                        }
                    }
                }

                MonitorDeleteDropZone(
                    visible = draggingSpaceIsAi,
                    isHovered = isHoveringDelete,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .zIndex(30f)
                )

                if (activeVanishBursts.isNotEmpty()) {
                    ParticleDisintegrationOverlay(
                        bursts = activeVanishBursts,
                        onBurstFinished = { bid -> finishVanishBurst(bid) }
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun MonitorSpaceCard(
    space: BrowserSpace,
    index: Int,
    isLeftSidebar: Boolean,
    contentAspectRatio: Float = 1.35f,
    trayVisible: Boolean,
    isReturning: Boolean,
    isDragging: Boolean,
    isDeleteHovered: Boolean = false,
    isDropHovered: Boolean = false,
    dragTranslationX: Float,
    dragTranslationY: Float,
    connectedAiName: String,
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val isPersonal = space.type == SpaceType.PERSONAL
    val spaceColor = if (isPersonal) EgoCobalt else parseSpaceColor(space.accentColorHex)
    val inkColor = if (spaceColor.luminance() > 0.36f) Color(0xFF042419) else Color(0xFFFFFFFF)

    val isWorking = !isPersonal && (space.aiStatus == AiSpaceStatus.WORKING || space.aiStatus == AiSpaceStatus.THINKING)
    val isDone = !isPersonal && space.aiStatus == AiSpaceStatus.DONE

    val activePhrase = when {
        isPersonal -> ""
        space.aiStatus == AiSpaceStatus.WORKING -> space.lastActionSummary.ifBlank { "작업 중..." }
        space.aiStatus == AiSpaceStatus.THINKING -> "생각 중..."
        space.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED -> "직접 조작 중"
        space.aiStatus == AiSpaceStatus.DONE -> "완료됨 ✓"
        else -> ""
    }

    val displayName = if (isPersonal) "Personal" else connectedAiName.ifBlank { space.name }

    // 트레이 동시 유기적 확장(0.32/16dp 블러 → 1.0/0dp) + 메인에서 복귀하는 카드 역방향 블러 모프 착지
    val cardScale = remember(space.id) { Animatable(if (isReturning) 0.42f else 0.32f) }
    val cardAlpha = remember(space.id) { Animatable(0f) }
    val cardBlur = remember(space.id) { Animatable(if (isReturning) 14f else 16f) }
    val returnOffset = remember(space.id) { Animatable(if (isReturning) 28f else 0f) }

    LaunchedEffect(trayVisible, space.id, isReturning) {
        if (trayVisible) {
            if (!isReturning) delay(index * 28L)
            launch { cardScale.animateTo(1f, animationSpec = EgoMotion.bouncySpring()) }
            launch { cardAlpha.animateTo(1f, animationSpec = tween(260)) }
            launch { cardBlur.animateTo(0f, animationSpec = tween(360)) }
            launch { returnOffset.animateTo(0f, animationSpec = EgoMotion.bouncySpring()) }
        } else {
            delay(index * 15L)
            launch { cardScale.animateTo(0.32f, animationSpec = tween(190)) }
            launch { cardAlpha.animateTo(0f, animationSpec = tween(160)) }
            launch { cardBlur.animateTo(14f, animationSpec = tween(180)) }
        }
    }

    val dragScale by animateFloatAsState(
        targetValue = when {
            isDeleteHovered -> 0.84f
            isDragging -> 1.05f
            isDropHovered -> 1.045f
            else -> 1f
        },
        animationSpec = EgoMotion.bouncySpring(),
        label = "monitorCardDragScale"
    )
    val veilAlpha by animateFloatAsState(
        targetValue = when {
            isWorking -> 1f
            isDone -> 0.48f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "veilAlpha"
    )
    val statusSlotHeight by animateDpAsState(
        targetValue = if (activePhrase.isNotBlank()) 28.dp else 0.dp,
        animationSpec = EgoMotion.smoothSpring(),
        label = "statusSlotHeight"
    )

    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && cardBlur.value > 0.3f) {
        Modifier.blur(cardBlur.value.dp)
    } else {
        Modifier
    }

    // 실제 웹 콘텐츠 뷰포트 비율(가로/세로/분할화면 모두 대응)에 맞춰 미리보기 프레임 크기 동적 조절
    val safeRatio = contentAspectRatio.coerceIn(0.48f, 2.0f)
    val cardWidth = if (isLeftSidebar) {
        216.dp
    } else {
        (186f * safeRatio + 7f).coerceIn(134f, 304f).dp
    }
    val previewHeight = if (isLeftSidebar) {
        (209f / safeRatio).coerceIn(116f, 286f).dp
    } else {
        186.dp
    }
    val cardHeight = previewHeight + 68.dp

    Box(
        modifier = modifier
            .zIndex(if (isDragging || isDropHovered) 20f else 0f)
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer {
                translationX = dragTranslationX + if (isLeftSidebar) returnOffset.value else 0f
                translationY = dragTranslationY + if (!isLeftSidebar) returnOffset.value else 0f
                scaleX = cardScale.value * dragScale
                scaleY = cardScale.value * dragScale
                alpha = if (isDragging) 0.92f else cardAlpha.value
            }
            .then(blurMod)
            .shadow(
                elevation = if (isDragging) 18.dp else if (isWorking) 14.dp else 6.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = spaceColor,
                spotColor = spaceColor
            )
            .clip(RoundedCornerShape(26.dp))
            .background(spaceColor)
            .clickable { onSelect() }
            .padding(3.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 고정 크기 웹 미리보기 영역 (활동/완료/대기 3단계 크기 불변)
            val previewShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .clip(previewShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null && space.activeTab.url != NEW_TAB_URL) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "$displayName 미리보기",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        EgoIcon(
                            imageVector = if (isPersonal) Icons.Default.Person else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = spaceColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(24.dp)
                        , symbolSize = 24.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersonal) "새 탭" else "대기 중",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 스페이스 색상 상향 그라데이션 (미리보기 영역 내부로만 엄격 제한)
                if (veilAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.32f to Color.Transparent,
                                    0.64f to spaceColor.copy(alpha = 0.28f * veilAlpha),
                                    0.86f to spaceColor.copy(alpha = 0.76f * veilAlpha),
                                    1.0f to spaceColor.copy(alpha = veilAlpha)
                                )
                            )
                    )
                }
            }

            // 고정 하단 푸터: 한 글자 웨이브 활동 문구 + AI 이름
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusSlotHeight.coerceAtLeast(0.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = activePhrase,
                        transitionSpec = {
                            (fadeIn(tween(260)) + scaleIn(EgoMotion.smoothSpring(), initialScale = 0.90f))
                                .togetherWith(fadeOut(tween(180)) + scaleOut(tween(160), targetScale = 0.92f))
                        },
                        label = "statusPhraseBlurMorph"
                    ) { phrase ->
                        if (phrase.isNotBlank()) {
                            if (isWorking) {
                                PerCharWaveText(text = phrase, color = inkColor)
                            } else {
                                Text(
                                    text = phrase,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = inkColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(inkColor.copy(alpha = 0.82f))
                    )
                    Text(
                        text = displayName,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = inkColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // AI 스페이스 카드 우측 상단 닫기(×) 버튼 (Personal에는 미표시)
        if (onClose != null && !isDropHovered) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                EgoIcon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "AI 스페이스 닫기",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                , symbolSize = 14.dp)
            }
        }

        if (isDropHovered) {
            val isIdleTarget = space.aiStatus == AiSpaceStatus.IDLE || space.aiStatus == AiSpaceStatus.DONE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = spaceColor
                    ) {
                        Text(
                            text = if (isIdleTarget) "+ 활성 탭으로 복제" else "+ 백그라운드 복제",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A0F18),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Text(
                        text = if (isIdleTarget) "대기 중 · 바로 전환됨" else "작업 방해 없이 추가",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NewAiSpaceDropZoneCard(
    isLeftSidebar: Boolean,
    isHovered: Boolean,
    isFullPane: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.025f else 1f,
        animationSpec = EgoMotion.bouncySpring(),
        label = "newSpaceDropScale"
    )
    val sizeMod = when {
        isFullPane && isLeftSidebar -> Modifier.width(216.dp)
        isFullPane && !isLeftSidebar -> Modifier.height(236.dp)
        isLeftSidebar -> Modifier.width(216.dp).height(108.dp)
        else -> Modifier.width(188.dp).height(258.dp)
    }

    Box(
        modifier = modifier
            .then(sizeMod)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(accent.copy(alpha = if (isHovered) 0.22f else 0.11f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = accent.copy(alpha = if (isHovered) 1f else 0.65f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(
                    width = (if (isHovered) 2.4.dp else 1.6.dp).toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            EgoIcon(
                imageVector = Icons.Default.Add,
                contentDescription = "새 AI 스페이스에 생성",
                tint = accent,
                modifier = Modifier.size(22.dp)
            , symbolSize = 22.dp)
            Text(
                text = "새 AI 스페이스에 생성",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = "놓으면 첫 페이지로 열림",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PerCharWaveText(
    text: String,
    color: Color
) {
    val clipped = remember(text) { if (text.length > 14) text.take(13) + "…" else text }
    val infinite = rememberInfiniteTransition(label = "charWaveLoop")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charWavePhase"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        clipped.forEachIndexed { idx, ch ->
            val offsetY = sin(phase - idx * 0.42f) * -3.5f
            Text(
                text = ch.toString(),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                modifier = Modifier.graphicsLayer {
                    translationY = offsetY
                }
            )
        }
    }
}


@Composable
private fun EmptyAiMonitorCard(
    isLeftSidebar: Boolean,
    onCreateAiSpace: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .width(if (isLeftSidebar) 216.dp else 230.dp)
            .height(if (isLeftSidebar) 168.dp else 220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onCreateAiSpace() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = accent.copy(alpha = 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    EgoIcon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "새 AI 스페이스",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    , symbolSize = 20.dp)
                }
            }
            Text(
                text = "생성된 AI 스페이스 없음",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "눌러서 새 AI 스페이스 만들기",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun MonitorDeleteDropZone(
    visible: Boolean,
    isHovered: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.82f, animationSpec = EgoMotion.bouncySpring()),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.82f, animationSpec = tween(140)),
        modifier = modifier
    ) {
        val scale by animateFloatAsState(
            targetValue = if (isHovered) 1.08f else 1f,
            animationSpec = EgoMotion.bouncySpring(),
            label = "deleteZoneScale"
        )
        Surface(
            shape = CircleShape,
            color = if (isHovered) EgoDangerRed else EgoDangerRed.copy(alpha = 0.16f),
            shadowElevation = if (isHovered) 8.dp else 2.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isHovered) EgoDangerRed else EgoDangerRed.copy(alpha = 0.55f)
            ),
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EgoIcon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "삭제",
                    tint = if (isHovered) Color.White else EgoDangerRed,
                    modifier = Modifier.size(18.dp)
                , symbolSize = 18.dp)
                Text(
                    text = if (isHovered) "놓아서 스페이스 삭제" else "여기로 끌어다 삭제",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHovered) Color.White else EgoDangerRed
                )
            }
        }
    }
}
