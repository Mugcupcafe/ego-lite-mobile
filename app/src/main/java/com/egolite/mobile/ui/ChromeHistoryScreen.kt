package com.egolite.mobile.ui

import android.net.Uri
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.CardVanishStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val HISTORY_URL = "ego://history"

@Composable
fun ChromeHistoryScreen(
    history: List<BookmarkItem>,
    onNavigate: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onDustVanish: (VanishSnapshot) -> Unit = {}
) {
    val prefs by SpaceManager.preferences.collectAsState()
    val chromeColors = LocalEgoChromeColors.current
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var vanishingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    val filtered = remember(history, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) history
        else history.filter {
            it.title.lowercase(Locale.getDefault()).contains(q) ||
                it.url.lowercase(Locale.getDefault()).contains(q)
        }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { formatFullKoreanDateHeader(it.visitedAtMs) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = chromeColors.settingsCanvasBg
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1) 제목: 방문 기록 (크롬 1:1)
                item(key = "hist_header") {
                    Text(
                        text = "방문 기록",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 2) 방문 기록 검색 필 바 (크롬 1:1)
                item(key = "hist_search") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(chromeColors.settingsCardBg)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "방문 기록 검색",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                EgoIcon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "검색어 지우기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                    symbolSize = 16.dp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3) 인터넷 사용 기록 삭제... 링크 + 구분선 (크롬 1:1, 앱별 필터링 제외)
                item(key = "hist_clear_link") {
                    Text(
                        text = "인터넷 사용 기록 삭제...",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showClearConfirm = true }
                            .padding(vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (filtered.isEmpty()) {
                    item(key = "hist_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "방문 기록이 없습니다." else "'$searchQuery'에 일치하는 방문 기록이 없습니다.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    grouped.forEach { (dateLabel, entries) ->
                        item(key = "hdate_$dateLabel") {
                            Text(
                                text = dateLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
                            )
                        }
                        items(entries, key = { it.id }) { entry ->
                            val isVanishing = entry.id in vanishingIds
                            ChromeHistoryRow(
                                entry = entry,
                                isVanishing = isVanishing,
                                modifier = Modifier.animateItem(placementSpec = spring(dampingRatio = 0.88f, stiffness = 360f)),
                                onClick = { onNavigate(entry.url) },
                                onDeleteWithSnapshot = { snap ->
                                    if (prefs.cardVanishStyle == CardVanishStyle.OFF || isVanishing) {
                                        onRemoveItem(entry.id)
                                    } else {
                                        vanishingIds = vanishingIds + entry.id
                                        onDustVanish(snap)
                                        val waitMs = when (prefs.cardVanishStyle) {
                                            CardVanishStyle.HARMONY_DUST, CardVanishStyle.HARMONY_GPU_SHADER, CardVanishStyle.HARMONY_7_SAND_WAVE ->
                                                (prefs.vanishDurationMs * 0.42f).toLong().coerceIn(240L, 580L)
                                            CardVanishStyle.LIGHT_PARTICLES -> 280L
                                            CardVanishStyle.SCALE_FADE -> 180L
                                            CardVanishStyle.OFF -> 0L
                                        }
                                        scope.launch {
                                            delay(waitMs)
                                            onRemoveItem(entry.id)
                                            vanishingIds = vanishingIds - entry.id
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("인터넷 사용 기록 삭제", fontWeight = FontWeight.SemiBold) },
            text = { Text("저장된 모든 방문 기록을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    }
                ) {
                    Text("모두 삭제", color = EgoDangerRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun ChromeHistoryRow(
    entry: BookmarkItem,
    isVanishing: Boolean,
    onClick: () -> Unit,
    onDeleteWithSnapshot: (VanishSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    var rowRect by remember { mutableStateOf<Rect?>(null) }
    val captureLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val host = remember(entry.url) {
        runCatching { Uri.parse(entry.url).host.orEmpty() }.getOrDefault(entry.url)
    }
    val badgeColor = remember(host) { historyHostColor(host) }
    val badgeInitial = remember(host) {
        host.removePrefix("www.").removePrefix("m.").firstOrNull()?.uppercaseChar()?.toString() ?: "W"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isVanishing) 0f else 1f }
            .onGloballyPositioned { coords -> rowRect = coords.boundsInRoot() }
            .drawWithContent {
                captureLayer.record { this@drawWithContent.drawContent() }
                drawLayer(captureLayer)
            }
            .clickable(enabled = !isVanishing, onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeInitial,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = entry.title.ifBlank { host },
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = host,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(enabled = !isVanishing) {
                    scope.launch {
                        val bmp = runCatching { captureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                        val origin = rowRect?.let { Offset(it.right - 20f, it.center.y) }
                        onDeleteWithSnapshot(VanishSnapshot(rect = rowRect, touchRoot = origin, bitmap = bmp))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            EgoIcon(
                imageVector = Icons.Default.HighlightOff,
                contentDescription = "방문 기록 항목 삭제",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
                filled = true,
                symbolSize = 19.dp
            )
        }
    }
}

private fun historyHostColor(host: String): Color {
    val h = host.lowercase(Locale.getDefault())
    return when {
        "google" in h -> Color(0xFF4285F4)
        "github" in h -> Color(0xFF334155)
        "naver" in h -> Color(0xFF03C75A)
        "youtube" in h -> Color(0xFFEA4335)
        "huawei" in h || "harmony" in h -> Color(0xFFC8102E)
        "instagram" in h -> Color(0xFFE1306C)
        "codemagic" in h -> Color(0xFF2563EB)
        else -> {
            val palette = listOf(
                Color(0xFF3B82F6),
                Color(0xFF10B981),
                Color(0xFF8B5CF6),
                Color(0xFFF59E0B),
                Color(0xFFEC4899),
                Color(0xFF06B6D4)
            )
            palette[(h.hashCode().and(0x7FFFFFFF)) % palette.size]
        }
    }
}

internal fun formatFullKoreanDateHeader(ts: Long): String {
    val nowCal = Calendar.getInstance()
    val targetCal = Calendar.getInstance().apply { timeInMillis = ts }
    val fmt = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
    val dateStr = fmt.format(Date(ts))
    val sameYear = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
    val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - targetCal.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "오늘 - $dateStr"
        sameYear && dayDiff == 1 -> "어제 - $dateStr"
        else -> dateStr
    }
}
