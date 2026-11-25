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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.SpaceType

/**
 * 좌상단 알약 캡슐 (Drag-to-Open & Drag-to-Select 지원)
 * - 누르는 즉시 하단 공간 전환 바가 열리고, 손을 떼지 않고 좌우/아래로 드래그 후 떼면 해당 공간으로 즉시 전환
 * - 짧게 탭하면 일반 토글로 동작
 */
@Composable
fun PillSpaceCapsule(
    activeSpace: BrowserSpace,
    isAnyAiWorking: Boolean,
    spaceCount: Int,
    canAddAiSpace: Boolean,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    onHoverIndexChanged: (Int) -> Unit,
    onCommitIndex: (Int) -> Unit
) {
    val isPersonal = activeSpace.type == SpaceType.PERSONAL
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val chipWidthPx = with(density) { 112.dp.toPx() }
    val dragThresholdY = with(density) { 26.dp.toPx() }
    val totalSlots = spaceCount + if (canAddAiSpace) 1 else 0

    val infiniteTransition = rememberInfiniteTransition(label = "aiGlow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowAngle"
    )

    val borderModifier = if (isAnyAiWorking) {
        Modifier.drawBehind {
            val strokePx = 2.5.dp.toPx()
            val radius = size.height / 2f
            rotate(degrees = angle) {
                drawRoundRect(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            EgoEmeraldGlow.copy(alpha = 0.25f),
                            EgoEmeraldGlow,
                            Color(0xFF6EE7B7),
                            Color.Transparent
                        )
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = strokePx)
                )
            }
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
    val latestHoverChanged by androidx.compose.runtime.rememberUpdatedState(onHoverIndexChanged)
    val latestCommit by androidx.compose.runtime.rememberUpdatedState(onCommitIndex)

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .then(borderModifier)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val wasAlreadyOpen = latestExpanded
                    if (!wasAlreadyOpen) {
                        latestSetExpanded(true)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    var dragged = false
                    var lastHover = -1
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (dragged && lastHover >= 0) {
                                latestCommit(lastHover)
                                latestHoverChanged(-1)
                                latestSetExpanded(false)
                            } else if (wasAlreadyOpen) {
                                latestSetExpanded(false)
                                latestHoverChanged(-1)
                            } else {
                                latestHoverChanged(-1)
                            }
                            break
                        }
                        val dy = change.position.y - down.position.y
                        val dx = change.position.x
                        if (dy > dragThresholdY) {
                            dragged = true
                            val idx = (dx / chipWidthPx).toInt().coerceIn(0, (latestTotalSlots - 1).coerceAtLeast(0))
                            if (idx != lastHover) {
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
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpaceDotIndicator(color = EgoCobalt, filled = isPersonal)
            SpaceDotIndicator(color = EgoEmerald, filled = !isPersonal)
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
    onCloseAiSpace: (String) -> Unit
) {
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
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spaces.forEachIndexed { idx, space ->
                    val selected = space.id == activeSpaceId
                    val isHovered = hoveredIndex == idx
                    val isAi = space.type == SpaceType.AI
                    val dotColor = if (isAi) EgoEmerald else EgoCobalt
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
                        if (isAi && space.aiStatus == AiSpaceStatus.WORKING) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "작업중",
                                fontSize = 11.sp,
                                color = EgoEmerald,
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
        }
    }
}
