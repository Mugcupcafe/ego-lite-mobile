package com.egolite.mobile.ui

import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.BuildConfig
import com.egolite.mobile.CrashLogger
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.EgoWebViewFactory
import com.egolite.mobile.browser.FaviconCache
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.browser.TranslationManager
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.SearchEngine
import com.egolite.mobile.model.ThemeMode
import com.egolite.mobile.model.TranslationEngine
import com.egolite.mobile.model.WebFontMode
import com.egolite.mobile.tunnel.EgoForegroundService
import java.util.Locale

private enum class SettingsCategory(val title: String, val section: String) {
    PROFILE("ego(lite) 서비스", "내 서비스 설정"),
    SEARCH_ENGINE("검색엔진", "기본설정"),
    PRIVACY("개인 정보 보호 및 보안", "기본설정"),
    SAFETY("안전 확인", "기본설정"),
    DEFAULT_BROWSER("기본 브라우저", "기본설정"),
    TABS("탭 및 탭 그룹", "고급"),
    HOMEPAGE("홈페이지", "고급"),
    APPEARANCE("디자인", "고급"),
    ACCESSIBILITY("접근성", "고급"),
    SITE_SETTINGS("사이트 설정", "고급"),
    LANGUAGES("언어", "고급"),
    DOWNLOADS("다운로드", "고급"),
    ABOUT("ego-lite 정보", "고급")
}

