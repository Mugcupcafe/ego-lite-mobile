package com.egolite.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.FaviconCache
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.LongPressTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * M3 Expressive 1+2 하이브리드 롱프레스 컨텍스트 메뉴
 * - 컴팩트 236dp 폭 + 분리 섹션 카드(외곽 20dp · 내측 6dp · 4dp 간격)
 * - 좌상단 34dp 썸네일 슬롯: 이미지 롱프레스 시 실제 이미지 축소판, 링크 전용 시 파비콘 자동 표시
 * - 등장: 터치 원점(TransformOrigin) 기준 M3 스프링 탄성 전개
 * - 퇴장: GraphicsLayer 실픽셀 캡처 -> 파티클 소멸 오버레이
 */
@Composable
fun ChromeLongPressContextMenu(
    target: LongPressTarget,
    activeSpaceId: String,
    darkTheme: Boolean,
    onPreviewPage: (String) -> Unit,
    onDismiss: () -> Unit,
    onDustDismiss: (VanishSnapshot) -> Unit = { onDismiss() }
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val rawUrl = target.linkUrl ?: target.imageUrl.orEmpty()
    val host = remember(rawUrl) {
        runCatching { Uri.parse(rawUrl).host?.removePrefix("www.").orEmpty() }.getOrDefault("")
    }
    val shortPath = remember(rawUrl) {
        runCatching {
            val u = Uri.parse(rawUrl)
            val h = u.host?.removePrefix("www.").orEmpty()
            val p = u.path.orEmpty()
            if (p.isNotBlank() && p != "/") "$h$p" else h
        }.getOrDefault(rawUrl)
    }
    val headerTitle = target.linkText?.trim()?.takeIf { it.isNotBlank() } ?: host.ifBlank { rawUrl }
    val avatarChar = host.firstOrNull()?.uppercaseChar()?.toString() ?: "W"

    // 좌상단 34dp 썸네일 비동기 로드 (이미지 URL 우선 -> 링크 파비콘 폴백)
    var thumbBitmap by remember(target.imageUrl, rawUrl) {
        mutableStateOf<Bitmap?>(if (target.imageUrl.isNullOrBlank()) FaviconCache.getMemoryCached(rawUrl) else null)
    }
    LaunchedEffect(target.imageUrl, rawUrl) {
        val imgUrl = target.imageUrl
        if (!imgUrl.isNullOrBlank()) {
            val loaded = loadContextMenuThumbnail(imgUrl, target.linkUrl ?: rawUrl)
            if (loaded != null) {
                thumbBitmap = loaded
                return@LaunchedEffect
            }
        }
        if (thumbBitmap == null && rawUrl.startsWith("http")) {
            thumbBitmap = FaviconCache.loadFavicon(context, rawUrl)
        }
    }

    // M3 32테마 자동 연동 색상 토큰
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val quickPillBg = MaterialTheme.colorScheme.surfaceContainer
    val cardBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.32f else 0.45f)
    val accentBg = MaterialTheme.colorScheme.primaryContainer
    val onAccentBg = MaterialTheme.colorScheme.onPrimaryContainer
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant

    var containerWinPos by remember { mutableStateOf(Offset.Zero) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    var menuCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuRect by remember { mutableStateOf<Rect?>(null) }
    val captureLayer = rememberGraphicsLayer()
    var dismissing by remember(target) { mutableStateOf(false) }

    // M3 Expressive Touch-Anchor Spring Unfold 등장 애니메이션
    val enterScale = remember(target) { Animatable(0.78f) }
    val enterAlpha = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        launch { enterScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)) }
        launch { enterAlpha.animateTo(1f, animationSpec = tween(160)) }
    }

    fun dismissWithParticles(afterAction: (() -> Unit)? = null) {
        if (dismissing) return
        dismissing = true
        scope.launch {
            val bmp = runCatching { captureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            val liveRect = menuCoords?.takeIf { it.isAttached }?.boundsInRoot() ?: menuRect
            val origin = if (target.touchX >= 0f && target.touchY >= 0f) {
                Offset(target.touchX, target.touchY)
            } else {
                liveRect?.center
            }
            onDustDismiss(VanishSnapshot(rect = liveRect, touchRoot = origin, bitmap = bmp))
            afterAction?.invoke()
        }
    }

    val hasLink = !target.linkUrl.isNullOrBlank()
    val hasImage = !target.imageUrl.isNullOrBlank()
    val saveTargetUrl = target.imageUrl ?: target.linkUrl.orEmpty()
    val copyTargetUrl = target.linkUrl ?: target.imageUrl.orEmpty()
    val copyTargetLabel = if (hasLink) "링크 주소" else "이미지 주소"

    val topCardShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    val midCardShape = RoundedCornerShape(6.dp)
    val botCardShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords -> containerWinPos = coords.positionInWindow() }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { dismissWithParticles() }
    ) {
        val containerW = constraints.maxWidth
        val containerH = constraints.maxHeight
        val marginPx = with(density) { 10.dp.roundToPx() }
        val touchOffsetPx = with(density) { 8.dp.roundToPx() }
        val fallbackMenuW = with(density) { 236.dp.roundToPx() }
        val effMenuW = if (menuSize.width > 0) menuSize.width else fallbackMenuW
        val effMenuH = if (menuSize.height > 0) menuSize.height else (containerH * 0.42f).roundToInt()

        val localTouchX = if (target.touchX >= 0f) (target.touchX - containerWinPos.x).roundToInt() else containerW / 2
        val localTouchY = if (target.touchY >= 0f) (target.touchY - containerWinPos.y).roundToInt() else containerH / 2

        val popupOffset = if (target.touchX >= 0f && target.touchY >= 0f) {
            val preferredX = if (localTouchX + touchOffsetPx + effMenuW <= containerW - marginPx) {
                localTouchX + touchOffsetPx
            } else {
                localTouchX - effMenuW - touchOffsetPx
            }
            val preferredY = if (localTouchY + touchOffsetPx + effMenuH <= containerH - marginPx) {
                localTouchY + touchOffsetPx
            } else {
                localTouchY - effMenuH - touchOffsetPx
            }
            val clampedX = preferredX.coerceIn(marginPx, (containerW - effMenuW - marginPx).coerceAtLeast(marginPx))
            val clampedY = preferredY.coerceIn(marginPx, (containerH - effMenuH - marginPx).coerceAtLeast(marginPx))
            IntOffset(clampedX, clampedY)
        } else {
            val cx = ((containerW - effMenuW) / 2).coerceAtLeast(marginPx)
            val cy = ((containerH - effMenuH) / 2).coerceAtLeast(marginPx)
            IntOffset(cx, cy)
        }

        val pivotX = ((localTouchX - popupOffset.x).toFloat() / effMenuW.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
        val pivotY = ((localTouchY - popupOffset.y).toFloat() / effMenuH.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .offset { popupOffset }
                .width(236.dp)
                .heightIn(max = (maxHeight - 24.dp).coerceAtLeast(220.dp))
                .onSizeChanged { menuSize = it }
                .onGloballyPositioned { coords ->
                    menuCoords = coords
                    menuRect = coords.boundsInRoot()
                }
                .drawWithContent {
                    captureLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(captureLayer)
                }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(pivotX, pivotY)
                    scaleX = enterScale.value
                    scaleY = enterScale.value
                    alpha = enterAlpha.value
                }
                .verticalScroll(rememberScrollState())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            // 1) 상단 헤더 카드 (좌상단 34dp 이미지 썸네일 / 파비콘 + 제목 + 축약 URL)
            Surface(
                shape = topCardShape,
                color = cardBg,
                border = BorderStroke(0.7.dp, cardBorder),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = thumbBitmap
                        if (bmp != null && !bmp.isRecycled) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = avatarChar,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = onAccentBg
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headerTitle,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = shortPath,
                            fontSize = 11.sp,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2) 상단 4-아이콘 퀵 액션 바 카드 (미리보기 · 복사 · 저장 · 공유)
            Surface(
                shape = if (!hasLink && !hasImage) botCardShape else midCardShape,
                color = cardBg,
                border = BorderStroke(0.7.dp, cardBorder),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QuickContextPill(
                        icon = Icons.Default.Visibility,
                        label = "미리보기",
                        bgColor = quickPillBg,
                        fgColor = titleColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        val previewTarget = target.linkUrl ?: target.imageUrl.orEmpty()
                        dismissWithParticles {
                            if (previewTarget.isNotBlank()) onPreviewPage(previewTarget)
                        }
                    }
                    QuickContextPill(
                        icon = Icons.Default.ContentCopy,
                        label = "복사",
                        bgColor = quickPillBg,
                        fgColor = titleColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        dismissWithParticles {
                            if (copyTargetUrl.isNotBlank()) copyToClipboard(context, copyTargetLabel, copyTargetUrl)
                        }
                    }
                    QuickContextPill(
                        icon = Icons.Default.Download,
                        label = "저장",
                        bgColor = quickPillBg,
                        fgColor = titleColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        dismissWithParticles {
                            if (saveTargetUrl.isNotBlank()) BrowserActivityBridge.enqueueDownload(context, saveTargetUrl)
                        }
                    }
                    QuickContextPill(
                        icon = Icons.Default.Share,
                        label = "공유",
                        bgColor = quickPillBg,
                        fgColor = titleColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        dismissWithParticles {
                            if (copyTargetUrl.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, headerTitle)
                                    putExtra(Intent.EXTRA_TEXT, copyTargetUrl)
                                }
                                context.startActivity(Intent.createChooser(intent, "공유"))
                            }
                        }
                    }
                }
            }

            // 3) 링크 액션 섹션 카드
            if (hasLink) {
                val link = target.linkUrl!!
                Surface(
                    shape = if (hasImage) midCardShape else botCardShape,
                    color = cardBg,
                    border = BorderStroke(0.7.dp, cardBorder),
                    shadowElevation = 10.dp
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        M3ContextMenuItemRow(Icons.Default.AddBox, "새 탭에서 열기", titleColor, subColor) {
                            dismissWithParticles {
                                SpaceManager.createTab(activeSpaceId, initialUrl = link, selectImmediately = true)
                            }
                        }
                        M3ContextMenuItemRow(Icons.Default.Layers, "그룹에서 열기", titleColor, subColor) {
                            dismissWithParticles {
                                SpaceManager.createTab(activeSpaceId, initialUrl = link, selectImmediately = false)
                                Toast.makeText(context, "백그라운드 탭에서 열림", Toast.LENGTH_SHORT).show()
                            }
                        }
                        M3ContextMenuItemRow(Icons.Default.OpenInNew, "새 AI 창에서 열기", titleColor, subColor) {
                            dismissWithParticles {
                                val ai = SpaceManager.createAiSpace()
                                if (ai != null) {
                                    SpaceManager.selectSpace(ai.id)
                                    SpaceManager.navigate(ai.id, link)
                                } else {
                                    SpaceManager.createTab(activeSpaceId, initialUrl = link, selectImmediately = true)
                                }
                            }
                        }
                        M3ContextMenuItemRow(Icons.Default.VisibilityOff, "시크릿 창으로 열기", titleColor, subColor) {
                            dismissWithParticles {
                                SpaceManager.createTab(activeSpaceId, initialUrl = link, incognito = true, selectImmediately = true)
                            }
                        }
                        M3ContextMenuItemRow(Icons.Default.BookmarkAdd, "읽기 목록에 추가", titleColor, subColor) {
                            dismissWithParticles {
                                SpaceManager.addToReadingList(headerTitle, link)
                                Toast.makeText(context, "읽기 목록에 추가됨", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            // 4) 이미지 액션 섹션 카드
            if (hasImage) {
                val img = target.imageUrl!!
                Surface(
                    shape = botCardShape,
                    color = cardBg,
                    border = BorderStroke(0.7.dp, cardBorder),
                    shadowElevation = 10.dp
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        M3ContextMenuItemRow(Icons.Default.Image, "새 탭에서 이미지 열기", titleColor, subColor) {
                            dismissWithParticles {
                                SpaceManager.createTab(activeSpaceId, initialUrl = img, selectImmediately = true)
                            }
                        }
                        M3ContextMenuItemRow(Icons.Default.Download, "이미지 다운로드", titleColor, subColor) {
                            dismissWithParticles {
                                BrowserActivityBridge.enqueueDownload(context, img)
                            }
                        }
                        M3ContextMenuItemRow(Icons.Default.ContentCopy, "이미지 주소 복사", titleColor, subColor) {
                            dismissWithParticles {
                                copyToClipboard(context, "이미지 주소", img)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickContextPill(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        EgoIcon(
            imageVector = icon,
            contentDescription = label,
            tint = fgColor,
            modifier = Modifier.size(16.dp),
            symbolSize = 16.dp
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun M3ContextMenuItemRow(
    icon: ImageVector,
    label: String,
    textColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EgoIcon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
            symbolSize = 16.dp
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private suspend fun loadContextMenuThumbnail(imageUrl: String, refererUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (imageUrl.startsWith("data:image", ignoreCase = true)) {
            val comma = imageUrl.indexOf(',')
            if (comma <= 0) return@runCatching null
            val bytes = Base64.decode(imageUrl.substring(comma + 1), Base64.DEFAULT)
            return@runCatching decodeDownsampledBitmap(bytes, 120)
        }
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) return@runCatching null
        val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 4500
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 16; K) AppleWebKit/537.36 Chrome/135.0.0.0 Mobile Safari/537.36")
            if (refererUrl.startsWith("http")) setRequestProperty("Referer", refererUrl)
            val cookie = runCatching { CookieManager.getInstance().getCookie(imageUrl) }.getOrNull()
            if (!cookie.isNullOrBlank()) setRequestProperty("Cookie", cookie)
        }
        try {
            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                decodeDownsampledBitmap(bytes, 120)
            } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

private fun decodeDownsampledBitmap(bytes: ByteArray, targetPx: Int): Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}

/**
 * 크롬 스타일 "페이지 미리보기" 하단 시트 오버레이
 */
@Composable
fun PagePreviewBottomSheet(
    url: String,
    onOpenInNewTab: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
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
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = url,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onOpenInNewTab(url) }) {
                        EgoIcon(Icons.Default.OpenInNew, contentDescription = "새 탭에서 열기")
                    }
                    IconButton(onClick = onDismiss) {
                        EgoIcon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
                HorizontalDivider()
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label 복사됨", Toast.LENGTH_SHORT).show()
}
