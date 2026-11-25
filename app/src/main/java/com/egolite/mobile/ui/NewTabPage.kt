package com.egolite.mobile.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.BookmarkItem

const val NEW_TAB_URL = "ego://newtab"

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChromeNewTabPage(
    bookmarks: List<BookmarkItem>,
    onOpenOmnibox: () -> Unit,
    onNavigate: (String) -> Unit,
    onEditBookmark: (BookmarkItem) -> Unit,
    onAddBookmark: () -> Unit
) {
    val palette = listOf(
        Color(0xFF059669), Color(0xFF2563EB), Color(0xFF7C3AED),
        Color(0xFFD97706), Color(0xFFDC2626), Color(0xFF0891B2)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // 깔끔한 ego(lite) 브랜드 워드마크
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append("ego")
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append("(lite)")
                }
            },
            fontSize = 42.sp,
            letterSpacing = (-1).sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 크롬 스타일 중앙 알약 검색창 (터치 시 Omnibox 오버레이 즉시 실행)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clip(CircleShape)
                .clickable { onOpenOmnibox() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Google 검색 또는 URL 입력",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // 자주 가는 사이트 / 북마크 바로가기 그리드 (길게 눌러 수정)
        FlowRow(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            maxItemsInEachRow = 5
        ) {
            bookmarks.take(9).forEachIndexed { idx, bm ->
                val accent = palette[idx % palette.size]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onNavigate(bm.url) },
                            onLongClick = { onEditBookmark(bm) }
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BookmarkMiniFavicon(bm = bm, sizeDp = 28)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bm.title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // + 바로가기 추가 버튼
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAddBookmark() }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "바로가기 추가",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "바로가기 추가",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
