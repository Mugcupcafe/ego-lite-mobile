package com.egolite.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.egolite.mobile.browser.DownloadBannerState
import com.egolite.mobile.browser.DownloadCenter
import com.egolite.mobile.browser.DownloadRecord
import com.egolite.mobile.browser.DownloadStatus
import com.egolite.mobile.browser.DuplicateDownloadPrompt
import java.util.Calendar

const val DOWNLOADS_URL = "ego://downloads"

@Composable
fun ChromeDownloadStatusBanner(
    banner: DownloadBannerState,
    onOpenDetails: () -> Unit,
    onOpenFile: (DownloadRecord) -> Unit,
    onDismiss: () -> Unit,
    onDustDismiss: (VanishSnapshot) -> Unit = { onDismiss() },
    modifier: Modifier = Modifier
) {
    var bannerRect by remember { mutableStateOf<Rect?>(null) }
    val bannerCaptureLayer = rememberGraphicsLayer()
    var dismissedViaDust by remember(banner.visible, banner.record?.id) { mutableStateOf(false) }
    AnimatedVisibility(
        visible = banner.visible && banner.record != null,
        enter = slideInVertically(EgoMotion.intOffsetSpring) { -it } + fadeIn(tween(180)),
        exit = if (dismissedViaDust) ExitTransition.None else (slideOutVertically(tween(170)) { -it } + fadeOut(tween(140))),
        modifier = modifier
    ) {
        val rec = banner.record ?: return@AnimatedVisibility
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val cardBg = if (isDark) Color(0xFF292C33) else Color(0xFFF3F6FC)
        val sizeStr = DownloadCenter.formatSize(rec.sizeBytes)
        val completedSub = buildString {
            if (sizeStr.isNotBlank()) append("($sizeStr) ")
            append(rec.sourceHost.ifBlank { rec.fileName })
        }

        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val dismissThresholdPx = with(density) { 92.dp.toPx() }
        val swipeX = remember(rec.id, banner.isCompleted) { Animatable(0f) }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .onGloballyPositioned { coords -> bannerRect = coords.boundsInRoot() }
                .drawWithContent {
                    bannerCaptureLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(bannerCaptureLayer)
                }
                .graphicsLayer {
                    translationX = swipeX.value
                    alpha = (1f - (abs(swipeX.value) / (dismissThresholdPx * 2.2f))).coerceIn(0.15f, 1f)
                }
                .pointerInput(rec.id, banner.isCompleted) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { swipeX.snapTo(swipeX.value + dragAmount) }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (abs(swipeX.value) >= dismissThresholdPx) {
                                    val target = if (swipeX.value >= 0f) dismissThresholdPx * 4.5f else -dismissThresholdPx * 4.5f
                                    swipeX.animateTo(target, animationSpec = tween(160))
                                    onDismiss()
                                } else {
                                    swipeX.animateTo(0f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                swipeX.animateTo(0f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f))
                            }
                        }
                    )
                }
                .clickable {
                    if (banner.isCompleted) onOpenFile(rec) else onOpenDetails()
                    onDismiss()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EgoIcon(
                    imageVector = if (banner.isCompleted) Icons.Default.DownloadDone else Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                , symbolSize = 22.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (banner.isCompleted) "다운로드 1개 완료" else "파일 ${banner.activeCount}개 다운로드 중...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (banner.isCompleted) completedSub else "알림에서 다운로드 상태를 확인하세요.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                TextButton(
                    onClick = {
                        if (banner.isCompleted) onOpenFile(rec) else onOpenDetails()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = if (banner.isCompleted) "열기" else "세부정보",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            dismissedViaDust = true
                            scope.launch {
                                val bmp = runCatching { bannerCaptureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                                val origin = bannerRect?.let { Offset(it.right - 22f, it.center.y) }
                                onDustDismiss(VanishSnapshot(rect = bannerRect, touchRoot = origin, bitmap = bmp))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "다운로드 알림 닫기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                        symbolSize = 16.dp
                    )
                }
            }
        }
    }
}

