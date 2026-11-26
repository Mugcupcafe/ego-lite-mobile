package com.egolite.mobile.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.model.BookmarkItem
import kotlinx.coroutines.delay

@Composable
fun ChromeOmniboxOverlay(
    currentTitle: String,
    currentUrl: String,
    bookmarks: List<BookmarkItem>,
    recentSearches: List<String>,
    anchorLeftPx: Float = 0f,
    anchorTopPx: Float = 0f,
    anchorWidthPx: Float = 0f,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var query by remember { mutableStateOf("") }
    var liveSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            liveSuggestions = emptyList()
        } else {
            delay(180)
            liveSuggestions = BrowserActivityBridge.fetchSearchSuggestions(query)
        }
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val filteredSearches = remember(query, recentSearches) {
        if (query.isBlank()) recentSearches
        else recentSearches.filter { it.contains(query, ignoreCase = true) }
    }

    val hasAnchor = anchorWidthPx > 120f
    val startDp = with(density) { anchorLeftPx.coerceAtLeast(0f).toDp() }
    val topDp = with(density) { anchorTopPx.coerceAtLeast(0f).toDp() }
    val widthDp = with(density) { anchorWidthPx.coerceAtLeast(320f).toDp() }

    // 전체 반투명 검정 배경 제거 — 투명 바깥 터치 감지 + 커진 검색창의 다층 그림자만 강조
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = if (hasAnchor) Alignment.TopStart else Alignment.TopCenter
    ) {
        Surface(
            modifier = if (hasAnchor) {
                Modifier
                    .padding(start = startDp, top = topDp)
                    .width(widthDp)
                    .clickable(enabled = false) {}
            } else {
                Modifier
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                // 1) 상단 검색 / URL 입력 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (query.isNotBlank()) {
                                    onNavigate(query)
                                    onDismiss()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Google 검색 또는 URL 입력",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "지우기", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider()

                // 2) 현재 페이지 빠른 액션 행 (제목 + URL + 공유 · 복사 · URL 편집)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTitle.ifBlank { currentUrl },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUrl,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, currentTitle)
                                putExtra(Intent.EXTRA_TEXT, currentUrl)
                            }
                            context.startActivity(Intent.createChooser(intent, "링크 공유"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "공유", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                            Toast.makeText(context, "링크 복사됨", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "복사", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { query = currentUrl },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "URL 편집", modifier = Modifier.size(18.dp))
                    }
                }

                // 3) 바로가기 원형 아이콘 행 (북마크 기반)
                if (bookmarks.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        bookmarks.take(10).forEachIndexed { idx, bm ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(56.dp)
                                    .clickable {
                                        onNavigate(bm.url)
                                        onDismiss()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BookmarkMiniFavicon(bm = bm, sizeDp = 24)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bm.title,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 3.5) Google 실시간 추천 검색어
                if (liveSuggestions.isNotEmpty()) {
                    liveSuggestions.forEach { sug ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigate(sug)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = sug,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { query = sug }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.NorthWest, contentDescription = "채우기", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // 4) 최근 검색어 목록 (크롬 스타일: 시계 아이콘 + 검색어 + ↖ 입력창 채우기)
                if (filteredSearches.isNotEmpty()) {
                    Text(
                        text = "최근 검색어",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(filteredSearches) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNavigate(term)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = term,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { query = term },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NorthWest,
                                        contentDescription = "검색창에 채우기",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
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

@Composable
fun BookmarkEditDialog(
    existing: BookmarkItem?,
    initialTitle: String,
    initialUrl: String,
    folders: List<String>,
    onSave: (id: String?, title: String, url: String, folder: String) -> Unit,
    onDelete: ((String) -> Unit)?,
    onAddFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: initialTitle) }
    var url by remember { mutableStateOf(existing?.url ?: initialUrl) }
    var selectedFolder by remember { mutableStateOf(existing?.folder ?: folders.firstOrNull() ?: "모바일 북마크") }
    var showFolderMenu by remember { mutableStateOf(false) }
    var newFolderInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "북마크 추가" else "북마크 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("폴더", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { showFolderMenu = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedFolder, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder) },
                                onClick = {
                                    selectedFolder = folder
                                    showFolderMenu = false
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newFolderInput,
                        onValueChange = { newFolderInput = it },
                        placeholder = { Text("새 폴더 이름", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (newFolderInput.isNotBlank()) {
                            onAddFolder(newFolderInput.trim())
                            selectedFolder = newFolderInput.trim()
                            newFolderInput = ""
                        }
                    }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "폴더 추가")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(existing?.id, title, url, selectedFolder)
                onDismiss()
            }) { Text("저장") }
        },
        dismissButton = {
            Row {
                if (existing != null && onDelete != null) {
                    TextButton(onClick = {
                        onDelete(existing.id)
                        onDismiss()
                    }) { Text("삭제", color = EgoDangerRed) }
                }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        }
    )
}

@Composable
fun BookmarkManagerDialog(
    bookmarks: List<BookmarkItem>,
    folders: List<String>,
    onOpenUrl: (String) -> Unit,
    onEditBookmark: (BookmarkItem) -> Unit,
    onMoveBookmark: (id: String, delta: Int) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onAddFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var activeFolder by remember { mutableStateOf("전체") }
    var search by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var showNewFolderRow by remember { mutableStateOf(false) }

    val filtered = remember(bookmarks, activeFolder, search) {
        bookmarks.filter { bm ->
            (activeFolder == "전체" || bm.folder == activeFolder) &&
                (search.isBlank() || bm.title.contains(search, true) || bm.url.contains(search, true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("북마크 (${bookmarks.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(
                    onClick = { showNewFolderRow = !showNewFolderRow },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "새 폴더 만들기", modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 크롬 스타일 둥근 북마크 검색 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (search.isEmpty()) Text("북마크 검색", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                }

                // 폴더 탭 행
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (listOf("전체") + folders).forEach { folder ->
                        val selected = folder == activeFolder
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                                .clickable { activeFolder = folder }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = folder,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 새 폴더 추가 (상단 폴더 아이콘 탭 시 표시)
                if (showNewFolderRow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text("새 폴더 이름", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (newFolderName.isNotBlank()) {
                                onAddFolder(newFolderName.trim())
                                newFolderName = ""
                                showNewFolderRow = false
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "폴더 추가")
                        }
                    }
                }

                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { bm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .clickable {
                                    onOpenUrl(bm.url)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookmarkMiniFavicon(bm)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bm.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${bm.folder} · ${bm.url}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                text = "▲",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onMoveBookmark(bm.id, -1) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            Text(
                                text = "▼",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onMoveBookmark(bm.id, 1) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            IconButton(onClick = { onEditBookmark(bm) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "수정", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteBookmark(bm.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(16.dp), tint = EgoDangerRed)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { BrowserActivityBridge.launchBookmarkImport?.invoke() }) { Text("HTML 가져오기") }
                TextButton(onClick = { BrowserActivityBridge.launchBookmarkExport?.invoke() }) { Text("내보내기") }
                TextButton(onClick = onDismiss) { Text("닫기") }
            }
        }
    )
}
