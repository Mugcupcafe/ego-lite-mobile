package com.egolite.mobile.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.AI_SPACE_COLOR_PRESETS
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.SpaceType
import kotlinx.coroutines.delay

/**
 * 상단 고정형 AI 라이브 모니터 트레이 (256dp)
 * - 좌측 상단 알약을 짧게 탭하면 펼쳐지고, 다시 눌러 닫기 전까지 유지됨 (웹툰·웹서핑과 동시 모니터링)
 * - 열려 있을 때만 1.3초 주기로 경량 RGB_565 썸네일을 갱신해 발열·배터리 부하를 최소화
 */
@Composable
fun AiLiveMonitorTray(
    visible: Boolean,
    spaces: List<BrowserSpace>,
    activeSpaceId: String,
    canAddAiSpace: Boolean,
    onSelectSpace: (String) -> Unit,
    onCreateAiSpace: () -> Unit,
    onCloseAiSpace: (String) -> Unit,
    onChangeSpaceColor: (String, String) -> Unit,
    onCollapse: () -> Unit
) {
    val aiSpaces = remember(spaces) { spaces.filter { it.type == SpaceType.AI } }
    var thumbnails by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }

    // 트레이가 열려 있을 때만 1.3초 주기로 AI Space 경량 썸네일 갱신 (닫히면 0% 부하)
    LaunchedEffect(visible, aiSpaces.map { "${it.id}:${it.activeTab.id}:${it.aiStatus}:${it.lastActionSummary}" }) {
        if (!visible) return@LaunchedEffect
        while (true) {
            val fresh = mutableMapOf<String, Bitmap>()
            aiSpaces.forEach { sp ->
                val bmp = SpaceManager.captureTabThumbnail(sp.activeTab.id)
                if (bmp != null) {
                    fresh[sp.id] = bmp
                } else {
                    thumbnails[sp.id]?.let { fresh[sp.id] = it }
                }
            }
            thumbnails = fresh
            delay(1300L)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + expandVertically(tween(240)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(258.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            // 상단 컨트롤 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EgoEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AI 라이브 모니터 (${aiSpaces.size})",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Personal 공간 복귀 버튼 (현재 AI Space를 보고 있을 때 편리하게 원클릭 복귀)
                    val isPersonalActive = activeSpaceId == "personal"
                    Surface(
                        shape = CircleShape,
                        color = if (isPersonalActive) EgoCobalt.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (isPersonalActive) EgoCobalt else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable { onSelectSpace("personal") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isPersonalActive) EgoCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Personal",
                                fontSize = 11.sp,
                                fontWeight = if (isPersonalActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPersonalActive) EgoCobalt else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canAddAiSpace) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clip(CircleShape)
                                .clickable { onCreateAiSpace() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "AI Space 추가",
                                    tint = EgoEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "새 AI 공간",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "AI 모니터 접기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 가로 스크롤 미니 화면 카드 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                aiSpaces.forEach { space ->
                    val isSelected = space.id == activeSpaceId
                    val accent = parseSpaceColor(space.accentColorHex)
                    val isActiveWork = space.aiStatus == AiSpaceStatus.WORKING || space.aiStatus == AiSpaceStatus.THINKING
                    val statusLabel = when (space.aiStatus) {
                        AiSpaceStatus.WORKING -> space.lastActionSummary.ifBlank { "작업 처리 중..." }
                        AiSpaceStatus.THINKING -> "다음 행동을 생각하고 있어요..."
                        AiSpaceStatus.TAKEOVER_PAUSED -> "직접 조작 중"
                        AiSpaceStatus.DONE -> "작업 완료"
                        AiSpaceStatus.IDLE -> "대기 중 · ${space.activeTab.title.ifBlank { "새 탭" }}"
                    }
                    val thumb = thumbnails[space.id]

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = if (isSelected || isActiveWork) 5.dp else 2.dp,
                        modifier = Modifier
                            .width(244.dp)
                            .height(202.dp)
                            .border(
                                width = if (isSelected || isActiveWork) 1.8.dp else 0.8.dp,
                                color = if (isSelected || isActiveWork) accent else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectSpace(space.id) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 카드 헤더 (색상 점 + 공간명 + 상태 배지 + 닫기)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .background(accent.copy(alpha = if (isActiveWork) 0.16f else 0.08f))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = space.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActiveWork) {
                                    Text(
                                        text = if (space.aiStatus == AiSpaceStatus.THINKING) "생각중" else "작업중",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                }
                                if (aiSpaces.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "AI 공간 닫기",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable { onCloseAiSpace(space.id) }
                                    )
                                }
                            }

                            // 미니 웹 화면 프리뷰 영역
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumb != null && space.activeTab.url != NEW_TAB_URL) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = "${space.name} 미니 화면",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = accent.copy(alpha = 0.65f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (space.activeTab.url == NEW_TAB_URL) "AI 명령 대기 중" else "화면 불러오는 중...",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 카드 하단: 현재 동작 문구 + 공간 색상 커스텀 점
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActiveWork) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isActiveWork) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AI_SPACE_COLOR_PRESETS.forEach { hex ->
                                        val c = parseSpaceColor(hex)
                                        val picked = space.accentColorHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(if (picked) 13.dp else 10.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .then(
                                                    if (picked) Modifier.border(1.2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                    else Modifier
                                                )
                                                .clickable { onChangeSpaceColor(space.id, hex) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
        }
    }
}
