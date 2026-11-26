package com.egolite.mobile.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.EgoWebViewFactory
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.mcp.OAuth21Manager
import com.egolite.mobile.model.McpConnectionState
import androidx.compose.material3.Slider
import com.egolite.mobile.model.SearchEngine
import com.egolite.mobile.model.TranslationEngine
import com.egolite.mobile.model.TunnelProvider
import com.egolite.mobile.tunnel.EgoForegroundService

@Composable
fun McpSettingsDrawer(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()
    val tunnelSettings by EgoForegroundService.currentTunnelSettings.collectAsState()
    val prefs by SpaceManager.preferences.collectAsState()
    val chromiumVersion = remember { EgoWebViewFactory.getCurrentChromiumVersion(context) }

    val isRunning = runtimeState.connectionState == McpConnectionState.CONNECTED ||
        runtimeState.connectionState == McpConnectionState.STARTING

    // 전체 반투명 검정 배경 제거 + MCP 버튼을 그대로 덮는 컴팩트 M3 모프 팝업 카드
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 96.dp)
                .width(368.dp)
                .heightIn(max = 540.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 22.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MCP · 터널 · 브라우저 설정", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                // MCP Server + HTTPS Tunnel Master Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MCP 서버 & HTTPS 터널", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            text = runtimeState.statusMessage,
                            fontSize = 12.sp,
                            color = if (runtimeState.connectionState == McpConnectionState.CONNECTED) EgoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isRunning,
                        onCheckedChange = { enable ->
                            if (enable) EgoForegroundService.startService(context)
                            else EgoForegroundService.stopService(context)
                        }
                    )
                }

                if (runtimeState.publicHttpsUrl.isNotBlank()) {
                    CopyableField(
                        label = "외부 HTTPS MCP 주소 (Gemini / Muse 입력용)",
                        value = runtimeState.publicHttpsUrl,
                        onCopy = { copyText(context, "MCP 주소", runtimeState.publicHttpsUrl) }
                    )
                }

                CopyableField(
                    label = "Muse용 Bearer 토큰",
                    value = tunnelSettings.bearerToken,
                    onCopy = { copyText(context, "Bearer 토큰", tunnelSettings.bearerToken) },
                    trailingExtra = {
                        IconButton(onClick = {
                            val newTok = OAuth21Manager.rotateBearerToken()
                            EgoForegroundService.updateTunnelSettings { it.copy(bearerToken = newTok) }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "토큰 재발급")
                        }
                    }
                )

                HorizontalDivider()

                // 터널 프로바이더 선택
                Text("터널 방식", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = tunnelSettings.provider == TunnelProvider.CLOUDFLARE,
                        onClick = { EgoForegroundService.updateTunnelSettings { it.copy(provider = TunnelProvider.CLOUDFLARE) } }
                    )
                    Text("Cloudflare", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = tunnelSettings.provider == TunnelProvider.NGROK,
                        onClick = { EgoForegroundService.updateTunnelSettings { it.copy(provider = TunnelProvider.NGROK) } }
                    )
                    Text("ngrok", fontSize = 14.sp)
                }

                if (tunnelSettings.provider == TunnelProvider.CLOUDFLARE) {
                    OutlinedTextField(
                        value = tunnelSettings.cloudflareToken,
                        onValueChange = { v -> EgoForegroundService.updateTunnelSettings { it.copy(cloudflareToken = v) } },
                        label = { Text("Cloudflare Named Tunnel 토큰 (비우면 무료 Quick Tunnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = tunnelSettings.ngrokAuthToken,
                        onValueChange = { v -> EgoForegroundService.updateTunnelSettings { it.copy(ngrokAuthToken = v) } },
                        label = { Text("ngrok Authtoken") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tunnelSettings.ngrokDomain,
                        onValueChange = { v -> EgoForegroundService.updateTunnelSettings { it.copy(ngrokDomain = v) } },
                        label = { Text("ngrok 고정 도메인 (선택)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = prefs.firecrawlApiKey,
                    onValueChange = { v -> SpaceManager.updatePreferences { it.copy(firecrawlApiKey = v) } },
                    label = { Text("Firecrawl API 키 (선택 · 미입력 시 로컬 Markdown 사용)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("배터리 최적화 제외 설정 (MagicOS 백그라운드 유지)")
                }

                HorizontalDivider()

                Text("시스템 WebView 엔진", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = chromiumVersion,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (runtimeState.recentLogs.isNotEmpty()) {
                    Text("최근 MCP · 터널 로그", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = runtimeState.recentLogs.takeLast(10).joinToString("\n"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CopyableField(
    label: String,
    value: String,
    onCopy: () -> Unit,
    trailingExtra: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                trailingExtra?.invoke()
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "복사")
                }
            }
        }
    }
}

private fun copyText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label 복사됨", Toast.LENGTH_SHORT).show()
}
