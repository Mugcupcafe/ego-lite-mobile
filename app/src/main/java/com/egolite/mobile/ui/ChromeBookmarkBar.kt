package com.egolite.mobile.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.BookmarkItem
import kotlin.math.abs

private val bookmarkBadgePalette = listOf(
    Color(0xFF03C75A), // Naver green
    Color(0xFF2563EB), // Cobalt blue
    Color(0xFF7C3AED), // Violet
    Color(0xFFEA580C), // Orange
    Color(0xFFDC2626), // Red
    Color(0xFF0891B2), // Cyan
    Color(0xFF4F46E5), // Indigo
    Color(0xFF059669)  // Emerald
)

fun badgeColorForBookmark(title: String, url: String): Color {
    val lower = url.lowercase()
    return when {
        "naver" in lower -> Color(0xFF03C75A)
        "google" in lower -> Color(0xFF4285F4)
        "youtube" in lower -> Color(0xFFFF0000)
        "github" in lower -> Color(0xFF334155)
        "algumon" in lower -> Color(0xFF2563EB)
        else -> bookmarkBadgePalette[abs((title + url).hashCode()) % bookmarkBadgePalette.size]
    }
}

/**
 * 태블릿 크롬 1:1 북마크 바
 * - 좌측: 폴더 아이콘(탭 시 하위 북마크 드롭다운) + 플랫 사이트 아이콘·제목(길게 눌러 편집·좌우 위치 변경·삭제)
 * - 우측: `>` + 구분선 `|` + `☆ 모든 북마크`
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChromeBookmarkBar(
    bookmarks: List<BookmarkItem>,
    folders: List<String>,
    onNavigate: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onEditBookmark: (BookmarkItem) -> Unit,
    onMoveBookmark: (id: String, delta: Int) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onOpenAllBookmarks: () -> Unit
) {
    val activeIconColor = MaterialTheme.colorScheme.onSurface
    val mutedIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 북마크 바에 직접 표시할 폴더("북마크 바"·"모바일 북마크" 외 커스텀 폴더, 예: AI)
    val barFolders = remember(folders, bookmarks) {
        folders.filter { f -> f != "북마크 바" && f != "모바일 북마크" }
    }
    // 북마크 바 직접 항목 ("북마크 바" 폴더 우선, 비어있으면 전체)
    val barItems = remember(bookmarks) {
        val direct = bookmarks.filter { it.folder == "북마크 바" }
        if (direct.isNotEmpty()) direct else bookmarks
    }

    var showOverflowMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp)
    ) {
        // 우측 " | ☆ 모든 북마크" 고정 영역(약 118dp) + 좌우 패딩 제외 후 가용 너비 산출
        val maxAvailableDp = (maxWidth.value - 148f).coerceAtLeast(120f)
        fun estimateChipDp(title: String, maxTextDp: Float): Float {
            val charW = title.sumOf { ch -> if (ch.code > 0x7F) 11.5 else 7.2 }.toFloat().coerceAtMost(maxTextDp)
            return 34f + charW
        }

        val visibleFolders = mutableListOf<String>()
        val overflowFolders = mutableListOf<String>()
        val visibleItems = mutableListOf<BookmarkItem>()
        val overflowItems = mutableListOf<BookmarkItem>()

        var usedDp = 0f
        barFolders.forEach { folderName ->
            val need = estimateChipDp(folderName, 110f) + if (usedDp > 0f) 14f else 0f
            if (usedDp + need <= maxAvailableDp) {
                visibleFolders.add(folderName)
                usedDp += need
            } else {
                overflowFolders.add(folderName)
            }
        }
        barItems.forEach { bm ->
            val need = estimateChipDp(bm.title, 124f) + if (usedDp > 0f) 14f else 0f
            if (overflowFolders.isEmpty() && overflowItems.isEmpty() && usedDp + need <= maxAvailableDp) {
                visibleItems.add(bm)
                usedDp += need
            } else {
                overflowItems.add(bm)
            }
        }
        val hasOverflow = overflowFolders.isNotEmpty() || overflowItems.isNotEmpty()

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) 화면에 들어가는 폴더 항목 (예: 📁 AI)
            visibleFolders.forEach { folderName ->
                var expanded by remember(folderName) { mutableStateOf(false) }
                val folderItems = remember(bookmarks, folderName) {
                    bookmarks.filter { it.folder == folderName }
                }
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EgoIcon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = folderName,
                            modifier = Modifier.size(16.dp),
                            tint = mutedIconColor
                        , symbolSize = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = folderName,
                            fontSize = 13.sp,
                            color = activeIconColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (folderItems.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("비어 있음", fontSize = 13.sp, color = mutedIconColor) },
                                onClick = { expanded = false }
                            )
                        } else {
                            folderItems.forEach { bm ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            BookmarkMiniFavicon(bm)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(bm.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    },
                                    onClick = {
                                        expanded = false
                                        onNavigate(bm.url)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2) 북마크 바 직접 항목 (플랫 아이콘 + 텍스트, 길게 눌러 편집/위치변경/삭제)
            visibleItems.forEachIndexed { idx, bm ->
                var showItemMenu by remember(bm.id) { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = { onNavigate(bm.url) },
                                onLongClick = { showItemMenu = true }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookmarkMiniFavicon(bm)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bm.title,
                            fontSize = 13.sp,
                            color = activeIconColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 124.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showItemMenu,
                        onDismissRequest = { showItemMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("새 탭에서 열기") },
                            leadingIcon = { EgoIcon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp), symbolSize = 18.dp) },
                            onClick = {
                                showItemMenu = false
                                onOpenInNewTab(bm.url)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("북마크 수정") },
                            leadingIcon = { EgoIcon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), symbolSize = 18.dp) },
                            onClick = {
                                showItemMenu = false
                                onEditBookmark(bm)
                            }
                        )
                        if (idx > 0) {
                            DropdownMenuItem(
                                text = { Text("왼쪽으로 이동") },
                                leadingIcon = { EgoIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), symbolSize = 18.dp) },
                                onClick = {
                                    showItemMenu = false
                                    onMoveBookmark(bm.id, -1)
                                }
                            )
                        }
                        if (idx < barItems.lastIndex) {
                            DropdownMenuItem(
                                text = { Text("오른쪽으로 이동") },
                                leadingIcon = { EgoIcon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), symbolSize = 18.dp) },
                                onClick = {
                                    showItemMenu = false
                                    onMoveBookmark(bm.id, 1)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("삭제", color = EgoDangerRed) },
                            leadingIcon = { EgoIcon(Icons.Default.Delete, contentDescription = null, tint = EgoDangerRed, modifier = Modifier.size(18.dp), symbolSize = 18.dp) },
                            onClick = {
                                showItemMenu = false
                                onDeleteBookmark(bm.id)
                            }
                        )
                    }
                }
            }
        }

        // 우측 오버플로우 '>' 확장 버튼 (북마크바가 가득 차서 숨겨진 항목이 있을 때만 표시 — Chrome 1:1)
        if (hasOverflow) {
            Box {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (showOverflowMenu) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                        )
                        .clickable { showOverflowMenu = !showOverflowMenu },
                    contentAlignment = Alignment.Center
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "숨겨진 북마크 펼치기",
                        modifier = Modifier.size(18.dp),
                        tint = activeIconColor
                    , symbolSize = 18.dp)
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
                ) {
                    overflowFolders.forEach { folderName ->
                        val folderBookmarks = bookmarks.filter { it.folder == folderName }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EgoIcon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = mutedIconColor
                                    , symbolSize = 18.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "$folderName (${folderBookmarks.size})",
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenAllBookmarks()
                            }
                        )
                    }
                    overflowItems.forEach { bm ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BookmarkMiniFavicon(bm, sizeDp = 18)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bm.title,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                showOverflowMenu = false
                                onNavigate(bm.url)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onOpenAllBookmarks() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EgoIcon(
                imageVector = Icons.Default.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = mutedIconColor
            , symbolSize = 16.dp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "모든 북마크",
                fontSize = 13.sp,
                color = activeIconColor,
                maxLines = 1
            )
        }
        }
    }
}

@Composable
fun BookmarkMiniFavicon(bm: BookmarkItem, sizeDp: Int = 18) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var iconBitmap by remember(bm.url) { mutableStateOf(com.egolite.mobile.browser.FaviconCache.getMemoryCached(bm.url)) }
    androidx.compose.runtime.LaunchedEffect(bm.url) {
        if (iconBitmap == null) {
            iconBitmap = com.egolite.mobile.browser.FaviconCache.loadFavicon(context, bm.url)
        }
    }

    val bmp = iconBitmap
    if (bmp != null) {
        androidx.compose.foundation.Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = bm.title,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    } else {
        val bg = badgeColorForBookmark(bm.title, bm.url)
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bm.title.trim().take(1).uppercase().ifBlank { "B" },
                fontSize = (sizeDp * 0.56f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
