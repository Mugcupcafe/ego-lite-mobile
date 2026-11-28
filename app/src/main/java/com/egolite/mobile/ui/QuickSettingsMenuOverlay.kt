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
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.HorizontalRule
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.positionInRoot
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
private val QS_KEEP_OPEN_KEYS = setOf("dark", "bmbar", "translate", "clear", "copy_link", "desktop")

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUnifiedOpticalRim(
    shape: androidx.compose.ui.graphics.Shape,
    lightCenter: Offset,
    glowColor: Color,
    lightAlpha: Float,
    intensity: Float
) {
    if (lightAlpha <= 0.01f) return
    val strokePx = (1.35.dp.toPx() * (0.85f + 0.25f * intensity)).coerceAtMost(2.2.dp.toPx())
    val half = strokePx * 0.5f
    val radiusPx = 210.dp.toPx()
    val rimPeak = (0.90f * intensity * lightAlpha).coerceAtMost(0.96f)
    val rimMid = (0.55f * intensity * lightAlpha).coerceAtMost(0.86f)
    val rimTail = (0.14f * intensity * lightAlpha).coerceAtMost(0.35f)
    inset(half, half, half, half) {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = rimPeak),
                    glowColor.copy(alpha = rimMid),
                    glowColor.copy(alpha = rimTail),
                    Color.Transparent
                ),
                center = Offset(lightCenter.x - half, lightCenter.y - half),
                radius = radiusPx
            ),
            style = Stroke(width = strokePx)
        )
    }
}

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
    data class Section(
        val sectionId: Int,
        val rows: List<List<QsPlacedTile?>>,
        val endLayoutIndex: Int
    ) : QsRenderBlock()
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
    var nextSectionId = 0

    fun flushSection(endIdx: Int) {
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
        blocks.add(QsRenderBlock.Section(nextSectionId++, rows, endIdx))
        currentSectionTiles.clear()
    }

    entries.forEachIndexed { idx, entry ->
        when (entry) {
            is QsLayoutEntry.Divider -> {
                flushSection(idx)
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
    flushSection(entries.size)
    return blocks
}

/**
 * 3열 퀵세팅 스타일 커스텀 브라우저 메뉴:
 * - 모든 행 높이 동일(70.dp)
 * - 구분선 내부 섹션 격리 dense 채움 (바깥 항목 끌어오지 않음)
 * - 편집 모드: 우측 하단 모서리 핸들 드래그로 1×1 / 1×2 / 1×3 리사이즈, + 구분선 삽입/이동/삭제, 숨기기/복원
 * - Drag-to-Open / Drag-to-Select: 각 타일의 live root bounds를 보고해 손가락 위치 타일 즉시 실행
 */

class QsExternalDropState {
    var targetInsertIndex: Int? by mutableStateOf(null)
}

private val BUILTIN_QS_TO_TB = mapOf(
    "newtab" to "NEW_TAB",
    "dark" to "DARKMODE",
    "share" to "SHARE",
    "find" to "FIND",
    "desktop" to "DESKTOP",
    "reader" to "READER",
    "mcp" to "MCP",
    "translate" to "TRANSLATE"
)

private val BUILTIN_TB_TO_QS = BUILTIN_QS_TO_TB.entries.associate { (k, v) -> v to k }

fun resolveTbKeyFromQs(qsKey: String): String = BUILTIN_QS_TO_TB[qsKey] ?: "QS:$qsKey"

fun resolveQsKeyFromTb(tbKey: String): String? = when {
    tbKey.startsWith("QS:") -> tbKey.removePrefix("QS:")
    else -> BUILTIN_TB_TO_QS[tbKey]
}

fun getVisibleToolbarKeys(p: com.egolite.mobile.model.BrowserPreferences): List<String> {
    val base = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
    val ord = p.toolbarButtonOrder.filter { it in base || it.startsWith("QS:") } + base.filter { it !in p.toolbarButtonOrder }
    return ord.filter { k ->
        when {
            k.startsWith("QS:") -> true
            k == "MCP" -> p.showMcpBadgeButton
            k == "ADBLOCK" -> p.showAdBlockButton
            k == "TRANSLATE" -> p.showTranslateButton
            k == "DARKMODE" -> p.showDarkModeButton
            k == "NEW_TAB" -> p.showNewTabToolbarButton
            k == "SHARE" -> p.showShareButton
            k == "FIND" -> p.showFindInPageButton
            k == "DESKTOP" -> p.showDesktopSiteButton
            k == "READER" -> p.showReaderModeButton
            k == "PROFILE" -> p.showProfileAvatarButton
            else -> false
        }
    }
}

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
    onEditModeChanged: (Boolean) -> Unit = {},
    toolbarIconsRect: Rect = Rect.Zero,
    externalDragKey: String? = null,
    externalDragRootPos: Offset? = null,
    externalDropState: QsExternalDropState? = null,
    onToolbarInsertSlotChanged: (Int?) -> Unit = {},
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
    LaunchedEffect(editMode, visible) {
        onEditModeChanged(visible && editMode)
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
                .padding(top = if (editMode) topPadding else 0.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )
    }

    // 루트 레벨 드래그 고스트 상태 (Surface 클리핑 바깥에서 렌더링해 상단 툴바 위로 자연스럽게 올라감!)
    var overlayRootPos by remember { mutableStateOf(Offset.Zero) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }
    var dragTileSizePx by remember { mutableStateOf(Size.Zero) }
    var dragFingerRoot by remember { mutableStateOf(Offset.Zero) }
    val latestToolbarRect by rememberUpdatedState(toolbarIconsRect)
    val latestOnTbSlotChanged by rememberUpdatedState(onToolbarInsertSlotChanged)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) overlayRootPos = coords.positionInRoot()
            }
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, end = endPadding, start = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        // 확정 프리셋: Liquid Drop (stagger=40ms · bounce=26% · dur=379ms · int=150%)
        // 각 행 독립 Animatable 스프링으로 열림/닫힘 중간 반전 100% 연속 보간 + 닫힘 시 최하단부터 접힘!
        val totalContentRows = remember(blocks) {
            blocks.sumOf { b ->
                when (b) {
                    is QsRenderBlock.Section -> b.rows.size
                    is QsRenderBlock.DividerRow -> 1
                }
            }.coerceIn(1, 14)
        }
        val panelAnim = remember { Animatable(0f) }
        val rowAnims = remember { List(16) { Animatable(0f) } }
        val latestTotalRows by rememberUpdatedState(totalContentRows)

        LaunchedEffect(visible) {
            val maxRowIdx = latestTotalRows.coerceIn(1, 15) // row 0 = header, 1..maxRowIdx = content rows
            val staggerMs = 40f
            if (visible) {
                // 열림: 패널 즉시 시작 + 최상단(Row 0) -> 최하단 순차 출발 (bounce=26% -> damping 0.633, stiffness 455)
                launch {
                    panelAnim.animateTo(1f, spring(dampingRatio = 0.76f, stiffness = 455f))
                }
                for (r in 0..maxRowIdx) {
                    val anim = rowAnims[r]
                    val remain = (1f - anim.value).coerceIn(0f, 1f)
                    val waitMs = if (remain > 0.05f) (r * staggerMs * (0.35f + 0.65f * remain)).toLong() else 0L
                    launch {
                        if (waitMs > 0L) delay(waitMs)
                        anim.animateTo(1f, spring(dampingRatio = 0.633f, stiffness = 455f))
                    }
                }
            } else {
                // 닫힘: 최하단 행부터 순차 접힘 -> 최상단 -> 패널 수납 (damping 0.94, stiffness 437)
                val activeMax = (0..maxRowIdx).lastOrNull { rowAnims[it].value > 0.04f } ?: 0
                for (r in 0..maxRowIdx) {
                    val anim = rowAnims[r]
                    if (anim.value <= 0.02f) {
                        launch { anim.snapTo(0f) }
                    } else {
                        val distFromBottom = (activeMax - r).coerceAtLeast(0)
                        val waitMs = (distFromBottom * staggerMs * 0.78f).toLong()
                        launch {
                            if (waitMs > 0L) delay(waitMs)
                            anim.animateTo(0f, spring(dampingRatio = 0.94f, stiffness = 437f))
                        }
                    }
                }
                val panelWaitMs = (activeMax * staggerMs * 0.62f).toLong()
                launch {
                    if (panelWaitMs > 0L) delay(panelWaitMs)
                    panelAnim.animateTo(0f, spring(dampingRatio = 0.94f, stiffness = 437f))
                }
            }
        }

        val anyRowVisible = rowAnims.any { it.value > 0.015f }
        if (visible || panelAnim.value > 0.015f || anyRowVisible) {
            val pv = panelAnim.value.coerceIn(0f, 1.06f)
            val p = pv.coerceIn(0f, 1f)
            val maxRowIdxNow = totalContentRows.coerceIn(1, 15)
            // 최하단 행이 접히면 패널 하단도 함께 위로 따라 올라가도록 동적 높이 비율 계산
            var lowestVisibleRatio = 0f
            for (r in 0..maxRowIdxNow) {
                val rv = rowAnims[r].value.coerceIn(0f, 1f)
                if (rv > 0.01f) {
                    val rb = (r + 1f) / (maxRowIdxNow + 1f)
                    val prevRb = r.toFloat() / (maxRowIdxNow + 1f)
                    val reached = prevRb + (rb - prevRb) * rv
                    if (reached > lowestVisibleRatio) lowestVisibleRatio = reached
                }
            }
            val dynamicScaleY = (0.24f + 0.76f * maxOf(pv * 0.35f, lowestVisibleRatio)).coerceIn(0.24f, 1.04f)
            val morphCornerDp = (30f + (1f - p) * 44f).dp
            val morphShape = RoundedCornerShape(morphCornerDp)
            val panelGlowColor = MaterialTheme.colorScheme.primary
            val rimIntensity = LocalRimGlowIntensity.current.coerceIn(0.4f, 2.6f)
            var panelRootPos by remember { mutableStateOf(Offset.Zero) }
            var panelTouchLocal by remember { mutableStateOf(Offset.Zero) }
            var panelTouching by remember { mutableStateOf(false) }
            val panelGlowAlpha by animateFloatAsState(
                targetValue = if (panelTouching) 1f else 0f,
                animationSpec = tween(durationMillis = if (panelTouching) 75 else 380),
                label = "qsPanelLightAlpha"
            )
            val panelTouchRootPos = panelRootPos + panelTouchLocal
            Surface(
                shape = morphShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = (16f * p).dp,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .width(384.dp)
                    .heightIn(max = if (editMode) 1600.dp else 820.dp)
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) panelRootPos = coords.positionInRoot()
                    }
                    .graphicsLayer {
                        scaleX = 0.28f + 0.72f * pv
                        scaleY = dynamicScaleY
                        alpha = (p * 2.2f).coerceIn(0f, 1f)
                        translationY = (1f - p) * (-14.dp.toPx())
                        transformOrigin = TransformOrigin(0.94f, 0f)
                    }
                    .animateContentSize(
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 260f)
                    )
                    .pointerInput("qs_panel_light_field") {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                panelTouchLocal = down.position
                                panelTouching = true
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val cur = ev.changes.firstOrNull { it.id == down.id }
                                    if (cur == null || !cur.pressed) break
                                    panelTouchLocal = cur.position
                                }
                                panelTouching = false
                            }
                        }
                    }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        morphShape
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                // 광감 번짐은 패널 배경 레이어(drawBehind)에 그려져 모든 버튼·조절바보다 뒤에 위치!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (panelGlowAlpha > 0.01f) {
                                val radiusPx = (size.maxDimension * 0.56f).coerceIn(140f, 400f)
                                val outline = morphShape.createOutline(size, layoutDirection, this)
                                val surfCore = (0.16f * rimIntensity * panelGlowAlpha).coerceAtMost(0.42f)
                                val surfMid = (0.24f * rimIntensity * panelGlowAlpha).coerceAtMost(0.55f)
                                drawOutline(
                                    outline = outline,
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = surfCore),
                                            panelGlowColor.copy(alpha = surfMid),
                                            Color.Transparent
                                        ),
                                        center = panelTouchLocal,
                                        radius = radiusPx
                                    ),
                                    style = Fill
                                )
                                drawUnifiedOpticalRim(
                                    shape = morphShape,
                                    lightCenter = panelTouchLocal,
                                    glowColor = panelGlowColor,
                                    lightAlpha = panelGlowAlpha,
                                    intensity = rimIntensity
                                )
                            }
                        }
                        .padding(12.dp)
                ) {
                    // 헤더: 텍스트·안내문구 없이 아이콘 전용 — 편집 진입 시 펜 아이콘이 원래 위치를 유지한 채 원형으로 감싸지며 토글
                    val editBtnBg by animateColorAsState(
                        targetValue = if (editMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(180),
                        label = "qsEditBtnBg"
                    )
                    val editBtnTint by animateColorAsState(
                        targetValue = if (editMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(180),
                        label = "qsEditBtnTint"
                    )
                    val headerV = rowAnims[0].value
                    val headerInv = 1f - headerV
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(30f)
                            .graphicsLayer {
                                translationX = (14f * headerInv * 1.50f).dp.toPx()
                                translationY = (-24f * headerInv * 1.50f).dp.toPx()
                                scaleX = (0.48f + 0.52f * headerV).coerceAtLeast(0.20f)
                                scaleY = (0.20f + 0.80f * headerV).coerceAtLeast(0.14f)
                                alpha = (headerV.coerceIn(0f, 1f) * 1.75f).coerceIn(0f, 1f)
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 펜(편집) 아이콘 — 원래 위치 고정, 편집 모드일 때 원형 배경이 감싸며 다시 누르면 완료
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(editBtnBg)
                                    .clickable { editMode = !editMode },
                                contentAlignment = Alignment.Center
                            ) {
                                EgoIcon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = if (editMode) "편집 완료" else "편집",
                                    tint = editBtnTint,
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

                        // 편집 모드 전용 우측 아이콘 버튼: 구분선 추가 + 초기화 (텍스트 없음)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = editMode,
                            enter = fadeIn(tween(150)) + scaleIn(tween(170), initialScale = 0.8f),
                            exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.8f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable {
                                            val next = listOf(QsLayoutEntry.Divider("d_${System.currentTimeMillis()}")) + parsedEntries
                                            commit(next)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.Default.HorizontalRule,
                                        contentDescription = "구분선 추가",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp),
                                        symbolSize = 19.dp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { onSaveLayout(emptyList()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "초기화",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp),
                                        symbolSize = 19.dp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4열 섹션 격리 영역 + 안정적 부모 레벨 영구 드래그 오버레이 (리컴포지션 시에도 드래그 절대 끊기지 않음!)
                    val colWidthPx = with(density) { 84.dp.toPx() }
                    val tileRootBounds = remember { mutableStateMapOf<String, Rect>() }
                    val emptySlotBounds = remember { mutableStateMapOf<String, Pair<Rect, Int>>() }
                    var hoveredEmptySlotId by remember { mutableStateOf<String?>(null) }
                    val prevTileRoots = remember { mutableMapOf<String, Offset>() }
                    val tileAnimScope = rememberCoroutineScope()
                    var gridRootPos by remember { mutableStateOf(Offset.Zero) }
                    var dragFingerLocal by remember { mutableStateOf(Offset.Zero) }
                    var lastSwapMs by remember { mutableStateOf(0L) }
                    var showCompactZoomSlider by remember { mutableStateOf(false) }
                    val latestEntries by rememberUpdatedState(parsedEntries)
                    val latestHidden by rememberUpdatedState(hiddenKeys)

                    // 상단 툴바에서 끌어내리는 중인 아이콘의 실시간 드롭 타겟 계산 (타일 위 / 공백 슬롯 위)
                    val extHoverTileKey = remember(editMode, externalDragKey, externalDragRootPos, tileRootBounds.size) {
                        if (!editMode || externalDragKey == null || externalDragRootPos == null) null
                        else tileRootBounds.entries.firstOrNull { (_, r) -> r.contains(externalDragRootPos) }?.key
                    }
                    val extHoverEmptyEntry = remember(editMode, externalDragKey, externalDragRootPos, extHoverTileKey, emptySlotBounds.size) {
                        if (!editMode || externalDragKey == null || externalDragRootPos == null || extHoverTileKey != null) null
                        else emptySlotBounds.entries.firstOrNull { (_, pair) -> pair.first.contains(externalDragRootPos) }
                    }
                    LaunchedEffect(extHoverTileKey, extHoverEmptyEntry?.key, parsedEntries) {
                        externalDropState?.targetInsertIndex = when {
                            extHoverTileKey != null -> {
                                val idx = parsedEntries.indexOfFirst { it is QsLayoutEntry.Tile && it.key == extHoverTileKey }
                                if (idx >= 0) idx else null
                            }
                            extHoverEmptyEntry != null -> extHoverEmptyEntry.value.second
                            else -> null
                        }
                    }

                    LaunchedEffect(editMode, visible) {
                        if (!editMode || !visible) {
                            draggingKey = null
                            hoveredEmptySlotId = null
                            latestOnTbSlotChanged(null)
                            externalDropState?.targetInsertIndex = null
                            tileRootBounds.clear()
                            emptySlotBounds.clear()
                            prevTileRoots.clear()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                if (coords.isAttached) gridRootPos = coords.positionInRoot()
                            }
                            .then(
                                if (editMode) {
                                    Modifier.pointerInput("qs_persistent_grid_drag") {
                                        val topEscapePx = (-22).dp.toPx()
                                        val tbHitPadPx = 18.dp.toPx()
                                        fun calcTbSlot(rootPt: Offset): Int {
                                            val tbRect = latestToolbarRect
                                            val vis = getVisibleToolbarKeys(SpaceManager.preferences.value)
                                            if (vis.isEmpty() || tbRect.width <= 1f) return 0
                                            val slotW = tbRect.width / vis.size.toFloat()
                                            return ((rootPt.x - tbRect.left) / slotW).roundToInt().coerceIn(0, vis.size)
                                        }
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { startLocal ->
                                                val rootPt = gridRootPos + startLocal
                                                val hit = tileRootBounds.entries.firstOrNull { (_, r) -> r.contains(rootPt) }
                                                if (hit != null) {
                                                    draggingKey = hit.key
                                                    dragTileSizePx = Size(hit.value.width, hit.value.height)
                                                    val tileTopLeftLocal = Offset(hit.value.left - gridRootPos.x, hit.value.top - gridRootPos.y)
                                                    dragGrabOffset = startLocal - tileTopLeftLocal
                                                    dragFingerLocal = startLocal
                                                    dragFingerRoot = rootPt
                                                    lastSwapMs = 0L
                                                    latestOnTbSlotChanged(null)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            },
                                            onDragEnd = {
                                                val dk = draggingKey
                                                val fy = dragFingerLocal.y
                                                val endRoot = dragFingerRoot
                                                val tbRect = latestToolbarRect
                                                val inToolbarZone = dk != null && dk != "zoom" && (
                                                    (tbRect.width > 1f && endRoot.y <= tbRect.bottom + tbHitPadPx) ||
                                                        fy < topEscapePx
                                                    )
                                                draggingKey = null
                                                hoveredEmptySlotId = null
                                                latestOnTbSlotChanged(null)
                                                // 상단 주소창 툴바 위로 끌어올리면 확대바 제외 모든 버튼을 원하는 X 슬롯 위치에 정확히 삽입!
                                                if (inToolbarZone && dk != null) {
                                                    val tbKey = resolveTbKeyFromQs(dk)
                                                    val desiredVisSlot = calcTbSlot(endRoot)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    SpaceManager.updatePreferences { p ->
                                                        val base = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
                                                        val visBefore = getVisibleToolbarKeys(p)
                                                        val full = (p.toolbarButtonOrder.filter { it in base || it.startsWith("QS:") } + base.filter { it !in p.toolbarButtonOrder }).toMutableList()
                                                        full.remove(tbKey)
                                                        val anchorKey = visBefore.getOrNull(desiredVisSlot)
                                                        val insertPos = if (anchorKey != null) {
                                                            full.indexOf(anchorKey).takeIf { it >= 0 } ?: full.size
                                                        } else {
                                                            full.size
                                                        }
                                                        full.add(insertPos.coerceIn(0, full.size), tbKey)
                                                        val updated = p.copy(toolbarButtonOrder = full)
                                                        when (tbKey) {
                                                            "MCP" -> updated.copy(showMcpBadgeButton = true)
                                                            "AI" -> updated.copy(showAiCommandButton = true)
                                                            "DARKMODE" -> updated.copy(showDarkModeButton = true)
                                                            "NEW_TAB" -> updated.copy(showNewTabToolbarButton = true)
                                                            "SHARE" -> updated.copy(showShareButton = true)
                                                            "FIND" -> updated.copy(showFindInPageButton = true)
                                                            "DESKTOP" -> updated.copy(showDesktopSiteButton = true)
                                                            "READER" -> updated.copy(showReaderModeButton = true)
                                                            "TRANSLATE" -> updated.copy(showTranslateButton = true)
                                                            else -> updated
                                                        }
                                                    }
                                                    val mut = latestEntries.toMutableList()
                                                    mut.removeAll { it is QsLayoutEntry.Tile && it.key == dk }
                                                    commit(mut, latestHidden + dk)
                                                }
                                            },
                                            onDragCancel = {
                                                draggingKey = null
                                                hoveredEmptySlotId = null
                                                latestOnTbSlotChanged(null)
                                            },
                                            onDrag = { change, dragAmount ->
                                                val dk = draggingKey ?: return@detectDragGesturesAfterLongPress
                                                change.consume()
                                                dragFingerLocal += dragAmount
                                                val rootPt = gridRootPos + dragFingerLocal
                                                dragFingerRoot = rootPt
                                                val tbRect = latestToolbarRect
                                                val inTbZone = dk != "zoom" && (
                                                    (tbRect.width > 1f && rootPt.y <= tbRect.bottom + tbHitPadPx) ||
                                                        dragFingerLocal.y < topEscapePx
                                                    )
                                                if (inTbZone) {
                                                    hoveredEmptySlotId = null
                                                    latestOnTbSlotChanged(calcTbSlot(rootPt))
                                                    return@detectDragGesturesAfterLongPress
                                                } else {
                                                    latestOnTbSlotChanged(null)
                                                }
                                                val overKey = tileRootBounds.entries.firstOrNull { (k, r) -> k != dk && r.contains(rootPt) }?.key
                                                val overEmpty = if (overKey == null) {
                                                    emptySlotBounds.entries.firstOrNull { (_, pair) -> pair.first.contains(rootPt) }
                                                } else null
                                                hoveredEmptySlotId = overEmpty?.key
                                                val nowMs = System.currentTimeMillis()
                                                if (nowMs - lastSwapMs < 220L) return@detectDragGesturesAfterLongPress
                                                if (overKey != null) {
                                                    val curList = latestEntries
                                                    val fromIdx = curList.indexOfFirst { it is QsLayoutEntry.Tile && it.key == dk }
                                                    val toIdx = curList.indexOfFirst { it is QsLayoutEntry.Tile && it.key == overKey }
                                                    if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                                                        lastSwapMs = nowMs
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val mut = curList.toMutableList()
                                                        val item = mut.removeAt(fromIdx)
                                                        mut.add(toIdx, item)
                                                        commit(mut)
                                                    }
                                                } else if (overEmpty != null) {
                                                    val curList = latestEntries
                                                    val fromIdx = curList.indexOfFirst { it is QsLayoutEntry.Tile && it.key == dk }
                                                    val targetEndIdx = overEmpty.value.second
                                                    if (fromIdx >= 0 && curList.isNotEmpty()) {
                                                        val rawInsert = if (fromIdx < targetEndIdx) targetEndIdx - 1 else targetEndIdx
                                                        val insertAt = rawInsert.coerceIn(0, curList.lastIndex)
                                                        if (insertAt != fromIdx) {
                                                            lastSwapMs = nowMs
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            val mut = curList.toMutableList()
                                                            val item = mut.removeAt(fromIdx)
                                                            mut.add(insertAt, item)
                                                            commit(mut)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var visualRowCursor = 1
                        blocks.forEach { block ->
                            when (block) {
                                is QsRenderBlock.DividerRow -> {
                                    val divRowIdx = (visualRowCursor++).coerceIn(1, 15)
                                    val divV = rowAnims[divRowIdx].value
                                    val divInv = 1f - divV
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex((28 - divRowIdx).coerceAtLeast(1).toFloat())
                                            .graphicsLayer {
                                                translationY = ((-34f - divRowIdx * 8f) * divInv * 1.50f).dp.toPx()
                                                scaleX = (0.55f + 0.45f * divV).coerceAtLeast(0.2f)
                                                alpha = (divV.coerceIn(0f, 1f) * 1.75f).coerceIn(0f, 1f)
                                            }
                                    ) {
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
                                }

                                is QsRenderBlock.Section -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        block.rows.forEachIndexed { rowIdx, rowSlots ->
                                            val curVisualRow = (visualRowCursor++).coerceIn(1, 15)
                                            val rowV = rowAnims[curVisualRow].value
                                            val rowInv = 1f - rowV
                                            // 확정 리퀴드 드롭 행별 모핑 (int=150%) + 내림차순 zIndex로 윗행이 항상 앞 레이어!
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp)
                                                    .zIndex((28 - curVisualRow).coerceAtLeast(1).toFloat())
                                                    .graphicsLayer {
                                                        translationX = ((14f + curVisualRow * 2.5f) * rowInv * 1.50f).dp.toPx()
                                                        translationY = ((-48f - curVisualRow * 10f) * rowInv * 1.50f).dp.toPx()
                                                        scaleX = (0.48f + 0.52f * rowV).coerceAtLeast(0.20f)
                                                        scaleY = (0.20f + 0.80f * rowV).coerceAtLeast(0.14f)
                                                        alpha = (rowV.coerceIn(0f, 1f) * 1.75f).coerceIn(0f, 1f)
                                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                                    },
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                var col = 0
                                                while (col < 4) {
                                                    val placed = rowSlots[col]
                                                    if (placed != null) {
                                                        val sp = placed.span.coerceIn(1, 4 - col)
                                                        val isThisLifted = editMode && draggingKey == placed.spec.key
                                                        key(placed.spec.key) {
                                                            val flipX = remember { Animatable(0f) }
                                                            val flipY = remember { Animatable(0f) }
                                                            QsMenuTile(
                                                                spec = placed.spec,
                                                                span = sp,
                                                                editMode = editMode,
                                                                isHovered = (!editMode && hoveredKey == placed.spec.key) || (editMode && extHoverTileKey == placed.spec.key),
                                                                isPlaceholder = isThisLifted,
                                                                isCompactZoomOpen = showCompactZoomSlider,
                                                                colWidthPx = colWidthPx,
                                                                panelLightRootPos = panelTouchRootPos,
                                                                panelLightAlpha = panelGlowAlpha,
                                                                modifier = Modifier
                                                                    .weight(sp.toFloat())
                                                                    .fillMaxHeight()
                                                                    .onGloballyPositioned { coords ->
                                                                        if (coords.isAttached) {
                                                                            val br = coords.boundsInRoot()
                                                                            onTileBoundsUpdated(placed.spec.key, br)
                                                                            tileRootBounds[placed.spec.key] = br
                                                                            // 2D FLIP 자리바꿈 스프링은 편집 모드에서만 동작 (열기/닫기 모프와 충돌 방지)
                                                                            if (editMode && p > 0.98f) {
                                                                                val curPos = Offset(br.left, br.top)
                                                                                val oldPos = prevTileRoots[placed.spec.key]
                                                                                if (oldPos != null && !isThisLifted) {
                                                                                    val dx = oldPos.x - curPos.x
                                                                                    val dy = oldPos.y - curPos.y
                                                                                    if (kotlin.math.abs(dx) > 1.5f || kotlin.math.abs(dy) > 1.5f) {
                                                                                        tileAnimScope.launch {
                                                                                            flipX.snapTo(dx)
                                                                                            flipX.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 230f))
                                                                                        }
                                                                                        tileAnimScope.launch {
                                                                                            flipY.snapTo(dy)
                                                                                            flipY.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 230f))
                                                                                        }
                                                                                    }
                                                                                }
                                                                                prevTileRoots[placed.spec.key] = curPos
                                                                            }
                                                                        }
                                                                    }
                                                                    .graphicsLayer {
                                                                        translationX = flipX.value
                                                                        translationY = flipY.value
                                                                    },
                                                                onClick = {
                                                                    if (placed.spec.zoomPercent != null && sp == 1) {
                                                                        showCompactZoomSlider = !showCompactZoomSlider
                                                                    } else if (placed.spec.zoomPercent == null) {
                                                                        if (placed.spec.key !in QS_KEEP_OPEN_KEYS && !placed.spec.isToggleCheckbox) {
                                                                            onDismiss()
                                                                        }
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
                                                        }
                                                        col += sp
                                                    } else {
                                                        if (editMode && rowIdx == block.rows.lastIndex) {
                                                            val slotId = "empty_${block.sectionId}_r${rowIdx}_c${col}"
                                                            QsEmptyPlaceholderSlot(
                                                                slotId = slotId,
                                                                targetEndIdx = block.endLayoutIndex,
                                                                isHovered = hoveredEmptySlotId == slotId || extHoverEmptyEntry?.key == slotId,
                                                                emptySlotBounds = emptySlotBounds,
                                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                                            )
                                                        } else {
                                                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                                                        }
                                                        col += 1
                                                    }
                                                }
                                            }
                                        }
                                        // 편집 모드 전용: 마지막 행이 덜 찼든 가득 찼든 다음 행(4칸) 빈 원형 공백 슬롯 생성 (편집 종료 시 자동 소멸)
                                        if (editMode) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                for (emptyCol in 0 until 4) {
                                                    val slotId = "empty_${block.sectionId}_next_c${emptyCol}"
                                                    QsEmptyPlaceholderSlot(
                                                        slotId = slotId,
                                                        targetEndIdx = block.endLayoutIndex,
                                                        isHovered = hoveredEmptySlotId == slotId || extHoverEmptyEntry?.key == slotId,
                                                        emptySlotBounds = emptySlotBounds,
                                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 맨 아래가 구분선으로 끝나는 경우에도 편집 모드 하단 공백 버튼 행 생성
                        if (editMode && (blocks.isEmpty() || blocks.lastOrNull() is QsRenderBlock.DividerRow)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (emptyCol in 0 until 4) {
                                    val slotId = "empty_tail_c${emptyCol}"
                                    QsEmptyPlaceholderSlot(
                                        slotId = slotId,
                                        targetEndIdx = parsedEntries.size,
                                        isHovered = hoveredEmptySlotId == slotId || extHoverEmptyEntry?.key == slotId,
                                        emptySlotBounds = emptySlotBounds,
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
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

                    // 1x1 확대 버튼 탭 시 하단에 부드럽게 펼쳐지는 전용 하모니 확대 조절바!
                    val zoomSpec = itemByKey["zoom"]
                    val zoomEntrySpan = (parsedEntries.firstOrNull { it is QsLayoutEntry.Tile && it.key == "zoom" } as? QsLayoutEntry.Tile)?.span ?: 4
                    if (zoomSpec != null && zoomEntrySpan == 1 && !editMode) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showCompactZoomSlider,
                            enter = expandVertically(spring(dampingRatio = 0.78f, stiffness = 320f)) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .zIndex(18f)
                        ) {
                            val prefsNow by SpaceManager.preferences.collectAsState()
                            val curZ = (zoomSpec.zoomPercent ?: 100).coerceIn(50, 150)
                            Surface(
                                shape = RoundedCornerShape(35.dp),
                                shadowElevation = 12.dp,
                                color = MaterialTheme.colorScheme.surfaceContainerLowest
                            ) {
                                HorizontalHarmonyCapsuleSlider(
                                    valueFraction = ((curZ - 50) / 100f).coerceIn(0f, 1f),
                                    valueLabel = "${curZ}%",
                                    icon = zoomSpec.icon,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    lightIntensity = prefsNow.progressLightIntensity,
                                    particleDensity = prefsNow.progressParticleDensity,
                                    darkUseThemeColor = prefsNow.progressDarkUseThemeColor,
                                    dayParticleColor = Color(prefsNow.lightModeParticleColorHex),
                                    interactive = true,
                                    iconCenterOffsetDp = 42.dp,
                                    onValueChange = { f ->
                                        val snapped = ((50f + f.coerceIn(0f, 1f) * 100f) / 5f).roundToInt() * 5
                                        onSetZoomPercent(snapped.coerceIn(50, 150))
                                    },
                                    onDoubleTapReset = { onSetZoomPercent(100) }
                                )
                            }
                        }
                    }

                    }
                }
            }
        }
    }

    // 루트 오버레이 최상단 플로팅 고스트 (Surface 바깥이므로 빠른메뉴 상단을 넘어 주소창 툴바 위까지 절대 잘리지 않음!)
    val activeDragKey = draggingKey
    if (editMode && activeDragKey != null) {
        val dragSpec = itemByKey[activeDragKey]
        val dragSpan = (parsedEntries.firstOrNull { it is QsLayoutEntry.Tile && it.key == activeDragKey } as? QsLayoutEntry.Tile)?.span ?: 1
        val tbRectNow = toolbarIconsRect
        val goingToToolbar = activeDragKey != "zoom" && (
            (tbRectNow.width > 1f && dragFingerRoot.y <= tbRectNow.bottom + with(density) { 18.dp.toPx() }) ||
                dragFingerRoot.y < overlayRootPos.y + with(density) { topPadding.toPx() }
            )
        if (dragSpec != null) {
            val origW = with(density) { dragTileSizePx.width.toDp() }.coerceAtLeast(70.dp)
            val origH = with(density) { dragTileSizePx.height.toDp() }.coerceAtLeast(70.dp)
            val ghostW by animateDpAsState(
                targetValue = if (goingToToolbar) 40.dp else origW,
                animationSpec = spring(dampingRatio = 0.76f, stiffness = 320f),
                label = "dragGhostW"
            )
            val ghostH by animateDpAsState(
                targetValue = if (goingToToolbar) 40.dp else origH,
                animationSpec = spring(dampingRatio = 0.76f, stiffness = 320f),
                label = "dragGhostH"
            )
            val colWidthPxRoot = with(density) { 84.dp.toPx() }
            val fingerInOverlay = dragFingerRoot - overlayRootPos
            Box(
                modifier = Modifier
                    .offset {
                        val ox = if (goingToToolbar) (fingerInOverlay.x - 20.dp.toPx()) else (fingerInOverlay.x - dragGrabOffset.x)
                        val oy = if (goingToToolbar) (fingerInOverlay.y - 20.dp.toPx()) else (fingerInOverlay.y - dragGrabOffset.y)
                        IntOffset(ox.roundToInt(), oy.roundToInt())
                    }
                    .width(ghostW)
                    .height(ghostH)
                    .zIndex(98f)
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                        shadowElevation = 18.dp.toPx()
                    }
            ) {
                if (goingToToolbar) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        EgoIcon(
                            imageVector = dragSpec.icon,
                            contentDescription = dragSpec.title,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            symbolSize = 20.dp
                        )
                    }
                } else {
                    QsMenuTile(
                        spec = dragSpec,
                        span = dragSpan,
                        editMode = false,
                        isHovered = true,
                        isPlaceholder = false,
                        isCompactZoomOpen = false,
                        colWidthPx = colWidthPxRoot,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {},
                        onSetZoomPercent = {},
                        onResizeSpan = {},
                        onMoveDelta = {},
                        onHide = {}
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun QsEmptyPlaceholderSlot(
    slotId: String,
    targetEndIdx: Int,
    isHovered: Boolean,
    emptySlotBounds: MutableMap<String, Pair<Rect, Int>>,
    modifier: Modifier = Modifier
) {
    DisposableEffect(slotId) {
        onDispose { emptySlotBounds.remove(slotId) }
    }
    val fillAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.72f else 0.36f,
        animationSpec = tween(140),
        label = "emptySlotFillAlpha"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.75f else 0.14f,
        animationSpec = tween(140),
        label = "emptySlotRingAlpha"
    )
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    emptySlotBounds[slotId] = coords.boundsInRoot() to targetEndIdx
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = fillAlpha))
                .border(
                    width = if (isHovered) 1.5.dp else 1.dp,
                    color = if (isHovered) {
                        MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = ringAlpha)
                    },
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun QsMenuTile(
    spec: ChromeMenuItemSpec,
    span: Int,
    editMode: Boolean,
    isHovered: Boolean,
    isPlaceholder: Boolean = false,
    isCompactZoomOpen: Boolean = false,
    colWidthPx: Float,
    panelLightRootPos: Offset = Offset.Zero,
    panelLightAlpha: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onSetZoomPercent: (Int) -> Unit = {},
    onResizeSpan: (Int) -> Unit,
    onMoveDelta: (Int) -> Unit,
    onHide: () -> Unit
) {
    val prefs by SpaceManager.preferences.collectAsState()
    val haptic = LocalHapticFeedback.current
    val isZoomTile = spec.zoomPercent != null
    val isZoomBar = isZoomTile && span >= 2
    val isActive = (spec.isToggleCheckbox && spec.checked) || spec.bold || (isZoomTile && span == 1 && isCompactZoomOpen)
    val isSingle = span == 1
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
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(220),
        label = "a17TileBg"
    )
    val iconFg by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.onPrimary
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

    val editControlsProgress by animateFloatAsState(
        targetValue = if (editMode) 1f else 0f,
        animationSpec = if (editMode) {
            spring(dampingRatio = 0.78f, stiffness = 320f)
        } else {
            tween(durationMillis = 180, easing = FastOutSlowInEasing)
        },
        label = "qsEditControlsProgress_${spec.key}"
    )
    val showEditControls = editMode || editControlsProgress > 0.015f

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
    } else 0.78f

    if (isPlaceholder) {
        val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        Box(
            modifier = modifier
                .height(70.dp)
                .clip(tileShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(35.dp.toPx(), 35.dp.toPx()),
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }
        }
        return
    }

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
        var zoomRootPos by remember { mutableStateOf(Offset.Zero) }
        val zoomRimIntensity = LocalRimGlowIntensity.current.coerceIn(0.4f, 2.6f)
        val zoomRimColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = modifier
                .height(70.dp)
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) zoomRootPos = coords.positionInRoot()
                }
                .zIndex(if (zoomDraggingRow) 20f else 0f)
                .graphicsLayer {
                    scaleX = zoomLiftScale
                    scaleY = zoomLiftScale
                    translationY = if (zoomDraggingRow) zoomLiftY else 0f
                }
                .then(
                    if (showEditControls) {
                        val ep = editControlsProgress.coerceIn(0f, 1f)
                        Modifier
                            .border(
                                width = if (zoomDraggingRow) 2.dp else 1.dp,
                                color = if (zoomDraggingRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f * ep),
                                shape = RoundedCornerShape(35.dp)
                            )
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
                modifier = Modifier
                    .align(Alignment.Center)
                    .drawWithContent {
                        drawContent()
                        if (panelLightAlpha > 0.01f && prefs.rimGlowEnabled) {
                            drawUnifiedOpticalRim(
                                shape = RoundedCornerShape(35.dp),
                                lightCenter = panelLightRootPos - zoomRootPos,
                                glowColor = zoomRimColor,
                                lightAlpha = panelLightAlpha,
                                intensity = zoomRimIntensity
                            )
                        }
                    }
            )
            if (showEditControls) {
                val ep = editControlsProgress.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(22.dp)
                        .graphicsLayer {
                            alpha = ep
                            scaleX = 0.45f + 0.55f * ep
                            scaleY = 0.45f + 0.55f * ep
                            transformOrigin = TransformOrigin(0.25f, 0.25f)
                        }
                        .clip(CircleShape)
                        .background(EgoDangerRed)
                        .clickable(enabled = editMode, onClick = onHide),
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
                var zResizeAccumX by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .graphicsLayer {
                            alpha = ep
                            scaleX = 0.65f + 0.35f * ep
                            scaleY = 0.65f + 0.35f * ep
                            transformOrigin = TransformOrigin(0.85f, 0.85f)
                        }
                        .pointerInput("resize_zoom_${spec.key}") {
                            detectHorizontalDragGestures(
                                onDragStart = { zResizeAccumX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    zResizeAccumX += dragAmount
                                    val threshold = (colWidthPx * 0.58f).coerceAtLeast(36f)
                                    val deltaCols = (zResizeAccumX / threshold).roundToInt()
                                    if (deltaCols != 0) {
                                        val next = (latestSpan + deltaCols).coerceIn(1, 4)
                                        if (next != latestSpan) {
                                            zResizeAccumX = 0f
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
                        val arcSize = size.minDimension * 1.85f
                        drawArc(
                            color = Color.White.copy(alpha = 0.78f),
                            startAngle = 8f,
                            sweepAngle = 74f,
                            useCenter = false,
                            topLeft = Offset(size.width - arcSize, size.height - arcSize),
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
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
if (!editMode) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        var tileBodyRootPos by remember { mutableStateOf(Offset.Zero) }
        val tileRimIntensity = LocalRimGlowIntensity.current.coerceIn(0.4f, 2.6f)
        val tileRimColor = MaterialTheme.colorScheme.primary
        // 내부 클립된 타일 본체: 패널 광감 번짐보다 앞 레이어에 위치하며, 패널·확대바와 100% 동일한 두께/밝기 테두리로 반응!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) tileBodyRootPos = coords.positionInRoot()
                }
                .clip(tileShape)
                .background(containerBg)
                .drawWithContent {
                    drawContent()
                    if (panelLightAlpha > 0.01f && prefs.rimGlowEnabled) {
                        drawUnifiedOpticalRim(
                            shape = tileShape,
                            lightCenter = panelLightRootPos - tileBodyRootPos,
                            glowColor = tileRimColor,
                            lightAlpha = panelLightAlpha,
                            intensity = tileRimIntensity
                        )
                    }
                }
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
                    targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "wideBadgeBg"
                )
                val badgeFg by animateColorAsState(
                    targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary else (spec.accentColor ?: MaterialTheme.colorScheme.onSurface),
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
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (spec.isToggleCheckbox) {
                            Text(
                                text = if (spec.checked) "켜짐" else "꺼짐",
                                fontSize = 11.sp,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 편집 모드 오버레이 (외곽 Box 기준이라 절대 잘리지 않으며 부드러운 스프링 페이드/스케일로 등장·퇴장)
        if (showEditControls) {
            val ep = editControlsProgress.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(22.dp)
                    .graphicsLayer {
                        alpha = ep
                        scaleX = 0.45f + 0.55f * ep
                        scaleY = 0.45f + 0.55f * ep
                        transformOrigin = TransformOrigin(0.25f, 0.25f)
                    }
                    .clip(CircleShape)
                    .background(EgoDangerRed)
                    .clickable(enabled = editMode, onClick = onHide),
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

            // 우하단 둘레 밀착 두꺼운 호흡 크기조절 핸들 (원형/사각 둘레 자동 맞춤, 1x1~1x4, 부드러운 등장·퇴장)
            val handleTint = if (isActive) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = handleBreathAlpha)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = handleBreathAlpha)
            }
            var resizeAccumX by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .graphicsLayer {
                        alpha = ep
                        scaleX = 0.65f + 0.35f * ep
                        scaleY = 0.65f + 0.35f * ep
                        transformOrigin = TransformOrigin(0.85f, 0.85f)
                    }
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

@Composable
fun ToolbarToQuickMenuDragGhost(
    tbKey: String,
    rootPos: Offset,
    inQuickMenuZone: Boolean,
    chromeMenuItems: List<ChromeMenuItemSpec> = emptyList()
) {
    val dynIcon = if (tbKey.startsWith("QS:")) {
        chromeMenuItems.firstOrNull { it.key == tbKey.removePrefix("QS:") }?.icon
    } else null
    val icon = dynIcon ?: when (tbKey) {
        "MCP" -> Icons.Default.LinkOff
        "AI" -> Icons.Default.AutoAwesome
        "ADBLOCK" -> Icons.Default.Shield
        "TRANSLATE" -> Icons.Default.Translate
        "DARKMODE" -> Icons.Default.DarkMode
        "NEW_TAB" -> Icons.Default.Add
        "SHARE" -> Icons.Default.Share
        "FIND" -> Icons.Default.FindInPage
        "DESKTOP" -> Icons.Default.DesktopWindows
        "READER" -> Icons.Default.MenuBook
        "PROFILE" -> Icons.Default.Person
        else -> Icons.Default.Add
    }
    val ghostSize by animateDpAsState(
        targetValue = if (inQuickMenuZone) 70.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 320f),
        label = "tbDragGhostSize"
    )
    val bgColor by animateColorAsState(
        targetValue = if (inQuickMenuZone) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
        animationSpec = tween(160),
        label = "tbDragGhostBg"
    )
    val fgColor by animateColorAsState(
        targetValue = if (inQuickMenuZone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(160),
        label = "tbDragGhostFg"
    )
    val density = LocalDensity.current
    val halfPx = with(density) { (ghostSize / 2).toPx() }
    Box(
        modifier = Modifier
            .offset {
                IntOffset((rootPos.x - halfPx).roundToInt(), (rootPos.y - halfPx).roundToInt())
            }
            .size(ghostSize)
            .zIndex(95f)
            .graphicsLayer {
                scaleX = 1.06f
                scaleY = 1.06f
                shadowElevation = 18.dp.toPx()
            }
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        EgoIcon(
            imageVector = icon,
            contentDescription = tbKey,
            tint = fgColor,
            modifier = Modifier.size(if (inQuickMenuZone) 22.dp else 20.dp),
            symbolSize = if (inQuickMenuZone) 22.dp else 20.dp
        )
    }
}

@Composable
fun ToolbarEditDragCaptureOverlay(
    tbIconsRootPos: Offset,
    qsDropState: QsExternalDropState,
    modifier: Modifier = Modifier,
    onDraggingChanged: (key: String?, rootPos: Offset, inQsZone: Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val latestRootPos by rememberUpdatedState(tbIconsRootPos)
    val latestOnChanged by rememberUpdatedState(onDraggingChanged)
    Box(
        modifier = modifier
            .pointerInput("real_toolbar_edit_drag_overlay") {
                val downThresholdPx = 38.dp.toPx()
                val stepXPx = 34.dp.toPx()
                var activeTbKey: String? = null
                var accumX = 0f
                var accumY = 0f
                var fingerLocal = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { startPos ->
                        val enabled = getVisibleToolbarKeys(SpaceManager.preferences.value)
                        if (enabled.isNotEmpty() && size.width > 0) {
                            val slotW = size.width.toFloat() / enabled.size.toFloat()
                            val idx = (startPos.x / slotW).toInt().coerceIn(0, enabled.lastIndex)
                            val picked = enabled[idx]
                            activeTbKey = picked
                            accumX = 0f
                            accumY = 0f
                            fingerLocal = startPos
                            latestOnChanged(picked, latestRootPos + startPos, false)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDragEnd = {
                        val k = activeTbKey
                        val droppedInQs = accumY > downThresholdPx
                        val targetIdx = qsDropState.targetInsertIndex
                        activeTbKey = null
                        qsDropState.targetInsertIndex = null
                        latestOnChanged(null, Offset.Zero, false)
                        if (k != null && (droppedInQs || targetIdx != null)) {
                            val qsKey = resolveQsKeyFromTb(k)
                            if (qsKey != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                SpaceManager.updatePreferences { p ->
                                    val (baseEntries, baseHidden) = parseLayoutTokens(p.quickMenuLayout)
                                    val mut = baseEntries.toMutableList()
                                    val oldPos = mut.indexOfFirst { it is QsLayoutEntry.Tile && it.key == qsKey }
                                    if (oldPos >= 0) mut.removeAt(oldPos)
                                    val rawIdx = targetIdx ?: mut.size
                                    val adjIdx = if (oldPos in 0 until rawIdx) rawIdx - 1 else rawIdx
                                    mut.add(adjIdx.coerceIn(0, mut.size), QsLayoutEntry.Tile(qsKey, 1))
                                    val newLayout = serializeLayoutTokens(mut, baseHidden - qsKey)
                                    val updated = p.copy(quickMenuLayout = newLayout)
                                    if (k.startsWith("QS:")) {
                                        updated.copy(toolbarButtonOrder = p.toolbarButtonOrder.filter { it != k })
                                    } else {
                                        when (k) {
                                            "MCP" -> updated.copy(showMcpBadgeButton = false)
                                            "TRANSLATE" -> updated.copy(showTranslateButton = false)
                                            "DARKMODE" -> updated.copy(showDarkModeButton = false)
                                            "NEW_TAB" -> updated.copy(showNewTabToolbarButton = false)
                                            "SHARE" -> updated.copy(showShareButton = false)
                                            "FIND" -> updated.copy(showFindInPageButton = false)
                                            "DESKTOP" -> updated.copy(showDesktopSiteButton = false)
                                            "READER" -> updated.copy(showReaderModeButton = false)
                                            else -> updated
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        activeTbKey = null
                        qsDropState.targetInsertIndex = null
                        latestOnChanged(null, Offset.Zero, false)
                    },
                    onDrag = { change, dragAmount ->
                        val k = activeTbKey ?: return@detectDragGesturesAfterLongPress
                        change.consume()
                        accumX += dragAmount.x
                        accumY += dragAmount.y
                        fingerLocal += dragAmount
                        val inQs = accumY > downThresholdPx
                        latestOnChanged(k, latestRootPos + fingerLocal, inQs)
                        if (!inQs) {
                            val dir = when {
                                accumX >= stepXPx -> 1
                                accumX <= -stepXPx -> -1
                                else -> 0
                            }
                            if (dir != 0) {
                                accumX = 0f
                                val p = SpaceManager.preferences.value
                                val base = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
                                val full = (p.toolbarButtonOrder.filter { it in base || it.startsWith("QS:") } + base.filter { it !in p.toolbarButtonOrder }).toMutableList()
                                val fi = full.indexOf(k)
                                val ti = (fi + dir).coerceIn(0, full.lastIndex)
                                if (fi >= 0 && ti != fi) {
                                    full.removeAt(fi)
                                    full.add(ti, k)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    SpaceManager.updatePreferences { it.copy(toolbarButtonOrder = full) }
                                }
                            }
                        }
                    }
                )
            }
    )
}

@Composable
fun ToolbarDynamicQsIconButton(
    qsKey: String,
    chromeMenuItems: List<ChromeMenuItemSpec>,
    activeIconColor: Color
) {
    val spec = chromeMenuItems.firstOrNull { it.key == qsKey } ?: return
    val tint = when {
        spec.isToggleCheckbox && spec.checked -> MaterialTheme.colorScheme.primary
        spec.accentColor != null -> spec.accentColor
        else -> activeIconColor
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .pressBounceScale(0.86f)
            .clip(CircleShape)
            .clickable { spec.onClick() },
        contentAlignment = Alignment.Center
    ) {
        EgoIcon(
            imageVector = spec.icon,
            contentDescription = spec.title,
            tint = tint,
            modifier = Modifier.size(20.dp),
            symbolSize = 20.dp
        )
    }
}

@Composable
fun ToolbarInsertSlotIndicator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(3.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}
