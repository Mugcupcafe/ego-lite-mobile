package com.egolite.mobile.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.egolite.mobile.browser.BrowserScripts
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.mcp.Last30DaysEngine
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.SpaceType
import com.egolite.mobile.tunnel.EgoForegroundService
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen() {
    val spaces by SpaceManager.spaces.collectAsState()
    val activeSpaceId by SpaceManager.activeSpaceId.collectAsState()
    val prefs by SpaceManager.preferences.collectAsState()
    val bookmarks by SpaceManager.bookmarks.collectAsState()
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()

    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId } ?: spaces.first()
    val activeTab = activeSpace.activeTab
    val isAnyAiWorking = spaces.any { it.type == SpaceType.AI && it.aiStatus == AiSpaceStatus.WORKING }
    val isMcpDisconnected = runtimeState.connectionState != McpConnectionState.CONNECTED

    var addressInput by remember(activeTab.id, activeTab.url) { mutableStateOf(activeTab.url) }
    var spaceBarExpanded by remember { mutableStateOf(false) }
    var showMcpDrawer by remember { mutableStateOf(false) }
    var showSpacesOverview by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeTab.url) {
        addressInput = activeTab.url
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1행: 최상단 — 알약 캡슐(Personal/AI) + 현재 공간의 탭 목록만 배치
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillSpaceCapsule(
                    activeSpace = activeSpace,
                    isAnyAiWorking = isAnyAiWorking,
                    onToggleExpand = { spaceBarExpanded = !spaceBarExpanded }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeSpace.tabs.forEach { tab ->
                        val isSelected = tab.id == activeSpace.activeTabId
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .widthIn(min = 110.dp, max = 210.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                                .clickable { SpaceManager.selectTab(activeSpace.id, tab.id) }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title.ifBlank { tab.url },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "탭 닫기",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { SpaceManager.closeTab(activeSpace.id, tab.id) },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { SpaceManager.createTab(activeSpace.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "새 탭",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2행: 주소창 + 조건부 붉은 MCP 연결끊김 배지 + 편의기능 + 메뉴(⋮)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { SpaceManager.goBack(activeSpace.id) },
                    enabled = activeTab.canGoBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
                IconButton(
                    onClick = { SpaceManager.goForward(activeSpace.id) },
                    enabled = activeTab.canGoForward,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "앞으로")
                }
                IconButton(
                    onClick = { SpaceManager.reload(activeSpace.id) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                }

                // 주소창
                val isBookmarked = bookmarks.any { it.url == activeTab.url }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(EgoCobalt),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                SpaceManager.navigate(activeSpace.id, addressInput)
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "북마크",
                        tint = if (isBookmarked) EgoCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { SpaceManager.toggleBookmark(activeTab.title, activeTab.url) }
                    )
                }

                // 조건부 경고 배지: MCP 연결 끊김일 때만 주소창과 광고차단 버튼 사이에 붉은색으로 표시
                if (isMcpDisconnected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(EgoDangerRed.copy(alpha = 0.12f))
                            .border(1.dp, EgoDangerRed.copy(alpha = 0.45f), CircleShape)
                            .clickable { showMcpDrawer = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = "MCP 연결 끊김",
                            modifier = Modifier.size(15.dp),
                            tint = EgoDangerRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MCP 끊김",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EgoDangerRed
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 광고 차단
                IconButton(
                    onClick = { SpaceManager.updatePreferences { it.copy(adBlockEnabled = !it.adBlockEnabled) } },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "광고 차단",
                        tint = if (prefs.adBlockEnabled) EgoCobalt else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 원클릭 자동 번역
                IconButton(
                    onClick = {
                        scope.launch {
                            translateActiveSpacePage(activeSpace.id)
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "한국어 번역",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 다크모드
                IconButton(
                    onClick = { SpaceManager.updatePreferences { it.copy(darkModeEnabled = !it.darkModeEnabled) } },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "다크모드",
                        tint = if (prefs.darkModeEnabled) EgoCobalt else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Spaces Overview
                IconButton(
                    onClick = { showSpacesOverview = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Spaces 전체 보기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 우상단 메뉴 버튼 (2행으로 이동 — MCP/터널 설정 드로어 열기)
                IconButton(
                    onClick = { showMcpDrawer = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "메뉴 및 MCP 설정",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 3행 (선택): 설정에서 ON일 때만 주소창 하단 북마크 바 표시
            if (prefs.showBookmarkBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bookmarks.forEach { bm ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .combinedClickable(
                                    onClick = { SpaceManager.navigate(activeSpace.id, bm.url) },
                                    onLongClick = { SpaceManager.removeBookmark(bm.id) }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = bm.title,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (activeTab.isLoading && activeTab.progress in 1..99) {
                LinearProgressIndicator(
                    progress = { activeTab.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = if (activeSpace.type == SpaceType.AI) EgoEmerald else EgoCobalt
                )
            }

            // AI Space Takeover 제어권 바 (AI Space에서 작업 중이거나 일시정지 상태일 때만)
            if (activeSpace.type == SpaceType.AI &&
                (activeSpace.aiStatus == AiSpaceStatus.WORKING || activeSpace.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED)
            ) {
                val isPaused = activeSpace.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EgoEmerald.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPaused) "직접 조작 모드 (AI 일시정지됨)" else "AI 작업 중: ${activeSpace.lastActionSummary}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EgoEmerald
                    )
                    TextButton(onClick = { SpaceManager.toggleTakeover(activeSpace.id) }) {
                        Text(
                            text = if (isPaused) "AI 제어 재개" else "직접 조작 (Takeover)",
                            color = EgoEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 웹 캔버스 (AI Space일 때 2dp 에메랄드 테두리 + Pull-to-Refresh + 백그라운드 AI WebView 유지)
            val canvasBorder = if (activeSpace.type == SpaceType.AI) {
                Modifier.border(2.dp, EgoEmerald, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .then(canvasBorder)
            ) {
                WebViewHostContainer(
                    spaces = spaces,
                    activeSpace = activeSpace
                )
            }
        }

        // 알약 캡슐 터치 시 바로 아래(주소창 줄 위)로 확장되는 슬라이드 Space 전환 바
        Box(
            modifier = Modifier
                .padding(top = 46.dp)
                .fillMaxWidth()
        ) {
            ExpandedSpaceSlideBar(
                visible = spaceBarExpanded,
                spaces = spaces,
                activeSpaceId = activeSpaceId,
                canAddAiSpace = spaces.count { it.type == SpaceType.AI } < prefs.maxAiSpaces,
                onSelectSpace = { id ->
                    SpaceManager.selectSpace(id)
                    spaceBarExpanded = false
                },
                onCreateAiSpace = {
                    SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
                },
                onCloseAiSpace = { id -> SpaceManager.closeAiSpace(id) }
            )
        }

        // 우측 MCP / 터널 / 편의 설정 슬라이드 드로어
        if (showMcpDrawer) {
            McpSettingsDrawer(
                onDismiss = { showMcpDrawer = false }
            )
        }

        // Spaces Overview 다이얼로그
        if (showSpacesOverview) {
            AlertDialog(
                onDismissRequest = { showSpacesOverview = false },
                title = { Text("Spaces 전체 보기") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        spaces.forEach { sp ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (sp.id == activeSpaceId) EgoCobalt.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SpaceManager.selectSpace(sp.id)
                                        showSpacesOverview = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(sp.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${sp.tabs.size}개 탭 · ${sp.activeTab.title}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
                        showSpacesOverview = false
                    }) {
                        Text("+ AI Space 추가")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSpacesOverview = false }) {
                        Text("닫기")
                    }
                }
            )
        }
    }
}

@Composable
private fun WebViewHostContainer(
    spaces: List<BrowserSpace>,
    activeSpace: BrowserSpace
) {
    val activeTab = activeSpace.activeTab
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { root ->
            // 1) 백그라운드 AI Space WebView도 렌더링·스크린샷이 멈추지 않도록 숨김 레이어에 attached 유지
            spaces.filter { it.type == SpaceType.AI && it.id != activeSpace.id }.forEach { bgSpace ->
                val bgWv = SpaceManager.getOrCreateWebView(bgSpace.id, bgSpace.activeTab)
                if (bgWv.parent == null) {
                    root.addView(bgWv, FrameLayout.LayoutParams(1080, 720))
                }
                bgWv.alpha = 0f
                bgWv.visibility = View.INVISIBLE
            }

            // 2) 현재 활성 탭 WebView를 SwipeRefreshLayout 안에 표시
            val activeWv = SpaceManager.getOrCreateWebView(activeSpace.id, activeTab)
            val existingSwipe = root.findViewWithTag<SwipeRefreshLayout>("active_swipe")
            val swipe = existingSwipe ?: SwipeRefreshLayout(root.context).apply {
                tag = "active_swipe"
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                root.addView(this)
            }

            swipe.setOnRefreshListener {
                activeWv.reload()
                swipe.isRefreshing = false
            }

            if (activeWv.parent !== swipe) {
                (activeWv.parent as? ViewGroup)?.removeView(activeWv)
                swipe.removeAllViews()
                swipe.addView(
                    activeWv,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
            activeWv.alpha = 1f
            activeWv.visibility = View.VISIBLE
            swipe.bringToFront()
        }
    )
}

private suspend fun translateActiveSpacePage(spaceId: String) {
    runCatching {
        val rawNodes = SpaceManager.evaluateJsInSpace(spaceId, BrowserScripts.COLLECT_TRANSLATABLE_NODES_JS)
        val arr = JSONArray(rawNodes)
        if (arr.length() == 0) return
        val originals = (0 until arr.length()).map { arr.getJSONObject(it).getString("t") }
        val translated = Last30DaysEngine.translateBatchToKorean(originals)
        val outArr = JSONArray()
        for (i in translated.indices) {
            outArr.put(JSONObject().put("i", arr.getJSONObject(i).getInt("i")).put("t", translated[i]))
        }
        SpaceManager.evaluateJsInSpace(spaceId, BrowserScripts.applyTranslationsJs(outArr.toString()))
    }
}
