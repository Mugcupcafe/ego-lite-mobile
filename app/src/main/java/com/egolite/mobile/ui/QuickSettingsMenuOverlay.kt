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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.zIndex
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
                for (c in 0..(4 - sp)) {
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
                occupancy.add(BooleanArray(4))
                rows.add(mutableListOf(null, null, null, null))
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
                    // 헤더: 안드로이드 17 스타일 — 일반 모드에서는 글자 없이 [펜(편집)] + [톱니(설정)] 아이콘만, 편집 모드에서는 [+ 구분선 / 초기화 / 완료]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (editMode) {
                            Text(
                                text = "메뉴 · 툴바 편집",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                QsHeaderPill(
                                    label = "완료",
                                    icon = Icons.Default.Check,
                                    accent = true,
                                    onClick = { editMode = false }
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 글자 없는 펜(편집) 아이콘 버튼
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable { editMode = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "편집",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp),
                                        symbolSize = 19.dp
                                    )
                                }
                                // 바로 옆 설정 톱니바퀴 아이콘 버튼
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            onDismiss()
                                            itemByKey["settings"]?.onClick?.invoke()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "설정",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp),
                                        symbolSize = 19.dp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }

                    if (editMode) {
                        Text(
                            text = "툴바·타일을 길게 눌러 위치 이동 · 우하단 핸들로 1×1~1×4 크기 조절",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        QsToolbarEditStrip()
                        Spacer(modifier = Modifier.height(8.dp))
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
                                                while (col < 4) {
                                                    val placed = rowSlots[col]
                                                    if (placed != null) {
                                                        val sp = placed.span.coerceIn(1, 4 - col)
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
    val haptic = LocalHapticFeedback.current
    val isZoomBar = spec.zoomPercent != null
    val isActive = (spec.isToggleCheckbox && spec.checked) || spec.bold
    val isSingle = span == 1 && !isZoomBar
    val accent = spec.accentColor ?: MaterialTheme.colorScheme.primary

    val latestOnMoveDelta by rememberUpdatedState(onMoveDelta)
    val latestOnResizeSpan by rememberUpdatedState(onResizeSpan)
    val latestSpan by rememberUpdatedState(span)

    val targetCorner by animateDpAsState(
        targetValue = when {
            isZoomBar -> 35.dp
            isSingle && isActive -> 20.dp
            isSingle -> 35.dp
            isActive -> 22.dp
            else -> 35.dp
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

    var isTileLifted by remember { mutableStateOf(false) }
    var liftOffsetX by remember { mutableFloatStateOf(0f) }
    var liftOffsetY by remember { mutableFloatStateOf(0f) }
    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }

    val tileScale by animateFloatAsState(
        targetValue = when {
            isTileLifted -> 1.07f
            isHovered -> 1.035f
            else -> 1f
        },
        animationSpec = EgoMotion.responsiveSpring(),
        label = "qsTileScale"
    )

    val handleBreathAlpha = if (editMode && !isZoomBar) {
        val inf = rememberInfiniteTransition(label = "qsHandleBreath_${spec.key}")
        inf.animateFloat(
            initialValue = 0.42f,
            targetValue = 0.96f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "handleBreathAlpha"
        ).value
    } else 0f

    if (isZoomBar) {
        // 전폭 70.dp M3 하모니 가로 광감 확대바 (상대 드래그 + 퍼센티지 표시 + 편집모드 롱프레스 무제한 상하 이동)
        val curZoom = (spec.zoomPercent ?: 100).coerceIn(50, 150)
        val frac = ((curZoom - 50) / 100f).coerceIn(0f, 1f)
        var zoomDraggingRow by remember { mutableStateOf(false) }
        var zoomDragAccumY by remember { mutableFloatStateOf(0f) }
        var zoomLiftY by remember { mutableFloatStateOf(0f) }
        val zoomLiftScale by animateFloatAsState(
            targetValue = if (zoomDraggingRow) 1.035f else 1f,
            animationSpec = EgoMotion.responsiveSpring(),
            label = "qsZoomLift"
        )
        Box(
            modifier = modifier
                .height(70.dp)
                .zIndex(if (zoomDraggingRow) 20f else 0f)
                .graphicsLayer {
                    scaleX = zoomLiftScale
                    scaleY = zoomLiftScale
                    translationY = if (zoomDraggingRow) zoomLiftY else 0f
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
                                val stepY = 44.dp.toPx()
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        zoomDraggingRow = true
                                        zoomDragAccumY = 0f
                                        zoomLiftY = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = { zoomDraggingRow = false; zoomDragAccumY = 0f; zoomLiftY = 0f },
                                    onDragCancel = { zoomDraggingRow = false; zoomDragAccumY = 0f; zoomLiftY = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        zoomDragAccumY += dragAmount.y
                                        zoomLiftY = (zoomLiftY + dragAmount.y * 0.35f).coerceIn(-38f, 38f)
                                        if (zoomDragAccumY <= -stepY) {
                                            zoomDragAccumY = 0f
                                            zoomLiftY = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            latestOnMoveDelta(-1)
                                        } else if (zoomDragAccumY >= stepY) {
                                            zoomDragAccumY = 0f
                                            zoomLiftY = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            latestOnMoveDelta(1)
                                        }
                                    }
                                )
                            }
                    } else Modifier
                )
        ) {
            HorizontalHarmonyCapsuleSlider(
                valueFraction = frac,
                valueLabel = "${curZoom}%",
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

    // 외곽 Box는 clip하지 않음! -> 좌상단 '-' 제거 버튼과 우하단 크기조절 핸들이 절대 잘리지 않고 둘레에 딱 밀착됨!
    Box(
        modifier = modifier
            .height(70.dp)
            .zIndex(if (isTileLifted) 24f else 0f)
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
                translationX = if (isTileLifted) liftOffsetX else 0f
                translationY = if (isTileLifted) liftOffsetY else 0f
            }
            .then(
                if (editMode) {
                    Modifier.pointerInput("tile_drag_${spec.key}") {
                        val stepX = (colWidthPx * 0.62f).coerceAtLeast(44f)
                        val stepY = 56.dp.toPx()
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isTileLifted = true
                                liftOffsetX = 0f
                                liftOffsetY = 0f
                                dragAccumX = 0f
                                dragAccumY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                isTileLifted = false
                                liftOffsetX = 0f
                                liftOffsetY = 0f
                                dragAccumX = 0f
                                dragAccumY = 0f
                            },
                            onDragCancel = {
                                isTileLifted = false
                                liftOffsetX = 0f
                                liftOffsetY = 0f
                                dragAccumX = 0f
                                dragAccumY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                liftOffsetX = (liftOffsetX + dragAmount.x * 0.45f).coerceIn(-64f, 64f)
                                liftOffsetY = (liftOffsetY + dragAmount.y * 0.45f).coerceIn(-64f, 64f)
                                dragAccumX += dragAmount.x
                                dragAccumY += dragAmount.y
                                when {
                                    dragAccumX >= stepX -> {
                                        dragAccumX = 0f; liftOffsetX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        latestOnMoveDelta(1)
                                    }
                                    dragAccumX <= -stepX -> {
                                        dragAccumX = 0f; liftOffsetX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        latestOnMoveDelta(-1)
                                    }
                                    dragAccumY >= stepY -> {
                                        dragAccumY = 0f; liftOffsetY = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        latestOnMoveDelta(2)
                                    }
                                    dragAccumY <= -stepY -> {
                                        dragAccumY = 0f; liftOffsetY = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        latestOnMoveDelta(-2)
                                    }
                                }
                            }
                        )
                    }
                } else {
                    Modifier.clickable { onClick() }
                }
            )
    ) {
        // 내부 클립된 타일 본체
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(tileShape)
                .background(containerBg)
                .harmonyRimGlow(enabled = prefs.rimGlowEnabled, shape = tileShape, glowColor = accent)
                .then(
                    if (isHovered || isTileLifted) Modifier.border(2.dp, accent, tileShape)
                    else Modifier
                )
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
                // 1x2 / 1x3 / 1x4 와이드 타일: 좌측 44dp 아이콘 중심 42dp 고정 (1x1 및 확대바와 0dp 수직 정렬!)
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
        }

        // 편집 모드 오버레이 (외곽 Box 기준이라 절대 잘리지 않음! < > 화살표 제거 완료)
        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(EgoDangerRed)
                    .clickable(onClick = onHide),
                contentAlignment = Alignment.Center
            ) {
                EgoIcon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "숨기기",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                    symbolSize = 14.dp
                )
            }

            // 우하단 둘레 밀착 두꺼운 호흡 크기조절 핸들 (원형/사각 둘레 자동 맞춤, 1x1~1x4, 잘림 0%)
            val handleTint = if (isSingle && isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = handleBreathAlpha)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = handleBreathAlpha)
            }
            var resizeAccumX by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .pointerInput("resize_${spec.key}") {
                        detectHorizontalDragGestures(
                            onDragStart = { resizeAccumX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                resizeAccumX += dragAmount
                                val threshold = (colWidthPx * 0.58f).coerceAtLeast(36f)
                                val deltaCols = (resizeAccumX / threshold).roundToInt()
                                if (deltaCols != 0) {
                                    val next = (latestSpan + deltaCols).coerceIn(1, 4)
                                    if (next != latestSpan) {
                                        resizeAccumX = 0f
                                        latestOnResizeSpan(next)
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                Canvas(
                    modifier = Modifier
                        .size(26.dp)
                        .padding(end = 2.dp, bottom = 2.dp)
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


/**
 * 빠른메뉴 편집 모드 상단에 표시되는 주소창 툴바 아이콘 직접 편집 스트립.
 * - 아이콘을 길게 눌러 좌우 드래그로 툴바 순서 실시간 변경
 * - 좌상단 '-' 버튼으로 툴바에서 빼기 / 하단 숨김 칩 탭으로 즉시 복원
 */
@Composable
private fun QsToolbarEditStrip() {
    val prefs by SpaceManager.preferences.collectAsState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val defaultOrder = remember {
        listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
    }
    val normalizedOrder = remember(prefs.toolbarButtonOrder) {
        val known = prefs.toolbarButtonOrder.filter { it in defaultOrder }
        known + defaultOrder.filter { it !in known }
    }
    fun isToolbarKeyEnabled(key: String): Boolean = when (key) {
        "MCP" -> prefs.showMcpBadgeButton
        "AI" -> prefs.showAiCommandButton
        "ADBLOCK" -> prefs.showAdBlockButton
        "TRANSLATE" -> prefs.showTranslateButton
        "DARKMODE" -> prefs.showDarkModeButton
        "NEW_TAB" -> prefs.showNewTabToolbarButton
        "SHARE" -> prefs.showShareButton
        "FIND" -> prefs.showFindInPageButton
        "DESKTOP" -> prefs.showDesktopSiteButton
        "READER" -> prefs.showReaderModeButton
        "PROFILE" -> prefs.showProfileAvatarButton
        else -> false
    }
    fun setToolbarKeyEnabled(key: String, enabled: Boolean) {
        SpaceManager.updatePreferences { p ->
            when (key) {
                "MCP" -> p.copy(showMcpBadgeButton = enabled)
                "AI" -> p.copy(showAiCommandButton = enabled)
                "ADBLOCK" -> p.copy(showAdBlockButton = enabled)
                "TRANSLATE" -> p.copy(showTranslateButton = enabled)
                "DARKMODE" -> p.copy(showDarkModeButton = enabled)
                "NEW_TAB" -> p.copy(showNewTabToolbarButton = enabled)
                "SHARE" -> p.copy(showShareButton = enabled)
                "FIND" -> p.copy(showFindInPageButton = enabled)
                "DESKTOP" -> p.copy(showDesktopSiteButton = enabled)
                "READER" -> p.copy(showReaderModeButton = enabled)
                "PROFILE" -> p.copy(showProfileAvatarButton = enabled)
                else -> p
            }
        }
    }
    fun keyMeta(key: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, String> = when (key) {
        "MCP" -> Icons.Default.LinkOff to "MCP"
        "AI" -> Icons.Default.AutoAwesome to "AI 명령"
        "ADBLOCK" -> Icons.Default.Shield to "광고차단"
        "TRANSLATE" -> Icons.Default.Translate to "번역"
        "DARKMODE" -> Icons.Default.DarkMode to "다크"
        "NEW_TAB" -> Icons.Default.Add to "새 탭"
        "SHARE" -> Icons.Default.Share to "공유"
        "FIND" -> Icons.Default.FindInPage to "찾기"
        "DESKTOP" -> Icons.Default.DesktopWindows to "데스크톱"
        "READER" -> Icons.Default.MenuBook to "리더"
        else -> Icons.Default.Person to "프로필"
    }

    val enabledKeys = remember(normalizedOrder, prefs) { normalizedOrder.filter { isToolbarKeyEnabled(it) } }
    val hiddenKeys = remember(normalizedOrder, prefs) { normalizedOrder.filter { !isToolbarKeyEnabled(it) } }
    val latestEnabled by rememberUpdatedState(enabledKeys)
    val latestFull by rememberUpdatedState(normalizedOrder)
    val stepPx = with(density) { 42.dp.toPx() }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "주소창 툴바 아이콘 (길게 눌러 좌우 이동 · − 빼기)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                enabledKeys.forEach { key ->
                    val (iconVec, label) = keyMeta(key)
                    var dragging by remember(key) { mutableStateOf(false) }
                    var offsetX by remember(key) { mutableFloatStateOf(0f) }
                    var accumX by remember(key) { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .zIndex(if (dragging) 12f else 0f)
                            .graphicsLayer {
                                translationX = if (dragging) offsetX else 0f
                                scaleX = if (dragging) 1.16f else 1f
                                scaleY = if (dragging) 1.16f else 1f
                            }
                            .pointerInput("qs_tb_$key") {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragging = true
                                        offsetX = 0f
                                        accumX = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = { dragging = false; offsetX = 0f; accumX = 0f },
                                    onDragCancel = { dragging = false; offsetX = 0f; accumX = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX = (offsetX + dragAmount.x * 0.55f).coerceIn(-56f, 56f)
                                        accumX += dragAmount.x
                                        val dir = when {
                                            accumX >= stepPx -> 1
                                            accumX <= -stepPx -> -1
                                            else -> 0
                                        }
                                        if (dir != 0) {
                                            accumX = 0f
                                            offsetX = 0f
                                            val curEn = latestEnabled
                                            val idx = curEn.indexOf(key)
                                            val targetEnIdx = (idx + dir).coerceIn(0, curEn.lastIndex)
                                            if (idx >= 0 && targetEnIdx != idx) {
                                                val targetKey = curEn[targetEnIdx]
                                                val mut = latestFull.toMutableList()
                                                val fi = mut.indexOf(key)
                                                val ti = mut.indexOf(targetKey)
                                                if (fi >= 0 && ti >= 0) {
                                                    mut.removeAt(fi)
                                                    mut.add(ti, key)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    SpaceManager.updatePreferences { it.copy(toolbarButtonOrder = mut) }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (dragging) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                imageVector = iconVec,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp),
                                symbolSize = 17.dp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(EgoDangerRed)
                                .clickable { setToolbarKeyEnabled(key, false) },
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "툴바에서 빼기",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp),
                                symbolSize = 10.dp
                            )
                        }
                    }
                }
            }
            if (hiddenKeys.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    hiddenKeys.forEach { hKey ->
                        val (iconVec, label) = keyMeta(hKey)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.clickable { setToolbarKeyEnabled(hKey, true) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                EgoIcon(iconVec, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp), symbolSize = 12.dp)
                                Text("+ $label", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
