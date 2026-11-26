package com.egolite.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.LongPressTarget
import kotlin.math.roundToInt

/**
 * 모바일 크롬 태블릿 1:1 롱프레스 컨텍스트 메뉴
 * - 배경 어두워짐(Dim) 없음 (완전 투명 백드롭)
 * - 터치한 위치 기준으로 팝업 생성 및 화면 경계 자동 보정
 */
@Composable
fun ChromeLongPressContextMenu(
    target: LongPressTarget,
    activeSpaceId: String,
    darkTheme: Boolean,
    onPreviewPage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
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

    // 크롬 주/야간 팝업 팔레트 1:1 매칭
    val cardBg = if (darkTheme) Color(0xFF282A2E) else Color(0xFFFFFFFF)
    val cardBorder = if (darkTheme) Color(0xFF3C4043) else Color(0xFFDADCE0)
    val avatarBg = if (darkTheme) Color(0xFF3C4043) else Color(0xFFE8EAED)
    val titleColor = if (darkTheme) Color(0xFFE8EAED) else Color(0xFF1F1F1F)
    val subColor = if (darkTheme) Color(0xFF9AA0A6) else Color(0xFF5F6368)
    val dividerColor = if (darkTheme) Color(0xFF3C4043) else Color(0xFFE0E3E7)

    var containerWinPos by remember { mutableStateOf(Offset.Zero) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords -> containerWinPos = coords.positionInWindow() }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        val containerW = constraints.maxWidth
        val containerH = constraints.maxHeight
        val marginPx = with(density) { 10.dp.roundToPx() }
        val touchOffsetPx = with(density) { 8.dp.roundToPx() }
        val fallbackMenuW = with(density) { 292.dp.roundToPx() }
        val effMenuW = if (menuSize.width > 0) menuSize.width else fallbackMenuW
        val effMenuH = if (menuSize.height > 0) menuSize.height else (containerH * 0.5f).roundToInt()

        val popupOffset = if (target.touchX >= 0f && target.touchY >= 0f) {
            val localX = (target.touchX - containerWinPos.x).roundToInt()
            val localY = (target.touchY - containerWinPos.y).roundToInt()
            val preferredX = if (localX + touchOffsetPx + effMenuW <= containerW - marginPx) {
                localX + touchOffsetPx
            } else {
                localX - effMenuW - touchOffsetPx
            }
            val preferredY = if (localY + touchOffsetPx + effMenuH <= containerH - marginPx) {
                localY + touchOffsetPx
            } else {
                localY - effMenuH - touchOffsetPx
            }
            val clampedX = preferredX.coerceIn(marginPx, (containerW - effMenuW - marginPx).coerceAtLeast(marginPx))
            val clampedY = preferredY.coerceIn(marginPx, (containerH - effMenuH - marginPx).coerceAtLeast(marginPx))
            IntOffset(clampedX, clampedY)
        } else {
            val cx = ((containerW - effMenuW) / 2).coerceAtLeast(marginPx)
            val cy = ((containerH - effMenuH) / 2).coerceAtLeast(marginPx)
            IntOffset(cx, cy)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(0.6.dp, cardBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .offset { popupOffset }
                .width(292.dp)
                .heightIn(max = (maxHeight - 24.dp).coerceAtLeast(240.dp))
                .onSizeChanged { menuSize = it }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                // 상단 헤더 (원형 이니셜 아이콘 + 제목 + 축약 URL)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarChar,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headerTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shortPath,
                            fontSize = 12.sp,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

                // 링크 항목 (모바일 크롬 10종 1:1)
                if (!target.linkUrl.isNullOrBlank()) {
                    val link = target.linkUrl
                    ContextMenuItemRow("새 탭에서 열기", titleColor) {
                        SpaceManager.createTab(activeSpaceId, initialUrl = link, selectImmediately = true)
                        onDismiss()
                    }
                    ContextMenuItemRow("새 탭을 그룹에서 열기", titleColor) {
                        SpaceManager.createTab(activeSpaceId, initialUrl = link, selectImmediately = false)
                        Toast.makeText(context, "백그라운드 탭에서 열림", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                    ContextMenuItemRow("새 창에서 열기", titleColor) {
                        val ai = SpaceManager.createAiSpace()
                        if (ai != null) {
                            SpaceManager.selectSpace(ai.id)
                            SpaceManager.navigate(ai.id, link)
                        } else {
                            SpaceManager.createTab(activeSpaceId, initialUrl = link, selectImmediately = true)
                        }
                        onDismiss()
                    }
                    ContextMenuItemRow("시크릿 창으로 열기", titleColor) {
                        SpaceManager.createTab(activeSpaceId, initialUrl = link, incognito = true, selectImmediately = true)
                        onDismiss()
                    }
                    ContextMenuItemRow("페이지 미리보기", titleColor) {
                        onDismiss()
                        onPreviewPage(link)
                    }
                    ContextMenuItemRow("링크 주소 복사", titleColor) {
                        copyToClipboard(context, "링크 주소", link)
                        onDismiss()
                    }
                    ContextMenuItemRow("링크 텍스트 복사", titleColor) {
                        copyToClipboard(context, "링크 텍스트", target.linkText?.ifBlank { link } ?: link)
                        onDismiss()
                    }
                    ContextMenuItemRow("링크 다운로드", titleColor) {
                        BrowserActivityBridge.enqueueDownload(context, link)
                        onDismiss()
                    }
                    ContextMenuItemRow("읽기 목록에 추가", titleColor) {
                        SpaceManager.addToReadingList(headerTitle, link)
                        Toast.makeText(context, "읽기 목록에 추가됨", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                    ContextMenuItemRow("링크 공유", titleColor) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, headerTitle)
                            putExtra(Intent.EXTRA_TEXT, link)
                        }
                        context.startActivity(Intent.createChooser(intent, "링크 공유"))
                        onDismiss()
                    }
                }

                // 이미지 항목
                if (!target.imageUrl.isNullOrBlank()) {
                    val img = target.imageUrl
                    if (!target.linkUrl.isNullOrBlank()) {
                        HorizontalDivider(color = dividerColor, thickness = 0.8.dp)
                    }
                    ContextMenuItemRow("새 탭에서 이미지 열기", titleColor) {
                        SpaceManager.createTab(activeSpaceId, initialUrl = img, selectImmediately = true)
                        onDismiss()
                    }
                    ContextMenuItemRow("이미지 다운로드", titleColor) {
                        BrowserActivityBridge.enqueueDownload(context, img)
                        onDismiss()
                    }
                    ContextMenuItemRow("이미지 주소 복사", titleColor) {
                        copyToClipboard(context, "이미지 주소", img)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItemRow(
    label: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