@Composable
fun ChromeDuplicateDownloadDialog(
    prompt: DuplicateDownloadPrompt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dialogBg = if (isDark) Color(0xFF292C33) else Color(0xFFF3F6FC)
    val sizeText = DownloadCenter.formatSize(prompt.existingSizeBytes)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "파일을 다시 다운로드하시겠습니까?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prompt.fileName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable {
                                DownloadCenter.openFile(context, prompt.existingUri, prompt.mimeType)
                                onDismiss()
                            }
                    )
                    Text(
                        text = if (sizeText.isNotBlank()) " ($sizeText) 파일을 다시 다운로드하시겠습니까?" else " 파일을 다시 다운로드하시겠습니까?",
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val uniqueName = DownloadCenter.makeUniqueFileName(context, prompt.fileName)
                            prompt.onConfirmRedownload(uniqueName)
                        }
                    ) {
                        Text("다시 다운로드", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChromeDownloadsScreen(
    embeddedInTab: Boolean = false,
    onDismiss: () -> Unit
) {
    if (!embeddedInTab) {
        BackHandler { onDismiss() }
    }
    val context = LocalContext.current
    val records by DownloadCenter.records.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("전체") }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dlChrome = LocalEgoChromeColors.current
    val canvasBg = dlChrome.settingsCanvasBg
    val cardBg = dlChrome.settingsCardBg

    val filters = listOf("전체", "이미지", "문서", "압축/앱", "미디어")
    val filtered = remember(records, searchQuery, selectedFilter) {
        records.filter { r ->
            val matchQuery = searchQuery.isBlank() ||
                r.fileName.contains(searchQuery, ignoreCase = true) ||
                r.sourceHost.contains(searchQuery, ignoreCase = true)
            val m = r.mimeType.lowercase()
            val n = r.fileName.lowercase()
            val matchFilter = when (selectedFilter) {
                "이미지" -> m.startsWith("image/") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")
                "문서" -> m.contains("pdf") || m.startsWith("text/") || n.endsWith(".pdf") || n.endsWith(".txt") || n.endsWith(".csv") || n.endsWith(".docx")
                "압축/앱" -> m.contains("zip") || m.contains("android.package") || n.endsWith(".zip") || n.endsWith(".apk")
                "미디어" -> m.startsWith("video/") || m.startsWith("audio/") || n.endsWith(".mp4") || n.endsWith(".mp3")
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    val grouped = remember(filtered) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterdayStart = todayStart - 24L * 60L * 60L * 1000L
        filtered.groupBy { r ->
            when {
                r.timestampMs >= todayStart -> "오늘"
                r.timestampMs >= yesterdayStart -> "어제"
                else -> "이전"
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = canvasBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!embeddedInTab) {
                    IconButton(onClick = onDismiss) {
                        EgoIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "닫기")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "다운로드",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(18.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EgoIcon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("다운로드 파일 검색", fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        EgoIcon(
                            Icons.Default.Close,
                            contentDescription = "지우기",
                            modifier = Modifier.size(18.dp).clickable { searchQuery = "" },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        , symbolSize = 18.dp)
                    }
                }
                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { DownloadCenter.clearAllRecords() }) {
                        Text("모두 비우기", fontSize = 13.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    val sel = selectedFilter == f
                    Surface(
                        shape = CircleShape,
                        color = if (sel) MaterialTheme.colorScheme.primaryContainer else cardBg,
                        modifier = Modifier.clickable { selectedFilter = f }
                    ) {
                        Text(
                            text = f,
                            fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EgoIcon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        , symbolSize = 44.dp)
                        Text(
                            text = "다운로드한 파일이 여기에 표시됩니다",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    grouped.forEach { (dateLabel, itemsInGroup) ->
                        item(key = "hdr_$dateLabel") {
                            Text(
                                text = dateLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 6.dp)
                            )
                        }
                        itemsIndexed(itemsInGroup, key = { _, it -> it.id }) { idx, rec ->
                            val shape = when {
                                itemsInGroup.size == 1 -> RoundedCornerShape(20.dp)
                                idx == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                idx == itemsInGroup.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                else -> RoundedCornerShape(4.dp)
                            }
                            val fileIcon = when {
                                rec.mimeType.startsWith("image/") -> Icons.Default.Image
                                rec.mimeType.startsWith("video/") -> Icons.Default.VideoFile
                                rec.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
                                rec.mimeType.contains("zip") || rec.fileName.endsWith(".apk", true) -> Icons.Default.Archive
                                rec.mimeType.contains("pdf") || rec.mimeType.startsWith("text/") -> Icons.Default.Description
                                else -> Icons.Default.InsertDriveFile
                            }
                            val sizeStr = DownloadCenter.formatSize(rec.sizeBytes)
                            val metaLine = buildString {
                                if (rec.status == DownloadStatus.DOWNLOADING) {
                                    append("다운로드 중...")
                                } else if (rec.status == DownloadStatus.FAILED) {
                                    append("다운로드 실패")
                                } else {
                                    if (sizeStr.isNotBlank()) append("$sizeStr · ")
                                    append(rec.sourceHost.ifBlank { "로컬 저장됨" })
                                }
                            }

                            Surface(
                                shape = shape,
                                color = cardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        DownloadCenter.openFile(context, rec.contentUri, rec.mimeType)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EgoIcon(
                                            imageVector = fileIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        , symbolSize = 20.dp)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = rec.fileName,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = metaLine,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (rec.contentUri.isNotBlank()) {
                                        IconButton(onClick = { DownloadCenter.shareFile(context, rec) }) {
                                            EgoIcon(Icons.Default.Share, contentDescription = "공유", modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 19.dp)
                                        }
                                    }
                                    IconButton(onClick = { DownloadCenter.deleteRecord(rec.id) }) {
                                        EgoIcon(Icons.Default.DeleteOutline, contentDescription = "목록에서 삭제", modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 19.dp)
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
