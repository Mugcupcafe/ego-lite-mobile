package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val QS_DIV_PREFIX = "div:"
private const val QS_HIDE_PREFIX = "hide:"

private sealed class QsLayoutEntry {
    data class Tile(val key: String, val span: Int) : QsLayoutEntry()
    data class Divider(val id: String) : QsLayoutEntry()
}

private data class QsPlacedTile(
    val spec: ChromeMenuItemSpec,
    val span: Int,
    val layoutIndex: Int
)

private sealed class QsRenderBlock {
    data class Section(val rows: List<List<QsPlacedTile?>>) : QsRenderBlock()
    data class DividerRow(val id: String, val layoutIndex: Int) : QsRenderBlock()
}

private val DEFAULT_QUICK_MENU_LAYOUT: List<String> = listOf(
    "mcp:2",
    "aispace:1",
    "newtab:1",
    "incognito:1",
    "bookmark:1",
    "div:d1",
    "history:1",
    "download:1",
    "recent:1",
    "site:1",
    "clear:1",
    "settings:1",
    "div:d2",
    "zoom:3",
    "bmbar:2",
    "dark:1",
    "default_app:3"
)

private fun parseLayoutTokens(raw: List<String>): Pair<List<QsLayoutEntry>, Set<String>> {
    val src = raw.ifEmpty { DEFAULT_QUICK_MENU_LAYOUT }
    val entries = mutableListOf<QsLayoutEntry>()
    val hidden = linkedSetOf<String>()
    for (token in src) {
        when {
            token.startsWith(QS_DIV_PREFIX) -> {
                val id = token.removePrefix(QS_DIV_PREFIX).ifBlank { "d_${entries.size}" }
                entries.add(QsLayoutEntry.Divider(id))
            }
            token.startsWith(QS_HIDE_PREFIX) -> {
                val key = token.removePrefix(QS_HIDE_PREFIX)
                if (key.isNotBlank()) hidden.add(key)
            }
            else -> {
                val sep = token.lastIndexOf(':')
                if (sep > 0) {
                    val key = token.substring(0, sep)
                    val span = token.substring(sep + 1).toIntOrNull()?.coerceIn(1, 3) ?: 1
                    entries.add(QsLayoutEntry.Tile(key, span))
                } else if (token.isNotBlank()) {
                    entries.add(QsLayoutEntry.Tile(token, 1))
                }
            }
        }
    }
    return entries to hidden
}

private fun serializeLayoutTokens(entries: List<QsLayoutEntry>, hidden: Set<String>): List<String> {
    val out = mutableListOf<String>()
    for (e in entries) {
        when (e) {
            is QsLayoutEntry.Divider -> out.add("$QS_DIV_PREFIX${e.id}")
            is QsLayoutEntry.Tile -> out.add("${e.key}:${e.span.coerceIn(1, 3)}")
        }
    }
    for (h in hidden) out.add("$QS_HIDE_PREFIX$h")
    return out
}

/**
 * 구분선(Divider) 경계로 섹션을 엄격히 격리해 각 섹션 내부에서만 3열 dense 빈칸 채움을 수행한다.
 * 구분선 바깥의 타일은 절대 구분선을 넘어 끌어오지 않는다.
 */
private fun buildSectionIsolatedBlocks(
    entries: List<QsLayoutEntry>,
    itemByKey: Map<String, ChromeMenuItemSpec>
): List<QsRenderBlock> {
    val blocks = mutableListOf<QsRenderBlock>()
    val currentSectionTiles = mutableListOf<QsPlacedTile>()

    fun flushSection() {
        if (currentSectionTiles.isEmpty()) return
        val rows = mutableListOf<MutableList<QsPlacedTile?>>()
        val occupancy = mutableListOf<BooleanArray>()

        for (placed in currentSectionTiles) {
            val sp = placed.span.coerceIn(1, 3)
            var foundRow = -1
            var foundCol = -1
            outer@ for (r in occupancy.indices) {
                for (c in 0..(3 - sp)) {
                    var fits = true
                    for (k in 0 until sp) {
                        if (occupancy[r][c + k]) {
                            fits = false
                            break
                        }
                    }
                    if (fits) {
                        foundRow = r
                        foundCol = c
                        break@outer
                    }
                }
            }
            if (foundRow == -1) {
                foundRow = occupancy.size
                foundCol = 0
                occupancy.add(BooleanArray(3))
                rows.add(mutableListOf(null, null, null))
            }
            for (k in 0 until sp) {
                occupancy[foundRow][foundCol + k] = true
            }
            rows[foundRow][foundCol] = placed
        }
        blocks.add(QsRenderBlock.Section(rows))
        currentSectionTiles.clear()
    }

    entries.forEachIndexed { idx, entry ->
        when (entry) {
            is QsLayoutEntry.Divider -> {
                flushSection()
                blocks.add(QsRenderBlock.DividerRow(entry.id, idx))
            }
            is QsLayoutEntry.Tile -> {
                val spec = itemByKey[entry.key]
                if (spec != null) {
                    currentSectionTiles.add(QsPlacedTile(spec, entry.span.coerceIn(1, 3), idx))
                }
            }
        }
    }
    flushSection()
    return blocks
}

