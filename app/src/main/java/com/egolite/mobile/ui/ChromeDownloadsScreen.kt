package com.egolite.mobile.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.egolite.mobile.browser.DownloadBannerState
import com.egolite.mobile.browser.DownloadCenter
import com.egolite.mobile.browser.DownloadRecord
import com.egolite.mobile.browser.DuplicateDownloadPrompt
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.CardVanishStyle
import com.egolite.mobile.model.ProgressBarStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

const val DOWNLOADS_URL = "ego://downloads"

/**
 * 크롬 1:1 상단 다운로드 상태 팝업
 */
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
                    val vt = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { vt.resetTracking() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            vt.addPosition(change.uptimeMillis, change.position)
                            scope.launch { swipeX.snapTo(swipeX.value + dragAmount) }
                        },
                        onDragEnd = {
                            val vx = vt.calculateVelocity().x
                            scope.launch {
                                if (abs(swipeX.value) >= dismissThresholdPx || abs(vx) >= 950f) {
                                    val dir = if (abs(vx) >= 950f) (if (vx >= 0f) 1f else -1f) else (if (swipeX.value >= 0f) 1f else -1f)
                                    val target = dir * dismissThresholdPx * 4.5f
                                    swipeX.animateTo(
                                        targetValue = target,
                                        animationSpec = EgoMotion.responsiveSpring(),
                                        initialVelocity = vx
                                    )
                                    onDismiss()
                                } else {
                                    swipeX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = EgoMotion.responsiveSpring(),
                                        initialVelocity = vx
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                swipeX.animateTo(0f, animationSpec = EgoMotion.responsiveSpring())
                            }
                        }
                    )
                }
                .clickable {
                    if (banner.isCompleted) onOpenFile(rec) else onOpenDetails()
                    onDismiss()
                }
        ) {
            val prefs by SpaceManager.preferences.collectAsState()
            val showDlProgress = prefs.progressShowInDownload && !banner.isCompleted
            val dlProg = banner.progress.coerceIn(0.05f, 0.99f)
            val dlSubText = if (banner.isCompleted) completedSub else {
                if (banner.totalBytes > 0L) {
                    "${DownloadCenter.formatSize(banner.downloadedBytes)} / ${DownloadCenter.formatSize(banner.totalBytes)} (${(dlProg * 100).roundToInt()}%)"
                } else {
                    "알림에서 다운로드 상태를 확인하세요."
                }
            }
            val invTextCol = if (isDark) Color(0xFF090D16) else Color.White

            Box(modifier = Modifier.fillMaxWidth()) {
                if (showDlProgress && prefs.progressBarStyle != ProgressBarStyle.MATERIAL) {
                    HarmonyHorizontalProgressCanvas(
                        style = prefs.progressBarStyle,
                        progress = dlProg,
                        isActive = true,
                        accentColor = MaterialTheme.colorScheme.primary,
                        lightIntensity = prefs.progressLightIntensity,
                        particleDensity = prefs.progressParticleDensity,
                        cornerRadiusDp = 22.dp,
                        modifier = Modifier.matchParentSize()
                    )
                }

                @Composable
                fun BannerContentRow(titleCol: Color, subCol: Color, actionCol: Color, iconCol: Color) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EgoIcon(
                            imageVector = if (banner.isCompleted) Icons.Default.DownloadDone else Icons.Default.Download,
                            contentDescription = null,
                            tint = iconCol,
                            modifier = Modifier.size(22.dp),
                            symbolSize = 22.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (banner.isCompleted) "다운로드 1개 완료" else "파일 ${banner.activeCount}개 다운로드 중...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = titleCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dlSubText,
                                fontSize = 12.5.sp,
                                color = subCol,
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
                                color = actionCol
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
                                tint = iconCol,
                                modifier = Modifier.size(16.dp),
                                symbolSize = 16.dp
                            )
                        }
                    }
                }

                BannerContentRow(
                    titleCol = MaterialTheme.colorScheme.onSurface,
                    subCol = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionCol = MaterialTheme.colorScheme.primary,
                    iconCol = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HarmonyModeBClippedOverlay(
                    enabled = showDlProgress && prefs.progressBarStyle == ProgressBarStyle.HARMONY_OPTICAL,
                    progress = dlProg,
                    isActive = true
                ,
                    modifier = Modifier.matchParentSize()
                ) {
                    BannerContentRow(
                        titleCol = invTextCol,
                        subCol = invTextCol.copy(alpha = 0.85f),
                        actionCol = invTextCol,
                        iconCol = invTextCol
                    )
                }

                if (showDlProgress && prefs.progressBarStyle == ProgressBarStyle.MATERIAL) {
                    LinearProgressIndicator(
                        progress = { dlProg },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary
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
    val linkColor = MaterialTheme.colorScheme.primary
    val sizeStr = DownloadCenter.formatSize(prompt.existingSizeBytes).ifBlank { "" }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            shadowElevation = 10.dp,
            modifier = Modifier.widthIn(min = 300.dp, max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "파일을 다시 다운로드하시겠습니까?",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val bodyText = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(prompt.fileName)
                    }
                    if (sizeStr.isNotBlank()) append(" ($sizeStr)")
                    append("을(를) 다시 다운로드하시겠습니까?")
                }
                Text(
                    text = bodyText,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        if (prompt.existingUri.isNotBlank()) {
                            DownloadCenter.openFile(context, prompt.existingUri, prompt.mimeType, prompt.fileName, forceChooser = true)
                            onDismiss()
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = {
                            val uniqueName = DownloadCenter.makeUniqueFileName(context, prompt.fileName)
                            onDismiss()
                            prompt.onConfirmRedownload(uniqueName)
                        }
                    ) {
                        Text(
                            "다시 다운로드",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = linkColor
                        )
                    }
                }
            }
        }
    }
}

