package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.AI_SPACE_COLOR_PRESETS
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.SpaceType
import kotlin.math.hypot

fun parseSpaceColor(hex: String, fallback: Color = EgoEmerald): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

/**
 * 좌상단 알약 캡슐 (Drag-to-Open & Drag-to-Select 지원)
 * - AI 작업 중일 때 알약 본체는 고정된 채 테두리 둘레를 따라 빛줄기만 회전(Border Beam)
 */
@Composable
fun PillSpaceCapsule(
    activeSpace: BrowserSpace,
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
    val isPersonal = activeSpace.type == SpaceType.PERSONAL
    val aiDotColor = if (!isPersonal) parseSpaceColor(activeSpace.accentColorHex) else workingAccentColor
    val surfaceColor = MaterialTheme.colorScheme.surface
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val chipWidthPx = with(density) { 112.dp.toPx() }
    val dragThresholdY = with(density) { 26.dp.toPx() }
    val totalSlots = spaceCount + if (canAddAiSpace) 1 else 0

    // 발열 최적화: AI가 실제 작업 중일 때만 120Hz 회전 광채 애니메이션 구동
    val angle = if (isAnyAiWorking) {
        val infiniteTransition = rememberInfiniteTransition(label = "aiGlow")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "glowAngle"
        ).value
    } else {
        0f
    }

    val borderModifier = if (isAnyAiWorking) {
        Modifier.drawWithContent {
            val radius = size.height / 2f
            val pillPath = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius, radius)))
            }
            clipPath(pillPath) {
                // 1) 알약 본체는 고정하고, 외곽 경로 안에서 회전하는 빛줄기(SweepGradient)만 그림
                rotate(degrees = angle, pivot = center) {
                    val diag = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() * 1.15f
                    drawRect(
                        brush = Brush.sweepGradient(
                            0.00f to Color.Transparent,
                            0.68f to Color.Transparent,
                            0.84f to workingAccentColor.copy(alpha = 0.65f),
                            0.94f to workingAccentColor,
                            0.98f to Color.White,
                            1.00f to Color.Transparent,
                            center = center
                        ),
                        topLeft = Offset(center.x - diag / 2f, center.y - diag / 2f),
                        size = Size(diag, diag)
                    )
                }
                // 2) 내부를 surface 색상으로 마스킹해 2.3dp 테두리에만 빛줄기가 흐르게 만듦
                val inset = 2.3.dp.toPx()
                val innerR = ((size.height - inset * 2f) / 2f).coerceAtLeast(1f)
                drawRoundRect(
                    color = surfaceColor,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                    cornerRadius = CornerRadius(innerR, innerR)
                )
            }
            drawContent()
        }
    } else {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = CircleShape
        )
    }

    val latestExpanded by androidx.compose.runtime.rememberUpdatedState(isExpanded)
    val latestTotalSlots by androidx.compose.runtime.rememberUpdatedState(totalSlots)
    val latestSetExpanded by androidx.compose.runtime.rememberUpdatedState(onSetExpanded)
    val latestToggleMonitor by androidx.compose.runtime.rememberUpdatedState(onToggleAiMonitor)
    val latestHoverChanged by androidx.compose.runtime.rememberUpdatedState(onHoverIndexChanged)
    val latestCommit by androidx.compose.runtime.rememberUpdatedState(onCommitIndex)

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(surfaceColor)
            .then(borderModifier)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragged = false
                    var lastHover = -1
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (dragged) {
                                if (lastHover >= 0) {
                                    latestCommit(lastHover)
                                }
                                latestSetExpanded(false)
                                latestHoverChanged(-1)
                            } else {
                                // 짧게 눌렀다 떼기: 상단 AI 라이브 모니터 트레이 고정 토글 (닫기 전까지 계속 유지)
                                latestSetExpanded(false)
                                latestHoverChanged(-1)
                                latestToggleMonitor()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            break
                        }
                        val dy = change.position.y - down.position.y
                        val dx = change.position.x
                        val totalBarWidthPx = latestTotalSlots * chipWidthPx
                        val maxBarBottomPx = dragThresholdY + chipWidthPx * 0.9f
                        if (!dragged && (kotlin.math.abs(dy) > dragThresholdY * 0.55f || kotlin.math.abs(dx - down.position.x) > dragThresholdY * 0.55f)) {
                            dragged = true
                            latestSetExpanded(true)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        if (dragged) {
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
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpaceDotIndicator(color = EgoCobalt, filled = isPersonal)
            SpaceDotIndicator(color = aiDotColor, filled = !isPersonal)
        }
    }
}

@Composable
private fun SpaceDotIndicator(color: Color, filled: Boolean) {
    Canvas(modifier = Modifier.size(12.dp)) {
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
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "공간 닫기",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onCloseAiSpace(space.id) },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "AI Space 추가",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
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
