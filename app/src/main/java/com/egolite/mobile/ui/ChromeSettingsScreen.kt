package com.egolite.mobile.ui

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.SearchEngine
import com.egolite.mobile.model.TranslationEngine
import com.egolite.mobile.model.WebFontMode
import com.egolite.mobile.tunnel.EgoForegroundService

private enum class SettingsCategory(val title: String, val section: String) {
    PROFILE("ego(lite) 상태", "내 서비스 설정"),
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

/**
 * 태블릿 크롬 1:1 2-패널 전체 화면 설정 페이지
 */
@Composable
fun ChromeSettingsScreen(
    onOpenMcpDrawer: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs by SpaceManager.preferences.collectAsState()
    val adBlockState by AdBlockEngine.state.collectAsState()
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()
    val chromiumVersion = remember { EgoWebViewFactory.getCurrentChromiumVersion(context) }

    var selectedCategory by remember { mutableStateOf(SettingsCategory.PROFILE) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCategories = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) SettingsCategory.entries
        else SettingsCategory.entries.filter { it.title.lowercase().contains(q) || it.section.lowercase().contains(q) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 좌측 카테고리 레일 (크롬 태블릿 설정과 동일한 구조)
            Column(
                modifier = Modifier
                    .width(268.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                    Text("설정", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }

                // 상단: 내 ego(lite) 프로필/MCP 카드
                val isProfileSelected = selectedCategory == SettingsCategory.PROFILE
                val isConnected = runtimeState.connectionState == McpConnectionState.CONNECTED
                Text(
                    text = "내 ego(lite) 설정",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isProfileSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectedCategory = SettingsCategory.PROFILE }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) EgoEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = null,
                            tint = if (isConnected) EgoEmerald else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("ego-lite-mobile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (isConnected) "MCP · HTTPS 연결됨" else "로컬 브라우저 모드",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 기본설정 섹션
                val basicItems = filteredCategories.filter { it.section == "기본설정" }
                if (basicItems.isNotEmpty()) {
                    Text(
                        text = "기본설정",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    basicItems.forEach { cat ->
                        val sub = when (cat) {
                            SettingsCategory.SEARCH_ENGINE -> prefs.searchEngine.label
                            else -> null
                        }
                        SettingsRailItem(
                            title = cat.title,
                            subtitle = sub,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 고급 섹션
                val advItems = filteredCategories.filter { it.section == "고급" }
                if (advItems.isNotEmpty()) {
                    Text(
                        text = "고급",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    advItems.forEach { cat ->
                        val sub = when (cat) {
                            SettingsCategory.HOMEPAGE -> if (prefs.homepageUrl == NEW_TAB_URL) "사용 (새 탭)" else prefs.homepageUrl
                            SettingsCategory.APPEARANCE -> if (prefs.darkModeEnabled) "어둡게" else "밝게"
                            SettingsCategory.ACCESSIBILITY -> "${prefs.textZoom}% · ${prefs.webFontMode.label}"
                            else -> null
                        }
                        SettingsRailItem(
                            title = cat.title,
                            subtitle = sub,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 우측 상세 패널 (상단 설정 검색 바 + 선택 카테고리 카드)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 상단 설정 검색 바 (크롬 1:1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
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

                Text(
                    text = selectedCategory.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (selectedCategory) {
                    SettingsCategory.PROFILE -> {
                        SettingsCard {
                            Text("MCP · HTTPS 터널 제어", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                text = "상태: ${runtimeState.statusMessage}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                onDismiss()
                                onOpenMcpDrawer()
                            }) {
                                Text("MCP · 터널 설정 패널 열기")
                            }
                        }
                        SettingsCard {
                            Text("로컬 데이터 관리", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                text = "북마크·열린 탭·설정·광고차단 규칙은 기기에 자동 저장됩니다.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { BrowserActivityBridge.launchBookmarkImport?.invoke() }) {
                                    Text("북마크 가져오기 (.html)")
                                }
                                OutlinedButton(onClick = { BrowserActivityBridge.launchBookmarkExport?.invoke() }) {
                                    Text("북마크 내보내기 (.html)")
                                }
                            }
                        }
                    }

                    SettingsCategory.SEARCH_ENGINE -> {
                        SettingsCard {
                            SearchEngine.entries.forEach { se ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { SpaceManager.updatePreferences { it.copy(searchEngine = se) } }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = prefs.searchEngine == se,
                                        onClick = { SpaceManager.updatePreferences { it.copy(searchEngine = se) } }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(se.label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text(se.searchUrlPrefix, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.PRIVACY -> {
                        SettingsCard {
                            SettingsSwitchItem(
                                title = "광고 및 트래커 차단",
                                subtitle = "누적 ${adBlockState.totalBlockedCount}건 차단됨",
                                checked = adBlockState.globalEnabled,
                                onCheckedChange = { enabled ->
                                    AdBlockEngine.setGlobalEnabled(enabled)
                                    SpaceManager.updatePreferences { it.copy(adBlockEnabled = enabled) }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SettingsSwitchItem(
                                title = "배너·제휴 영역 코스메틱 숨김",
                                subtitle = "페이지 내 광고 프레임을 CSS로 숨깁니다",
                                checked = adBlockState.cosmeticEnabled,
                                onCheckedChange = { AdBlockEngine.setCosmeticEnabled(it) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            OutlinedButton(
                                onClick = {
                                    SpaceManager.clearHistoryAndCache()
                                    Toast.makeText(context, "방문 기록 및 캐시 삭제됨", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("방문 기록 및 웹 캐시 삭제")
                            }
                        }
                    }

                    SettingsCategory.SAFETY -> {
                        SettingsCard {
                            Text("WebView 보안 엔진", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(chromiumVersion, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("크래시 로그 저장 경로", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = CrashLogger.getExternalCrashDirPath(context),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                val log = CrashLogger.readLastCrash(context)
                                if (log != null) {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("CrashLog", log))
                                    Toast.makeText(context, "최근 크래시 로그 복사됨", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "저장된 크래시 로그가 없습니다", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("최근 크래시 로그 복사")
                            }
                        }
                    }

                    SettingsCategory.DEFAULT_BROWSER -> {
                        SettingsCard {
                            Text("기본 브라우저 앱 설정", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                text = "시스템 기본 앱 설정에서 ego-lite-mobile을 기본 브라우저로 지정할 수 있습니다.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
                            }) {
                                Text("안드로이드 기본 앱 설정 열기")
                            }
                        }
                    }

                    SettingsCategory.TABS -> {
                        SettingsCard {
                            Text("최대 AI Space 개수 (${prefs.maxAiSpaces}개)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Slider(
                                value = prefs.maxAiSpaces.toFloat(),
                                onValueChange = { v -> SpaceManager.updatePreferences { it.copy(maxAiSpaces = v.toInt()) } },
                                valueRange = 1f..8f,
                                steps = 6
                            )
                            Text(
                                text = "Personal Space의 열린 탭은 앱 재시작 시 자동으로 복원됩니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SettingsCategory.HOMEPAGE -> {
                        var hpInput by remember(prefs.homepageUrl) { mutableStateOf(prefs.homepageUrl) }
                        SettingsCard {
                            Text("홈페이지 / 새 탭 주소", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            OutlinedTextField(
                                value = hpInput,
                                onValueChange = { hpInput = it },
                                label = { Text("기본값: ego://newtab") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    SpaceManager.updatePreferences { it.copy(homepageUrl = hpInput.trim().ifBlank { NEW_TAB_URL }) }
                                    Toast.makeText(context, "홈페이지 저장됨", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("저장")
                                }
                                OutlinedButton(onClick = {
                                    hpInput = NEW_TAB_URL
                                    SpaceManager.updatePreferences { it.copy(homepageUrl = NEW_TAB_URL) }
                                }) {
                                    Text("기본 새 탭으로 초기화")
                                }
                            }
                        }
                    }

                    SettingsCategory.APPEARANCE -> {
                        SettingsCard {
                            SettingsSwitchItem(
                                title = "야간(다크) 모드",
                                subtitle = "앱 UI 및 웹페이지에 어두운 테마 적용",
                                checked = prefs.darkModeEnabled,
                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(darkModeEnabled = c) } }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SettingsSwitchItem(
                                title = "주소창 하단 북마크 바 표시",
                                subtitle = "크롬처럼 주소창 아래에 북마크 바 고정",
                                checked = prefs.showBookmarkBar,
                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showBookmarkBar = c) } }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SettingsSwitchItem(
                                title = "데스크톱 사이트 모드",
                                subtitle = "13.3인치 태블릿에서 PC 화면으로 로드 (끄면 유튜브 등이 더 가볍게 로드됨)",
                                checked = prefs.desktopModeEnabled,
                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(desktopModeEnabled = c) } }
                            )
                        }
                    }

                    SettingsCategory.ACCESSIBILITY -> {
                        SettingsCard {
                            Text("페이지 글자 크기 (${prefs.textZoom}%)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Slider(
                                value = prefs.textZoom.toFloat(),
                                onValueChange = { v -> SpaceManager.updatePreferences { it.copy(textZoom = v.toInt()) } },
                                valueRange = 75f..150f,
                                steps = 14
                            )
                        }
                        SettingsCard {
                            Text("웹페이지 폰트 설정", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                text = "웹페이지 본문에 적용할 폰트 방식을 선택합니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            WebFontMode.entries.forEach { mode ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { SpaceManager.updatePreferences { it.copy(webFontMode = mode) } }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = prefs.webFontMode == mode,
                                        onClick = { SpaceManager.updatePreferences { it.copy(webFontMode = mode) } }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(mode.label, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    SettingsCategory.SITE_SETTINGS -> {
                        SettingsCard {
                            Text("권한 및 사이트 호환성", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                text = "• JavaScript · DOM 저장소 · 서드파티 쿠키: 허용됨\n• 한국 간편인증/결제 딥링크(intent://, kakaotalk:// 등): 허용됨\n• 팝업창(window.open) 및 파일 첨부: 허용됨\n• Google 로그인 WebView 차단 우회(X-Requested-With 제거): 활성화됨",
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SettingsCategory.LANGUAGES -> {
                        SettingsCard {
                            Text("번역 엔진 선택", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = prefs.translationEngine == TranslationEngine.ONLINE_GOOGLE,
                                    onClick = { SpaceManager.updatePreferences { it.copy(translationEngine = TranslationEngine.ONLINE_GOOGLE) } }
                                )
                                Text("온라인 (Google 번역)", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(
                                    selected = prefs.translationEngine == TranslationEngine.OFFLINE_MLKIT,
                                    onClick = { SpaceManager.updatePreferences { it.copy(translationEngine = TranslationEngine.OFFLINE_MLKIT) } }
                                )
                                Text("온디바이스 오프라인 (ML Kit)", fontSize = 14.sp)
                            }
                        }
                        SettingsCard {
                            Text("항상 자동 번역 사이트 (${prefs.autoTranslateHosts.size}개)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (prefs.autoTranslateHosts.isEmpty()) {
                                Text("등록된 사이트가 없습니다. 번역 알약 팝업의 ☆ 아이콘으로 추가할 수 있습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                prefs.autoTranslateHosts.forEach { host ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(host, fontSize = 13.sp)
                                        OutlinedButton(onClick = { SpaceManager.toggleAutoTranslateHost(host) }) {
                                            Text("해제", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.DOWNLOADS -> {
                        SettingsCard {
                            Text("다운로드 위치", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("기기 기본 Downloads 폴더에 저장됩니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                            }) {
                                Text("시스템 다운로드 폴더 열기")
                            }
                        }
                    }

                    SettingsCategory.ABOUT -> {
                        SettingsCard {
                            Text("ego-lite-mobile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("버전: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})", fontSize = 13.sp)
                            Text("패키지: ${context.packageName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("렌더링 엔진: $chromiumVersion", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRailItem(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
