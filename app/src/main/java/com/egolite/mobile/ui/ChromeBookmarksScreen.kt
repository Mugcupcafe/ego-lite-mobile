package com.egolite.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.BookmarkItem

const val BOOKMARKS_URL = "ego://bookmarks"

/**
 * 크롬 태블릿 1:1 풀스크린 북마크 페이지 (`ego://bookmarks`, 19048.jpg · 19050.jpg 레퍼런스 기반).
 */
@Composable
fun ChromeBookmarksScreen(
    bookmarks: List<BookmarkItem>,
    folders: List<String>,
    onNavigate: (String) -> Unit,
    onEditBookmark: (BookmarkItem) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onMoveBookmark: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val chromeCols = LocalEgoChromeColors.current
    var currentFolder by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortByName by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var movingItem by remember { mutableStateOf<BookmarkItem?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val inSelectionMode = selectedIds.isNotEmpty()

    val rootFolders = remember(folders, bookmarks) {
        val base = linkedSetOf("모바일 북마크", "북마크바", "읽기 목록")
        folders.forEach { if (it.isNotBlank()) base.add(it) }
        bookmarks.forEach { if (it.folder.isNotBlank()) base.add(it.folder) }
        base.toList()
    }

    val filteredBookmarks = remember(bookmarks, currentFolder, searchQuery, sortByName) {
        val q = searchQuery.trim()
        val list = bookmarks.filter { bm ->
            val matchFolder = if (q.isNotEmpty()) true else (currentFolder == null || bm.folder == currentFolder)
            val matchQuery = q.isEmpty() || bm.title.contains(q, ignoreCase = true) || bm.url.contains(q, ignoreCase = true)
            matchFolder && matchQuery
        }
        if (sortByName) list.sortedBy { it.title.lowercase() } else list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chromeCols.backdropBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // 상단 헤더: 북마크 / ← 폴더명 + 정렬 + 새 폴더 + 다중 선택 삭제
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (inSelectionMode) {
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        EgoIcon(Icons.Default.Close, contentDescription = "선택 취소", symbolSize = 20.dp)
                    }
                    Text(
                        text = "${selectedIds.size}개 선택됨",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        selectedIds.forEach { onDeleteBookmark(it) }
                        selectedIds = emptySet()
                    }) {
                        EgoIcon(Icons.Default.Delete, contentDescription = "선택 삭제", tint = EgoDangerRed, symbolSize = 20.dp)
                    }
                } else {
                    if (currentFolder != null) {
                        IconButton(onClick = { currentFolder = null }) {
                            EgoIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "상위 폴더", symbolSize = 20.dp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = currentFolder ?: "북마크",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { sortByName = !sortByName }) {
                        EgoIcon(
                            Icons.Default.Sort,
                            contentDescription = if (sortByName) "기본순 정렬" else "이름순 정렬",
                            tint = if (sortByName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            symbolSize = 20.dp
                        )
                    }
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        EgoIcon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = "새 폴더",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            symbolSize = 20.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 크롬 1:1 풀와이드 둥근 검색 바 ("북마크 검색")
            Surface(
                shape = CircleShape,
                color = chromeCols.groupSurface,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EgoIcon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                        symbolSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("북마크 검색", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            EgoIcon(Icons.Default.Close, contentDescription = "검색어 지우기", symbolSize = 16.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 콘텐츠 리스트: 루트 폴더 목록 (19048.jpg) vs 폴더 내부 북마크 목록 (19050.jpg)
            if (currentFolder == null && searchQuery.isBlank()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(rootFolders, key = { _, f -> f }) { _, folderName ->
                        val count = bookmarks.count { it.folder == folderName }
                        val isReadingList = folderName == "읽기 목록"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { currentFolder = folderName }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 크롬 1:1 54dp 둥근 사각 폴더 아이콘 + 우하단 항목 수 배지
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
                            ) {
                                EgoIcon(
                                    imageVector = if (isReadingList) Icons.Outlined.Bookmarks else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(20.dp),
                                    symbolSize = 20.dp
                                )
                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 9.dp, bottom = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = folderName,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                if (filteredBookmarks.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "검색 결과가 없습니다." else "이 폴더에 북마크가 없습니다.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredBookmarks, key = { _, bm -> bm.id }) { idx, bm ->
                            val isSelected = bm.id in selectedIds
                            var menuExpanded by remember(bm.id) { mutableStateOf(false) }
                            val displayHost = remember(bm.url) {
                                AdBlockEngine.extractHost(bm.url).ifBlank { bm.url.take(40) }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                    .clickable {
                                        if (inSelectionMode) {
                                            selectedIds = if (isSelected) selectedIds - bm.id else selectedIds + bm.id
                                        } else {
                                            onNavigate(bm.url)
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 54dp 둥근 사각 파비콘 타일 (19050.jpg)
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(chromeCols.settingsCardBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        EgoIcon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp), symbolSize = 26.dp)
                                    } else {
                                        BookmarkMiniFavicon(bm = bm, sizeDp = 26)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = bm.title.ifBlank { displayHost },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = displayHost,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // 우측 ⋮ 오버플로우 메뉴 (19050.jpg: 선택 · 수정 · 다음 폴더로 이동 · 삭제 · 위로/아래로 이동)
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        EgoIcon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "북마크 옵션",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            symbolSize = 20.dp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("선택") },
                                            onClick = { menuExpanded = false; selectedIds = selectedIds + bm.id }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("수정") },
                                            onClick = { menuExpanded = false; onEditBookmark(bm) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("다음 폴더로 이동...") },
                                            onClick = { menuExpanded = false; movingItem = bm }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("삭제", color = EgoDangerRed) },
                                            onClick = { menuExpanded = false; onDeleteBookmark(bm.id) }
                                        )
                                        if (idx > 0) {
                                            DropdownMenuItem(
                                                text = { Text("위로 이동") },
                                                onClick = { menuExpanded = false; onMoveBookmark(bm.id, -1) }
                                            )
                                        }
                                        if (idx < filteredBookmarks.lastIndex) {
                                            DropdownMenuItem(
                                                text = { Text("아래로 이동") },
                                                onClick = { menuExpanded = false; onMoveBookmark(bm.id, 1) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 하단 HTML 가져오기 / 내보내기 액션 바
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { BrowserActivityBridge.launchBookmarkImport?.invoke() }) {
                    Text("HTML 가져오기", fontSize = 12.5.sp)
                }
                TextButton(onClick = { BrowserActivityBridge.launchBookmarkExport?.invoke() }) {
                    Text("내보내기", fontSize = 12.5.sp)
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("새 북마크 폴더") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    placeholder = { Text("폴더 이름 입력") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newFolderName.trim()
                    if (trimmed.isNotEmpty()) {
                        SpaceManager.addBookmarkFolder(trimmed)
                        newFolderName = ""
                        showCreateFolderDialog = false
                    }
                }) { Text("만들기") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("취소") }
            }
        )
    }

    movingItem?.let { bm ->
        AlertDialog(
            onDismissRequest = { movingItem = null },
            title = { Text("폴더 이동") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rootFolders.forEach { targetFolder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    SpaceManager.addOrUpdateBookmark(bm.id, bm.title, bm.url, targetFolder)
                                    movingItem = null
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EgoIcon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, symbolSize = 18.dp)
                            Text(
                                text = targetFolder,
                                fontSize = 14.sp,
                                fontWeight = if (targetFolder == bm.folder) FontWeight.Bold else FontWeight.Normal,
                                color = if (targetFolder == bm.folder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { movingItem = null }) { Text("닫기") } }
        )
    }
}
