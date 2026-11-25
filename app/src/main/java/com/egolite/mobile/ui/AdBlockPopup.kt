package com.egolite.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.SpaceManager

@Composable
fun AdBlockControlDialog(
    currentHost: String,
    activeTabId: String,
    onReloadPage: () -> Unit,
    onDismiss: () -> Unit
) {
    val st by AdBlockEngine.state.collectAsState()
    val pageBlocked = st.perTabCounts[activeTabId] ?: 0
    val isSiteActive = AdBlockEngine.isEnabledForHost(currentHost)

    var customHostInput by remember { mutableStateOf("") }
    var customCssInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isSiteActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("광고 차단 (확장 제어)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 차단 통계
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("현재 페이지 차단", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$pageBlocked 건", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("누적 총 차단", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${st.totalBlockedCount} 건", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EgoEmerald)
                        }
                    }
                }

                // 전역 ON/OFF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("전체 광고 차단", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Switch(
                        checked = st.globalEnabled,
                        onCheckedChange = { enabled ->
                            AdBlockEngine.setGlobalEnabled(enabled)
                            SpaceManager.updatePreferences { it.copy(adBlockEnabled = enabled) }
                            onReloadPage()
                        }
                    )
                }

                // 현재 사이트 차단 ON/OFF
                if (currentHost.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("이 사이트에서 차단", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(currentHost, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = currentHost !in st.whitelistedHosts,
                            enabled = st.globalEnabled,
                            onCheckedChange = {
                                AdBlockEngine.toggleWhitelistForHost(currentHost)
                                onReloadPage()
                            }
                        )
                    }
                }

                // 코스메틱(배너/제휴 영역) 숨김
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("배너·제휴 영역 CSS 숨김", fontSize = 14.sp)
                    Switch(
                        checked = st.cosmeticEnabled,
                        onCheckedChange = {
                            AdBlockEngine.setCosmeticEnabled(it)
                            onReloadPage()
                        }
                    )
                }

                HorizontalDivider()

                // 사용자 정의 차단 도메인 추가
                Text("사용자 차단 도메인 추가", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customHostInput,
                        onValueChange = { customHostInput = it },
                        placeholder = { Text("예: ad.example.com", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (customHostInput.isNotBlank()) {
                            AdBlockEngine.addCustomBlockedHost(customHostInput)
                            customHostInput = ""
                            onReloadPage()
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "도메인 추가")
                    }
                }
                st.customBlockedHosts.forEach { host ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(host, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "삭제",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    AdBlockEngine.removeCustomBlockedHost(host)
                                    onReloadPage()
                                }
                        )
                    }
                }

                // 사용자 정의 CSS 선택자 숨김 (자체 도메인 제휴 배너 차단용)
                Text("사용자 CSS 영역 숨김 (선택자)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customCssInput,
                        onValueChange = { customCssInput = it },
                        placeholder = { Text("예: .coupang-widget", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (customCssInput.isNotBlank()) {
                            AdBlockEngine.addCustomCssSelector(customCssInput)
                            customCssInput = ""
                            onReloadPage()
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "선택자 추가")
                    }
                }
                st.customCssSelectors.forEach { sel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(sel, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "삭제",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    AdBlockEngine.removeCustomCssSelector(sel)
                                    onReloadPage()
                                }
                        )
                    }
                }

                if (st.recentBlockedHosts.isNotEmpty()) {
                    HorizontalDivider()
                    Text("최근 차단된 도메인", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = st.recentBlockedHosts.take(8).joinToString(", "),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}
