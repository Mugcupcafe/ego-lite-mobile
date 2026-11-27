package com.egolite.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.EgoWebViewFactory
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.mcp.OAuth21Manager
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.TunnelProvider
import com.egolite.mobile.tunnel.EgoForegroundService

@Composable
fun McpSettingsDrawer(
    anchorRect: Rect? = null,
    onDismiss: (VanishSnapshot) -> Unit
) {
    val context = LocalContext.current
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()
    val tunnelSettings by EgoForegroundService.currentTunnelSettings.collectAsState()
    val prefs by SpaceManager.preferences.collectAsState()
    val chromiumVersion = remember { EgoWebViewFactory.getCurrentChromiumVersion(context) }

    val isRunning = runtimeState.connectionState == McpConnectionState.CONNECTED ||
        runtimeState.connectionState == McpConnectionState.STARTING

    var cardVisible by remember { mutableStateOf(false) }
    var cardRect by remember { mutableStateOf<Rect?>(null) }
    val captureLayer = rememberGraphicsLayer()
    val captureScope = rememberCoroutineScope()
    fun dismissWithCapture(touchRoot: Offset?) {
        captureScope.launch {
            val bmp = runCatching { captureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            onDismiss(VanishSnapshot(rect = cardRect, touchRoot = touchRoot, bitmap = bmp))
        }
    }
    var showLogs by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset -> dismissWithCapture(offset) }
            }
    ) {
        HarmonyAnchoredMorphContainer(
            anchorRect = anchorRect,
            cardWidth = 336.dp,
            fallbackTopPadding = 96.dp,
            fallbackEndPadding = 92.dp
        ) { morphCorner ->
            val cardShape = RoundedCornerShape(morphCorner)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> cardRect = coords.boundsInRoot() }
                    .drawWithContent {
                        captureLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(captureLayer)
                    }
                    .harmonyRimGlow(enabled = prefs.rimGlowEnabled, shape = cardShape, glowColor = MaterialTheme.colorScheme.primary)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), cardShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {},
                shape = cardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 14.dp,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1) 광고 차단 · 번역 설정과 동일한 컴팩트 헤더 (15.sp Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = if (runtimeState.connectionState == McpConnectionState.CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                                filled = runtimeState.connectionState == McpConnectionState.CONNECTED,
                                symbolSize = 19.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MCP · 터널 설정",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { dismissWithCapture(cardRect?.let { Offset(it.right - 24f, it.top + 24f) }) },
                                contentAlignment = Alignment.Center
                            ) {
                                EgoIcon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "닫기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                    symbolSize = 18.dp
                                )
                            }
                        }

                        // 2) MCP 서버 & HTTPS 터널 마스터 토글 (광고 차단 스위치 행과 동일 밀도)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MCP 서버 & HTTPS 터널",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = runtimeState.statusMessage,
                                    fontSize = 11.sp,
                                    color = if (runtimeState.connectionState == McpConnectionState.CONNECTED) EgoEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = isRunning,
                                onCheckedChange = { enable ->
                                    if (enable) EgoForegroundService.startService(context)
                                    else EgoForegroundService.stopService(context)
                                },
                                modifier = Modifier.scale(0.86f)
                            )
                        }

                        // 3) 외부 HTTPS 주소 (연결 시 한 줄 캡슐)
                        if (runtimeState.publicHttpsUrl.isNotBlank()) {
                            CompactCopyPill(
                                prefix = "URL",
                                value = runtimeState.publicHttpsUrl,
                                onCopy = { copyText(context, "MCP 주소", runtimeState.publicHttpsUrl) }
                            )
                        }

                        // 4) Bearer 토큰 (한 줄 컴팩트 캡슐 + 재발급/복사)
                        CompactCopyPill(
                            prefix = "토큰",
                            value = tunnelSettings.bearerToken,
                            onCopy = { copyText(context, "Bearer 토큰", tunnelSettings.bearerToken) },
                            onRefresh = {
                                val newTok = OAuth21Manager.rotateBearerToken()
                                EgoForegroundService.updateTunnelSettings { it.copy(bearerToken = newTok) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 5) 터널 방식 (한 줄 세그먼트 칩)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "터널 방식",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    TunnelProvider.CLOUDFLARE to "Cloudflare",
                                    TunnelProvider.NGROK to "ngrok"
                                ).forEach { (provider, label) ->
                                    val sel = tunnelSettings.provider == provider
                                    Surface(
                                        shape = CircleShape,
                                        color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                EgoForegroundService.updateTunnelSettings { it.copy(provider = provider) }
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 6) 컴팩트 단선 입력 필 (광고 차단 입력 바 스타일 통일)
                        if (tunnelSettings.provider == TunnelProvider.CLOUDFLARE) {
                            CompactMcpInputPill(
                                value = tunnelSettings.cloudflareToken,
                                placeholder = "Cloudflare 토큰 (비우면 무료 Quick)",
                                onValueChange = { v -> EgoForegroundService.updateTunnelSettings { it.copy(cloudflareToken = v) } }
                            )
                        } else {
                            CompactMcpInputPill(
                                value = tunnelSettings.ngrokAuthToken,
                                placeholder = "ngrok Authtoken 입력",
                                onValueChange = { v -> EgoForegroundService.updateTunnelSettings { it.copy(ngrokAuthToken = v) } }
                            )
                            CompactMcpInputPill(
                                value = tunnelSettings.ngrokDomain,
                                placeholder = "ngrok 고정 도메인 (선택)",
                                onValueChange = { v -> EgoForegroundService.updateTunnelSettings { it.copy(ngrokDomain = v) } }
                            )
                        }

                        CompactMcpInputPill(
                            value = prefs.firecrawlApiKey,
                            placeholder = "Firecrawl API 키 (선택 · 미입력 시 로컬)",
                            onValueChange = { v -> SpaceManager.updatePreferences { it.copy(firecrawlApiKey = v) } }
                        )

                        // 7) 하단 한 줄 액션 칩 (배터리 최적화 제외 + 로그 토글 + 엔진 버전)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        runCatching {
                                            val intent = Intent(
                                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "배터리 최적화 제외",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                            if (runtimeState.recentLogs.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (showLogs) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showLogs = !showLogs }
                                ) {
                                    Text(
                                        text = if (showLogs) "로그 닫기" else "로그(${runtimeState.recentLogs.size})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (showLogs) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                            Text(
                                text = chromiumVersion.substringBefore(" "),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        if (showLogs && runtimeState.recentLogs.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = runtimeState.recentLogs.takeLast(5).joinToString("\n"),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun CompactCopyPill(
    prefix: String,
    value: String,
    onCopy: () -> Unit,
    onRefresh: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = prefix,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onRefresh != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                EgoIcon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "토큰 재발급",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                    symbolSize = 16.dp
                )
            }
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onCopy),
            contentAlignment = Alignment.Center
        ) {
            EgoIcon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "복사",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
                symbolSize = 15.dp
            )
        }
    }
}

@Composable
private fun CompactMcpInputPill(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun copyText(context: Context, label: String, value: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label 복사됨", Toast.LENGTH_SHORT).show()
}