/**
 * 3열 퀵세팅 스타일 커스텀 브라우저 메뉴:
 * - 모든 행 높이 동일(70.dp)
 * - 구분선 내부 섹션 격리 dense 채움 (바깥 항목 끌어오지 않음)
 * - 편집 모드: 우측 하단 모서리 핸들 드래그로 1×1 / 1×2 / 1×3 리사이즈, + 구분선 삽입/이동/삭제, 숨기기/복원
 * - Drag-to-Open / Drag-to-Select: 각 타일의 live root bounds를 보고해 손가락 위치 타일 즉시 실행
 */
@Composable
fun QuickSettingsBrowserMenuOverlay(
    visible: Boolean,
    items: List<ChromeMenuItemSpec>,
    savedLayout: List<String>,
    hoveredKey: String?,
    topPadding: Dp = 98.dp,
    endPadding: Dp = 8.dp,
    onTileBoundsUpdated: (key: String, rect: Rect) -> Unit,
    onClearTileBounds: () -> Unit,
    onSaveLayout: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var editMode by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (!visible) {
            editMode = false
            onClearTileBounds()
        }
    }

    val itemByKey = remember(items) { items.associateBy { it.key } }
    val (parsedEntries, hiddenKeys) = remember(savedLayout, items) {
        val (baseEntries, baseHidden) = parseLayoutTokens(savedLayout)
        val presentKeys = baseEntries.filterIsInstance<QsLayoutEntry.Tile>().map { it.key }.toSet()
        val merged = baseEntries.toMutableList()
        for (it in items) {
            if (it.key !in presentKeys && it.key !in baseHidden) {
                merged.add(QsLayoutEntry.Tile(it.key, 1))
            }
        }
        merged.toList() to baseHidden
    }

    val blocks = remember(parsedEntries, itemByKey) {
        buildSectionIsolatedBlocks(parsedEntries, itemByKey)
    }
    val hiddenSpecs = remember(items, hiddenKeys) {
        items.filter { it.key in hiddenKeys }
    }

    fun commit(newEntries: List<QsLayoutEntry>, newHidden: Set<String> = hiddenKeys) {
        onSaveLayout(serializeLayoutTokens(newEntries, newHidden))
    }

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
            .padding(top = topPadding, end = endPadding, start = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(130)) + scaleIn(
                animationSpec = EgoMotion.bouncySpring(),
                initialScale = 0.78f,
                transformOrigin = TransformOrigin(1f, 0f)
            ),
            exit = fadeOut(tween(110)) + scaleOut(
                animationSpec = tween(140),
                targetScale = 0.86f,
                transformOrigin = TransformOrigin(1f, 0f)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 16.dp,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .width(344.dp)
                    .heightIn(max = 620.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // 헤더: 타이틀 + (+ 구분선 / 초기화) + 커스텀 편집 토글
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (editMode) "메뉴 편집" else "빠른 메뉴",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editMode) {
                                QsHeaderPill(
                                    label = "+ 구분선",
                                    accent = false,
                                    onClick = {
                                        val next = listOf(QsLayoutEntry.Divider("d_${System.currentTimeMillis()}")) + parsedEntries
                                        commit(next)
                                    }
                                )
                                QsHeaderPill(
                                    label = "초기화",
                                    accent = false,
                                    onClick = { onSaveLayout(emptyList()) }
                                )
                            }
                            QsHeaderPill(
                                label = if (editMode) "완료" else "편집",
                                icon = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                                accent = editMode,
                                onClick = { editMode = !editMode }
                            )
                        }
                    }

                    if (editMode) {
                        Text(
                            text = "타일 우측 하단 모서리 핸들을 좌우로 드래그해 1×1 · 1×2 · 1×3 크기를 조절하세요.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 3열 섹션 격리 스크롤 영역
                    val colWidthPx = with(density) { ((320.dp - 16.dp) / 3f).toPx() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blocks.forEach { block ->
                            when (block) {
                                is QsRenderBlock.DividerRow -> {
                                    if (editMode) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "구분선",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier.weight(1f),
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                            QsTinyCircleBtn(Icons.Default.KeyboardArrowUp, "위로") {
                                                val i = block.layoutIndex
                                                if (i > 0) {
                                                    val mut = parsedEntries.toMutableList()
                                                    val item = mut.removeAt(i)
                                                    mut.add(i - 1, item)
                                                    commit(mut)
                                                }
                                            }
                                            QsTinyCircleBtn(Icons.Default.KeyboardArrowDown, "아래로") {
                                                val i = block.layoutIndex
                                                if (i < parsedEntries.lastIndex) {
                                                    val mut = parsedEntries.toMutableList()
                                                    val item = mut.removeAt(i)
                                                    mut.add(i + 1, item)
                                                    commit(mut)
                                                }
                                            }
                                            QsTinyCircleBtn(Icons.Default.Close, "삭제", tint = EgoDangerRed) {
                                                val mut = parsedEntries.toMutableList()
                                                mut.removeAt(block.layoutIndex)
                                                commit(mut)
                                            }
                                        }
                                    } else {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        )
                                    }
                                }

                                is QsRenderBlock.Section -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        block.rows.forEach { rowSlots ->
                                            // 모든 행 높이 70.dp 동일 고정!
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                var col = 0
                                                while (col < 3) {
                                                    val placed = rowSlots[col]
                                                    if (placed != null) {
                                                        val sp = placed.span.coerceIn(1, 3 - col)
                                                        QsMenuTile(
                                                            spec = placed.spec,
                                                            span = sp,
                                                            editMode = editMode,
                                                            isHovered = !editMode && hoveredKey == placed.spec.key,
                                                            colWidthPx = colWidthPx,
                                                            modifier = Modifier
                                                                .weight(sp.toFloat())
                                                                .fillMaxHeight()
                                                                .onGloballyPositioned { coords ->
                                                                    if (coords.isAttached) {
                                                                        onTileBoundsUpdated(placed.spec.key, coords.boundsInRoot())
                                                                    }
                                                                },
                                                            onClick = {
                                                                if (placed.spec.zoomPercent == null) {
                                                                    onDismiss()
                                                                    placed.spec.onClick()
                                                                }
                                                            },
                                                            onResizeSpan = { nextSpan ->
                                                                val clamped = nextSpan.coerceIn(1, 3)
                                                                if (clamped != placed.span) {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    val mut = parsedEntries.toMutableList()
                                                                    mut[placed.layoutIndex] = QsLayoutEntry.Tile(placed.spec.key, clamped)
                                                                    commit(mut)
                                                                }
                                                            },
                                                            onMoveDelta = { delta ->
                                                                val i = placed.layoutIndex
                                                                val target = (i + delta).coerceIn(0, parsedEntries.lastIndex)
                                                                if (target != i) {
                                                                    val mut = parsedEntries.toMutableList()
                                                                    val moved = mut.removeAt(i)
                                                                    mut.add(target, moved)
                                                                    commit(mut)
                                                                }
                                                            },
                                                            onHide = {
                                                                val mut = parsedEntries.toMutableList()
                                                                mut.removeAt(placed.layoutIndex)
                                                                commit(mut, hiddenKeys + placed.spec.key)
                                                            }
                                                        )
                                                        col += sp
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                                                        col += 1
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 숨겨진 항목 복원 트레이 (편집 모드)
                        if (editMode && hiddenSpecs.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Text(
                                text = "숨겨진 항목 (탭해서 추가)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                hiddenSpecs.forEach { spec ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                val nextEntries = parsedEntries + QsLayoutEntry.Tile(spec.key, 1)
                                                commit(nextEntries, hiddenKeys - spec.key)
                                            }
                                    ) {
                                        Text(
                                            text = "+ ${spec.title}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QsMenuTile(
    spec: ChromeMenuItemSpec,
    span: Int,
    editMode: Boolean,
    isHovered: Boolean,
    colWidthPx: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onResizeSpan: (Int) -> Unit,
    onMoveDelta: (Int) -> Unit,
    onHide: () -> Unit
) {
    val isActiveToggle = spec.isToggleCheckbox && spec.checked
    val accent = spec.accentColor ?: MaterialTheme.colorScheme.primary
    val baseBg = when {
        isHovered -> MaterialTheme.colorScheme.primaryContainer
        isActiveToggle -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val animBg by animateColorAsState(baseBg, tween(130), label = "qsTileBg")
    val hoverScale by animateFloatAsState(if (isHovered) 1.045f else 1f, EgoMotion.responsiveSpring(), label = "qsTileScale")
    val tileShape = RoundedCornerShape(18.dp)
    val borderColor = when {
        isHovered -> MaterialTheme.colorScheme.primary
        editMode -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        isActiveToggle -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val iconTint = when {
        spec.accentColor != null -> spec.accentColor
        isHovered || isActiveToggle -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    var dragAccumX by remember(spec.key, span) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = hoverScale
                scaleY = hoverScale
            }
            .clip(tileShape)
            .background(animBg)
            .border(if (isHovered) 1.5.dp else 1.dp, borderColor, tileShape)
            .then(
                if (!editMode) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        if (span == 1) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EgoIcon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                    symbolSize = 20.dp
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = if (spec.zoomPercent != null) "${spec.zoomPercent}%" else spec.title,
                    fontSize = 11.sp,
                    fontWeight = if (spec.bold || isActiveToggle || isHovered) FontWeight.Bold else FontWeight.Medium,
                    color = spec.accentColor ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (span == 3) 14.dp else 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    EgoIcon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                        symbolSize = 20.dp
                    )
                    Text(
                        text = spec.title,
                        fontSize = 12.5.sp,
                        fontWeight = if (spec.bold || isActiveToggle || isHovered) FontWeight.Bold else FontWeight.SemiBold,
                        color = spec.accentColor ?: MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (spec.zoomPercent != null && !editMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QsTinyCircleBtn(Icons.Default.Remove, "축소") { spec.onZoomOut?.invoke() }
                        Text(
                            text = "${spec.zoomPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { spec.onZoomReset?.invoke() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        QsTinyCircleBtn(Icons.Default.Add, "확대") { spec.onZoomIn?.invoke() }
                    }
                } else if (spec.isToggleCheckbox && !editMode) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (spec.checked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            .padding(2.dp),
                        contentAlignment = if (spec.checked) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (spec.checked) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.surface
                                )
                        )
                    }
                }
            }
        }

        // 편집 모드 오버레이: 좌상단 숨기기, 우상단 순서 이동, 우측 하단 모서리 리사이즈 핸들
        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                    .clickable(onClick = onHide),
                contentAlignment = Alignment.Center
            ) {
                EgoIcon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "숨기기",
                    tint = EgoDangerRed,
                    modifier = Modifier.size(12.dp),
                    symbolSize = 12.dp
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QsTinyCircleBtn(Icons.Default.KeyboardArrowLeft, "이전으로") { onMoveDelta(-1) }
                QsTinyCircleBtn(Icons.Default.KeyboardArrowRight, "다음으로") { onMoveDelta(1) }
            }

            // 우측 하단 모서리 곡선 리사이즈 핸들 (좌우 드래그로 1x1 / 1x2 / 1x3 조절)
            val handleColor = accent
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .pointerInput(spec.key, span) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumX += dragAmount
                                val threshold = (colWidthPx * 0.58f).coerceAtLeast(36f)
                                val deltaCols = (dragAccumX / threshold).roundToInt()
                                if (deltaCols != 0) {
                                    val next = (span + deltaCols).coerceIn(1, 3)
                                    if (next != span) {
                                        dragAccumX = 0f
                                        onResizeSpan(next)
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                Canvas(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 3.dp, bottom = 3.dp)
                ) {
                    drawArc(
                        color = handleColor,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(-size.width * 0.35f, -size.height * 0.35f),
                        size = Size(size.width * 1.25f, size.height * 1.25f),
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun QsHeaderPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accent: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                EgoIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp),
                    symbolSize = 12.dp
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QsTinyCircleBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        EgoIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(13.dp),
            symbolSize = 13.dp
        )
    }
}
