package com.egolite.mobile.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.egolite.mobile.model.CardVanishStyle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.BrowserTab
import com.egolite.mobile.model.SpaceType

/**
 * 크롬 태블릿 1:1 탭 모아보기 + Personal / AI Space 멀티 그룹 통합 관리 화면
 * - 우측 상단 ⋮ 메뉴 버튼: 크롬 1:1 Drag-to-Open / Drag-to-Select 완벽 지원
 * - 탭 카드 길게 누르기: 드래그하여 탭 순서 자유롭게 이동 (Personal + AI Space 모두 지원)
 */
@Composable
fun ChromeTabSwitcherOverlay(
    spaces: List<BrowserSpace>,
    activeSpace: BrowserSpace,
    darkTheme: Boolean,
    canAddAiSpace: Boolean,
    onSelectSpaceAndTab: (spaceId: String, tabId: String) -> Unit,
    onCloseTab: (spaceId: String, tabId: String) -> Unit,
    onMoveTab: (spaceId: String, fromTabId: String, toTabId: String) -> Unit,
    onCreateTab: (spaceId: String, incognito: Boolean) -> Unit,
    onCreateAiSpace: () -> Unit,
    onCloseAiSpace: (spaceId: String) -> Unit,
    onCloseAllTabs: (spaceId: String) -> Unit,
    onClearBrowsingData: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val personalSpace = remember(spaces) { spaces.firstOrNull { it.type == SpaceType.PERSONAL } ?: activeSpace }
    val aiSpaces = remember(spaces) { spaces.filter { it.type == SpaceType.AI } }

    // 0: Personal 탭 그룹, 1: AI Space 탭 그룹
    var selectedSection by remember {
        mutableIntStateOf(if (activeSpace.type == SpaceType.AI) 1 else 0)
    }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var hoveredMenuIndex by remember { mutableIntStateOf(-1) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedTabIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val prefs by SpaceManager.preferences.collectAsState()
    val vanishingTabIds = remember { mutableStateListOf<String>() }
    val activeTabVanishBursts = remember { mutableStateListOf<DustVanishRequest>() }
    val pendingTabCloseByBurstId = remember { mutableMapOf<Long, Pair<String, String>>() }
    val tabCardRootRects = remember { mutableMapOf<String, Rect>() }
    var overlayOriginRoot by remember { mutableStateOf(Offset.Zero) }


    val allTabsKey = remember(spaces) { spaces.flatMap { s -> s.tabs.map { "${s.id}:${it.id}" } } }
    val thumbnails: Map<String, Bitmap?> = remember(allTabsKey) {
        spaces.flatMap { it.tabs }.associate { tab ->
            tab.id to if (tab.url == NEW_TAB_URL) null else SpaceManager.captureTabThumbnail(tab.id)
        }
    }

    fun triggerTabVanishClose(spaceId: String, tabId: String, accentColor: Color) {
        if (tabId in vanishingTabIds) return
        val style = prefs.cardVanishStyle
        val rootRect = tabCardRootRects[tabId]
        if (style == CardVanishStyle.OFF || rootRect == null || rootRect.width <= 10f || rootRect.height <= 10f) {
            onCloseTab(spaceId, tabId)
            return
        }
        val localRect = Rect(
            left = rootRect.left - overlayOriginRoot.x,
            top = rootRect.top - overlayOriginRoot.y,
            right = rootRect.right - overlayOriginRoot.x,
            bottom = rootRect.bottom - overlayOriginRoot.y
        )
        vanishingTabIds.add(tabId)
        val burstId = System.nanoTime()
        pendingTabCloseByBurstId[burstId] = spaceId to tabId
        activeTabVanishBursts.add(
            DustVanishRequest(
                id = burstId,
                rect = localRect,
                accentColor = accentColor,
                thumbnail = thumbnails[tabId],
                style = style,
                particleCount = prefs.vanishParticleCount,
                durationMs = prefs.vanishDurationMs
            )
        )
    }

    fun finishTabVanishBurst(burstId: Long) {
        activeTabVanishBursts.removeAll { it.id == burstId }
        val target = pendingTabCloseByBurstId.remove(burstId)
        if (target != null) {
            vanishingTabIds.remove(target.second)
            onCloseTab(target.first, target.second)
        }
    }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // 전체 공간의 열린 탭 썸네일 캡처

    // 탭 스위처 ⋮ 메뉴 항목 (Drag-to-Select 공용 오버레이)
    val switcherMenuItems = remember(personalSpace.id, activeSpace.id, selectedSection) {
        listOf(
            ChromeMenuItemSpec("새 탭", Icons.Default.AddBox) {
                onCreateTab(personalSpace.id, false)
            },
            ChromeMenuItemSpec("새 창 (AI Space)", Icons.Default.SmartToy, accentColor = EgoEmerald) {
                onCreateAiSpace()
            },
            ChromeMenuItemSpec("새 시크릿 창", Icons.Default.VisibilityOff) {
                onCreateTab(personalSpace.id, true)
            },
            ChromeMenuItemSpec("새 AI 탭 그룹", Icons.Default.GridView, dividerAfter = true) {
                onCreateAiSpace()
                selectedSection = 1
            },
            ChromeMenuItemSpec("탭 모두 닫기", Icons.Default.Close) {
                onCloseAllTabs(if (selectedSection == 0) personalSpace.id else activeSpace.id)
            },
            ChromeMenuItemSpec("탭 선택", Icons.Default.CheckBox, dividerAfter = true) {
                selectedSection = 0
                selectionMode = true
                selectedTabIds = emptySet()
            },
            ChromeMenuItemSpec("인터넷 사용 기록 삭제", Icons.Default.DeleteOutline) {
                onClearBrowsingData()
            },
            ChromeMenuItemSpec("설정", Icons.Default.Settings) {
                onOpenSettings()
            }
        )
    }

    // 크롬 태블릿 1:1 다크/라이트 팔레트
    val bgColor = if (darkTheme) Color(0xFF141518) else Color(0xFFF1F3F4)
    val topBarBg = if (darkTheme) Color(0xFF141518) else Color(0xFFF1F3F4)
    val pillTrackBg = if (darkTheme) Color(0xFF23252A) else Color(0xFFE0E3E7)
    val pillActiveBg = if (darkTheme) Color(0xFF393D47) else Color(0xFFFFFFFF)
    val addBtnBg = if (darkTheme) Color(0xFFC2D1F0) else Color(0xFFD3E3FD)
    val addBtnTint = if (darkTheme) Color(0xFF182236) else Color(0xFF041E49)
    val activeCardBorder = if (darkTheme) Color(0xFFA8C7FA) else Color(0xFF1A73E8)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val b = coords.boundsInRoot()
                overlayOriginRoot = Offset(b.left, b.top)
            },
        color = bgColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 크롬 태블릿 1:1 상단 툴바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(topBarBg)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (selectionMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                selectionMode = false
                                selectedTabIds = emptySet()
                            }) {
                                EgoIcon(Icons.Default.Close, contentDescription = "선택 취소")
                            }
                            Text(
                                text = "${selectedTabIds.size}개 선택됨",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                enabled = selectedTabIds.isNotEmpty(),
                                onClick = {
                                    val targetSpaceId = if (selectedSection == 0) personalSpace.id else activeSpace.id
                                    selectedTabIds.forEach { tid -> onCloseTab(targetSpaceId, tid) }
                                    selectedTabIds = emptySet()
                                    selectionMode = false
                                }
                            ) {
                                Text("선택 탭 닫기", color = EgoDangerRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // 좌측: 크롬 1:1 둥근 사각 + 새 탭 버튼
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(addBtnBg)
                                .clickable {
                                    if (selectedSection == 0) {
                                        onCreateTab(personalSpace.id, false)
                                    } else if (canAddAiSpace) {
                                        onCreateAiSpace()
                                    } else {
                                        aiSpaces.firstOrNull()?.let { onCreateTab(it.id, false) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (selectedSection == 0) "새 탭" else "새 AI Space",
                                tint = addBtnTint,
                                modifier = Modifier.size(22.dp)
                            , symbolSize = 22.dp)
                        }

                        // 중앙: Personal 탭 그룹 | AI Space 탭 그룹 세그먼트 캡슐
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(pillTrackBg)
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Personal 탭 버튼
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (selectedSection == 0) pillActiveBg else Color.Transparent)
                                    .clickable { selectedSection = 0 }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(
                                            1.6.dp,
                                            if (selectedSection == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            RoundedCornerShape(5.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${personalSpace.tabs.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedSection == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Personal",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSection == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // AI Space 탭 그룹 버튼
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (selectedSection == 1) pillActiveBg else Color.Transparent)
                                    .clickable { selectedSection = 1 }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EgoIcon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "AI Space 탭 그룹",
                                    tint = if (selectedSection == 1) EgoEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                , symbolSize = 17.dp)
                                Text(
                                    text = "AI Space (${aiSpaces.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSection == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 우측: 검색 · 더보기(⋮ Drag-to-Select) · 닫기(✕)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            }) {
                                EgoIcon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "탭 검색",
                                    tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val menuItemHeightPx = with(density) { 44.dp.toPx() }
                            val menuDragStartPx = with(density) { 26.dp.toPx() }
                            val latestShowMenu by rememberUpdatedState(showMoreMenu)
                            val latestMenuItems by rememberUpdatedState(switcherMenuItems)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val wasAlreadyOpen = latestShowMenu
                                            if (!wasAlreadyOpen) {
                                                showMoreMenu = true
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            var dragged = false
                                            var lastHover = -1
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                val itemsNow = latestMenuItems
                                                if (!change.pressed) {
                                                    if (dragged && lastHover in itemsNow.indices) {
                                                        val chosen = itemsNow[lastHover]
                                                        hoveredMenuIndex = -1
                                                        showMoreMenu = false
                                                        chosen.onClick()
                                                    } else if (dragged || wasAlreadyOpen) {
                                                        showMoreMenu = false
                                                        hoveredMenuIndex = -1
                                                    } else {
                                                        hoveredMenuIndex = -1
                                                    }
                                                    break
                                                }
                                                val dy = change.position.y - down.position.y
                                                val dx = change.position.x - down.position.x
                                                val menuWidthPx = with(density) { 264.dp.toPx() }
                                                val totalMenuHeightPx = itemsNow.size * menuItemHeightPx
                                                if (kotlin.math.abs(dy) > menuDragStartPx * 0.5f || kotlin.math.abs(dx) > menuDragStartPx * 0.5f) {
                                                    dragged = true
                                                }
                                                val inMenuBounds = dy > menuDragStartPx &&
                                                    dy <= (menuDragStartPx + totalMenuHeightPx) &&
                                                    dx >= -menuWidthPx &&
                                                    dx <= with(density) { 48.dp.toPx() }
                                                if (inMenuBounds) {
                                                    val idx = ((dy - menuDragStartPx) / menuItemHeightPx).toInt()
                                                    if (idx in itemsNow.indices && idx != lastHover) {
                                                        lastHover = idx
                                                        hoveredMenuIndex = idx
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                } else if (lastHover != -1) {
                                                    lastHover = -1
                                                    hoveredMenuIndex = -1
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                EgoIcon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(onClick = onDismiss) {
                                EgoIcon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "닫기",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("열린 탭 제목 또는 URL 검색...", fontSize = 13.sp) },
                        leadingIcon = { EgoIcon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), symbolSize = 18.dp) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    EgoIcon(Icons.Default.Close, contentDescription = "지우기", modifier = Modifier.size(16.dp), symbolSize = 16.dp)
                                }
                            }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // 2. 본문 영역: Personal 4열 그리드 (길게 눌러 위치 이동) vs AI Space 멀티 그룹 (길게 눌러 위치 이동)
                if (selectedSection == 0) {
                    val q = searchQuery.trim().lowercase()
                    val visibleTabs = remember(personalSpace.tabs, q) {
                        if (q.isBlank()) personalSpace.tabs
                        else personalSpace.tabs.filter {
                            it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
                        }
                    }
                    val gridState = rememberLazyGridState()
                    var draggingTabId by remember { mutableStateOf<String?>(null) }
                    var dragOffset by remember { mutableStateOf(Offset.Zero) }
                    val latestCanDrag by rememberUpdatedState(!selectionMode && q.isBlank() && visibleTabs.size > 1)
                    val latestPersonalId by rememberUpdatedState(personalSpace.id)

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { startOffset ->
                                        if (!latestCanDrag) return@detectDragGesturesAfterLongPress
                                        val hit = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                            val x = startOffset.x
                                            val y = startOffset.y
                                            x >= item.offset.x && x <= item.offset.x + item.size.width &&
                                                y >= item.offset.y && y <= item.offset.y + item.size.height
                                        }
                                        val key = hit?.key as? String
                                        if (key != null) {
                                            draggingTabId = key
                                            dragOffset = Offset.Zero
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        val currentId = draggingTabId ?: return@detectDragGesturesAfterLongPress
                                        change.consume()
                                        dragOffset += dragAmount
                                        val visibleItems = gridState.layoutInfo.visibleItemsInfo
                                        val draggedInfo = visibleItems.firstOrNull { it.key == currentId } ?: return@detectDragGesturesAfterLongPress
                                        val centerX = draggedInfo.offset.x + draggedInfo.size.width / 2f + dragOffset.x
                                        val centerY = draggedInfo.offset.y + draggedInfo.size.height / 2f + dragOffset.y
                                        val targetInfo = visibleItems.firstOrNull { item ->
                                            item.key != currentId &&
                                                centerX >= item.offset.x && centerX <= item.offset.x + item.size.width &&
                                                centerY >= item.offset.y && centerY <= item.offset.y + item.size.height
                                        }
                                        val targetKey = targetInfo?.key as? String
                                        if (targetInfo != null && targetKey != null) {
                                            val dxShift = (draggedInfo.offset.x - targetInfo.offset.x).toFloat()
                                            val dyShift = (draggedInfo.offset.y - targetInfo.offset.y).toFloat()
                                            onMoveTab(latestPersonalId, currentId, targetKey)
                                            dragOffset = Offset(dragOffset.x + dxShift, dragOffset.y + dyShift)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                    onDragEnd = {
                                        draggingTabId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingTabId = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                    ) {
                        items(visibleTabs, key = { it.id }) { tab ->
                            val isCurrent = activeSpace.id == personalSpace.id && tab.id == personalSpace.activeTabId
                            val isPicked = tab.id in selectedTabIds
                            val isDragging = draggingTabId == tab.id
                            val cardScale by animateFloatAsState(
                                targetValue = if (isDragging) 1.06f else 1f,
                                animationSpec = EgoMotion.bouncySpring(),
                                label = "tabDragScale"
                            )
                            ChromeTabThumbnailCard(
                                tab = tab,
                                thumbnail = thumbnails[tab.id],
                                isActive = isCurrent,
                                isDragging = isDragging,
                                accentBorder = activeCardBorder,
                                darkTheme = darkTheme,
                                selectionMode = selectionMode,
                                isSelectedForBatch = isPicked,
                                modifier = Modifier
                                    .animateItem()
                                    .onGloballyPositioned { coords ->
                                        tabCardRootRects[tab.id] = coords.boundsInRoot()
                                    }
                                    .zIndex(if (isDragging) 12f else 0f)
                                    .graphicsLayer {
                                        if (isDragging) {
                                            translationX = dragOffset.x
                                            translationY = dragOffset.y
                                        }
                                        scaleX = cardScale
                                        scaleY = cardScale
                                        alpha = when {
                                            tab.id in vanishingTabIds -> 0f
                                            isDragging -> 0.94f
                                            else -> 1f
                                        }
                                    },
                                onClick = {
                                    if (selectionMode) {
                                        selectedTabIds = if (isPicked) selectedTabIds - tab.id else selectedTabIds + tab.id
                                    } else {
                                        onSelectSpaceAndTab(personalSpace.id, tab.id)
                                    }
                                },
                                onClose = { triggerTabVanishClose(personalSpace.id, tab.id, activeCardBorder) }
                            )
                        }
                    }
                } else {
                    // AI Space 탭 그룹: 여러 개의 AI Space를 공간별 그룹 섹션으로 모두 표시 + 길게 눌러 탭 이동
                    if (aiSpaces.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                EgoIcon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(56.dp)
                                , symbolSize = 56.dp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "여기에서 AI Space 탭 그룹을 확인할 수 있습니다",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "AI Space를 생성하면 공간별 탭이 자동으로 그룹화됩니다.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = EgoEmerald.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .border(1.dp, EgoEmerald, CircleShape)
                                        .clip(CircleShape)
                                        .clickable { onCreateAiSpace() }
                                ) {
                                    Text(
                                        text = "+ 새 AI Space 만들기",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EgoEmerald,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val q = searchQuery.trim().lowercase()
                        val aiCardBounds = remember { mutableStateMapOf<String, Rect>() }
                        var aiDraggingSpaceId by remember { mutableStateOf<String?>(null) }
                        var aiDraggingTabId by remember { mutableStateOf<String?>(null) }
                        var aiDragOffset by remember { mutableStateOf(Offset.Zero) }

                        LazyColumn(
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            aiSpaces.forEach { aiSpace ->
                                val spaceAccent = parseSpaceColor(aiSpace.accentColorHex)
                                val filteredTabs = if (q.isBlank()) aiSpace.tabs else aiSpace.tabs.filter {
                                    it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
                                }
                                val isWorking = aiSpace.aiStatus == AiSpaceStatus.WORKING || aiSpace.aiStatus == AiSpaceStatus.THINKING

                                item(key = "hdr_${aiSpace.id}") {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // AI Space 그룹 헤더
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(spaceAccent.copy(alpha = if (darkTheme) 0.16f else 0.10f))
                                                .border(1.dp, spaceAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(11.dp)
                                                    .clip(CircleShape)
                                                    .background(spaceAccent)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${aiSpace.name} (${aiSpace.tabs.size})",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isWorking) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = aiSpace.lastActionSummary.ifBlank { "작업 중..." },
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = spaceAccent,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                            TextButton(
                                                onClick = { onCreateTab(aiSpace.id, false) }
                                            ) {
                                                EgoIcon(Icons.Default.Add, contentDescription = null, tint = spaceAccent, modifier = Modifier.size(16.dp), symbolSize = 16.dp)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("새 탭", fontSize = 12.sp, color = spaceAccent, fontWeight = FontWeight.Bold)
                                            }
                                            if (aiSpaces.size > 1) {
                                                IconButton(
                                                    onClick = { onCloseAiSpace(aiSpace.id) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    EgoIcon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "AI Space 닫기",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(17.dp)
                                                    , symbolSize = 17.dp)
                                                }
                                            }
                                        }

                                        // 해당 AI Space의 탭 카드 4열 행 (길게 눌러 위치 이동 지원)
                                        filteredTabs.chunked(4).forEach { rowTabs ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowTabs.forEach { tab ->
                                                    val isCurrent = activeSpace.id == aiSpace.id && tab.id == aiSpace.activeTabId
                                                    val isDragging = aiDraggingSpaceId == aiSpace.id && aiDraggingTabId == tab.id
                                                    val latestCanMove by rememberUpdatedState(q.isBlank() && aiSpace.tabs.size > 1)
                                                    val latestTabIds by rememberUpdatedState(aiSpace.tabs.map { it.id })
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .zIndex(if (isDragging) 12f else 0f)
                                                            .onGloballyPositioned { coords ->
                                                                if (!isDragging) {
                                                                    aiCardBounds[tab.id] = coords.boundsInRoot()
                                                                }
                                                            }
                                                            .pointerInput(aiSpace.id, tab.id) {
                                                                detectDragGesturesAfterLongPress(
                                                                    onDragStart = {
                                                                        if (!latestCanMove) return@detectDragGesturesAfterLongPress
                                                                        aiDraggingSpaceId = aiSpace.id
                                                                        aiDraggingTabId = tab.id
                                                                        aiDragOffset = Offset.Zero
                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    },
                                                                    onDrag = { change, dragAmount ->
                                                                        if (aiDraggingTabId != tab.id) return@detectDragGesturesAfterLongPress
                                                                        change.consume()
                                                                        aiDragOffset += dragAmount
                                                                        val myRect = aiCardBounds[tab.id] ?: return@detectDragGesturesAfterLongPress
                                                                        val cx = myRect.center.x + aiDragOffset.x
                                                                        val cy = myRect.center.y + aiDragOffset.y
                                                                        val hitId = latestTabIds.firstOrNull { candidateId ->
                                                                            candidateId != tab.id &&
                                                                                aiCardBounds[candidateId]?.contains(Offset(cx, cy)) == true
                                                                        }
                                                                        if (hitId != null) {
                                                                            val targetRect = aiCardBounds[hitId]
                                                                            onMoveTab(aiSpace.id, tab.id, hitId)
                                                                            if (targetRect != null) {
                                                                                aiDragOffset = Offset(
                                                                                    aiDragOffset.x + (myRect.left - targetRect.left),
                                                                                    aiDragOffset.y + (myRect.top - targetRect.top)
                                                                                )
                                                                                aiCardBounds[tab.id] = targetRect
                                                                                aiCardBounds[hitId] = myRect
                                                                            }
                                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                        }
                                                                    },
                                                                    onDragEnd = {
                                                                        aiDraggingSpaceId = null
                                                                        aiDraggingTabId = null
                                                                        aiDragOffset = Offset.Zero
                                                                    },
                                                                    onDragCancel = {
                                                                        aiDraggingSpaceId = null
                                                                        aiDraggingTabId = null
                                                                        aiDragOffset = Offset.Zero
                                                                    }
                                                                )
                                                            }
                                                            .graphicsLayer {
                                                                if (isDragging) {
                                                                    translationX = aiDragOffset.x
                                                                    translationY = aiDragOffset.y
                                                                    scaleX = 1.05f
                                                                    scaleY = 1.05f
                                                                    alpha = 0.94f
                                                                }
                                                            }
                                                    ) {
                                                        ChromeTabThumbnailCard(
                                                            tab = tab,
                                                            thumbnail = thumbnails[tab.id],
                                                            isActive = isCurrent,
                                                            isDragging = isDragging,
                                                            accentBorder = spaceAccent,
                                                            darkTheme = darkTheme,
                                                            selectionMode = false,
                                                            isSelectedForBatch = false,
                                                            modifier = Modifier
                                                                .onGloballyPositioned { coords ->
                                                                    tabCardRootRects[tab.id] = coords.boundsInRoot()
                                                                }
                                                                .graphicsLayer {
                                                                    alpha = if (tab.id in vanishingTabIds) 0f else 1f
                                                                },
                                                            onClick = { onSelectSpaceAndTab(aiSpace.id, tab.id) },
                                                            onClose = { triggerTabVanishClose(aiSpace.id, tab.id, spaceAccent) }
                                                        )
                                                    }
                                                }
                                                repeat(4 - rowTabs.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
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

            // 탭 페이지 우상단 ⋮ 메뉴 Drag-to-Open / Drag-to-Select 오버레이
            DragToSelectChromeMenuOverlay(
                visible = showMoreMenu,
                items = switcherMenuItems,
                hoveredIndex = hoveredMenuIndex,
                topPadding = 56.dp,
                endPadding = 48.dp,
                onDismiss = {
                    showMoreMenu = false
                    hoveredMenuIndex = -1
                }
            )
            if (activeTabVanishBursts.isNotEmpty()) {
                ParticleDisintegrationOverlay(
                    bursts = activeTabVanishBursts,
                    onBurstFinished = { bid -> finishTabVanishBurst(bid) }
                )
            }
        }
    }
}

@Composable
private fun ChromeTabThumbnailCard(
    tab: BrowserTab,
    thumbnail: Bitmap?,
    isActive: Boolean,
    isDragging: Boolean = false,
    accentBorder: Color,
    darkTheme: Boolean,
    selectionMode: Boolean,
    isSelectedForBatch: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val cardBg = if (isActive) {
        if (darkTheme) Color(0xFF2E3545) else Color(0xFFE8F0FE)
    } else {
        if (darkTheme) Color(0xFF23252A) else Color(0xFFFFFFFF)
    }
    val headerBg = if (isActive) {
        if (darkTheme) Color(0xFF333C50) else Color(0xFFD2E3FC)
    } else {
        if (darkTheme) Color(0xFF292B31) else Color(0xFFF8F9FA)
    }
    val borderCol = when {
        isDragging -> accentBorder
        isSelectedForBatch -> accentBorder
        isActive -> accentBorder
        else -> if (darkTheme) Color(0xFF34373F) else Color(0xFFDADCE0)
    }
    val borderWidth = if (isActive || isSelectedForBatch || isDragging) 2.6.dp else 0.8.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = if (isDragging) 14.dp else if (isActive) 6.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.84f)
            .border(borderWidth, borderCol, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 크롬 1:1 카드 헤더 (파비콘 + 1줄 제목 + ✕ 닫기)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(headerBg)
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab.isIncognito) {
                    EgoIcon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    , symbolSize = 16.dp)
                } else if (tab.url != NEW_TAB_URL) {
                    BookmarkMiniFavicon(
                        bm = BookmarkItem(title = tab.title, url = tab.url),
                        sizeDp = 16
                    )
                } else {
                    EgoIcon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    , symbolSize = 16.dp)
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = tab.title.ifBlank { "새 탭" },
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selectionMode) {
                    EgoIcon(
                        imageVector = if (isSelectedForBatch) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "선택",
                        tint = if (isSelectedForBatch) accentBorder else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp).size(19.dp)
                    , symbolSize = 19.dp)
                } else {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(30.dp)
                    ) {
                        EgoIcon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "탭 닫기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        , symbolSize = 16.dp)
                    }
                }
            }

            // 썸네일 프리뷰 본문
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (darkTheme) Color(0xFF1A1C20) else Color(0xFFF1F3F4)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = tab.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        EgoIcon(
                            imageVector = if (tab.url == NEW_TAB_URL) Icons.Default.AutoAwesome else Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(28.dp)
                        , symbolSize = 28.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (tab.url == NEW_TAB_URL) "새 탭" else tab.url.removePrefix("https://").removePrefix("http://").take(26),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
