package com.egolite.mobile.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.BrowserTab
import com.egolite.mobile.model.SpaceType

/**
 * 크롬 태블릿 1:1 탭 모아보기 + Personal / AI Space 멀티 그룹 통합 관리 화면
 */
@Composable
fun ChromeTabSwitcherOverlay(
    spaces: List<BrowserSpace>,
    activeSpace: BrowserSpace,
    darkTheme: Boolean,
    canAddAiSpace: Boolean,
    onSelectSpaceAndTab: (spaceId: String, tabId: String) -> Unit,
    onCloseTab: (spaceId: String, tabId: String) -> Unit,
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
    var selectionMode by remember { mutableStateOf(false) }
    var selectedTabIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 전체 공간의 열린 탭 썸네일 캡처
    val allTabsKey = remember(spaces) { spaces.flatMap { s -> s.tabs.map { "${s.id}:${it.id}" } } }
    val thumbnails: Map<String, Bitmap?> = remember(allTabsKey) {
        spaces.flatMap { it.tabs }.associate { tab ->
            tab.id to if (tab.url == NEW_TAB_URL) null else SpaceManager.captureTabThumbnail(tab.id)
        }
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
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
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
                            Icon(Icons.Default.Close, contentDescription = "선택 취소")
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
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (selectedSection == 0) "새 탭" else "새 AI Space",
                            tint = addBtnTint,
                            modifier = Modifier.size(22.dp)
                        )
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
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "AI Space 탭 그룹",
                                tint = if (selectedSection == 1) EgoEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "AI Space (${aiSpaces.size})",
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedSection == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 우측: 검색 · 더보기(⋮) · 닫기(✕)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "탭 검색",
                                tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("새 탭") },
                                    leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        onCreateTab(personalSpace.id, false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("새 창 (AI Space)") },
                                    leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = EgoEmerald, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        onCreateAiSpace()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("새 시크릿 창") },
                                    leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        onCreateTab(personalSpace.id, true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("새 AI 탭 그룹") },
                                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        onCreateAiSpace()
                                        selectedSection = 1
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("탭 모두 닫기") },
                                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        onCloseAllTabs(if (selectedSection == 0) personalSpace.id else activeSpace.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("탭 선택") },
                                    leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        selectedSection = 0
                                        selectionMode = true
                                        selectedTabIds = emptySet()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("인터넷 사용 기록 삭제") },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        onClearBrowsingData()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("설정") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(19.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
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
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "지우기", modifier = Modifier.size(16.dp))
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

            // 2. 본문 영역: Personal 4열 그리드 vs AI Space 멀티 그룹
            if (selectedSection == 0) {
                val q = searchQuery.trim().lowercase()
                val visibleTabs = remember(personalSpace.tabs, q) {
                    if (q.isBlank()) personalSpace.tabs
                    else personalSpace.tabs.filter {
                        it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleTabs, key = { it.id }) { tab ->
                        val isCurrent = activeSpace.id == personalSpace.id && tab.id == personalSpace.activeTabId
                        val isPicked = tab.id in selectedTabIds
                        ChromeTabThumbnailCard(
                            tab = tab,
                            thumbnail = thumbnails[tab.id],
                            isActive = isCurrent,
                            accentBorder = activeCardBorder,
                            darkTheme = darkTheme,
                            selectionMode = selectionMode,
                            isSelectedForBatch = isPicked,
                            onClick = {
                                if (selectionMode) {
                                    selectedTabIds = if (isPicked) selectedTabIds - tab.id else selectedTabIds + tab.id
                                } else {
                                    onSelectSpaceAndTab(personalSpace.id, tab.id)
                                }
                            },
                            onClose = { onCloseTab(personalSpace.id, tab.id) }
                        )
                    }
                }
            } else {
                // AI Space 탭 그룹: 여러 개의 AI Space를 공간별 그룹 섹션으로 모두 표시
                if (aiSpaces.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
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
                                            Icon(Icons.Default.Add, contentDescription = null, tint = spaceAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("새 탭", fontSize = 12.sp, color = spaceAccent, fontWeight = FontWeight.Bold)
                                        }
                                        if (aiSpaces.size > 1) {
                                            IconButton(
                                                onClick = { onCloseAiSpace(aiSpace.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "AI Space 닫기",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 해당 AI Space의 탭 카드 4열 행
                                    filteredTabs.chunked(4).forEach { rowTabs ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowTabs.forEach { tab ->
                                                val isCurrent = activeSpace.id == aiSpace.id && tab.id == aiSpace.activeTabId
                                                Box(modifier = Modifier.weight(1f)) {
                                                    ChromeTabThumbnailCard(
                                                        tab = tab,
                                                        thumbnail = thumbnails[tab.id],
                                                        isActive = isCurrent,
                                                        accentBorder = spaceAccent,
                                                        darkTheme = darkTheme,
                                                        selectionMode = false,
                                                        isSelectedForBatch = false,
                                                        onClick = { onSelectSpaceAndTab(aiSpace.id, tab.id) },
                                                        onClose = { onCloseTab(aiSpace.id, tab.id) }
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
    }
}

@Composable
private fun ChromeTabThumbnailCard(
    tab: BrowserTab,
    thumbnail: Bitmap?,
    isActive: Boolean,
    accentBorder: Color,
    darkTheme: Boolean,
    selectionMode: Boolean,
    isSelectedForBatch: Boolean,
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
        isSelectedForBatch -> accentBorder
        isActive -> accentBorder
        else -> if (darkTheme) Color(0xFF34373F) else Color(0xFFDADCE0)
    }
    val borderWidth = if (isActive || isSelectedForBatch) 2.6.dp else 0.8.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = if (isActive) 6.dp else 1.dp,
        modifier = Modifier
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
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (tab.url != NEW_TAB_URL) {
                    BookmarkMiniFavicon(
                        bm = BookmarkItem(title = tab.title, url = tab.url),
                        sizeDp = 16
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
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
                    Icon(
                        imageVector = if (isSelectedForBatch) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "선택",
                        tint = if (isSelectedForBatch) accentBorder else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp).size(19.dp)
                    )
                } else {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "탭 닫기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 썸네일 프리뷰 본문 (약간의 내부 패딩 + 둥근 모서리로 크롬 태블릿 카드 느낌 1:1 재현)
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
                        Icon(
                            imageVector = if (tab.url == NEW_TAB_URL) Icons.Default.AutoAwesome else Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(28.dp)
                        )
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
