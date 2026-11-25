package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChromeMenuItemSpec(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color? = null,
    val bold: Boolean = false,
    val checked: Boolean = false,
    val dividerAfter: Boolean = false,
    val onClick: () -> Unit
)

/**
 * 크롬 스타일 Drag-to-Open / Drag-to-Select 우상단 앵커 확장 메뉴
 */
@Composable
fun DragToSelectChromeMenuOverlay(
    visible: Boolean,
    items: List<ChromeMenuItemSpec>,
    hoveredIndex: Int,
    onDismiss: () -> Unit
) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 98.dp, end = 8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(120)) + scaleIn(
                animationSpec = tween(150),
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(1f, 0f)
            ),
            exit = fadeOut(tween(100)) + scaleOut(
                animationSpec = tween(120),
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(1f, 0f)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                tonalElevation = 4.dp,
                modifier = Modifier.width(248.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    items.forEachIndexed { idx, item ->
                        val isHovered = hoveredIndex == idx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    if (isHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onDismiss()
                                    item.onClick()
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.accentColor ?: if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = if (item.bold || isHovered) FontWeight.Bold else FontWeight.Normal,
                                color = item.accentColor ?: MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.checked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (item.dividerAfter) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
