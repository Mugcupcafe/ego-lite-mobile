package com.egolite.mobile.ui

import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.SpaceManager

/**
 * 크롬 확장프로그램(uBlock Origin) 스타일 우상단 앵커 팝업
 * - 스크롤 없이 전체 메뉴가 한 화면에 온전히 보이도록 컴팩트 설계
 */
@OptIn(ExperimentalLayoutApi::class)
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

    // 바깥 터치 시 닫히는 투명 스크림
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    )

    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    // 방패 버튼을 그대로 덮으며 카드 자체만 깔끔하게 모프
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 50.dp, end = 54.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        AnimatedVisibility(
            visible = cardVisible,
            enter = fadeIn(tween(130)) + scaleIn(
                animationSpec = EgoMotion.smoothSpring(),
                initialScale = 0.72f,
                transformOrigin = TransformOrigin(1f, 0f)
            ),
            exit = fadeOut(tween(100)) + scaleOut(
                animationSpec = tween(130),
                targetScale = 0.78f,
                transformOrigin = TransformOrigin(1f, 0f)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 14.dp,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .width(326.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1) 컴팩트 헤더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isSiteActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "광고 차단",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", modifier = Modifier.size(16.dp))
                        }
                    }

                    // 2) 차단 통계 2열 스트립 (컴팩트)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("현재 페이지", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$pageBlocked", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("누적 차단", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${st.totalBlockedCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EgoEmerald)
                            }
                        }
                    }

                    // 3) 3대 스위치 (컴팩트 1줄)
                    CompactSwitchRow(
                        label = "전체 광고 차단",
                        checked = st.globalEnabled,
                        onCheckedChange = { enabled ->
                            AdBlockEngine.setGlobalEnabled(enabled)
                            SpaceManager.updatePreferences { it.copy(adBlockEnabled = enabled) }
                            onReloadPage()
                        }
                    )

                    if (currentHost.isNotBlank()) {
                        CompactSwitchRow(
                            label = "이 사이트 차단 ($currentHost)",
                            checked = currentHost !in st.whitelistedHosts,
                            enabled = st.globalEnabled,
                            onCheckedChange = {
                                AdBlockEngine.toggleWhitelistForHost(currentHost)
                                onReloadPage()
                            }
                        )
                    }

                    CompactSwitchRow(
                        label = "배너·제휴 CSS 숨김",
                        checked = st.cosmeticEnabled,
                        onCheckedChange = {
                            AdBlockEngine.setCosmeticEnabled(it)
                            onReloadPage()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 4) 사용자 차단 도메인 추가 (슬림 입력창)
                    CompactRuleInputRow(
                        value = customHostInput,
                        onValueChange = { customHostInput = it },
                        placeholder = "차단 도메인 추가 (예: ad.example.com)",
                        onAdd = {
                            if (customHostInput.isNotBlank()) {
                                AdBlockEngine.addCustomBlockedHost(customHostInput)
                                customHostInput = ""
                                onReloadPage()
                            }
                        }
                    )
                    if (st.customBlockedHosts.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            st.customBlockedHosts.forEach { host ->
                                RemovableRuleChip(text = host) {
                                    AdBlockEngine.removeCustomBlockedHost(host)
                                    onReloadPage()
                                }
                            }
                        }
                    }

                    // 5) 사용자 CSS 선택자 추가 (슬림 입력창)
                    CompactRuleInputRow(
                        value = customCssInput,
                        onValueChange = { customCssInput = it },
                        placeholder = "CSS 선택자 숨김 (예: .coupang-widget)",
                        onAdd = {
                            if (customCssInput.isNotBlank()) {
                                AdBlockEngine.addCustomCssSelector(customCssInput)
                                customCssInput = ""
                                onReloadPage()
                            }
                        }
                    )
                    if (st.customCssSelectors.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            st.customCssSelectors.forEach { sel ->
                                RemovableRuleChip(text = sel) {
                                    AdBlockEngine.removeCustomCssSelector(sel)
                                    onReloadPage()
                                }
                            }
                        }
                    }

                    // 6) 최근 차단 도메인 요약
                    if (st.recentBlockedHosts.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "최근 차단: ${st.recentBlockedHosts.take(5).joinToString(", ")}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
private fun CompactRuleInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                inner()
            }
        )
        IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "추가", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RemovableRuleChip(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Default.Close,
            contentDescription = "삭제",
            modifier = Modifier
                .size(13.dp)
                .clickable { onRemove() },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
