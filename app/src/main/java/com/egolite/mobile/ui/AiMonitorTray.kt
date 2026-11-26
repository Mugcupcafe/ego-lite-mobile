package com.egolite.mobile.ui

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
    isTabDragging: Boolean = false,
    hoveredDropSpaceId: String? = null,
    isHoveredNewSpaceDrop: Boolean = false,
    onRegisterSpaceBounds: (String, Rect) -> Unit = { _, _ -> },
    onRegisterNewSpaceBounds: (Rect?) -> Unit = {},
    onSelectSpace: (String) -> Unit,
    onMoveSpace: (fromId: String, toId: String) -> Unit,
    onCreateAiSpace: () -> Unit,
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

    // 트레이가 열려 있을 때만 1.3초 주기로 경량 RGB_565 썸네일 갱신
    LaunchedEffect(visible, monitoredSpaces.map { "${it.id}:${it.activeTab.id}:${it.aiStatus}:${it.lastActionSummary}" }) {
        if (!visible) return@LaunchedEffect
        while (true) {
            val fresh = mutableMapOf<String, Bitmap>()
            monitoredSpaces.forEach { sp ->
                val bmp = SpaceManager.captureTabThumbnail(sp.activeTab.id)
                if (bmp != null) {
                    fresh[sp.id] = bmp
                } else {
                    thumbnails[sp.id]?.let { fresh[sp.id] = it }
                }
            }
            thumbnails = fresh
            delay(1300L)
        }
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
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            val latestCanDrag by rememberUpdatedState(monitoredSpaces.size > 1)
            val bgColor = MaterialTheme.colorScheme.background

            Box(
                modifier = Modifier
                    .width(244.dp)
                    .fillMaxHeight()
                    .background(Color.Transparent)
            ) {
                // 전체 높이 스크롤 리스트 (상단 54dp 여백 → 스크롤 시 그라데이션 아래로 자연스럽게 사라짐)
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp, start = 13.dp, end = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    if (!latestCanDrag) return@detectDragGesturesAfterLongPress
                                    val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        startOffset.y >= item.offset && startOffset.y <= item.offset + item.size
                                    }
                                    val key = hit?.key as? String
                                    if (key != null) {
                                        draggingId = key
                                        dragOffsetY = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val curId = draggingId ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val items = listState.layoutInfo.visibleItemsInfo
                                    val draggedInfo = items.firstOrNull { it.key == curId } ?: return@detectDragGesturesAfterLongPress
                                    val centerY = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetY
                                    val target = items.firstOrNull { item ->
                                        item.key != curId && centerY >= item.offset && centerY <= item.offset + item.size
                                    }
                                    val targetKey = target?.key as? String
                                    if (target != null && targetKey != null) {
                                        val shift = (draggedInfo.offset - target.offset).toFloat()
                                        onMoveSpace(curId, targetKey)
                                        dragOffsetY += shift
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = { draggingId = null; dragOffsetY = 0f },
                                onDragCancel = { draggingId = null; dragOffsetY = 0f }
                            )
                        }
                ) {
                    itemsIndexed(monitoredSpaces, key = { _, sp -> sp.id }) { idx, space ->
                        val isDragging = draggingId == space.id
                        MonitorSpaceCard(
                            space = space,
                            index = idx,
                            isLeftSidebar = true,
                            trayVisible = visible,
                            isReturning = space.id == returningSpaceId,
                            isDragging = isDragging,
                            isDropHovered = hoveredDropSpaceId == space.id,
                            dragTranslationX = 0f,
                            dragTranslationY = if (isDragging) dragOffsetY else 0f,
                            connectedAiName = connectedAiName,
                            thumbnail = thumbnails[space.id],
                            modifier = Modifier
                                .animateItem()
                                .onGloballyPositioned { coords ->
                                    onRegisterSpaceBounds(space.id, coords.boundsInRoot())
                                },
                            onSelect = { onSelectSpace(space.id) }
                        )
                    }
                    if (isTabDragging && canAddAiSpace) {
                        item(key = "__new_ai_space_drop_left__") {
                            NewAiSpaceDropZoneCard(
                                isLeftSidebar = true,
                                isHovered = isHoveredNewSpaceDrop,
                                modifier = Modifier
                                    .animateItem()
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
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "새 AI 공간",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onCollapse,
                            modifier = Modifier.size(32.dp).pressBounceScale()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "AI 모니터 접기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
            var dragOffsetX by remember { mutableFloatStateOf(0f) }
            val latestCanDrag by rememberUpdatedState(monitoredSpaces.size > 1)

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
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "AI Space 추가",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
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
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "AI 모니터 접기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 가로 스크롤 카드 행 (길게 눌러 좌우 순서 변경 + animateItem 스프링 정렬)
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    if (!latestCanDrag) return@detectDragGesturesAfterLongPress
                                    val hit = rowState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        startOffset.x >= item.offset && startOffset.x <= item.offset + item.size
                                    }
                                    val key = hit?.key as? String
                                    if (key != null) {
                                        draggingId = key
                                        dragOffsetX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val curId = draggingId ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    val items = rowState.layoutInfo.visibleItemsInfo
                                    val draggedInfo = items.firstOrNull { it.key == curId } ?: return@detectDragGesturesAfterLongPress
                                    val centerX = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetX
                                    val target = items.firstOrNull { item ->
                                        item.key != curId && centerX >= item.offset && centerX <= item.offset + item.size
                                    }
                                    val targetKey = target?.key as? String
                                    if (target != null && targetKey != null) {
                                        val shift = (draggedInfo.offset - target.offset).toFloat()
                                        onMoveSpace(curId, targetKey)
                                        dragOffsetX += shift
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = { draggingId = null; dragOffsetX = 0f },
                                onDragCancel = { draggingId = null; dragOffsetX = 0f }
                            )
                        }
                ) {
                    itemsIndexed(monitoredSpaces, key = { _, sp -> sp.id }) { idx, space ->
                        val isDragging = draggingId == space.id
                        MonitorSpaceCard(
                            space = space,
                            index = idx,
                            isLeftSidebar = false,
                            trayVisible = visible,
                            isReturning = space.id == returningSpaceId,
                            isDragging = isDragging,
                            isDropHovered = hoveredDropSpaceId == space.id,
                            dragTranslationX = if (isDragging) dragOffsetX else 0f,
                            dragTranslationY = 0f,
                            connectedAiName = connectedAiName,
                            thumbnail = thumbnails[space.id],
                            modifier = Modifier
                                .animateItem()
                                .onGloballyPositioned { coords ->
                                    onRegisterSpaceBounds(space.id, coords.boundsInRoot())
                                },
                            onSelect = { onSelectSpace(space.id) }
                        )
                    }
                    if (isTabDragging && canAddAiSpace) {
                        item(key = "__new_ai_space_drop_top__") {
                            NewAiSpaceDropZoneCard(
                                isLeftSidebar = false,
                                isHovered = isHoveredNewSpaceDrop,
                                modifier = Modifier
                                    .animateItem()
                                    .onGloballyPositioned { coords ->
                                        onRegisterNewSpaceBounds(coords.boundsInRoot())
                                    }
                            )
                        }
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
    trayVisible: Boolean,
    isReturning: Boolean,
    isDragging: Boolean,
    isDropHovered: Boolean = false,
    dragTranslationX: Float,
    dragTranslationY: Float,
    connectedAiName: String,
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
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
            isDragging -> 1.06f
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

    val cardHeight = if (isLeftSidebar) 216.dp else 258.dp
    val previewHeight = if (isLeftSidebar) 148.dp else 190.dp

    Box(
        modifier = modifier
            .zIndex(if (isDragging || isDropHovered) 20f else 0f)
            .width(216.dp)
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
                        Icon(
                            imageVector = if (isPersonal) Icons.Default.Person else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = spaceColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(24.dp)
                        )
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
                        .height(statusSlotHeight),
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
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.045f else 1f,
        animationSpec = EgoMotion.bouncySpring(),
        label = "newSpaceDropScale"
    )
    val boxHeight = if (isLeftSidebar) 108.dp else 258.dp
    val boxWidth = if (isLeftSidebar) 216.dp else 188.dp

    Box(
        modifier = modifier
            .width(boxWidth)
            .height(boxHeight)
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
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "새 AI 스페이스에 생성",
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
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
