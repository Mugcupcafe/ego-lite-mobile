package com.egolite.mobile.ui

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.mcp.BrowserAiCommand
import com.egolite.mobile.mcp.CommandTransport
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.SpaceType

private val QUICK_AI_PROMPTS = listOf(
    "이 페이지 핵심 내용 3줄로 요약해 줘",
    "가격·스펙을 한눈에 보기 좋게 표로 정리해 줘",
    "본문에서 중요한 수치와 링크만 추출해 줘",
    "다음 단계로 이동해서 이어서 확인해 줘"
)

/**
 * 주소창 팝업(OmniboxOverlay) 형식의 전용 AI 명령 입력 팝업
 * - 주소창과 혼동되지 않도록 AI 포인트 컬러 테두리 + 'AI 명령' 배지 + 대상 AI/탭 선택 칩 제공
 * - 하단에는 북마크/기록 자리에 빠른 AI 명령 프리셋과 최근 AI 명령 기록을 표시
 */
@Composable
fun AiCommandOverlay(
    visible: Boolean,
    spaces: List<BrowserSpace>,
    activeSpaceId: String,
    preselectedSpaceId: String?,
    preselectedTabId: String?,
    connectedClients: List<String>,
    defaultAiName: String,
    recentCommands: List<BrowserAiCommand>,
    onDismiss: () -> Unit,
    onSubmitCommand: (BrowserAiCommand) -> Unit
) {
    val availableClients = remember(connectedClients, defaultAiName) {
        val base = connectedClients.ifEmpty { listOf(defaultAiName.ifBlank { "Gemini" }) }
        base.distinct()
    }

    var selectedClient by remember(visible, availableClients) {
        mutableStateOf(availableClients.firstOrNull() ?: "Gemini")
    }
    var selectedSpaceId by remember(visible, preselectedSpaceId, activeSpaceId) {
        mutableStateOf(
            preselectedSpaceId
                ?: spaces.firstOrNull { it.id == activeSpaceId && it.type == SpaceType.AI }?.id
                ?: spaces.firstOrNull { it.type == SpaceType.AI }?.id
                ?: activeSpaceId
        )
    }
    val selectedSpace = spaces.firstOrNull { it.id == selectedSpaceId } ?: spaces.firstOrNull()
    var selectedTabId by remember(visible, preselectedTabId, selectedSpaceId) {
        mutableStateOf(
            preselectedTabId?.takeIf { tid -> selectedSpace?.tabs?.any { it.id == tid } == true }
                ?: selectedSpace?.activeTabId
                ?: selectedSpace?.tabs?.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedTransport by remember(visible) { mutableStateOf(CommandTransport.QUEUE) }
    var promptText by remember(visible) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(170)),
        exit = fadeOut(tween(150))
    ) {
        // 전체 반투명 검정 배경 제거 — AI 버튼을 덮으며 확장되는 모프 팝업
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(top = 50.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.30f,
                    transformOrigin = TransformOrigin(0.86f, 0.04f),
                    animationSpec = EgoMotion.smoothSpring()
                ) + fadeIn(tween(150)),
                exit = scaleOut(
                    targetScale = 0.30f,
                    transformOrigin = TransformOrigin(0.86f, 0.04f),
                    animationSpec = tween(170)
                ) + fadeOut(tween(130))
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 6.dp,
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxWidth()
                        .border(1.5.dp, accentColor, RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 상단 AI 전용 헤더 + 자연어 입력바 (일반 주소창과 명확히 구분)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(accentColor.copy(alpha = 0.10f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        EgoIcon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(13.dp)
                                        , symbolSize = 13.dp)
                                        Text(
                                            text = "AI 명령",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                // 대상 AI 세션 / 전송 방식 가로 칩
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    availableClients.forEach { client ->
                                        val isSel = selectedClient == client && selectedTransport != CommandTransport.BUILT_IN_API
                                        PopupFilterChip(
                                            label = client,
                                            selected = isSel,
                                            onClick = {
                                                selectedClient = client
                                                if (selectedTransport == CommandTransport.BUILT_IN_API) {
                                                    selectedTransport = CommandTransport.QUEUE
                                                }
                                            }
                                        )
                                    }
                                    CommandTransport.entries.forEach { mode ->
                                        PopupFilterChip(
                                            label = mode.label,
                                            selected = selectedTransport == mode,
                                            onClick = { selectedTransport = mode }
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    EgoIcon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "AI 명령창 닫기",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    , symbolSize = 18.dp)
                                }
                            }

                            // 대상 스페이스 & 탭(tabId) 지정 칩 행
                            if (selectedSpace != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    spaces.forEach { sp ->
                                        PopupFilterChip(
                                            label = sp.name,
                                            selected = sp.id == selectedSpace.id,
                                            onClick = {
                                                selectedSpaceId = sp.id
                                                selectedTabId = sp.activeTabId
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    selectedSpace.tabs.forEachIndexed { idx, tab ->
                                        val isBg = tab.id != selectedSpace.activeTabId
                                        val shortTitle = tab.title.ifBlank { tab.url }.take(14)
                                        PopupFilterChip(
                                            label = if (isBg) "탭${idx + 1}·$shortTitle(배경)" else "탭${idx + 1}·$shortTitle",
                                            selected = tab.id == selectedTabId,
                                            onClick = { selectedTabId = tab.id }
                                        )
                                    }
                                }
                            }

                            // 자연어 명령 입력 필드 + 전송 버튼
                            val submitAction = {
                                val trimmed = promptText.trim()
                                val sp = selectedSpace
                                val tab = sp?.tabs?.firstOrNull { it.id == selectedTabId } ?: sp?.activeTab
                                if (trimmed.isNotEmpty() && sp != null && tab != null) {
                                    onSubmitCommand(
                                        BrowserAiCommand(
                                            targetClient = if (selectedTransport == CommandTransport.BUILT_IN_API) "내장 API" else selectedClient,
                                            spaceId = sp.id,
                                            spaceName = sp.name,
                                            tabId = tab.id,
                                            tabTitle = tab.title.ifBlank { tab.url },
                                            tabUrl = tab.url,
                                            isBackgroundTab = tab.id != sp.activeTabId,
                                            prompt = trimmed,
                                            transport = selectedTransport
                                        )
                                    )
                                    promptText = ""
                                    onDismiss()
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (promptText.isEmpty()) {
                                        Text(
                                            text = "AI에게 보낼 자연어 지시 입력 (주소 검색 아님)",
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = promptText,
                                        onValueChange = { promptText = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(accentColor),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = { submitAction() }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (promptText.isNotBlank()) accentColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = promptText.isNotBlank()) { submitAction() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        EgoIcon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "명령 전송",
                                            tint = if (promptText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        , symbolSize = 16.dp)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                        // 하단: 빠른 AI 명령(북마크 자리) + 최근 AI 명령 기록(검색 기록 자리)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 310.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(
                                    text = "빠른 AI 명령",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    QUICK_AI_PROMPTS.forEach { preset ->
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { promptText = preset }
                                        ) {
                                            Text(
                                                text = preset,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "최근 AI 명령 기록",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (recentCommands.isEmpty()) {
                                    Text(
                                        text = "아직 보낸 명령이 없습니다. 위에서 자연어로 지시를 입력해 보세요.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    recentCommands.take(6).forEach { cmd ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                .clickable { promptText = cmd.prompt }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            EgoIcon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            , symbolSize = 16.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cmd.prompt,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${cmd.targetClient} · ${cmd.spaceName} · ${cmd.tabTitle}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = accentColor.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = cmd.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
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
}

@Composable
private fun PopupFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = CircleShape,
        color = bg,
        modifier = Modifier
            .border(1.dp, borderColor, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
