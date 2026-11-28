package com.egolite.mobile.ui
import androidx.compose.animation.core.animateFloat
import com.egolite.mobile.browser.SpaceManager
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing

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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
    "zoom:4",
    "newtab:1",
    "incognito:1",
    "aispace:2",
    "bmbar:2",
    "dark:2",
    "div:d1",
    "bookmark:1",
    "site:1",
    "history:1",
    "download:1",
    "recent:1",
    "share:1",
    "clear:1",
    "settings:1",
    "mcp:2",
    "default_app:2"
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
                    val span = token.substring(sep + 1).toIntOrNull()?.coerceIn(1, 4) ?: 1
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
            is QsLayoutEntry.Tile -> out.add("${e.key}:${e.span.coerceIn(1, 4)}")
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
            val sp = placed.span.coerceIn(1, 4)
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
                    currentSectionTiles.add(QsPlacedTile(spec, entry.span.coerceIn(1, 4), idx))
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
    onSetZoomPercent: (Int) -> Unit = {},
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
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 16.dp,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .width(384.dp)
                    .heightIn(max = 760.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(30.dp)
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
                    val colWidthPx = with(density) { 84.dp.toPx() }
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
                                                            onSetZoomPercent = onSetZoomPercent,
                                        onResizeSpan = { nextSpan ->
                                                                val clamped = nextSpan.coerceIn(1, 4)
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
    onSetZoomPercent: (Int) -> Unit = {},
    onResizeSpan: (Int) -> Unit,
    onMoveDelta: (Int) -> Unit,
    onHide: () -> Unit
) {
    val prefs by SpaceManager.preferences.collectAsState()
    val isZoomBar = spec.zoomPercent != null
    val isActive = (spec.isToggleCheckbox && spec.checked) || spec.bold
    val isSingle = span == 1 && !isZoomBar
    val accent = spec.accentColor ?: MaterialTheme.colorScheme.primary

    val targetCorner by animateDpAsState(
        targetValue = when {
            isZoomBar -> 35.dp
            isSingle && isActive -> 20.dp
            isSingle -> 36.dp
            isActive -> 22.dp
            else -> 36.dp
        },
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
        label = "a17TileCorner"
    )
    val tileShape = RoundedCornerShape(targetCorner)

    val containerBg by animateColorAsState(
        targetValue = when {
            isHovered -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            isSingle && isActive -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(220),
        label = "a17TileBg"
    )
    val iconFg by animateColorAsState(
        targetValue = when {
            isSingle && isActive -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> spec.accentColor ?: MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "a17IconFg"
    )
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 1.035f else 1f,
        animationSpec = EgoMotion.responsiveSpring(),
        label = "qsTileScale"
    )
    var dragAccumX by remember(spec.key, span) { mutableFloatStateOf(0f) }

    val handleBreathAlpha = if (editMode && !isZoomBar) {
        val inf = rememberInfiniteTransition(label = "qsHandleBreath_${spec.key}")
        inf.animateFloat(
            initialValue = 0.40f,
            targetValue = 0.96f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "handleBreathAlpha"
        ).value
    } else 0f

    if (isZoomBar) {
        // 전폭 70.dp M3 하모니 가로 광감 확대바 (상대 드래그, 1열 수직축 42.dp 정렬, 편집모드에서는 확대 비활성 + 롱프레스 상하 위치 이동)
        val curZoom = (spec.zoomPercent ?: 100).coerceIn(50, 150)
        val frac = ((curZoom - 50) / 100f).coerceIn(0f, 1f)
        val haptic = LocalHapticFeedback.current
        var zoomDraggingRow by remember { mutableStateOf(false) }
        var zoomDragAccumY by remember { mutableFloatStateOf(0f) }
        val zoomLiftScale by animateFloatAsState(
            targetValue = if (zoomDraggingRow) 1.03f else 1f,
            animationSpec = EgoMotion.responsiveSpring(),
            label = "qsZoomLift"
        )
        Box(
            modifier = modifier
                .height(70.dp)
                .graphicsLayer {
                    scaleX = zoomLiftScale
                    scaleY = zoomLiftScale
                }
                .then(
                    if (editMode) {
                        Modifier
                            .border(
                                width = if (zoomDraggingRow) 2.dp else 1.dp,
                                color = if (zoomDraggingRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(35.dp)
                            )
                            .pointerInput("zoom_row_drag") {
                                val stepY = 48.dp.toPx()
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        zoomDraggingRow = true
                                        zoomDragAccumY = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = { zoomDraggingRow = false; zoomDragAccumY = 0f },
                                    onDragCancel = { zoomDraggingRow = false; zoomDragAccumY = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        zoomDragAccumY += dragAmount.y
                                        if (zoomDragAccumY <= -stepY) {
                                            zoomDragAccumY = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onMoveDelta(-1)
                                        } else if (zoomDragAccumY >= stepY) {
                                            zoomDragAccumY = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onMoveDelta(1)
                                        }
                                    }
                                )
                            }
                    } else Modifier
                )
        ) {
            HorizontalHarmonyCapsuleSlider(
                valueFraction = frac,
                icon = spec.icon,
                accentColor = MaterialTheme.colorScheme.primary,
                lightIntensity = prefs.progressLightIntensity,
                particleDensity = prefs.progressParticleDensity,
                darkUseThemeColor = prefs.progressDarkUseThemeColor,
                dayParticleColor = Color(prefs.lightModeParticleColorHex),
                interactive = !editMode,
                iconCenterOffsetDp = 42.dp,
                onValueChange = { f ->
                    val snapped = ((50f + f.coerceIn(0f, 1f) * 100f) / 5f).roundToInt() * 5
                    onSetZoomPercent(snapped.coerceIn(50, 150))
                },
                onDoubleTapReset = { onSetZoomPercent(100) },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    Box(
        modifier = modifier
            .height(70.dp)
            .graphicsLayer {
                scaleX = hoverScale
                scaleY = hoverScale
            }
            .clip(tileShape)
            .background(containerBg)
            .harmonyRimGlow(enabled = prefs.rimGlowEnabled, shape = tileShape, glowColor = accent)
            .then(
                if (isHovered) Modifier.border(1.5.dp, accent, tileShape)
                else Modifier
            )
            .clickable(enabled = !editMode) { onClick() }
    ) {
        if (isSingle) {
            // 1x1 타일: 텍스트·크기 배지 완전 제거, 중앙 22dp 아이콘 전용 (Android 17 M3)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EgoIcon(
                    imageVector = spec.icon,
                    contentDescription = spec.title,
                    tint = iconFg,
                    modifier = Modifier.size(22.dp),
                    symbolSize = 22.dp
                )
            }
        } else {
            // 1x2 / 1x3 / 1x4 와이드 타일: 좌측 44dp 아이콘 컨테이너 고정(OFF↔ON 시 0px 위치 변화!)
            val badgeBg by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                animationSpec = tween(220),
                label = "wideBadgeBg"
            )
            val badgeFg by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else (spec.accentColor ?: MaterialTheme.colorScheme.onSurface),
                animationSpec = tween(220),
                label = "wideBadgeFg"
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    EgoIcon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(21.dp),
                        symbolSize = 21.dp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spec.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (spec.isToggleCheckbox) {
                        Text(
                            text = if (spec.checked) "켜짐" else "꺼짐",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

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

            // 우하단 둘레 밀착 두꺼운 호흡 크기조절 핸들 (원형/사각 둘레 자동 맞춤, 1x1~1x4)
            val handleTint = if (isSingle && isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = handleBreathAlpha)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = handleBreathAlpha)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .pointerInput(spec.key, span) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumX += dragAmount
                                val threshold = (colWidthPx * 0.62f).coerceAtLeast(36f)
                                val deltaCols = (dragAccumX / threshold).roundToInt()
                                if (deltaCols != 0) {
                                    val next = (span + deltaCols).coerceIn(1, 4)
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
                        .size(28.dp)
                        .padding(end = 3.dp, bottom = 3.dp)
                ) {
                    val strokeW = 5.5.dp.toPx()
                    val isCircleContour = !isActive
                    val arcSize = if (isCircleContour) size.minDimension * 1.85f else size.minDimension * 1.15f
                    drawArc(
                        color = handleTint,
                        startAngle = 8f,
                        sweepAngle = 74f,
                        useCenter = false,
                        topLeft = Offset(size.width - arcSize, size.height - arcSize),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
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
