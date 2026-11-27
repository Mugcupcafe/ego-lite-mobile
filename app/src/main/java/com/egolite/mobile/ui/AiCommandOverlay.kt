package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.NorthWest
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch

private val QUICK_AI_PROMPTS = listOf(
    "이 페이지 핵심 내용 3줄로 요약해 줘",
    "가격·스펙을 한눈에 보기 좋게 표로 정리해 줘",
    "본문에서 중요한 수치와 링크만 추출해 줘",
    "다음 단계로 이동해서 이어서 확인해 줘"
)

/**
 * 주소창(ChromeOmniboxOverlay)과 1:1 동일한 위치·너비·일경도저 모핑을 사용하는 전용 AI 명령 입력 팝업.
 */
@Composable
fun AiCommandOverlay(
    spaces: List<BrowserSpace>,
    activeSpaceId: String,
    preselectedSpaceId: String?,
    preselectedTabId: String?,
    connectedClients: List<String>,
    defaultAiName: String,
    recentCommands: List<BrowserAiCommand>,
    anchorLeftPx: Float = 0f,
    anchorTopPx: Float = 0f,
    anchorWidthPx: Float = 0f,
    onDismiss: () -> Unit,
    onSubmitCommand: (BrowserAiCommand) -> Unit
) {
    val density = LocalDensity.current
    val availableClients = remember(connectedClients, defaultAiName) {
        val base = connectedClients.ifEmpty { listOf(defaultAiName.ifBlank { "Gemini" }) }
        base.distinct()
    }

    var selectedClient by remember(availableClients) {
        mutableStateOf(availableClients.firstOrNull() ?: "Gemini")
    }
    var selectedSpaceId by remember(preselectedSpaceId, activeSpaceId) {
        mutableStateOf(
            preselectedSpaceId
                ?: spaces.firstOrNull { it.id == activeSpaceId && it.type == SpaceType.AI }?.id
                ?: spaces.firstOrNull { it.type == SpaceType.AI }?.id
                ?: activeSpaceId
        )
    }
    val selectedSpace = spaces.firstOrNull { it.id == selectedSpaceId } ?: spaces.firstOrNull()
    var selectedTabId by remember(preselectedTabId, selectedSpaceId) {
        mutableStateOf(
            preselectedTabId?.takeIf { tid -> selectedSpace?.tabs?.any { it.id == tid } == true }
                ?: selectedSpace?.activeTabId
                ?: selectedSpace?.tabs?.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedTransport by remember { mutableStateOf(CommandTransport.QUEUE) }
    var promptText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val hasAnchor = anchorWidthPx > 120f
    val startDp = with(density) { anchorLeftPx.coerceAtLeast(0f).toDp() }
    val topDp = with(density) { anchorTopPx.coerceAtLeast(0f).toDp() }
    val widthDp = with(density) { anchorWidthPx.coerceAtLeast(320f).toDp() }

    // 일경도저(一镜到底) 주소창 모핑: 6dp Connected 주소창 -> 22dp 확장 AI 명령창 (닫기 시 역방향 모핑)
    val morphScope = rememberCoroutineScope()
    val morphProgress = remember { Animatable(0f) }
    var isClosingMorph by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        morphProgress.animateTo(1f, animationSpec = EgoMotion.morphSpring())
    }

    fun dismissWithMorph() {
        if (isClosingMorph) return
        isClosingMorph = true
        morphScope.launch {
            morphProgress.animateTo(0f, animationSpec = EgoMotion.responsiveSpring())
            onDismiss()
        }
    }

    val mp = morphProgress.value.coerceIn(0f, 1f)
    val cornerDp = (6f + 16f * mp).dp
    val shadowDp = (1f + 13f * mp).dp
    val accentColor = MaterialTheme.colorScheme.primary

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
            dismissWithMorph()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { dismissWithMorph() },
        contentAlignment = if (hasAnchor) Alignment.TopStart else Alignment.TopCenter
    ) {
        Surface(
            modifier = (if (hasAnchor) {
                Modifier
                    .padding(start = startDp, top = topDp)
                    .width(widthDp)
            } else {
                Modifier
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
            })
                .animateContentSize(animationSpec = EgoMotion.morphIntSizeSpring)
                .graphicsLayer {
                    scaleX = 0.985f + 0.015f * mp
                    alpha = (0.35f + 0.65f * mp).coerceIn(0f, 1f)
                }
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.38f),
                    shape = RoundedCornerShape(cornerDp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            shape = RoundedCornerShape(cornerDp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = shadowDp
        ) {
            Column(modifier = Modifier.padding(bottom = if (mp > 0.25f) 10.dp else 0.dp)) {
                // 1) 상단 54dp 자연어 AI 명령 입력 바 (주소창 1:1 높이·위치 일치)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(start = 16.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EgoIcon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                        symbolSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitAction() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (promptText.isEmpty()) {
                                Text(
                                    text = "AI에게 자연어로 명령하기",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        }
                    )
                    if (promptText.isNotEmpty()) {
                        IconButton(
                            onClick = { promptText = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "입력 지우기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp),
                                symbolSize = 17.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (promptText.isNotBlank()) accentColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(enabled = promptText.isNotBlank()) { submitAction() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EgoIcon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "AI 명령 전송",
                                tint = if (promptText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(15.dp),
                                symbolSize = 15.dp
                            )
                        }
                    }
                }

                // 2) 일경도저 세로 확장 본문 (단일 콤팩트 타깃 바 + 빠른 명령 + 최근 AI 명령 기록)
                AnimatedVisibility(
                    visible = !isClosingMorph && mp > 0.08f,
                    enter = expandVertically(animationSpec = EgoMotion.morphIntSizeSpring, expandFrom = Alignment.Top) + fadeIn(tween(150)),
                    exit = shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(120))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))

                        // 2-A) 깔끔한 단일 라인 대상 선택 스트립 (테두리 없는 톤 칩)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 대상 AI 클라이언트 (복수 시 탭해서 순환)
                            val clientLabel = if (selectedTransport == CommandTransport.BUILT_IN_API) "내장 API" else selectedClient
                            AiTonalChip(
                                label = clientLabel,
                                selected = true,
                                onClick = {
                                    if (selectedTransport == CommandTransport.BUILT_IN_API) {
                                        selectedTransport = CommandTransport.QUEUE
                                    } else if (availableClients.size > 1) {
                                        val nextIdx = (availableClients.indexOf(selectedClient) + 1) % availableClients.size
                                        selectedClient = availableClients[nextIdx]
                                    }
                                }
                            )

                            // 전송 방식 순환 칩 (명령 큐 -> MCP Sampling -> 내장 API)
                            AiTonalChip(
                                label = selectedTransport.label,
                                selected = false,
                                onClick = {
                                    val modes = CommandTransport.entries
                                    val next = (modes.indexOf(selectedTransport) + 1) % modes.size
                                    selectedTransport = modes[next]
                                }
                            )

                            // 세로 구분선
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .width(1.dp)
                                    .height(14.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // 대상 스페이스 칩
                            spaces.forEach { sp ->
                                AiTonalChip(
                                    label = sp.name,
                                    selected = sp.id == selectedSpace?.id,
                                    onClick = {
                                        selectedSpaceId = sp.id
                                        selectedTabId = sp.activeTabId
                                    }
                                )
                            }

                            // 대상 탭 칩 (탭이 2개 이상이거나 현재 탭 표시)
                            selectedSpace?.tabs?.forEachIndexed { idx, tab ->
                                val isBg = tab.id != selectedSpace.activeTabId
                                val shortTitle = tab.title.ifBlank { tab.url }.take(12)
                                AiTonalChip(
                                    label = if (isBg) "탭${idx + 1}·$shortTitle(배경)" else "탭${idx + 1}·$shortTitle",
                                    selected = tab.id == selectedTabId,
                                    onClick = { selectedTabId = tab.id }
                                )
                            }
                        }

                        // 2-B) 빠른 AI 명령 + 최근 AI 명령 기록
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "빠른 명령",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QUICK_AI_PROMPTS.forEach { preset ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { promptText = preset }
                                    ) {
                                        Text(
                                            text = preset,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "최근 AI 명령",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            if (recentCommands.isEmpty()) {
                                Text(
                                    text = "최근 보낸 AI 명령이 없습니다.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            } else {
                                recentCommands.take(6).forEach { cmd ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { promptText = cmd.prompt }
                                            .padding(horizontal = 16.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        EgoIcon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                            symbolSize = 18.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cmd.prompt,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${cmd.targetClient} · ${cmd.spaceName} · ${cmd.tabTitle}",
                                                fontSize = 11.5.sp,
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
                                        EgoIcon(
                                            imageVector = Icons.Default.NorthWest,
                                            contentDescription = "입력창에 채우기",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                            symbolSize = 16.dp
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

@Composable
private fun AiTonalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
