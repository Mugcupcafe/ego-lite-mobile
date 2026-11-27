package com.egolite.mobile.ui

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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.AI_SPACE_COLOR_PRESETS
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.SpaceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun parseSpaceColor(hex: String, fallback: Color = EgoEmerald): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

/**
 * Material 3 Expressive 7-shape geometric buddy library.
 * Smooth polar-radius sampling enables seamless path morphing between any two shapes.
 */
enum class M3BuddyShape {
    CIRCLE,
    CLOVER4,
    DIAMOND,
    SQUIRCLE,
    SCALLOP,
    PILLOW,
    PEBBLE
}

private val AI_BUDDY_SHAPES = listOf(
    M3BuddyShape.CLOVER4,
    M3BuddyShape.SQUIRCLE,
    M3BuddyShape.SCALLOP,
    M3BuddyShape.DIAMOND,
    M3BuddyShape.PILLOW,
    M3BuddyShape.PEBBLE,
    M3BuddyShape.CIRCLE
)

fun buddyShapeForSpace(spaceId: String, index: Int): M3BuddyShape {
    val hash = (spaceId.hashCode() and 0x7FFFFFFF) + index * 3
    return AI_BUDDY_SHAPES[hash % AI_BUDDY_SHAPES.size]
}

fun morphTargetShape(base: M3BuddyShape): M3BuddyShape {
    val idx = AI_BUDDY_SHAPES.indexOf(base).coerceAtLeast(0)
    return AI_BUDDY_SHAPES[(idx + 2) % AI_BUDDY_SHAPES.size]
}

private fun sampleShapeRadius(shape: M3BuddyShape, theta: Double): Double {
    return when (shape) {
        M3BuddyShape.CIRCLE -> 0.94
        M3BuddyShape.CLOVER4 -> 0.84 + 0.16 * cos(4.0 * theta)
        M3BuddyShape.SCALLOP -> 0.88 + 0.10 * cos(8.0 * theta)
        M3BuddyShape.SQUIRCLE -> {
            val c = abs(cos(theta)).pow(3.6)
            val s = abs(sin(theta)).pow(3.6)
            0.94 / (c + s).pow(1.0 / 3.6)
        }
        M3BuddyShape.DIAMOND -> {
            val c = abs(cos(theta)).pow(1.45)
            val s = abs(sin(theta)).pow(1.45)
            (0.95 / (c + s).pow(1.0 / 1.45)).coerceAtMost(1.02)
        }
        M3BuddyShape.PILLOW -> {
            val base = 0.88 + 0.08 * cos(4.0 * (theta - PI / 4.0))
            base + 0.04 * cos(2.0 * theta)
        }
        M3BuddyShape.PEBBLE -> {
            0.88 + 0.08 * cos(3.0 * theta + 0.4) + 0.04 * sin(2.0 * theta)
        }
    }
}

