package com.egolite.mobile.ui

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.gestures.detectTapGestures
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.DiscoverFeedRepository
import com.egolite.mobile.browser.NewsArticleItem
import com.egolite.mobile.browser.NewsCategory
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.browser.TrendIdeaItem
import com.egolite.mobile.model.BookmarkItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val NEW_TAB_URL = "ego://newtab"

private data class FeedMenuTarget(
    val title: String,
    val url: String,
    val source: String
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChromeNewTabPage(
    bookmarks: List<BookmarkItem>,
    onOpenOmnibox: () -> Unit,
    onNavigate: (String) -> Unit,
    onEditBookmark: (BookmarkItem) -> Unit,
    onAddBookmark: () -> Unit,
    onOpenInNewTab: (String) -> Unit = onNavigate,
    onOpenInIncognito: (String) -> Unit = onNavigate,
    onDeleteBookmark: (BookmarkItem) -> Unit = {},
    onDustVanish: (VanishSnapshot) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val todayLabel = remember {
        SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN).format(Date())
    }

    var selectedFeedTab by remember { mutableIntStateOf(0) }
    var selectedNewsCategory by remember { mutableStateOf(NewsCategory.TOP) }
    var newsItems by remember { mutableStateOf<List<NewsArticleItem>>(emptyList()) }
    var trendItems by remember { mutableStateOf<List<TrendIdeaItem>>(emptyList()) }
    var isLoadingFeed by remember { mutableStateOf(false) }

    // 기사 숨기기 / 언론사 차단 / 하단 ⋮ 메뉴 시트 상태
    var hiddenUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var blockedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var menuTarget by remember { mutableStateOf<FeedMenuTarget?>(null) }
    var shortcutMenuTarget by remember { mutableStateOf<Pair<BookmarkItem, Rect?>?>(null) }
    val haptic = LocalHapticFeedback.current

    fun reloadFeed(force: Boolean = false) {
        scope.launch {
            isLoadingFeed = true
            if (selectedFeedTab == 0) {
                newsItems = DiscoverFeedRepository.fetchKoreaNews(selectedNewsCategory, forceRefresh = force)
            } else {
                trendItems = DiscoverFeedRepository.fetchLast30DaysTrends(forceRefresh = force)
            }
            isLoadingFeed = false
        }
    }

    fun shareArticle(title: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }
        context.startActivity(Intent.createChooser(intent, "기사 공유"))
    }

    LaunchedEffect(selectedFeedTab, selectedNewsCategory) {
        reloadFeed(force = false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 날짜 칩 + ego(lite) 브랜드 워드마크
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Text(
                    text = todayLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)) {
                            append("ego")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("(lite)")
                        }
                    },
                    fontSize = 40.sp,
                    letterSpacing = (-1.2).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(EgoEmerald)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 크롬 스타일 중앙 알약 검색창
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    // M3 Tonal Search Pill (외곽선 제거, 톤+그림자 분리)
                    .clip(CircleShape)
                    .clickable { onOpenOmnibox() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    , symbolSize = 22.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Google 검색 또는 URL 입력",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 크롬 태블릿 1:1 바로가기 캡슐 트레이
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    // M3 Tonal Shortcut Dock (외곽선 제거)
            ) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 9
                ) {
                    bookmarks.take(8).forEach { bm ->
                        var tileRect by remember(bm.id) { mutableStateOf<Rect?>(null) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .onGloballyPositioned { coords -> tileRect = coords.boundsInRoot() }
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { onNavigate(bm.url) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        shortcutMenuTarget = bm to tileRect
                                    }
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                BookmarkMiniFavicon(bm = bm, sizeDp = 26)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = bm.title,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAddBookmark() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "새로 추가",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "새로 추가",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 하단 크롬 디스커버 1:1 피드 영역 (2열 비정형 매거진 Masonry 레이아웃)
            Column(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiscoverTabChip(
                            label = "실시간 뉴스",
                            icon = Icons.Default.Newspaper,
                            selected = selectedFeedTab == 0,
                            accent = EgoCobalt,
                            onClick = { selectedFeedTab = 0 }
                        )
                        DiscoverTabChip(
                            label = "Last30Days 트렌드",
                            icon = Icons.Default.TrendingUp,
                            selected = selectedFeedTab == 1,
                            accent = EgoEmerald,
                            onClick = { selectedFeedTab = 1 }
                        )
                    }
                    IconButton(
                        onClick = { reloadFeed(force = true) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        if (isLoadingFeed) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            EgoIcon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "피드 새로고침",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            , symbolSize = 20.dp)
                        }
                    }
                }

                if (selectedFeedTab == 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NewsCategory.entries.forEach { cat ->
                            val active = selectedNewsCategory == cat
                            Surface(
                                shape = CircleShape,
                                color = if (active) EgoCobalt.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (active) EgoCobalt else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable { selectedNewsCategory = cat }
                            ) {
                                Text(
                                    text = cat.label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) EgoCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedFeedTab == 0) {
                    val visibleNews = newsItems.filterNot { it.url in hiddenUrls || it.source in blockedSources }
                    if (visibleNews.isEmpty() && !isLoadingFeed) {
                        EmptyFeedCard("표시할 뉴스가 없습니다. 새로고침을 눌러 다시 불러와 주세요.")
                    } else {
                        // 크롬 태블릿 1:1 2열 비정형(Masonry) 카드 레이아웃 — 좌/우 열 독립 적재
                        val leftCol = visibleNews.filterIndexed { i, _ -> i % 2 == 0 }
                        val rightCol = visibleNews.filterIndexed { i, _ -> i % 2 == 1 }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                leftCol.forEachIndexed { idx, article ->
                                    ChromeDiscoverNewsCard(
                                        article = article,
                                        compactSideLayout = (idx % 3 == 0),
                                        onClick = { onNavigate(article.url) },
                                        onShare = { shareArticle(article.title, article.url) },
                                        onOpenMenu = {
                                            menuTarget = FeedMenuTarget(article.title, article.url, article.source)
                                        }
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rightCol.forEachIndexed { idx, article ->
                                    ChromeDiscoverNewsCard(
                                        article = article,
                                        compactSideLayout = (idx % 3 == 2),
                                        onClick = { onNavigate(article.url) },
                                        onShare = { shareArticle(article.title, article.url) },
                                        onOpenMenu = {
                                            menuTarget = FeedMenuTarget(article.title, article.url, article.source)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val visibleTrends = trendItems.filterNot { it.url in hiddenUrls || it.source in blockedSources }
                    if (visibleTrends.isEmpty() && !isLoadingFeed) {
                        EmptyFeedCard("트렌드 데이터를 불러오지 못했습니다. 새로고침을 눌러 다시 시도해 주세요.")
                    } else {
                        val leftCol = visibleTrends.filterIndexed { i, _ -> i % 2 == 0 }
                        val rightCol = visibleTrends.filterIndexed { i, _ -> i % 2 == 1 }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                leftCol.forEach { trend ->
                                    ChromeDiscoverTrendCard(
                                        trend = trend,
                                        onClick = { onNavigate(trend.url) },
                                        onShare = { shareArticle(trend.title, trend.url) },
                                        onOpenMenu = {
                                            menuTarget = FeedMenuTarget(trend.title, trend.url, trend.source)
                                        }
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rightCol.forEach { trend ->
                                    ChromeDiscoverTrendCard(
                                        trend = trend,
                                        onClick = { onNavigate(trend.url) },
                                        onShare = { shareArticle(trend.title, trend.url) },
                                        onOpenMenu = {
                                            menuTarget = FeedMenuTarget(trend.title, trend.url, trend.source)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // 크롬 1:1 뉴스 카드 ⋮ 하단 메뉴 시트 (첨부 5 동일)
        shortcutMenuTarget?.let { (bm, anchorRect) ->
            var menuCardRect by remember { mutableStateOf<Rect?>(null) }
        var menuCardCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
            val menuLayer = rememberGraphicsLayer()
            fun dismissShortcutMenu(touchRoot: Offset? = null, afterDismiss: () -> Unit = {}) {
                scope.launch {
                    val bmp = runCatching { menuLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                    val origin = touchRoot ?: menuCardRect?.center
                    val liveMenuRect = menuCardCoords?.takeIf { it.isAttached }?.boundsInRoot() ?: menuCardRect
                onDustVanish(VanishSnapshot(rect = liveMenuRect, touchRoot = origin, bitmap = bmp))
                    shortcutMenuTarget = null
                    afterDismiss()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bm.id) {
                        detectTapGestures { offset -> dismissShortcutMenu(offset) }
                    }
            ) {
                HarmonyAnchoredMorphContainer(
                    anchorRect = anchorRect,
                    cardWidth = 226.dp,
                    fallbackTopPadding = 180.dp,
                    fallbackEndPadding = 80.dp
                ) { morphCorner ->
                    val cardShape = RoundedCornerShape(morphCorner.coerceAtMost(20.dp))
                    Surface(
                        shape = cardShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 14.dp,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords -> menuCardCoords = coords; menuCardRect = coords.boundsInRoot() }
                            .drawWithContent {
                                menuLayer.record { this@drawWithContent.drawContent() }
                                drawLayer(menuLayer)
                            }
                            .harmonyRimGlow(enabled = true, shape = cardShape, glowColor = MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), cardShape)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            listOf(
                                "새 탭에서 열기" to { onOpenInNewTab(bm.url) },
                                "시크릿 창으로 열기" to { onOpenInIncognito(bm.url) },
                                "링크 복사" to {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText(bm.title, bm.url))
                                    Toast.makeText(context, "링크 복사됨", Toast.LENGTH_SHORT).show()
                                },
                                "편집" to { onEditBookmark(bm) },
                                "삭제" to { onDeleteBookmark(bm) },
                                "이 바로가기 고정" to {
                                    Toast.makeText(context, "${bm.title} 바로가기 고정됨", Toast.LENGTH_SHORT).show()
                                }
                            ).forEach { (label, action) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { dismissShortcutMenu(afterDismiss = action) }
                                        .padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (label == "삭제") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        menuTarget?.let { target ->
            val activeSpaceId = SpaceManager.getActiveSpace().id
            ChromeNewsActionBottomSheet(
                target = target,
                onOpenInNewTab = {
                    SpaceManager.createTab(activeSpaceId, initialUrl = target.url, selectImmediately = true)
                    menuTarget = null
                },
                onOpenInGroup = {
                    SpaceManager.createTab(activeSpaceId, initialUrl = target.url, selectImmediately = false)
                    Toast.makeText(context, "새 백그라운드 탭에서 열림", Toast.LENGTH_SHORT).show()
                    menuTarget = null
                },
                onOpenInIncognito = {
                    SpaceManager.createTab(activeSpaceId, initialUrl = target.url, incognito = true, selectImmediately = true)
                    menuTarget = null
                },
                onAddToReadingList = {
                    SpaceManager.addToReadingList(target.title, target.url)
                    Toast.makeText(context, "읽기 목록에 추가됨", Toast.LENGTH_SHORT).show()
                    menuTarget = null
                },
                onHideArticle = {
                    hiddenUrls = hiddenUrls + target.url
                    Toast.makeText(context, "기사를 숨겼습니다", Toast.LENGTH_SHORT).show()
                    menuTarget = null
                },
                onBlockSource = {
                    blockedSources = blockedSources + target.source
                    Toast.makeText(context, "${target.source}의 콘텐츠를 표시하지 않습니다", Toast.LENGTH_SHORT).show()
                    menuTarget = null
                },
                onDismiss = { menuTarget = null }
            )
        }
    }
}

@Composable
private fun ChromeDiscoverNewsCard(
    article: NewsArticleItem,
    compactSideLayout: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onOpenMenu: () -> Unit
) {
    var imageBitmap by remember(article.imageUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(article.imageUrl) {
        if (article.imageUrl.isNotBlank()) {
            imageBitmap = DiscoverFeedRepository.loadArticleImage(article.imageUrl)
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            // M3 Borderless Tonal Card
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (compactSideLayout) {
                // 크롬 컴팩트형 카드 (좌측 큰 제목 + 우측 96dp 둥근 사진)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = article.title,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 23.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = article.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            } else {
                // 크롬 히어로형 카드 (상단 16:9 큰 사진 + 하단 17.5sp 굵은 제목)
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!.asImageBitmap(),
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9.5f)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    text = article.title,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 크롬 1:1 카드 푸터: [파비콘] [웹사이트명] · [상대시간]   [공유] [⋮]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookmarkMiniFavicon(
                    bm = BookmarkItem(title = article.source, url = article.sourceUrl),
                    sizeDp = 16
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "${article.source} · ${article.relativeTime}",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp)
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "공유",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    , symbolSize = 17.dp)
                }
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(32.dp)
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "뉴스 메뉴",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    , symbolSize = 18.dp)
                }
            }
        }
    }
}

@Composable
private fun ChromeDiscoverTrendCard(
    trend: TrendIdeaItem,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val badgeColor = if (trend.source.startsWith("Hacker")) Color(0xFFF97316) else EgoEmerald
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            // M3 Borderless Tonal Card
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = trend.title,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 23.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            // 화력 시각화 바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(trend.normalizedHeat)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(badgeColor.copy(alpha = 0.65f), badgeColor)))
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookmarkMiniFavicon(
                    bm = BookmarkItem(title = trend.source, url = trend.sourceUrl),
                    sizeDp = 16
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trend.source,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                EgoIcon(Icons.Default.ThumbUpOffAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp), symbolSize = 13.dp)
                Spacer(modifier = Modifier.width(2.dp))
                Text("${trend.score}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                EgoIcon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp), symbolSize = 13.dp)
                Spacer(modifier = Modifier.width(2.dp))
                Text("${trend.comments}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                    EgoIcon(Icons.Default.Share, contentDescription = "공유", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp), symbolSize = 16.dp)
                }
                IconButton(onClick = onOpenMenu, modifier = Modifier.size(30.dp)) {
                    EgoIcon(Icons.Default.MoreVert, contentDescription = "메뉴", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp), symbolSize = 17.dp)
                }
            }
        }
    }
}

@Composable
private fun ChromeNewsActionBottomSheet(
    target: FeedMenuTarget,
    onOpenInNewTab: () -> Unit,
    onOpenInGroup: () -> Unit,
    onOpenInIncognito: () -> Unit,
    onAddToReadingList: () -> Unit,
    onHideArticle: () -> Unit,
    onBlockSource: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))
                SheetActionRow(Icons.Default.AddBox, "새 탭에서 열기", onOpenInNewTab)
                SheetActionRow(Icons.Default.GridView, "새 탭을 그룹에서 열기", onOpenInGroup)
                SheetActionRow(Icons.Default.VisibilityOff, "시크릿 탭에서 열기", onOpenInIncognito)
                SheetActionRow(Icons.Default.BookmarkAdd, "읽기 목록에 추가", onAddToReadingList)
                SheetActionRow(Icons.Default.HighlightOff, "숨기기", onHideArticle)
                SheetActionRow(Icons.Default.Block, "${target.source}의 콘텐츠 표시 안함", onBlockSource)
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "닫기",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EgoIcon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        , symbolSize = 21.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DiscoverTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            // M3 Tonal Filter Chip
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EgoIcon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            , symbolSize = 16.dp)
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyFeedCard(message: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            // M3 Tonal Tile
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}