private enum class DownloadFilter(val label: String) {
    ALL("전체"),
    VIDEO("동영상"),
    IMAGE("이미지"),
    OTHER("기타")
}

private fun isImageRecord(rec: DownloadRecord): Boolean {
    val ext = rec.fileName.substringAfterLast('.', "").lowercase(Locale.US)
    return rec.mimeType.startsWith("image/", true) || ext in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "avif")
}

private fun isVideoRecord(rec: DownloadRecord): Boolean {
    val ext = rec.fileName.substringAfterLast('.', "").lowercase(Locale.US)
    return rec.mimeType.startsWith("video/", true) || ext in setOf("mp4", "mkv", "webm", "mov", "avi", "m4v")
}

private fun queryDeviceStorageSummary(records: List<DownloadRecord>): String {
    val downloadedBytes = records.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    val totalGb = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
    }.getOrDefault(0.0)
    val usedStr = when {
        downloadedBytes >= 1024L * 1024L * 1024L ->
            String.format(Locale.US, "%.2fGB", downloadedBytes / (1024.0 * 1024.0 * 1024.0))
        downloadedBytes >= 1024L * 1024L ->
            String.format(Locale.US, "%.1fMB", downloadedBytes / (1024.0 * 1024.0))
        downloadedBytes >= 1024L ->
            String.format(Locale.US, "%.1fKB", downloadedBytes / 1024.0)
        else -> "${downloadedBytes}B"
    }
    return if (totalGb > 0.1) {
        String.format(Locale.US, "%.2fGB 중 %s 사용 중", totalGb, usedStr)
    } else {
        "$usedStr 사용 중"
    }
}

private fun loadDownloadThumbnail(context: Context, rec: DownloadRecord): Bitmap? {
    if (rec.contentUri.isBlank()) return null
    val uri = runCatching { Uri.parse(rec.contentUri) }.getOrNull() ?: return null
    return if (isVideoRecord(rec)) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            frame
        }.getOrNull()
    } else {
        runCatching {
            val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, optsBounds)
            }
            val maxDim = maxOf(optsBounds.outWidth, optsBounds.outHeight)
            var sample = 1
            while (maxDim / sample > 560) sample *= 2
            val optsDecode = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, optsDecode)
            }
        }.getOrNull()
    }
}

/**
 * 크롬 태블릿 1:1 다운로드 관리 페이지 (ego://downloads)
 * - 중앙 정렬 컬럼(좌우 여백) · 우측 설정 톱니 · 검색 바 · 용량 표기 · 전체/동영상/이미지/기타 칩
 * - 이미지/동영상 2열 정사각형 레터박스(ContentScale.Fit) + 동영상 재생 버튼 오버레이
 * - 온전한 날짜 표기("오늘 - 2026년 11월 26일") + Material 3 파일 아이콘 + 삭제 시 파티클 소멸 연동
 */
