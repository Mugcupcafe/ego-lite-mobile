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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.SpaceType

@Composable
fun PillSpaceCapsule(
    activeSpace: BrowserSpace,
    isAnyAiWorking: Boolean,
    onToggleExpand: () -> Unit
) {
    val isPersonal = activeSpace.type == SpaceType.PERSONAL
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

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .then(borderModifier)
            .clickable { onToggleExpand() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 파란 점(Personal): 선택 시 채워진 원, 비선택 시 원링
            SpaceDotIndicator(color = EgoCobalt, filled = isPersonal)
            // 초록 점(AI): 선택 시 채워진 원, 비선택 시 원링
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
    onSelectSpace: (String) -> Unit,
    onCreateAiSpace: () -> Unit,
    onCloseAiSpace: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + expandVertically(tween(180)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(150))
    ) {
        Surface(
            shadowElevation = 6.dp,
            tonalElevation = 3.dp,
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
                spaces.forEach { space ->
                    val selected = space.id == activeSpaceId
                    val isAi = space.type == SpaceType.AI
                    val dotColor = if (isAi) EgoEmerald else EgoCobalt
                    val bgColor = if (selected) {
                        dotColor.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }

                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { onSelectSpace(space.id) }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpaceDotIndicator(color = dotColor, filled = selected)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = space.name,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