private fun buildMorphedBuddyPath(
    fromShape: M3BuddyShape,
    toShape: M3BuddyShape,
    morphProgress: Float,
    centerX: Float,
    centerY: Float,
    radius: Float
): Path {
    val t = morphProgress.coerceIn(0f, 1f).toDouble()
    val samples = 64
    val path = Path()
    for (i in 0 until samples) {
        val theta = (2.0 * PI * i) / samples
        val r0 = sampleShapeRadius(fromShape, theta)
        val r1 = sampleShapeRadius(toShape, theta)
        val r = (r0 + (r1 - r0) * t) * radius
        val x = centerX + (cos(theta) * r).toFloat()
        val y = centerY + (sin(theta) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Fixed reserved slot width for the top-left M3 buddy strip (34dp each, 3dp resting gap).
 * Adjacent Row 1 page tabs use this fixed width so they never shift left when buddies huddle.
 */
fun buddySlotWidthDp(spaces: List<BrowserSpace>): Dp {
    val aiCount = spaces.count { it.type == SpaceType.AI }
    val visibleCount = if (aiCount == 0) 1 else aiCount.coerceAtMost(5)
    return (visibleCount * 34 + (visibleCount - 1) * 3 + 8).dp
}

/**
 * Single M3 Expressive geometric buddy avatar with minimal vertical-pill eyes (no mouth/blush).
 * Supports smooth path morphing, AI thinking/working motions, natural random blink, and > < squint.
 */
@Composable
fun M3ExpressiveBuddyAvatar(
    color: Color,
    baseShape: M3BuddyShape,
    targetShape: M3BuddyShape = morphTargetShape(baseShape),
    morphProgress: Float = 0f,
    isSquinting: Boolean = false,
    aiStatus: AiSpaceStatus = AiSpaceStatus.IDLE,
    size: Dp = 34.dp,
    blinkSeed: Int = 0,
    modifier: Modifier = Modifier
) {
    val isWorking = aiStatus == AiSpaceStatus.WORKING
    val isThinking = aiStatus == AiSpaceStatus.THINKING
    val eyeInk = if (color.luminance() > 0.42f) Color(0xFF0A1F18) else Color(0xFFFFFFFF)

    // Natural random blink when idle/done and not squinting
    var blinkScaleY by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(blinkSeed, isSquinting, isWorking) {
        if (isSquinting) {
            blinkScaleY = 1f
            return@LaunchedEffect
        }
        val baseDelay = 1800L + ((blinkSeed * 733) % 1900).toLong()
        delay(baseDelay)
        while (true) {
            blinkScaleY = 0.12f
            delay(110L)
            blinkScaleY = 1f
            delay(2700L + ((blinkSeed * 521) % 1800).toLong())
        }
    }

    // Status-driven micro-motion (only runs infinite transition when thinking or working)
    val statusPhase = if (isWorking || isThinking) {
        val inf = rememberInfiniteTransition(label = "buddyStatus_$blinkSeed")
        inf.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isWorking) 760 else 1600,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "buddyPhase"
        ).value
    } else {
        0.5f
    }

    val tiltDeg = if (isThinking && !isSquinting) (statusPhase - 0.5f) * 14f else 0f
    val eyeGlanceX = if (isThinking && !isSquinting) (statusPhase - 0.5f) * 3.4f else 0f
    val workHopDp = if (isWorking && !isSquinting) -(sin(statusPhase * PI).toFloat() * 2.6f) else 0f
    val glowPulse = if (isWorking) 0.24f + 0.20f * statusPhase else 0f

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = tiltDeg
                translationY = workHopDp.dp.toPx()
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.47f

        // Subtle aura glow when AI is actively working
        if (glowPulse > 0.01f) {
            val auraPath = buildMorphedBuddyPath(baseShape, targetShape, morphProgress, cx, cy, r * 1.13f)
            drawPath(path = auraPath, color = color.copy(alpha = glowPulse))
        }

        // Main M3 geometric body
        val bodyPath = buildMorphedBuddyPath(baseShape, targetShape, morphProgress, cx, cy, r)
        drawPath(path = bodyPath, color = color)

        // Subtle specular top highlight for tactile depth
        drawPath(
            path = bodyPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Minimal vertical-pill eyes (or > < squint during long-press huddle)
        val eyeOffsetX = r * 0.30f
        val eyeCenterY = cy - r * 0.02f
        val glancePx = eyeGlanceX.dp.toPx()

        if (isSquinting) {
            val sw = (this.size.minDimension * 0.078f).coerceAtLeast(1.8.dp.toPx())
            val arm = r * 0.17f
            val lx = cx - eyeOffsetX
            val rx = cx + eyeOffsetX
            // Left eye: '>'
            val leftSquint = Path().apply {
                moveTo(lx - arm * 0.7f, eyeCenterY - arm * 0.75f)
                lineTo(lx + arm * 0.65f, eyeCenterY)
                lineTo(lx - arm * 0.7f, eyeCenterY + arm * 0.75f)
            }
            // Right eye: '<'
            val rightSquint = Path().apply {
                moveTo(rx + arm * 0.7f, eyeCenterY - arm * 0.75f)
                lineTo(rx - arm * 0.65f, eyeCenterY)
                lineTo(rx + arm * 0.7f, eyeCenterY + arm * 0.75f)
            }
            val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(leftSquint, color = eyeInk, style = stroke)
            drawPath(rightSquint, color = eyeInk, style = stroke)
        } else {
            val pillW = (this.size.minDimension * 0.105f).coerceAtLeast(2.6.dp.toPx())
            val pillH = ((this.size.minDimension * 0.24f) * blinkScaleY).coerceAtLeast(1.4.dp.toPx())
            val corner = CornerRadius(pillW / 2f, pillW / 2f)
            drawRoundRect(
                color = eyeInk,
                topLeft = Offset(cx - eyeOffsetX - pillW / 2f + glancePx, eyeCenterY - pillH / 2f),
                size = Size(pillW, pillH),
                cornerRadius = corner
            )
            drawRoundRect(
                color = eyeInk,
                topLeft = Offset(cx + eyeOffsetX - pillW / 2f + glancePx, eyeCenterY - pillH / 2f),
                size = Size(pillW, pillH),
                cornerRadius = corner
            )
        }
    }
}

/**
 * 좌상단 캡슐리스 34dp M3 Expressive 캐릭터 트리거 (Drag-to-Open & Drag-to-Select 지원)
 * - 평소 간격 3dp, 롱프레스/드래그 시 -10dp 다닥다닥 모임 + 20% 확대 + 도형 모프 + > < 눈웃음
 * - 고정 슬롯 너비 유지로 인접 페이지 탭 위치 불변
 * - 공간 선택 좌우 드래그 시 모인 캐릭터들이 예약 영역 범위 내에서 손가락을 자기장처럼 추종
 */
@Composable
fun PillSpaceCapsule(
    activeSpace: BrowserSpace,
    spaces: List<BrowserSpace>,
    isAnyAiWorking: Boolean,
    workingAccentColor: Color = EgoEmerald,
    spaceCount: Int,
    canAddAiSpace: Boolean,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    onToggleAiMonitor: () -> Unit,
    onHoverIndexChanged: (Int) -> Unit,
    onCommitIndex: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val aiSpaces = remember(spaces) { spaces.filter { it.type == SpaceType.AI }.take(5) }
    val personalSpace = remember(spaces) { spaces.firstOrNull { it.type == SpaceType.PERSONAL } ?: activeSpace }
    val displaySpaces = remember(aiSpaces, personalSpace) {
        if (aiSpaces.isEmpty()) listOf(personalSpace) else aiSpaces
    }
    val buddyCount = displaySpaces.size
    val slotWidthDp = remember(buddyCount) { (buddyCount * 34 + (buddyCount - 1) * 3 + 8).dp }
    val slotWidthPx = with(density) { slotWidthDp.toPx() }

    val chipWidthPx = with(density) { 112.dp.toPx() }
    val dragThresholdY = with(density) { 26.dp.toPx() }
    val totalSlots = spaceCount + if (canAddAiSpace) 1 else 0

    var isHuddled by remember { mutableStateOf(false) }
    var magneticTargetX by remember { mutableFloatStateOf(0f) }

    // Short-tap organic squash-and-stretch hop synced with panel open/close
    val tapHopScaleX = remember { Animatable(1f) }
    val tapHopScaleY = remember { Animatable(1f) }
    val tapHopOffsetY = remember { Animatable(0f) }

    val huddleActive = isHuddled || isExpanded
    val buddyGapDp by animateDpAsState(
        targetValue = if (huddleActive) (-10).dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "buddyHuddleGap"
    )
    val buddyScale by animateFloatAsState(
        targetValue = if (huddleActive) 1.20f else 1.0f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 400f),
        label = "buddyHuddleScale"
    )
    val morphProgress by animateFloatAsState(
        targetValue = if (huddleActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "buddyMorphProgress"
    )
    val magneticShiftX by animateFloatAsState(
        targetValue = if (huddleActive) magneticTargetX else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 380f),
        label = "buddyMagneticX"
    )

    val latestTotalSlots by rememberUpdatedState(totalSlots)
    val latestSlotWidthPx by rememberUpdatedState(slotWidthPx)
    val latestBuddyCount by rememberUpdatedState(buddyCount)
    val latestSetExpanded by rememberUpdatedState(onSetExpanded)
    val latestToggleMonitor by rememberUpdatedState(onToggleAiMonitor)
    val latestHoverChanged by rememberUpdatedState(onHoverIndexChanged)
    val latestCommit by rememberUpdatedState(onCommitIndex)

    Box(
        modifier = Modifier
            .width(slotWidthDp)
            .height(38.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragged = false
                    var longPressed = false
                    var lastHover = -1

                    val longPressJob = scope.launch {
                        delay(260L)
                        longPressed = true
                        isHuddled = true
                        latestSetExpanded(true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                longPressJob.cancel()
                                if (dragged || longPressed) {
                                    if (lastHover >= 0) {
                                        latestCommit(lastHover)
                                    }
                                    latestSetExpanded(false)
                                    latestHoverChanged(-1)
                                } else {
                                    // 짧게 눌렀다 떼기: 유기적 스쿼시-홉과 함께 AI 라이브 모니터 토글
                                    latestSetExpanded(false)
                                    latestHoverChanged(-1)
                                    latestToggleMonitor()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        launch {
                                            tapHopScaleX.animateTo(1.08f, tween(70))
                                            tapHopScaleX.animateTo(0.95f, tween(95))
                                            tapHopScaleX.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                        launch {
                                            tapHopScaleY.animateTo(0.88f, tween(70))
                                            tapHopScaleY.animateTo(1.09f, tween(95))
                                            tapHopScaleY.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                        launch {
                                            tapHopOffsetY.animateTo(2.5f, tween(70))
                                            tapHopOffsetY.animateTo(-4.5f, tween(95))
                                            tapHopOffsetY.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                    }
                                }
                                isHuddled = false
                                magneticTargetX = 0f
                                break
                            }

                            val dy = change.position.y - down.position.y
                            val dx = change.position.x
                            val totalBarWidthPx = latestTotalSlots * chipWidthPx
                            val maxBarBottomPx = dragThresholdY + chipWidthPx * 0.9f

                            if (!dragged && (abs(dy) > dragThresholdY * 0.55f || abs(dx - down.position.x) > dragThresholdY * 0.55f)) {
                                longPressJob.cancel()
                                dragged = true
                                isHuddled = true
                                latestSetExpanded(true)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }

                            if (dragged || longPressed) {
                                // 모인 캐릭터들이 예약된 캐릭터 영역(slotWidthPx) 내에서만 손가락 X를 따라감
                                val huddledWidthPx = with(density) {
                                    (latestBuddyCount * 34 + (latestBuddyCount - 1) * (-10)).coerceAtLeast(34).dp.toPx()
                                }
                                val maxShift = ((latestSlotWidthPx - huddledWidthPx) * 0.5f).coerceAtLeast(0f)
                                val desiredOffset = (dx - latestSlotWidthPx * 0.5f) * 0.55f
                                magneticTargetX = desiredOffset.coerceIn(-maxShift, maxShift)

                                val inBarBounds = dy > dragThresholdY && dy <= maxBarBottomPx && dx >= 0f && dx <= totalBarWidthPx
                                if (inBarBounds) {
                                    val idx = (dx / chipWidthPx).toInt()
                                    if (idx in 0 until latestTotalSlots && idx != lastHover) {
                                        lastHover = idx
                                        latestHoverChanged(idx)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                } else if (lastHover != -1) {
                                    lastHover = -1
                                    latestHoverChanged(-1)
                                }
                            }
                        }
                    } finally {
                        longPressJob.cancel()
                        isHuddled = false
                        magneticTargetX = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.graphicsLayer {
                translationX = magneticShiftX
                translationY = tapHopOffsetY.value.dp.toPx()
                scaleX = buddyScale * tapHopScaleX.value
                scaleY = buddyScale * tapHopScaleY.value
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(buddyGapDp)
        ) {
            displaySpaces.forEachIndexed { idx, space ->
                val isPersonal = space.type == SpaceType.PERSONAL
                val buddyColor = if (isPersonal) EgoCobalt else parseSpaceColor(space.accentColorHex, workingAccentColor)
                val baseShape = if (isPersonal) M3BuddyShape.CIRCLE else buddyShapeForSpace(space.id, idx)
                val targetShape = if (isPersonal) M3BuddyShape.CLOVER4 else morphTargetShape(baseShape)
                M3ExpressiveBuddyAvatar(
                    color = buddyColor,
                    baseShape = baseShape,
                    targetShape = targetShape,
                    morphProgress = morphProgress,
                    isSquinting = huddleActive,
                    aiStatus = if (isPersonal) AiSpaceStatus.IDLE else space.aiStatus,
                    size = 34.dp,
                    blinkSeed = idx + (space.id.hashCode() and 0xFF)
                )
            }
        }
    }
}

@Composable
private fun SpaceDotIndicator(color: Color, filled: Boolean) {
    Canvas(modifier = Modifier.size(10.dp)) {
        if (filled) {
            drawCircle(color = color)
        } else {
            drawCircle(color = color, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun ExpandedSpaceSlideBar(
    visible: Boolean,
    spaces: List<BrowserSpace>,
    activeSpaceId: String,
    canAddAiSpace: Boolean,
    hoveredIndex: Int = -1,
    onSelectSpace: (String) -> Unit,
    onCreateAiSpace: () -> Unit,
    onCloseAiSpace: (String) -> Unit,
    onChangeSpaceColor: (String, String) -> Unit = { _, _ -> }
) {
    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(130)) + expandVertically(tween(160)),
        exit = fadeOut(tween(110)) + shrinkVertically(tween(130))
    ) {
        Surface(
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    spaces.forEachIndexed { idx, space ->
                        val selected = space.id == activeSpaceId
                        val isHovered = hoveredIndex == idx
                        val isAi = space.type == SpaceType.AI
                        val dotColor = if (isAi) parseSpaceColor(space.accentColorHex) else EgoCobalt
                        val bgColor = when {
                            isHovered -> dotColor.copy(alpha = 0.28f)
                            selected -> dotColor.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        }

                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isHovered) Modifier.border(1.5.dp, dotColor, CircleShape) else Modifier
                                )
                                .clickable { onSelectSpace(space.id) }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpaceDotIndicator(color = dotColor, filled = selected || isHovered)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = space.name,
                                fontSize = 13.sp,
                                fontWeight = if (selected || isHovered) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isAi && (space.aiStatus == AiSpaceStatus.WORKING || space.aiStatus == AiSpaceStatus.THINKING)) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (space.aiStatus == AiSpaceStatus.THINKING) "생각중" else "작업중",
                                    fontSize = 11.sp,
                                    color = dotColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isAi && spaces.count { it.type == SpaceType.AI } > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                EgoIcon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "공간 닫기",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onCloseAiSpace(space.id) },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    symbolSize = 16.dp
                                )
                            }
                        }
                    }

                    if (canAddAiSpace) {
                        val isAddHovered = hoveredIndex == spaces.size
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isAddHovered) EgoEmerald.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant)
                                .then(if (isAddHovered) Modifier.border(1.5.dp, EgoEmerald, CircleShape) else Modifier)
                                .clickable { onCreateAiSpace() },
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "AI Space 추가",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                                symbolSize = 18.dp
                            )
                        }
                    }
                }

                // 현재 선택된 공간이 AI Space일 때 하단에 커스텀 색상 팔레트 표시
                if (activeSpace != null && activeSpace.type == SpaceType.AI) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${activeSpace.name} 색상",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        AI_SPACE_COLOR_PRESETS.forEach { hex ->
                            val swatch = parseSpaceColor(hex)
                            val isSelected = activeSpace.accentColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 22.dp else 18.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onChangeSpaceColor(activeSpace.id, hex) }
                            )
                        }
                    }
                }
            }
        }
    }
}