@Composable
fun ChromeDownloadsScreen(
    onDismiss: () -> Unit,
    embeddedInTab: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onDustVanish: (VanishSnapshot) -> Unit = {}
) {
    val context = LocalContext.current
    val records by DownloadCenter.records.collectAsState()
    val prefs by SpaceManager.preferences.collectAsState()
    val dlChrome = LocalEgoChromeColors.current
    val canvasBg = dlChrome.settingsCanvasBg
    val cardBg = dlChrome.settingsCardBg
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }
    var vanishingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var previewMedia by remember { mutableStateOf<Triple<DownloadRecord, Bitmap?, Rect>?>(null) }
    var containerRootRect by remember { mutableStateOf<Rect?>(null) }
    val lightboxAnim = remember { Animatable(0f) }

    val storageSummary = remember(records) { queryDeviceStorageSummary(records) }

    val filtered = remember(records, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        records.filter { rec ->
            val matchQuery = q.isEmpty() ||
                rec.fileName.lowercase(Locale.getDefault()).contains(q) ||
                rec.sourceHost.lowercase(Locale.getDefault()).contains(q)
            val isImg = isImageRecord(rec)
            val isVid = isVideoRecord(rec)
            val matchFilter = when (selectedFilter) {
                DownloadFilter.ALL -> true
                DownloadFilter.VIDEO -> isVid
                DownloadFilter.IMAGE -> isImg
                DownloadFilter.OTHER -> !isImg && !isVid
            }
            matchQuery && matchFilter
        }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { formatFullKoreanDateHeader(it.timestampMs) }
    }

    fun triggerDeleteWithDust(rec: DownloadRecord, snap: VanishSnapshot) {
        if (prefs.cardVanishStyle == CardVanishStyle.OFF || rec.id in vanishingIds) {
            DownloadCenter.deleteRecord(rec.id)
            return
        }
        vanishingIds = vanishingIds + rec.id
        onDustVanish(snap)
        val waitMs = when (prefs.cardVanishStyle) {
            CardVanishStyle.HARMONY_DUST, CardVanishStyle.HARMONY_GPU_SHADER, CardVanishStyle.HARMONY_7_SAND_WAVE ->
                (prefs.vanishDurationMs * 0.44f).toLong().coerceIn(250L, 600L)
            CardVanishStyle.LIGHT_PARTICLES -> 280L
            CardVanishStyle.SCALE_FADE -> 180L
            CardVanishStyle.OFF -> 0L
        }
        scope.launch {
            delay(waitMs)
            DownloadCenter.deleteRecord(rec.id)
            vanishingIds = vanishingIds - rec.id
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = canvasBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords -> containerRootRect = coords.boundsInRoot() },
            contentAlignment = Alignment.TopCenter
        ) {
            val dlElastic = rememberElasticOverscroll(isVertical = true, maxPullPx = 88f)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .then(dlElastic.containerModifier),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp + dlElastic.extraSpacingDp)
            ) {
                // 1) 제목("다운로드") + 우측 설정 톱니바퀴 (크롬 1:1)
                item(key = "dl_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!embeddedInTab) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable { onDismiss() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "닫기",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "다운로드",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "다운로드 설정",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp),
                                symbolSize = 21.dp
                            )
                        }
                    }
                }

                // 2) 다운로드 항목 검색 바 (크롬 1:1)
                item(key = "dl_search") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "다운로드 항목 검색",
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
                                    contentDescription = "지우기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                    symbolSize = 16.dp
                                )
                            }
                        }
                    }
                }

                // 3) 용량 표기 ("485.07GB 중 1.97GB 사용 중")
                item(key = "dl_storage") {
                    Text(
                        text = storageSummary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )
                }

                // 4) 크롬 1:1 필터 칩 4종: 전체 · 동영상 · 이미지 · 기타
                item(key = "dl_filters") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DownloadFilter.entries.forEach { f ->
                            val sel = selectedFilter == f
                            val chipIcon = when (f) {
                                DownloadFilter.ALL -> Icons.Default.Check
                                DownloadFilter.VIDEO -> Icons.Default.VideoFile
                                DownloadFilter.IMAGE -> Icons.Default.Image
                                DownloadFilter.OTHER -> Icons.Default.InsertDriveFile
                            }
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .then(
                                        if (!sel) Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                            RoundedCornerShape(8.dp)
                                        ) else Modifier
                                    )
                                    .clickable { selectedFilter = f }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (sel || f != DownloadFilter.ALL) {
                                    EgoIcon(
                                        imageVector = chipIcon,
                                        contentDescription = null,
                                        tint = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp),
                                        symbolSize = 15.dp
                                    )
                                }
                                Text(
                                    text = f.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                                    color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 5) 날짜별 그룹 (이미지/동영상은 2열 정사각형 레터박스, 기타 파일은 M3 아이콘 리스트)
                if (filtered.isEmpty()) {
                    item(key = "dl_empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(44.dp),
                                symbolSize = 44.dp
                            )
                            Text(
                                text = if (searchQuery.isBlank()) "다운로드한 파일이 없습니다." else "'$searchQuery'에 일치하는 파일이 없습니다.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    grouped.forEach { (dateLabel, groupItems) ->
                        item(key = "dl_date_$dateLabel") {
                            Text(
                                text = dateLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        val mediaItems = groupItems.filter { isImageRecord(it) || isVideoRecord(it) }
                        val fileItems = groupItems.filter { !isImageRecord(it) && !isVideoRecord(it) }

                        // 이미지/동영상 2열 정사각형 그리드 (레터박스로 전체 표현 + 동영상 재생 버튼)
                        val mediaRows = mediaItems.chunked(2)
                        items(mediaRows, key = { row -> "mgrid_${row.joinToString("_") { it.id }}" }) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(placementSpec = spring(dampingRatio = 0.88f, stiffness = 360f)),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                pair.forEach { rec ->
                                    ChromeMediaDownloadTile(
                                        record = rec,
                                        isVanishing = rec.id in vanishingIds,
                                        onOpenPreview = { bmp, rect ->
                                            if (isImageRecord(rec) && rect != null) {
                                                previewMedia = Triple(rec, bmp, rect)
                                                scope.launch {
                                                    lightboxAnim.snapTo(0f)
                                                    lightboxAnim.animateTo(1f, animationSpec = EgoMotion.morphSpring())
                                                }
                                            } else {
                                                DownloadCenter.openFile(context, rec.contentUri, rec.mimeType, rec.fileName, forceChooser = true)
                                            }
                                        },
                                        onOpen = { DownloadCenter.openFile(context, rec.contentUri, rec.mimeType, rec.fileName, forceChooser = true) },
                                        onShare = { DownloadCenter.shareFile(context, rec) },
                                        onDeleteWithSnapshot = { snap -> triggerDeleteWithDust(rec, snap) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // 기타 파일 행 (M3 파일 형식 아이콘 + ⋮ 메뉴)
                        items(fileItems, key = { it.id }) { rec ->
                            ChromeFileDownloadRow(
                                record = rec,
                                isVanishing = rec.id in vanishingIds,
                                modifier = Modifier.animateItem(placementSpec = spring(dampingRatio = 0.88f, stiffness = 360f)),
                                onOpen = { DownloadCenter.openFile(context, rec.contentUri, rec.mimeType, rec.fileName, forceChooser = true) },
                                onShare = { DownloadCenter.shareFile(context, rec) },
                                onDeleteWithSnapshot = { snap -> triggerDeleteWithDust(rec, snap) }
                            )
                        }
                    }
                }
            }

            // 일경도저 이미지 타일 -> 확대 라이트박스 연속 모핑 미리보기
            previewMedia?.let { (rec, bmp, fromRect) ->
                val cRect = containerRootRect
                if (cRect != null) {
                    val p = lightboxAnim.value.coerceIn(0f, 1f)
                    val densityLocal = LocalDensity.current
                    val targetW = (cRect.width * 0.86f).coerceAtMost(with(densityLocal) { 680.dp.toPx() })
                    val targetH = (cRect.height * 0.78f).coerceAtMost(with(densityLocal) { 560.dp.toPx() })
                    val targetLeft = (cRect.width - targetW) / 2f
                    val targetTop = (cRect.height - targetH) / 2f

                    val startLeft = fromRect.left - cRect.left
                    val startTop = fromRect.top - cRect.top
                    val curLeft = startLeft + (targetLeft - startLeft) * p
                    val curTop = startTop + (targetTop - startTop) * p
                    val curW = fromRect.width + (targetW - fromRect.width) * p
                    val curH = fromRect.height + (targetH - fromRect.height) * p
                    val curCorner = (4f + 20f * p).dp

                    fun closeLightbox() {
                        scope.launch {
                            lightboxAnim.animateTo(0f, animationSpec = EgoMotion.responsiveSpring())
                            previewMedia = null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.68f * p))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { closeLightbox() }
                            .zIndex(80f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(curCorner),
                            color = Color(0xFF1A1D24),
                            shadowElevation = (18f * p).dp,
                            modifier = Modifier
                                .offset { IntOffset(curLeft.toInt(), curTop.toInt()) }
                                .width(with(densityLocal) { curW.toDp() })
                                .height(with(densityLocal) { curH.toDp() })
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {}
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (bmp != null && !bmp.isRecycled) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = rec.fileName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(bottom = 52.dp)
                                    )
                                }
                                if (p > 0.35f) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rec.fileName,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = {
                                            DownloadCenter.openFile(context, rec.contentUri, rec.mimeType, rec.fileName, forceChooser = true)
                                        }) {
                                            Text("열기", color = Color(0xFFA8C7FA), fontSize = 13.sp)
                                        }
                                        TextButton(onClick = {
                                            DownloadCenter.shareFile(context, rec)
                                        }) {
                                            Text("공유", color = Color.White, fontSize = 13.sp)
                                        }
                                        TextButton(onClick = { closeLightbox() }) {
                                            Text("닫기", color = Color(0xFFD1D5DB), fontSize = 13.sp)
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
private fun ChromeMediaDownloadTile(
    record: DownloadRecord,
    isVanishing: Boolean,
    onOpenPreview: (Bitmap?, Rect?) -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDeleteWithSnapshot: (VanishSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVideo = remember(record.id) { isVideoRecord(record) }
    var thumb by remember(record.id, record.contentUri) { mutableStateOf<Bitmap?>(null) }
    var tileRect by remember { mutableStateOf<Rect?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val captureLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    LaunchedEffect(record.id, record.contentUri) {
        thumb = withContext(Dispatchers.IO) { loadDownloadThumbnail(context, record) }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { alpha = if (isVanishing) 0f else 1f }
            .onGloballyPositioned { coords -> tileRect = coords.boundsInRoot() }
            .drawWithContent {
                captureLayer.record { this@drawWithContent.drawContent() }
                drawLayer(captureLayer)
            }
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF9EA2AA))
            .clickable(enabled = !isVanishing) { onOpenPreview(thumb, tileRect) },
        contentAlignment = Alignment.Center
    ) {
        val bmp = thumb
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = record.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            EgoIcon(
                imageVector = if (isVideo) Icons.Default.VideoFile else Icons.Default.Image,
                contentDescription = record.fileName,
                tint = Color(0xFF2B2F38),
                modifier = Modifier.size(36.dp),
                symbolSize = 36.dp
            )
        }

        // 동영상 중앙 재생 버튼 오버레이 (크롬 1:1)
        if (isVideo) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.56f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        // 우상단 ⋮ 메뉴 (열기 / 공유 / 삭제)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(enabled = !isVanishing) { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                EgoIcon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "항목 메뉴",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                    symbolSize = 16.dp
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(text = { Text("열기") }, onClick = { showMenu = false; onOpen() })
                DropdownMenuItem(text = { Text("공유") }, onClick = { showMenu = false; onShare() })
                DropdownMenuItem(
                    text = { Text("삭제", color = EgoDangerRed) },
                    onClick = {
                        showMenu = false
                        scope.launch {
                            val captured = runCatching { captureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull() ?: thumb
                            val origin = tileRect?.let { Offset(it.right - 24f, it.top + 24f) }
                            onDeleteWithSnapshot(VanishSnapshot(rect = tileRect, touchRoot = origin, bitmap = captured))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChromeFileDownloadRow(
    record: DownloadRecord,
    isVanishing: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDeleteWithSnapshot: (VanishSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    var rowRect by remember { mutableStateOf<Rect?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val captureLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    val ext = record.fileName.substringAfterLast('.', "").lowercase(Locale.US)
    val fileIcon = when {
        record.mimeType.startsWith("audio/", true) || ext in setOf("mp3", "wav", "ogg", "m4a", "flac") -> Icons.Default.AudioFile
        ext in setOf("zip", "rar", "7z", "tar", "gz", "apk", "aab") -> Icons.Default.Archive
        ext in setOf("pdf", "txt", "log", "md", "doc", "docx", "csv", "json", "xml", "html") -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
    val sizeStr = DownloadCenter.formatSize(record.sizeBytes)
    val subText = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (record.sourceHost.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(record.sourceHost)
        }
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
            .clickable(enabled = !isVanishing, onClick = onOpen)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            EgoIcon(
                imageVector = fileIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
                filled = true,
                symbolSize = 18.dp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = record.fileName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subText.isNotBlank()) {
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isVanishing) { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                EgoIcon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "항목 메뉴",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                    symbolSize = 19.dp
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(text = { Text("열기") }, onClick = { showMenu = false; onOpen() })
                DropdownMenuItem(text = { Text("공유") }, onClick = { showMenu = false; onShare() })
                DropdownMenuItem(
                    text = { Text("삭제", color = EgoDangerRed) },
                    onClick = {
                        showMenu = false
                        scope.launch {
                            val bmp = runCatching { captureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                            val origin = rowRect?.let { Offset(it.right - 20f, it.center.y) }
                            onDeleteWithSnapshot(VanishSnapshot(rect = rowRect, touchRoot = origin, bitmap = bmp))
                        }
                    }
                )
            }
        }
    }
}