@Composable
fun ChromeSettingsScreen(
    onOpenMcpDrawer: () -> Unit,
    onDismiss: () -> Unit
) {
    // 시스템 뒤로가기 시 앱 종료가 아니라 설정 페이지만 깔끔하게 닫힘
    BackHandler(enabled = true) {
        onDismiss()
    }

    val context = LocalContext.current
    val prefs by SpaceManager.preferences.collectAsState()
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()
    val adBlockState by AdBlockEngine.state.collectAsState()
    val chromiumVersion = remember { EgoWebViewFactory.getCurrentChromiumVersion(context) }

    var selectedCategory by remember { mutableStateOf(SettingsCategory.SEARCH_ENGINE) }
    var searchQuery by remember { mutableStateOf("") }

    val visibleCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) SettingsCategory.entries
        else SettingsCategory.entries.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 좌측 페인 (크롬 태블릿 1:1 카테고리 목록)
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "설정 닫기")
                    }
                    Text("설정", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }

                var lastSection = ""
                visibleCategories.forEach { cat ->
                    if (cat.section != lastSection) {
                        lastSection = cat.section
                        Text(
                            text = cat.section,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 6.dp)
                        )
                    }

                    val selected = selectedCategory == cat
                    val subtitle = when (cat) {
                        SettingsCategory.SEARCH_ENGINE -> prefs.searchEngine.label
                        SettingsCategory.HOMEPAGE -> if (prefs.homepageEnabled) "사용" else "사용 안 함"
                        SettingsCategory.APPEARANCE -> prefs.themeMode.label
                        SettingsCategory.PROFILE -> if (runtimeState.connectionState == McpConnectionState.CONNECTED) "MCP 연결됨" else "MCP 대기"
                        else -> null
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                            .clickable {
                                if (cat == SettingsCategory.DEFAULT_BROWSER) {
                                    // 요구사항 4: 기본 브라우저 클릭 시 즉시 시스템 기본 브라우저 설정 진입
                                    openSystemDefaultBrowserSettings(context)
                                }
                                selectedCategory = cat
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cat == SettingsCategory.PROFILE) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(EgoCobalt.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp), tint = EgoCobalt)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = cat.title,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 우측 페인 (상단 검색 바 + 크롬 스타일 Crossfade 전환)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                // 상단 설정 검색 바 (크롬 태블릿 1:1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("설정 검색", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 우측 페인 페이드인/페이드아웃 (Crossfade)
                Crossfade(
                    targetState = selectedCategory,
                    animationSpec = tween(durationMillis = 180),
                    label = "chromeSettingsRightPane",
                    modifier = Modifier.weight(1f)
                ) { category ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(category.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                        when (category) {
                            SettingsCategory.PROFILE -> {
                                ChromeGroupCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("MCP 서버 & HTTPS 터널", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                            Text(runtimeState.statusMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        OutlinedButton(onClick = {
                                            onDismiss()
                                            onOpenMcpDrawer()
                                        }) {
                                            Text("MCP 패널 열기")
                                        }
                                    }
                                }
                            }

                            SettingsCategory.SEARCH_ENGINE -> {
                                // 첨부 18008.jpg 크롬 검색엔진 1:1 카드 + 파비콘 아이콘
                                ChromeGroupCard {
                                    val primaries = SearchEngine.entries.filter { it.isPrimary }
                                    primaries.forEachIndexed { idx, engine ->
                                        SearchEngineRow(
                                            engine = engine,
                                            selected = prefs.searchEngine == engine,
                                            onSelect = { SpaceManager.updatePreferences { it.copy(searchEngine = engine) } }
                                        )
                                        if (idx < primaries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                                Text(
                                    text = "최근 방문",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                )
                                ChromeGroupCard {
                                    val recents = SearchEngine.entries.filterNot { it.isPrimary }
                                    recents.forEachIndexed { idx, engine ->
                                        SearchEngineRow(
                                            engine = engine,
                                            selected = prefs.searchEngine == engine,
                                            onSelect = { SpaceManager.updatePreferences { it.copy(searchEngine = engine) } }
                                        )
                                        if (idx < recents.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }

                            SettingsCategory.PRIVACY -> {
                                ChromeGroupCard {
                                    ChromeSwitchItem(
                                        title = "광고 및 트래커 차단",
                                        subtitle = "누적 ${adBlockState.totalBlockedCount}건 차단됨",
                                        checked = adBlockState.globalEnabled,
                                        onCheckedChange = { enabled ->
                                            AdBlockEngine.setGlobalEnabled(enabled)
                                            SpaceManager.updatePreferences { it.copy(adBlockEnabled = enabled) }
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                SpaceManager.clearHistoryAndCache()
                                                Toast.makeText(context, "방문 기록 및 캐시가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("인터넷 사용 기록 삭제", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                            Text("방문 기록, 캐시된 이미지 및 파일 삭제", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            SettingsCategory.SAFETY -> {
                                ChromeGroupCard {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("안전 확인 완료", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EgoEmerald)
                                        Text("Google Safe Browsing(WebView 기본 보호) 및 광고/트래커 차단이 활성화되어 있습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            SettingsCategory.DEFAULT_BROWSER -> {
                                ChromeGroupCard {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Ego Lite Mobile을 기본 브라우저로 설정", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text("외부 앱에서 웹 링크를 열 때 Ego Lite Mobile이 바로 실행됩니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        OutlinedButton(onClick = { openSystemDefaultBrowserSettings(context) }) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("기본 브라우저 설정 열기")
                                        }
                                    }
                                }
                            }

                            SettingsCategory.TABS -> {
                                // 첨부 18012.jpg 탭 및 탭 그룹 1:1
                                ChromeGroupCard {
                                    val dayOptions = listOf(0 to "안 함", 7 to "7일 동안 비활성 상태인 경우", 14 to "14일 동안 비활성 상태인 경우", 21 to "21일 동안 비활성 상태인 경우")
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("비활성 섹션으로 이동", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = dayOptions.firstOrNull { it.first == prefs.inactiveTabDays }?.second ?: "21일 동안 비활성 상태인 경우",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(0, 7, 14, 21).forEach { days ->
                                                val sel = prefs.inactiveTabDays == days
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.clickable { SpaceManager.updatePreferences { it.copy(inactiveTabDays = days) } }
                                                ) {
                                                    Text(
                                                        text = if (days == 0) "안 함" else "${days}일",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("동시 활성 AI Space 최대 개수: ${prefs.maxAiSpaces}개", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Slider(
                                            value = prefs.maxAiSpaces.toFloat(),
                                            onValueChange = { v -> SpaceManager.updatePreferences { it.copy(maxAiSpaces = v.toInt()) } },
                                            valueRange = 1f..8f,
                                            steps = 6
                                        )
                                    }
                                }
                            }

                            SettingsCategory.HOMEPAGE -> {
                                // 첨부 18013.jpg 홈페이지 1:1
                                ChromeGroupCard {
                                    ChromeSwitchItem(
                                        title = "사용",
                                        subtitle = null,
                                        checked = prefs.homepageEnabled,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(homepageEnabled = c, showHomeButton = c) } }
                                    )
                                }
                                Text("페이지 열기", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                ChromeGroupCard {
                                    val isDefaultHome = prefs.homepageUrl == NEW_TAB_URL
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { SpaceManager.updatePreferences { it.copy(homepageUrl = NEW_TAB_URL) } }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = isDefaultHome, onClick = { SpaceManager.updatePreferences { it.copy(homepageUrl = NEW_TAB_URL) } })
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ego Lite 홈페이지 (새 탭)", fontSize = 14.sp)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    var customHome by remember(prefs.homepageUrl) {
                                        mutableStateOf(if (isDefaultHome) "" else prefs.homepageUrl)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = !isDefaultHome,
                                            onClick = {
                                                val target = customHome.ifBlank { "https://www.google.com" }
                                                SpaceManager.updatePreferences { it.copy(homepageUrl = target) }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedTextField(
                                            value = customHome,
                                            onValueChange = { v ->
                                                customHome = v
                                                if (v.isNotBlank()) SpaceManager.updatePreferences { it.copy(homepageUrl = v) }
                                            },
                                            placeholder = { Text("맞춤 웹 주소 입력", fontSize = 13.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            SettingsCategory.APPEARANCE -> {
                                // 첨부 18014.jpg 디자인 1:1 + 웹페이지 기기 폰트 & 커스텀 .ttf 추가!
                                ChromeGroupCard {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("테마", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ThemeMode.entries.forEach { tm ->
                                                val sel = prefs.themeMode == tm
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.clickable {
                                                        SpaceManager.updatePreferences {
                                                            it.copy(themeMode = tm, darkModeEnabled = when (tm) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> SpaceManager.isSystemDarkMode() })
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        text = tm.label,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ChromeSwitchItem(
                                        title = "툴바 바로가기 (홈 버튼 표시)",
                                        subtitle = "주소창 왼쪽에 홈 버튼 표시",
                                        checked = prefs.showHomeButton,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showHomeButton = c) } }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ChromeSwitchItem(
                                        title = "북마크바 표시",
                                        subtitle = "주소창 아래에 북마크 바 표시",
                                        checked = prefs.showBookmarkBar,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showBookmarkBar = c) } }
                                    )
                                }

                                // 웹페이지 폰트 & 커스텀 .ttf 추가 섹션
                                Text("웹페이지 폰트", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                                ChromeGroupCard {
                                    WebFontMode.entries.forEachIndexed { idx, mode ->
                                        val selected = prefs.webFontMode == mode
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (mode == WebFontMode.CUSTOM_TTF && prefs.customFontPath.isBlank()) {
                                                        BrowserActivityBridge.launchFontPicker?.invoke()
                                                    } else {
                                                        SpaceManager.updatePreferences { it.copy(webFontMode = mode) }
                                                        SpaceManager.reload(SpaceManager.getActiveSpace().id)
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(mode.label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                                if (mode == WebFontMode.CUSTOM_TTF && prefs.customFontName.isNotBlank()) {
                                                    Text("적용 파일: ${prefs.customFontName}", fontSize = 11.sp, color = EgoEmerald)
                                                } else if (mode == WebFontMode.SYSTEM_FONT) {
                                                    Text("웹폰트 차단 후 기기 시스템 폰트로 렌더링", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            RadioButton(
                                                selected = selected,
                                                onClick = {
                                                    if (mode == WebFontMode.CUSTOM_TTF && prefs.customFontPath.isBlank()) {
                                                        BrowserActivityBridge.launchFontPicker?.invoke()
                                                    } else {
                                                        SpaceManager.updatePreferences { it.copy(webFontMode = mode) }
                                                        SpaceManager.reload(SpaceManager.getActiveSpace().id)
                                                    }
                                                }
                                            )
                                        }
                                        if (idx < WebFontMode.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(onClick = { BrowserActivityBridge.launchFontPicker?.invoke() }) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("커스텀 폰트 (.ttf / .otf) 파일 추가")
                                        }
                                    }
                                }
                            }

                            SettingsCategory.ACCESSIBILITY -> {
                                // 첨부 18015.jpg 접근성 1:1 (확대/축소 슬라이더 + 오즈의 마법사 미리보기 카드 + 토글 3종)
                                ChromeGroupCard {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("기본 확대/축소", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("방문하는 모든 사이트에서 텍스트 및 이미지를 확대 또는 축소합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${prefs.textZoom}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom - 5).coerceAtLeast(75)) }
                                            }) { Icon(Icons.Default.Remove, contentDescription = "축소") }
                                            Slider(
                                                value = prefs.textZoom.toFloat(),
                                                onValueChange = { v -> SpaceManager.updatePreferences { it.copy(textZoom = v.toInt()) } },
                                                valueRange = 75f..150f,
                                                steps = 14,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = {
                                                SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom + 5).coerceAtMost(150)) }
                                            }) { Icon(Icons.Default.Add, contentDescription = "확대") }
                                        }
                                    }
                                }
                                // 실시간 미리보기 카드 (오즈의 마법사)
                                ChromeGroupCard {
                                    val scale = prefs.textZoom / 100f
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("미리보기", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("오즈의 마법사", fontSize = (18f * scale).sp, fontWeight = FontWeight.Bold)
                                        Text("11장: 오즈의 멋진 에메랄드 도시", fontSize = (14f * scale).sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "도로시와 친구들은 녹색 안경으로 눈을 보호했지만, 처음에는 멋진 도시의 광채에 눈이 부셨습니다.",
                                            fontSize = (13f * scale).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                ChromeGroupCard {
                                    ChromeSwitchItem(
                                        title = "기본 메뉴에 확대/축소 옵션 표시",
                                        subtitle = "여러 사이트의 확대/축소 맞춤설정이 더 쉬워집니다.",
                                        checked = prefs.showZoomInMenu,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showZoomInMenu = c) } }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ChromeSwitchItem(
                                        title = "확대/축소 강제 사용",
                                        subtitle = "웹사이트의 확대 방지 요청 무시",
                                        checked = prefs.forceEnableZoom,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(forceEnableZoom = c) } }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ChromeSwitchItem(
                                        title = "웹페이지 간단히 보기",
                                        subtitle = "기사를 간단히 보기로 표시할 수 있을 때 알림 받기",
                                        checked = prefs.simplifiedViewPrompt,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(simplifiedViewPrompt = c) } }
                                    )
                                }
                            }

                            SettingsCategory.SITE_SETTINGS -> {
                                // 첨부 18016.jpg 사이트 설정 1:1
                                Text("권한", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                ChromeGroupCard {
                                    SitePermissionRow(Icons.Default.LocationOn, "위치", prefs.allowLocation) { c -> SpaceManager.updatePreferences { it.copy(allowLocation = c) } }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    SitePermissionRow(Icons.Default.CameraAlt, "카메라", prefs.allowCamera) { c -> SpaceManager.updatePreferences { it.copy(allowCamera = c) } }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    SitePermissionRow(Icons.Default.Mic, "마이크", prefs.allowMic) { c -> SpaceManager.updatePreferences { it.copy(allowMic = c) } }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    SitePermissionRow(Icons.Default.OpenInNew, "팝업 및 리디렉션 (본인인증)", prefs.allowPopups) { c -> SpaceManager.updatePreferences { it.copy(allowPopups = c) } }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    SitePermissionRow(Icons.Default.Code, "JavaScript", prefs.allowJavaScript) { c -> SpaceManager.updatePreferences { it.copy(allowJavaScript = c) } }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    SitePermissionRow(Icons.Default.Cookie, "타사 쿠키 허용", prefs.allowThirdPartyCookies) { c -> SpaceManager.updatePreferences { it.copy(allowThirdPartyCookies = c) } }
                                }
                            }

                            SettingsCategory.LANGUAGES -> {
                                // 첨부 18017.jpg 언어 1:1
                                ChromeGroupCard {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("기기의 현재 언어", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("${Locale.getDefault().displayLanguage} (${Locale.getDefault().displayCountry})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("번역 설정", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                ChromeGroupCard {
                                    ChromeSwitchItem(
                                        title = "다른 언어로 된 페이지를 번역하는 옵션 제공",
                                        subtitle = null,
                                        checked = prefs.offerTranslate,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(offerTranslate = c) } }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("번역 엔진", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = prefs.translationEngine == TranslationEngine.ONLINE_GOOGLE,
                                                onClick = { SpaceManager.updatePreferences { it.copy(translationEngine = TranslationEngine.ONLINE_GOOGLE) } }
                                            )
                                            Text("온라인 (Google)", fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            RadioButton(
                                                selected = prefs.translationEngine == TranslationEngine.OFFLINE_MLKIT,
                                                onClick = { SpaceManager.updatePreferences { it.copy(translationEngine = TranslationEngine.OFFLINE_MLKIT) } }
                                            )
                                            Text("온디바이스 오프라인 (ML Kit)", fontSize = 13.sp)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("번역 도착어", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(TranslationManager.labelFor(prefs.defaultTranslateTarget), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            SettingsCategory.DOWNLOADS -> {
                                // 첨부 18018.jpg 다운로드 1:1
                                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                                ChromeGroupCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("다운로드 위치", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                            Text("다운로드 $downloadDir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ChromeSwitchItem(
                                        title = "파일 저장 위치 묻기",
                                        subtitle = "다운로드 시작 전 알림 표시",
                                        checked = prefs.askDownloadLocation,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(askDownloadLocation = c) } }
                                    )
                                }
                            }

                            SettingsCategory.ABOUT -> {
                                // 첨부 18019.jpg 정보 1:1
                                ChromeGroupCard {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("애플리케이션 버전", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("Ego Lite Mobile ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE}) · $chromiumVersion", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("운영체제", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("법적 정보", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("Ego Lite Mobile Open Project · Android System WebView / Ktor / ML Kit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                val lastCrash = remember { CrashLogger.readLastCrash(context) }
                                if (!lastCrash.isNullOrBlank()) {
                                    OutlinedButton(onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("CrashLog", lastCrash))
                                        Toast.makeText(context, "최근 크래시 로그 복사됨", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("최근 크래시 로그 복사")
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
private fun ChromeGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SearchEngineRow(
    engine: SearchEngine,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    var iconBitmap by remember(engine.domain) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(engine.domain) {
        val bmp = FaviconCache.loadFavicon(context, "https://${engine.domain}")
        if (bmp != null) iconBitmap = bmp.asImageBitmap()
    }

    val badgeColor = when (engine) {
        SearchEngine.GOOGLE -> Color(0xFF4285F4)
        SearchEngine.BING -> Color(0xFF008373)
        SearchEngine.DUCKDUCKGO -> Color(0xFFDE5833)
        SearchEngine.ECOSIA -> Color(0xFF56B847)
        SearchEngine.BRAVE -> Color(0xFFFB542B)
        SearchEngine.NAVER -> Color(0xFF03C75A)
        SearchEngine.DAUM -> Color(0xFF4C80F1)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap!!,
                contentDescription = engine.label,
                modifier = Modifier.size(26.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (engine.isPrimary) {
                    Text(
                        text = engine.label.take(1).uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                } else {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(engine.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(engine.domain, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onSelect)
    }
}

@Composable
private fun ChromeSwitchItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SitePermissionRow(
    icon: ImageVector,
    title: String,
    allowed: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!allowed) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (allowed) "허용됨" else "허용되지 않음",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = allowed, onCheckedChange = onToggle)
    }
}

private fun openSystemDefaultBrowserSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER) && !rm.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "시스템 기본 앱 설정을 열 수 없습니다", Toast.LENGTH_SHORT).show()
    }
}
