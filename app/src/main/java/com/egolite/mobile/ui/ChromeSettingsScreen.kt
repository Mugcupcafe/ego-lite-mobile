package com.egolite.mobile.ui

import com.egolite.mobile.model.TabWidthMode
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.foundation.horizontalScroll
import kotlin.math.roundToInt
import com.egolite.mobile.model.EqualizerMorphStyle
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import com.egolite.mobile.browser.ExtensionManager
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import com.egolite.mobile.account.GoogleAccountManager
import kotlinx.coroutines.launch
import com.egolite.mobile.BuildConfig
import com.egolite.mobile.nativebridge.EgoRustBridge
import com.egolite.mobile.CrashLogger
import com.egolite.mobile.browser.AdBlockEngine
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.EgoWebViewFactory
import com.egolite.mobile.browser.FaviconCache
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.browser.TranslationManager
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.MonitorPosition
import com.egolite.mobile.model.SearchEngine
import com.egolite.mobile.model.CardVanishStyle
import com.egolite.mobile.model.VanishDirection
import com.egolite.mobile.model.ThemeMode
import com.egolite.mobile.model.ThemeColorPreset
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextOverflow
import com.egolite.mobile.model.TranslationEngine
import com.egolite.mobile.model.WebFontMode
import com.egolite.mobile.tunnel.EgoForegroundService
import java.util.Locale

enum class SettingsCategory(val title: String, val section: String) {
    ACCOUNT("Google 계정", "내 Google 서비스 설정"),
    PROFILE("ego(lite) 서비스", "내 Google 서비스 설정"),
    SEARCH_ENGINE("검색엔진", "기본설정"),
    PRIVACY("개인 정보 보호 및 보안", "기본설정"),
    SAFETY("안전 확인", "기본설정"),
    DEFAULT_BROWSER("기본 브라우저", "기본설정"),
    TABS("탭 및 탭 그룹", "고급"),
    HOMEPAGE("홈페이지", "고급"),
    APPEARANCE("디자인", "고급"),
    AI_MONITOR("AI 라이브 모니터", "고급"),
    ACCESSIBILITY("접근성", "고급"),
    SITE_SETTINGS("사이트 설정", "고급"),
    LANGUAGES("언어", "고급"),
    DOWNLOADS("다운로드", "고급"),
    ABOUT("ego-lite 정보", "고급")
}

@Composable
fun ChromeSettingsScreen(
    initialCategory: SettingsCategory = SettingsCategory.ACCOUNT,
    onOpenMcpDrawer: () -> Unit,
    onOpenGoogleSignInTab: () -> Unit = {},
    onExtractGoogleProfile: ((Boolean, String) -> Unit) -> Unit = { cb -> cb(false, "브라우저 탭을 확인해 주세요.") },
    onDismiss: () -> Unit
) {
    var showThemeSubpage by remember { mutableStateOf(false) }

    // 시스템 뒤로가기 시 하위 화면(디자인 > 테마)이 열려 있으면 상위 디자인 화면으로, 아니면 설정 닫기
    BackHandler(enabled = true) {
        if (showThemeSubpage) {
            showThemeSubpage = false
        } else {
            onDismiss()
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by SpaceManager.preferences.collectAsState()
    val runtimeState by EgoForegroundService.runtimeState.collectAsState()
    val adBlockState by AdBlockEngine.state.collectAsState()
    val googleAccount by GoogleAccountManager.state.collectAsState()
    val googleAvatar by GoogleAccountManager.avatarBitmap.collectAsState()
    val chromiumVersion = remember { EgoWebViewFactory.getCurrentChromiumVersion(context) }

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }

    val visibleCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) SettingsCategory.entries
        else SettingsCategory.entries.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val isDarkCanvas = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val egoChromeColors = LocalEgoChromeColors.current
    val settingsCanvasColor = egoChromeColors.settingsCanvasBg
    val settingsCardColor = egoChromeColors.settingsCardBg
    val selectedNavColor = egoChromeColors.selectedNavBg

    val csScope = rememberCoroutineScope()
    val dismissSwipeX = remember { Animatable(0f) }
    val leftPaneElastic = rememberElasticOverscroll(isVertical = true, maxPullPx = 80f)
    val rightPaneElastic = rememberElasticOverscroll(isVertical = true, maxPullPx = 88f)

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = settingsCanvasColor
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 좌측 페인 (Chrome Material 3 Expressive 2-패널, 가운데 구분선 없음)
            Column(
                modifier = Modifier
                    .width(292.dp)
                    .fillMaxHeight()
                    .background(settingsCanvasColor)
                    .then(leftPaneElastic.containerModifier)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        EgoIcon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "설정 닫기",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "설정",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                var lastSection = ""
                visibleCategories.forEach { cat ->
                    if (cat.section != lastSection) {
                        lastSection = cat.section
                        Text(
                            text = cat.section,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp)
                        )
                    }

                    val selected = selectedCategory == cat
                    val displayTitle = when (cat) {
                        SettingsCategory.ACCOUNT -> if (googleAccount.isSignedIn) googleAccount.displayName.ifBlank { "Google 계정" } else "Google 로그인"
                        else -> cat.title
                    }
                    val subtitle = when (cat) {
                        SettingsCategory.ACCOUNT -> if (googleAccount.isSignedIn) googleAccount.email else "프로필 및 자체 동기화 설정"
                        SettingsCategory.SEARCH_ENGINE -> prefs.searchEngine.label
                        SettingsCategory.HOMEPAGE -> if (prefs.homepageEnabled) "사용" else "사용 안 함"
                        SettingsCategory.APPEARANCE -> prefs.themeMode.label
                        SettingsCategory.AI_MONITOR -> "세로: ${prefs.monitorPosPortrait.label} · 가로: ${prefs.monitorPosLandscape.label}"
                        SettingsCategory.PROFILE -> if (runtimeState.connectionState == McpConnectionState.CONNECTED) "MCP 연결됨" else "MCP 대기"
                        else -> null
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) selectedNavColor else Color.Transparent)
                            .clickable {
                                if (cat == SettingsCategory.DEFAULT_BROWSER) {
                                    openSystemDefaultBrowserSettings(context)
                                }
                                selectedCategory = cat
                                showThemeSubpage = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cat == SettingsCategory.ACCOUNT) {
                            if (googleAccount.isSignedIn && googleAvatar != null) {
                                Image(
                                    bitmap = googleAvatar!!.asImageBitmap(),
                                    contentDescription = "Google 프로필 사진",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (googleAccount.isSignedIn) googleAccount.displayName.take(1).uppercase().ifBlank { "G" } else "G",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        } else if (cat == SettingsCategory.PROFILE) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                EgoIcon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                , symbolSize = 18.dp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = displayTitle,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 우측 페인 (M3 Expressive 검색 알약 바 + 개별 세그먼트 카드 그룹)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 28.dp, top = 14.dp, bottom = 14.dp)
            ) {
                // 상단 설정 검색 바 (M3 Expressive 화이트 필)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(settingsCardColor)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EgoIcon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    , symbolSize = 20.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("설정 검색", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            .then(rightPaneElastic.containerModifier)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp + rightPaneElastic.extraSpacingDp)
                    ) {
                        if (category == SettingsCategory.APPEARANCE && showThemeSubpage) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "디자인",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showThemeSubpage = false }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                EgoIcon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                , symbolSize = 22.dp)
                                Text(
                                    text = "테마",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Text(
                                text = category.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        when (category) {
                            SettingsCategory.ACCOUNT -> {
                                var clientIdInput by remember(googleAccount.oauthWebClientId) {
                                    mutableStateOf(googleAccount.oauthWebClientId)
                                }
                                var showOAuthAdvanced by remember { mutableStateOf(false) }
                                var statusMessage by remember { mutableStateOf<String?>(null) }

                                // 상단 중앙 프로필 헤더 (Chrome M3 Expressive 1:1)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (googleAccount.isSignedIn && googleAvatar != null) {
                                        Image(
                                            bitmap = googleAvatar!!.asImageBitmap(),
                                            contentDescription = "Google 프로필 사진",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (googleAccount.isSignedIn) googleAccount.displayName.take(1).uppercase().ifBlank { "G" } else "G",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (googleAccount.isSignedIn) googleAccount.displayName.ifBlank { "Google 계정" } else "로그인되지 않음",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (googleAccount.isSignedIn) googleAccount.email else "Google 계정으로 로그인해 프로필 사진과 자체 동기화 설정을 관리하세요",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!statusMessage.isNullOrBlank()) {
                                        Text(
                                            text = statusMessage!!,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // 로그인 / 세션 연동 액션 카드
                                M3SettingsGroup(settingsCardColor) {
                                    if (!googleAccount.isSignedIn) {
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("Google 계정 로그인", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                Text(
                                                    "Android Credential Manager 정식 로그인 또는 브라우저에 로그인된 Google 세션에서 프로필과 사진을 바로 연동할 수 있습니다.",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Button(onClick = {
                                                        val act = context as? Activity
                                                        if (act == null) {
                                                            statusMessage = "Activity 컨텍스트를 확인할 수 없습니다."
                                                        } else if (clientIdInput.isBlank()) {
                                                            showOAuthAdvanced = true
                                                            statusMessage = "Web Client ID를 입력하거나 오른쪽 '브라우저 세션 연동'을 눌러주세요."
                                                        } else {
                                                            GoogleAccountManager.updateOAuthWebClientId(clientIdInput)
                                                            scope.launch {
                                                                val res = GoogleAccountManager.signInWithCredentialManager(act, clientIdInput)
                                                                res.onSuccess {
                                                                    statusMessage = "Google 로그인 완료 (${it.email})"
                                                                }.onFailure { err ->
                                                                    statusMessage = err.message ?: "로그인에 실패했습니다."
                                                                }
                                                            }
                                                        }
                                                    }) {
                                                        Text("Google 계정으로 로그인")
                                                    }
                                                    OutlinedButton(onClick = {
                                                        onExtractGoogleProfile { ok, msg ->
                                                            statusMessage = msg
                                                            if (!ok) {
                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }) {
                                                        Text("브라우저 세션 연동")
                                                    }
                                                }
                                                TextButton(onClick = {
                                                    onDismiss()
                                                    onOpenGoogleSignInTab()
                                                }) {
                                                    Text("새 탭에서 Google 로그인 페이지 열기")
                                                }
                                            }
                                        }
                                    } else {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("연동 방식", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                    Text(
                                                        text = if (googleAccount.authMode == "CREDENTIAL_MANAGER") "Android Google 로그인 (Credential Manager)" else "브라우저 Google 세션 프로필 연동",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                OutlinedButton(onClick = {
                                                    scope.launch {
                                                        GoogleAccountManager.signOut(context as? Activity)
                                                        statusMessage = "로그아웃되었습니다."
                                                    }
                                                }) {
                                                    Text("로그아웃")
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showOAuthAdvanced = !showOAuthAdvanced }
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Google Cloud OAuth Web Client ID", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                    Text(
                                                        text = if (clientIdInput.isBlank()) "미설정 (정식 Credential Manager 로그인 시 입력)" else clientIdInput.take(28) + "...",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                EgoIcon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (showOAuthAdvanced) {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Text("발급 방법 (5단계 가이드)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Text("1. Google Cloud Console 접속 후 새 프로젝트 생성", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Text("2. [API 및 서비스 → OAuth 동의 화면]에서 외부(External) 선택 후 본인 Gmail을 테스트 사용자로 추가", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Text("3. [사용자 인증 정보 → + 사용자 인증 정보 만들기 → OAuth 클라이언트 ID] 클릭", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Text("4. 애플리케이션 유형을 반드시 '웹 애플리케이션(Web application)'으로 선택 후 생성", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Text("5. 발급된 'xxxx.apps.googleusercontent.com' 주소를 아래에 붙여넣고 저장", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Text("※ 간단하게 쓰려면 이 설정 없이 위 '브라우저 세션 연동'만 눌러도 바로 동기화됩니다.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                OutlinedTextField(
                                                    value = clientIdInput,
                                                    onValueChange = { clientIdInput = it },
                                                    singleLine = true,
                                                    placeholder = { Text("xxxx.apps.googleusercontent.com", fontSize = 13.sp) },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    TextButton(onClick = {
                                                        val spId = SpaceManager.spaces.value.firstOrNull()?.id ?: return@TextButton
                                                        SpaceManager.createTab(spId, initialUrl = "https://console.cloud.google.com/apis/credentials", selectImmediately = true)
                                                        onDismiss()
                                                    }) {
                                                        Text("Cloud Console 열기", fontSize = 12.5.sp)
                                                    }
                                                    Button(onClick = {
                                                        GoogleAccountManager.updateOAuthWebClientId(clientIdInput)
                                                        Toast.makeText(context, "Web Client ID 저장됨", Toast.LENGTH_SHORT).show()
                                                    }) {
                                                        Text("저장")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "내 Google 계정에서 (자체 동기화 항목)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                                )
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        ChromeSwitchItem(
                                            title = "방문 기록 및 탭",
                                            subtitle = null,
                                            checked = googleAccount.syncHistoryAndTabs,
                                            onCheckedChange = { GoogleAccountManager.updateSyncOptions(historyAndTabs = it) }
                                        )
                                    }
                                    item {
                                        ChromeSwitchItem(
                                            title = "북마크",
                                            subtitle = null,
                                            checked = googleAccount.syncBookmarks,
                                            onCheckedChange = { GoogleAccountManager.updateSyncOptions(bookmarks = it) }
                                        )
                                    }
                                    item {
                                        ChromeSwitchItem(
                                            title = "설정",
                                            subtitle = null,
                                            checked = googleAccount.syncSettings,
                                            onCheckedChange = { GoogleAccountManager.updateSyncOptions(settings = it) }
                                        )
                                    }
                                    item {
                                        ChromeSwitchItem(
                                            title = "AI 스페이스",
                                            subtitle = "생성한 AI 스페이스와 탭 보관",
                                            checked = googleAccount.syncAiSpaces,
                                            onCheckedChange = { GoogleAccountManager.updateSyncOptions(aiSpaces = it) }
                                        )
                                    }
                                }
                            }

                            SettingsCategory.PROFILE -> {
                                ChromeGroupCard(cardColor = settingsCardColor) {
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
                                M3SettingsGroup(settingsCardColor) {
                                    val primaries = SearchEngine.entries.filter { it.isPrimary }
                                    primaries.forEach { engine ->
                                        item {
                                            SearchEngineRow(
                                                engine = engine,
                                                selected = prefs.searchEngine == engine,
                                                onSelect = { SpaceManager.updatePreferences { it.copy(searchEngine = engine) } }
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "최근 방문",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                )
                                M3SettingsGroup(settingsCardColor) {
                                    val recents = SearchEngine.entries.filterNot { it.isPrimary }
                                    recents.forEach { engine ->
                                        item {
                                            SearchEngineRow(
                                                engine = engine,
                                                selected = prefs.searchEngine == engine,
                                                onSelect = { SpaceManager.updatePreferences { it.copy(searchEngine = engine) } }
                                            )
                                        }
                                    }
                                }
                            }

                            SettingsCategory.PRIVACY -> {
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        ChromeSwitchItem(
                                            title = "광고 및 트래커 차단",
                                            subtitle = "누적 ${adBlockState.totalBlockedCount}건 차단됨",
                                            checked = adBlockState.globalEnabled,
                                            onCheckedChange = { enabled ->
                                                AdBlockEngine.setGlobalEnabled(enabled)
                                                SpaceManager.updatePreferences { it.copy(adBlockEnabled = enabled) }
                                            }
                                        )
                                    }
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    SpaceManager.clearHistoryAndCache()
                                                    Toast.makeText(context, "방문 기록 및 캐시가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text("인터넷 사용 기록 삭제", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                Text("방문 기록, 캐시된 이미지 및 파일 삭제", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsCategory.SAFETY -> {
                                ChromeGroupCard(cardColor = settingsCardColor) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("안전 확인 완료", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EgoEmerald)
                                        Text("Google Safe Browsing(WebView 기본 보호) 및 광고/트래커 차단이 활성화되어 있습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            SettingsCategory.DEFAULT_BROWSER -> {
                                ChromeGroupCard(cardColor = settingsCardColor) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Ego Lite Mobile을 기본 브라우저로 설정", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text("외부 앱에서 웹 링크를 열 때 Ego Lite Mobile이 바로 실행됩니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        OutlinedButton(onClick = { openSystemDefaultBrowserSettings(context) }) {
                                            EgoIcon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), symbolSize = 16.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("기본 브라우저 설정 열기")
                                        }
                                    }
                                }
                            }

                            SettingsCategory.TABS -> {
                                // 첨부 18012.jpg 탭 및 탭 그룹 1:1
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        val dayOptions = listOf(0 to "안 함", 7 to "7일 동안 비활성 상태인 경우", 14 to "14일 동안 비활성 상태인 경우", 21 to "21일 동안 비활성 상태인 경우")
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("비활성 섹션으로 이동", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Text(
                                                text = dayOptions.firstOrNull { it.first == prefs.inactiveTabDays }?.second ?: "21일 동안 비활성 상태인 경우",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(0, 7, 14, 21).forEach { days ->
                                                    val sel = prefs.inactiveTabDays == days
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.clickable { SpaceManager.updatePreferences { it.copy(inactiveTabDays = days) } }
                                                    ) {
                                                        Text(
                                                            text = if (days == 0) "안 함" else "${days}일",
                                                            fontSize = 13.sp,
                                                            fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                                                            color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                            Text("동시 활성 AI 공간 최대 개수: ${prefs.maxAiSpaces}개", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Slider(
                                                value = prefs.maxAiSpaces.toFloat(),
                                                onValueChange = { v -> SpaceManager.updatePreferences { it.copy(maxAiSpaces = v.toInt()) } },
                                                valueRange = 1f..8f,
                                                steps = 6
                                            )
                                        }
                                    }
                                }
                            }

                            SettingsCategory.HOMEPAGE -> {
                                // 첨부 18013.jpg 홈페이지 1:1
                                ChromeGroupCard(cardColor = settingsCardColor) {
                                    ChromeSwitchItem(
                                        title = "사용",
                                        subtitle = null,
                                        checked = prefs.homepageEnabled,
                                        onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(homepageEnabled = c, showHomeButton = c) } }
                                    )
                                }
                                Text("페이지 열기", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                M3SettingsGroup(settingsCardColor) {
                                    val isDefaultHome = prefs.homepageUrl == NEW_TAB_URL
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { SpaceManager.updatePreferences { it.copy(homepageUrl = NEW_TAB_URL) } }
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = isDefaultHome, onClick = { SpaceManager.updatePreferences { it.copy(homepageUrl = NEW_TAB_URL) } })
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Ego Lite 홈페이지 (새 탭)", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                        }
                                    }
                                    item {
                                        var customHome by remember(prefs.homepageUrl) {
                                            mutableStateOf(if (isDefaultHome) "" else prefs.homepageUrl)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = !isDefaultHome,
                                                onClick = {
                                                    val target = customHome.ifBlank { "https://www.google.com" }
                                                    SpaceManager.updatePreferences { it.copy(homepageUrl = target) }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            OutlinedTextField(
                                                value = customHome,
                                                onValueChange = { v ->
                                                    customHome = v
                                                    if (v.isNotBlank()) SpaceManager.updatePreferences { it.copy(homepageUrl = v) }
                                                },
                                                placeholder = { Text("맞춤 웹 주소 입력", fontSize = 14.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            SettingsCategory.APPEARANCE -> {
                                AnimatedContent(
                                    targetState = showThemeSubpage,
                                    transitionSpec = {
                                        if (targetState) {
                                            (slideInHorizontally(EgoMotion.intOffsetSpring) { it / 3 } + fadeIn(tween(180)))
                                                .togetherWith(slideOutHorizontally(tween(150)) { -it / 4 } + fadeOut(tween(120)))
                                        } else {
                                            (slideInHorizontally(EgoMotion.intOffsetSpring) { -it / 3 } + fadeIn(tween(180)))
                                                .togetherWith(slideOutHorizontally(tween(150)) { it / 4 } + fadeOut(tween(120)))
                                        }
                                    },
                                    label = "appearanceThemeMorph"
                                ) { inThemePage ->
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (inThemePage) {
                                    // 크롬 태블릿 1:1 디자인 > 테마 하위 화면
                                    val selectTheme: (ThemeMode) -> Unit = { tm ->
                                        SpaceManager.updatePreferences {
                                            it.copy(
                                                themeMode = tm,
                                                darkModeEnabled = when (tm) {
                                                    ThemeMode.DARK -> true
                                                    ThemeMode.LIGHT -> false
                                                    ThemeMode.SYSTEM -> SpaceManager.isSystemDarkMode()
                                                }
                                            )
                                        }
                                    }

                                    // 1. 시스템 기본값 카드
                                    ChromeGroupCard(cardColor = settingsCardColor) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectTheme(ThemeMode.SYSTEM) }
                                                .padding(horizontal = 14.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = prefs.themeMode == ThemeMode.SYSTEM,
                                                onClick = { selectTheme(ThemeMode.SYSTEM) }
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("시스템 기본값", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "기기의 어두운 테마가 사용 설정되어 있을 때 어두운 테마를 사용 설정합니다.",
                                                    fontSize = 12.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }

                                    // 크롬 1:1 체크박스: 시스템 기본값 또는 어둡게 선택 시 표시
                                    if (prefs.themeMode != ThemeMode.LIGHT) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    SpaceManager.updatePreferences {
                                                        it.copy(darkenWebsitesWhenPossible = !it.darkenWebsitesWhenPossible)
                                                    }
                                                }
                                                .padding(horizontal = 24.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = prefs.darkenWebsitesWhenPossible,
                                                onCheckedChange = { checked ->
                                                    SpaceManager.updatePreferences {
                                                        it.copy(darkenWebsitesWhenPossible = checked)
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "가능하면 사이트에 어두운 테마를 적용합니다",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // 2. 밝게 / 어둡게 라디오 그룹 카드
                                    M3SettingsGroup(settingsCardColor) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectTheme(ThemeMode.LIGHT) }
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = prefs.themeMode == ThemeMode.LIGHT,
                                                    onClick = { selectTheme(ThemeMode.LIGHT) }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("밝게", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            }
                                        }
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectTheme(ThemeMode.DARK) }
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = prefs.themeMode == ThemeMode.DARK,
                                                    onClick = { selectTheme(ThemeMode.DARK) }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("어둡게", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            }
                                        }
                                    }
                                } else {
                                    // 크롬 태블릿 1:1 디자인 상위 화면 (테마 탭 시 디자인 > 테마 진입)
                                    M3SettingsGroup(settingsCardColor) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showThemeSubpage = true }
                                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text("테마", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                    Text(
                                                        text = prefs.themeMode.label,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                EgoIcon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = "테마 설정 열기",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "툴바 바로가기 (홈 버튼 표시)",
                                                subtitle = "주소창 왼쪽에 홈 버튼 표시",
                                                checked = prefs.showHomeButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showHomeButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "북마크바 표시",
                                                subtitle = "주소창 아래에 북마크 바 표시",
                                                checked = prefs.showBookmarkBar,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showBookmarkBar = c) } }
                                            )
                                        }
                                    }

                                    Text(
                                        text = "테마 색상 팔레트",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                    )
                                    M3SettingsGroup(settingsCardColor) {
                                        item {
                                            ThemeColorPalettePickerCard(
                                                selectedPreset = prefs.themeColorPreset,
                                                isDarkPreview = isDarkCanvas,
                                                onSelectPreset = { preset ->
                                                    SpaceManager.updatePreferences { it.copy(themeColorPreset = preset) }
                                                }
                                            )
                                        }
                                    }

                                    Text(
                                        text = "툴바 · 빠른메뉴 통합 커스텀",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                    )
                                    M3SettingsGroup(settingsCardColor) {
                                        item {
                                            ToolbarCustomPreviewCard(
                                                prefs = prefs,
                                                onReorder = { newOrder ->
                                                    SpaceManager.updatePreferences { it.copy(toolbarButtonOrder = newOrder) }
                                                }
                                            )
                                        }
                                        item {
                                            ExtensionSettingsBottomSection()
                                        }
                                    }
                                }

                                if (!inThemePage) {
                                // 탭 오디오 미니 이퀄라이저 유기적 모핑 스타일 선택 섹션
                                var eqPreviewMuted by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        delay(1750L)
                                        eqPreviewMuted = !eqPreviewMuted
                                    }
                                }
                                Text(
                                    text = "하모니OS 7 광학 모션",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        ChromeSwitchItem(
                                            title = "광수지동 · 광선구륵 (Rim Light)",
                                            subtitle = "상단 탭·툴바·설정 카드·팝업 터치 시 손가락 추적 광원 및 테두리 발광",
                                            checked = prefs.rimGlowEnabled,
                                            onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(rimGlowEnabled = c) } }
                                        )
                                    }
                                    if (prefs.rimGlowEnabled) {
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "광효과 강도",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${(prefs.rimGlowIntensity * 100).toInt()}%",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Slider(
                                                    value = prefs.rimGlowIntensity.coerceIn(0.5f, 2.5f),
                                                    onValueChange = { v ->
                                                        SpaceManager.updatePreferences { it.copy(rimGlowIntensity = v) }
                                                    },
                                                    valueRange = 0.5f..2.5f
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf(
                                                        0.7f to "은은함",
                                                        1.35f to "표준",
                                                        1.85f to "선명함",
                                                        2.4f to "최대"
                                                    ).forEach { (valPreset, label) ->
                                                        val sel = kotlin.math.abs(prefs.rimGlowIntensity - valPreset) < 0.14f
                                                        Surface(
                                                            shape = RoundedCornerShape(999.dp),
                                                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable {
                                                                    SpaceManager.updatePreferences { it.copy(rimGlowIntensity = valPreset) }
                                                                }
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(vertical = 6.dp),
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ProgressEffectSettingsSection(
                                    prefs = prefs,
                                    settingsCardColor = settingsCardColor
                                )

                                ParticleVanishSettingsSection(
                                    prefs = prefs,
                                    settingsCardColor = settingsCardColor
                                )

                                Text(
                                    "탭 오디오 이퀄라이저 모핑",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                )
                                M3SettingsGroup(settingsCardColor) {
                                    EqualizerMorphStyle.entries.forEach { style ->
                                        item {
                                            val selected = prefs.equalizerMorphStyle == style
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        SpaceManager.updatePreferences { it.copy(equalizerMorphStyle = style) }
                                                    }
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        TabEqualizerIcon(
                                                            isPlaying = true,
                                                            isMuted = eqPreviewMuted,
                                                            activeColor = MaterialTheme.colorScheme.primary,
                                                            mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            cutoutBgColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                            morphStyle = style,
                                                            onClick = { eqPreviewMuted = !eqPreviewMuted }
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        style.label,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                    Text(
                                                        style.description,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                RadioButton(
                                                    selected = selected,
                                                    onClick = {
                                                        SpaceManager.updatePreferences { it.copy(equalizerMorphStyle = style) }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 탭 너비 조절 (동적 조절 / 수동 고정 너비 슬라이더)
                                Text(
                                    "상단 탭 너비",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                )
                                M3SettingsGroup(settingsCardColor) {
                                    TabWidthMode.entries.forEach { mode ->
                                        item {
                                            val selected = prefs.tabWidthMode == mode
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        SpaceManager.updatePreferences { it.copy(tabWidthMode = mode) }
                                                    }
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(mode.label, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                    Text(
                                                        if (mode == TabWidthMode.DYNAMIC) {
                                                            "Google·NAVER 등 짧은 탭은 콤팩트하게, 긴 제목은 끝이 흐릿하게 표시"
                                                        } else {
                                                            "모든 탭을 지정한 너비(${prefs.manualTabWidthDp}dp)로 균일하게 표시"
                                                        },
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                RadioButton(
                                                    selected = selected,
                                                    onClick = {
                                                        SpaceManager.updatePreferences { it.copy(tabWidthMode = mode) }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (prefs.tabWidthMode == TabWidthMode.MANUAL) {
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("수동 탭 너비", fontSize = 15.sp, fontWeight = FontWeight.Normal)
                                                    Text(
                                                        "${prefs.manualTabWidthDp} dp",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Slider(
                                                    value = prefs.manualTabWidthDp.toFloat().coerceIn(110f, 240f),
                                                    onValueChange = { v ->
                                                        val snapped = (v / 5f).roundToInt() * 5
                                                        SpaceManager.updatePreferences { it.copy(manualTabWidthDp = snapped.coerceIn(110, 240)) }
                                                    },
                                                    valueRange = 110f..240f
                                                )
                                            }
                                        }
                                    }
                                }

                                // 웹페이지 폰트 & 커스텀 .ttf 추가 섹션
                                Text("웹페이지 폰트", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                                M3SettingsGroup(settingsCardColor) {
                                    WebFontMode.entries.forEach { mode ->
                                        item {
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
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                EgoIcon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 20.dp)
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(mode.label, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                    if (mode == WebFontMode.CUSTOM_TTF && prefs.customFontName.isNotBlank()) {
                                                        Text("적용 파일: ${prefs.customFontName}", fontSize = 13.sp, color = EgoEmerald)
                                                    } else if (mode == WebFontMode.SYSTEM_FONT) {
                                                        Text("웹폰트 차단 후 기기 시스템 폰트로 렌더링", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        }
                                    }
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(onClick = { BrowserActivityBridge.launchFontPicker?.invoke() }) {
                                                EgoIcon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), symbolSize = 16.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("커스텀 폰트 (.ttf / .otf) 파일 추가")
                                            }
                                        }
                                    }
                                }
                            }

                                                            }
                                }
                            }

                            SettingsCategory.AI_MONITOR -> {
                                val retentionPresets = listOf(
                                    1 to "1일",
                                    3 to "3일",
                                    5 to "5일",
                                    7 to "7일",
                                    30 to "30일",
                                    0 to "무제한"
                                )
                                val presetValues = retentionPresets.map { it.first }.toSet()
                                var showCustomDays by remember(prefs.aiSpaceRetentionDays) {
                                    mutableStateOf(prefs.aiSpaceRetentionDays !in presetValues)
                                }
                                var customDaysText by remember(prefs.aiSpaceRetentionDays) {
                                    mutableStateOf(
                                        if (prefs.aiSpaceRetentionDays > 0) prefs.aiSpaceRetentionDays.toString() else "14"
                                    )
                                }

                                Text(
                                    text = "AI 스페이스 저장 기간",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                ChromeGroupCard(cardColor = settingsCardColor) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (prefs.aiSpaceRetentionDays == 0) {
                                                "앱을 종료해도 생성한 AI 스페이스가 무제한 유지됩니다."
                                            } else {
                                                "앱 재시작 시 생성한 AI 스페이스를 최대 ${prefs.aiSpaceRetentionDays}일간 자동 복원합니다."
                                            },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            retentionPresets.forEach { (days, label) ->
                                                val sel = !showCustomDays && prefs.aiSpaceRetentionDays == days
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            showCustomDays = false
                                                            SpaceManager.updatePreferences { it.copy(aiSpaceRetentionDays = days) }
                                                        }
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = if (showCustomDays) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable { showCustomDays = true }
                                            ) {
                                                Text(
                                                    text = "커스텀",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (showCustomDays) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (showCustomDays) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                        if (showCustomDays) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = customDaysText,
                                                    onValueChange = { txt -> customDaysText = txt.filter { it.isDigit() }.take(3) },
                                                    singleLine = true,
                                                    label = { Text("보관 일수 (일)") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Button(
                                                    onClick = {
                                                        val parsed = customDaysText.toIntOrNull()?.coerceIn(1, 365) ?: 7
                                                        SpaceManager.updatePreferences { it.copy(aiSpaceRetentionDays = parsed) }
                                                    }
                                                ) {
                                                    Text("적용")
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "세로 모드 위치",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                M3SettingsGroup(settingsCardColor) {
                                    MonitorPosition.entries.forEach { pos ->
                                        item {
                                            val sel = prefs.monitorPosPortrait == pos
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { SpaceManager.updatePreferences { it.copy(monitorPosPortrait = pos) } }
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = sel,
                                                    onClick = { SpaceManager.updatePreferences { it.copy(monitorPosPortrait = pos) } }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        text = if (pos == MonitorPosition.TOP) "상단 (기본값)" else "좌측 사이드바",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = if (pos == MonitorPosition.TOP) "세로 화면에서 탭 바 위로 확장됩니다." else "세로 화면에서도 왼쪽 사이드바로 확장됩니다.",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "가로 모드 위치",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                )
                                M3SettingsGroup(settingsCardColor) {
                                    MonitorPosition.entries.forEach { pos ->
                                        item {
                                            val sel = prefs.monitorPosLandscape == pos
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { SpaceManager.updatePreferences { it.copy(monitorPosLandscape = pos) } }
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = sel,
                                                    onClick = { SpaceManager.updatePreferences { it.copy(monitorPosLandscape = pos) } }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        text = if (pos == MonitorPosition.LEFT) "좌측 사이드바 (기본값)" else "상단",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = if (pos == MonitorPosition.LEFT) "가로 화면에서 왼쪽 사이드바로 확장됩니다." else "가로 화면에서도 탭 바 위로 확장됩니다.",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsCategory.ACCESSIBILITY -> {
                                // 첨부 18015.jpg 접근성 1:1 (확대/축소 슬라이더 + 오즈의 마법사 미리보기 카드 + 토글 3종)
                                ChromeGroupCard(cardColor = settingsCardColor) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("기본 확대/축소", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("방문하는 모든 사이트에서 텍스트 및 이미지를 확대 또는 축소합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${prefs.textZoom}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom - 5).coerceAtLeast(75)) }
                                            }) { EgoIcon(Icons.Default.Remove, contentDescription = "축소") }
                                            Slider(
                                                value = prefs.textZoom.toFloat(),
                                                onValueChange = { v -> SpaceManager.updatePreferences { it.copy(textZoom = v.toInt()) } },
                                                valueRange = 75f..150f,
                                                steps = 14,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = {
                                                SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom + 5).coerceAtMost(150)) }
                                            }) { EgoIcon(Icons.Default.Add, contentDescription = "확대") }
                                        }
                                    }
                                }
                                // 실시간 미리보기 카드 (오즈의 마법사)
                                ChromeGroupCard(cardColor = settingsCardColor) {
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
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        ChromeSwitchItem(
                                            title = "기본 메뉴에 확대/축소 옵션 표시",
                                            subtitle = "여러 사이트의 확대/축소 맞춤설정이 더 쉬워집니다.",
                                            checked = prefs.showZoomInMenu,
                                            onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showZoomInMenu = c) } }
                                        )
                                    }
                                    item {
                                        ChromeSwitchItem(
                                            title = "확대/축소 강제 사용",
                                            subtitle = "웹사이트의 확대 방지 요청 무시",
                                            checked = prefs.forceEnableZoom,
                                            onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(forceEnableZoom = c) } }
                                        )
                                    }
                                    item {
                                        ChromeSwitchItem(
                                            title = "웹페이지 간단히 보기",
                                            subtitle = "기사를 간단히 보기로 표시할 수 있을 때 알림 받기",
                                            checked = prefs.simplifiedViewPrompt,
                                            onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(simplifiedViewPrompt = c) } }
                                        )
                                    }
                                }
                            }

                            SettingsCategory.SITE_SETTINGS -> {
                                // 첨부 18016.jpg 사이트 설정 1:1
                                Text("권한", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                M3SettingsGroup(settingsCardColor) {
                                    item { SitePermissionRow(Icons.Default.LocationOn, "위치", prefs.allowLocation) { c -> SpaceManager.updatePreferences { it.copy(allowLocation = c) } } }
                                    item { SitePermissionRow(Icons.Default.CameraAlt, "카메라", prefs.allowCamera) { c -> SpaceManager.updatePreferences { it.copy(allowCamera = c) } } }
                                    item { SitePermissionRow(Icons.Default.Mic, "마이크", prefs.allowMic) { c -> SpaceManager.updatePreferences { it.copy(allowMic = c) } } }
                                    item { SitePermissionRow(Icons.Default.OpenInNew, "팝업 및 리디렉션 (본인인증)", prefs.allowPopups) { c -> SpaceManager.updatePreferences { it.copy(allowPopups = c) } } }
                                    item { SitePermissionRow(Icons.Default.Code, "JavaScript", prefs.allowJavaScript) { c -> SpaceManager.updatePreferences { it.copy(allowJavaScript = c) } } }
                                    item { SitePermissionRow(Icons.Default.Cookie, "타사 쿠키 허용", prefs.allowThirdPartyCookies) { c -> SpaceManager.updatePreferences { it.copy(allowThirdPartyCookies = c) } } }
                                }
                            }

                            SettingsCategory.LANGUAGES -> {
                                // 첨부 18017.jpg 언어 1:1
                                ChromeGroupCard(cardColor = settingsCardColor) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("기기의 현재 언어", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("${Locale.getDefault().displayLanguage} (${Locale.getDefault().displayCountry})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("번역 설정", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        ChromeSwitchItem(
                                            title = "다른 언어로 된 페이지를 번역하는 옵션 제공",
                                            subtitle = null,
                                            checked = prefs.offerTranslate,
                                            onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(offerTranslate = c) } }
                                        )
                                    }
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("번역 엔진", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = prefs.translationEngine == TranslationEngine.ONLINE_GOOGLE,
                                                    onClick = { SpaceManager.updatePreferences { it.copy(translationEngine = TranslationEngine.ONLINE_GOOGLE) } }
                                                )
                                                Text("온라인 (Google)", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                RadioButton(
                                                    selected = prefs.translationEngine == TranslationEngine.OFFLINE_MLKIT,
                                                    onClick = { SpaceManager.updatePreferences { it.copy(translationEngine = TranslationEngine.OFFLINE_MLKIT) } }
                                                )
                                                Text("온디바이스 오프라인 (ML Kit)", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("번역 도착어", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Text(TranslationManager.labelFor(prefs.defaultTranslateTarget), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            SettingsCategory.DOWNLOADS -> {
                                // 첨부 18018.jpg 다운로드 1:1
                                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                                                }
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text("다운로드 위치", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                                Text("다운로드 $downloadDir", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    item {
                                        ChromeSwitchItem(
                                            title = "파일 저장 위치 묻기",
                                            subtitle = "다운로드 시작 전 알림 표시",
                                            checked = prefs.askDownloadLocation,
                                            onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(askDownloadLocation = c) } }
                                        )
                                    }
                                }
                            }

                            SettingsCategory.ABOUT -> {
                                // 첨부 18019.jpg 정보 1:1
                                M3SettingsGroup(settingsCardColor) {
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("애플리케이션 버전", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Text("Ego Lite Mobile ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE}) · $chromiumVersion", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("운영체제", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Text("Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("네이티브 가속 엔진", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Text(
                                                text = if (EgoRustBridge.isLoaded) "${EgoRustBridge.engineVersion} · 활성" else "Kotlin 폴백 모드",
                                                fontSize = 13.sp,
                                                color = if (EgoRustBridge.isLoaded) EgoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("법적 정보", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                            Text("Ego Lite Mobile Open Project · Android System WebView / Ktor / ML Kit", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
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

internal class M3SettingsGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()
    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

@Composable
internal fun M3SettingsGroup(
    cardColor: Color,
    block: M3SettingsGroupScope.() -> Unit
) {
    val scope = M3SettingsGroupScope().apply(block)
    val prefs by SpaceManager.preferences.collectAsState()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        scope.items.forEachIndexed { idx, itemContent ->
            val shape = when {
                scope.items.size == 1 -> RoundedCornerShape(20.dp)
                idx == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                idx == scope.items.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                else -> RoundedCornerShape(4.dp)
            }
            val rimRadius = if (scope.items.size == 1 || idx == 0 || idx == scope.items.lastIndex) 16.dp else 4.dp
            Surface(
                shape = shape,
                color = cardColor,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .harmonyRimGlow(
                        enabled = prefs.rimGlowEnabled,
                        cornerRadius = rimRadius,
                        glowColor = MaterialTheme.colorScheme.primary
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    itemContent()
                }
            }
        }
    }
}

@Composable
private fun ChromeGroupCard(
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        tonalElevation = 0.dp,
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    EgoIcon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 16.dp)
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(engine.label, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
            Text(engine.domain, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EgoIcon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 22.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (allowed) "허용됨" else "허용되지 않음",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
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


@Composable
private fun ToolbarCustomPreviewCard(
    prefs: com.egolite.mobile.model.BrowserPreferences,
    onReorder: (List<String>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val installedExts by ExtensionManager.extensions.collectAsState()
    val googleAccount by GoogleAccountManager.state.collectAsState()
    val googleAvatar by GoogleAccountManager.avatarBitmap.collectAsState()
    var renamingExtId by remember { mutableStateOf<String?>(null) }
    val extById = remember(installedExts) { installedExts.associateBy { it.id } }

    val defaultOrder = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
    val normalizedOrder = remember(prefs.toolbarButtonOrder) {
        val known = prefs.toolbarButtonOrder.filter { it in defaultOrder || it.startsWith("QS:") }
        known + defaultOrder.filter { it !in known }
    }
    val enabledKeys = remember(normalizedOrder, prefs, installedExts) {
        getVisibleToolbarKeys(prefs)
    }

    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragStartIdx by remember { mutableIntStateOf(-1) }
    var dragTargetIdx by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val slotStepPx = with(density) { 40.dp.toPx() }
    val latestEnabled by rememberUpdatedState(enabledKeys)
    val latestFullOrder by rememberUpdatedState(normalizedOrder)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("실시간 툴바 미리보기", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "아래 칩을 길게 눌러 좌우로 순서 이동",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 축소형 크롬/Ego 툴바 미리보기
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prefs.showHomeButton) {
                    EgoIcon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 18.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                EgoIcon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))

                // 미니 주소창 필
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EgoIcon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 13.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("ego-lite.app", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 켜져 있는 커스텀 버튼들 (롱프레스 좌우 드래그로 순서 변경)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    enabledKeys.forEachIndexed { idx, key ->
                        androidx.compose.runtime.key(key) {
                            val isDragging = draggingKey == key
                            val rawShift = when {
                                draggingKey == null || isDragging || dragStartIdx < 0 || dragTargetIdx < 0 -> 0f
                                dragStartIdx < dragTargetIdx && idx in (dragStartIdx + 1)..dragTargetIdx -> -slotStepPx
                                dragStartIdx > dragTargetIdx && idx in dragTargetIdx until dragStartIdx -> slotStepPx
                                else -> 0f
                            }
                            val animShift by animateFloatAsState(
                                targetValue = rawShift,
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
                                label = "tbPreviewShift_$key"
                            )
                            val (iconVec, tintCol) = when (key) {
                                "MCP" -> Icons.Default.LinkOff to EgoDangerRed
                                "AI" -> Icons.Default.AutoAwesome to MaterialTheme.colorScheme.primary
                                "ADBLOCK" -> Icons.Default.Shield to MaterialTheme.colorScheme.primary
                                "TRANSLATE" -> Icons.Default.Translate to MaterialTheme.colorScheme.onSurface
                                "DARKMODE" -> Icons.Default.DarkMode to MaterialTheme.colorScheme.onSurface
                                "NEW_TAB" -> Icons.Default.Add to MaterialTheme.colorScheme.onSurface
                                "SHARE" -> Icons.Default.Share to MaterialTheme.colorScheme.onSurface
                                "FIND" -> Icons.Default.FindInPage to MaterialTheme.colorScheme.onSurface
                                "DESKTOP" -> Icons.Default.DesktopWindows to MaterialTheme.colorScheme.onSurface
                                "READER" -> Icons.Default.MenuBook to MaterialTheme.colorScheme.onSurface
                                else -> Icons.Default.Person to MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .zIndex(if (isDragging) 12f else 0f)
                                    .graphicsLayer {
                                        translationX = if (isDragging) dragOffsetX else animShift
                                        scaleX = if (isDragging) 1.18f else 1f
                                        scaleY = if (isDragging) 1.18f else 1f
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (isDragging) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    )
                                    .pointerInput(key) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                val sIdx = latestEnabled.indexOf(key)
                                                if (sIdx >= 0) {
                                                    draggingKey = key
                                                    dragStartIdx = sIdx
                                                    dragTargetIdx = sIdx
                                                    dragOffsetX = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetX += dragAmount.x
                                                val curEnabled = latestEnabled
                                                if (curEnabled.size > 1 && dragStartIdx >= 0) {
                                                    val delta = kotlin.math.round(dragOffsetX / slotStepPx).toInt()
                                                    val next = (dragStartIdx + delta).coerceIn(0, curEnabled.lastIndex)
                                                    if (next != dragTargetIdx) {
                                                        dragTargetIdx = next
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                val from = dragStartIdx
                                                val to = dragTargetIdx
                                                val curEnabled = latestEnabled
                                                if (from >= 0 && to >= 0 && from != to && from in curEnabled.indices && to in curEnabled.indices) {
                                                    val reorderedEnabled = curEnabled.toMutableList().apply {
                                                        val moved = removeAt(from)
                                                        add(to, moved)
                                                    }
                                                    val reorderedCore = reorderedEnabled.filter { !it.startsWith("EXT:") }
                                                    val disabledRest = latestFullOrder.filter { it !in reorderedCore }
                                                    onReorder(reorderedCore + disabledRest)
                                                }
                                                draggingKey = null
                                                dragStartIdx = -1
                                                dragTargetIdx = -1
                                                dragOffsetX = 0f
                                            },
                                            onDragCancel = {
                                                draggingKey = null
                                                dragStartIdx = -1
                                                dragTargetIdx = -1
                                                dragOffsetX = 0f
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "MCP") {
                                    McpHubStatusIcon(
                                        disconnected = true,
                                        contentDescription = "MCP",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        cutoutColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        symbolSize = 18.dp
                                    )
                                } else if (key.startsWith("EXT:")) {
                                    val ext = extById[key.removePrefix("EXT:")]
                                    ExtensionIconBadge(
                                        iconPath = ext?.iconPath,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        sizeDp = 18
                                    )
                                } else if (key == "PROFILE" && googleAccount.isSignedIn && googleAvatar != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = googleAvatar!!.asImageBitmap(),
                                        contentDescription = "프로필",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.size(22.dp).clip(CircleShape)
                                    )
                                } else {
                                    val resolvedIcon = if (key.startsWith("QS:")) {
                                        when (key.removePrefix("QS:")) {
                                            "incognito" -> Icons.Default.VisibilityOff
                                            "bmbar" -> Icons.Default.Bookmarks
                                            "bookmark" -> Icons.Default.Star
                                            "site" -> Icons.Default.Tune
                                            "history" -> Icons.Default.History
                                            "download" -> Icons.Default.Download
                                            "recent" -> Icons.Default.Tab
                                            "clear" -> Icons.Default.DeleteOutline
                                            "settings" -> Icons.Default.Settings
                                            "default_app" -> Icons.Default.Language
                                            "copy_link" -> Icons.Default.ContentCopy
                                            "add_home" -> Icons.Default.AddToHomeScreen
                                            "save_mht" -> Icons.Default.Save
                                            "print" -> Icons.Default.Print
                                            else -> Icons.Default.Tune
                                        }
                                    } else iconVec
                                    EgoIcon(
                                        imageVector = resolvedIcon,
                                        contentDescription = key,
                                        modifier = Modifier.size(18.dp),
                                        tint = tintCol,
                                        filled = key == "ADBLOCK" || key == "PROFILE",
                                        symbolSize = 18.dp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                EgoIcon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, symbolSize = 18.dp)
            }
        }

        // =========================================================================
        // ZONE ② 빠른메뉴 통합 미리보기 (4열 · 편집 전용 · 잘림 0% · 롱프레스 이동 · 툴바↔빠른메뉴 이동)
        // =========================================================================
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        val defaultQsLayout = remember {
            listOf(
                "zoom:4", "newtab:1", "incognito:1", "aispace:2",
                "bmbar:2", "dark:2", "div:d1",
                "bookmark:1", "site:1", "history:1", "download:1",
                "recent:1", "share:1", "clear:1", "settings:1",
                "mcp:2", "default_app:2"
            )
        }
        val qsCatalog = remember(installedExts, googleAccount) {
            val profileLabel = if (googleAccount.isSignedIn && googleAccount.displayName.isNotBlank()) googleAccount.displayName else "Google 계정"
            val baseList = listOf(
                Triple("zoom", "페이지 확대", Icons.Default.Search),
                Triple("newtab", "새 탭", Icons.Default.Add),
                Triple("incognito", "시크릿 창", Icons.Default.VisibilityOff),
                Triple("aispace", "AI 공간", Icons.Default.AutoAwesome),
                Triple("bmbar", "북마크 바", Icons.Default.Bookmarks),
                Triple("dark", "어두운 테마", Icons.Default.DarkMode),
                Triple("bookmark", "북마크", Icons.Default.Star),
                Triple("site", "사이트 제어", Icons.Default.Tune),
                Triple("history", "방문 기록", Icons.Default.History),
                Triple("download", "다운로드", Icons.Default.Download),
                Triple("recent", "최근 탭", Icons.Default.Tab),
                Triple("share", "공유", Icons.Default.Share),
                Triple("find", "페이지 내 찾기", Icons.Default.FindInPage),
                Triple("desktop", "데스크톱", Icons.Default.DesktopWindows),
                Triple("reader", "리더 모드", Icons.Default.MenuBook),
                Triple("clear", "기록 삭제", Icons.Default.DeleteOutline),
                Triple("settings", "설정", Icons.Default.Settings),
                Triple("mcp", "MCP · 터널", Icons.Default.Hub),
                Triple("default_app", "기본 브라우저", Icons.Default.Language),
                Triple("adblock", "광고 차단", Icons.Default.Shield),
                Triple("translate", "번역", Icons.Default.Translate),
                Triple("profile", profileLabel, Icons.Default.Person),
                Triple("copy_link", "링크 복사", Icons.Default.ContentCopy),
                Triple("add_home", "홈 화면에 추가", Icons.Default.AddToHomeScreen),
                Triple("save_mht", "오프라인 저장", Icons.Default.Save),
                Triple("print", "인쇄 / PDF", Icons.Default.Print)
            )
            val extList = installedExts.map { ext ->
                Triple("ext:${ext.id}", ext.displayName, Icons.Default.Extension)
            }
            baseList + extList
        }
        val qsCatalogMap = remember(qsCatalog) { qsCatalog.associateBy { it.first } }

        fun setToolbarKeyEnabled(tbKey: String, enabled: Boolean) {
            if (tbKey.startsWith("EXT:")) {
                ExtensionManager.toggleShowInToolbar(tbKey.removePrefix("EXT:"), enabled)
                return
            }
            if (tbKey.startsWith("QS:")) {
                SpaceManager.updatePreferences { p ->
                    val nextOrd = p.toolbarButtonOrder.toMutableList()
                    if (enabled) {
                        if (tbKey !in nextOrd) nextOrd.add(tbKey)
                    } else {
                        nextOrd.removeAll { it == tbKey }
                    }
                    p.copy(toolbarButtonOrder = nextOrd)
                }
                return
            }
            SpaceManager.updatePreferences { p ->
                when (tbKey) {
                    "MCP" -> p.copy(showMcpBadgeButton = enabled)
                    "AI" -> p.copy(showAiCommandButton = enabled)
                    "ADBLOCK" -> p.copy(showAdBlockButton = enabled)
                    "TRANSLATE" -> p.copy(showTranslateButton = enabled)
                    "DARKMODE" -> p.copy(showDarkModeButton = enabled)
                    "NEW_TAB" -> p.copy(showNewTabToolbarButton = enabled)
                    "SHARE" -> p.copy(showShareButton = enabled)
                    "FIND" -> p.copy(showFindInPageButton = enabled)
                    "DESKTOP" -> p.copy(showDesktopSiteButton = enabled)
                    "READER" -> p.copy(showReaderModeButton = enabled)
                    "PROFILE" -> p.copy(showProfileAvatarButton = enabled)
                    else -> p
                }
            }
        }

        // 미등록 레거시 키 필터링 + 중복 키 제거로 Tune 아이콘 중복 버그 완전 해소!
        val activeQsTokens = remember(prefs.quickMenuLayout, qsCatalogMap) {
            val raw = prefs.quickMenuLayout.ifEmpty { defaultQsLayout }
            val seen = mutableSetOf<String>()
            val cleaned = mutableListOf<String>()
            for (tok in raw) {
                when {
                    tok.startsWith("div:") -> if (seen.add(tok)) cleaned.add(tok)
                    tok.startsWith("hide:") -> {
                        val k = tok.removePrefix("hide:")
                        if (k in qsCatalogMap && seen.add(tok)) cleaned.add(tok)
                    }
                    else -> {
                        val k = tok.substringBefore(':')
                        if (k in qsCatalogMap && seen.add("tile:$k")) cleaned.add(tok)
                    }
                }
            }
            cleaned
        }
        val latestActiveQsTokens by rememberUpdatedState(activeQsTokens)

        val qsVisibleEntries = remember(activeQsTokens) {
            activeQsTokens.filter { !it.startsWith("hide:") }
        }
        val qsHiddenKeys = remember(activeQsTokens, qsCatalog, prefs) {
            val present = qsVisibleEntries.mapNotNull { t ->
                if (t.startsWith("div:")) null else t.substringBefore(':')
            }.toSet()
            val onToolbarNow = getVisibleToolbarKeys(prefs).mapNotNull { resolveQsKeyFromTb(it) }.toSet()
            qsCatalog.map { it.first }.filter { it !in present && it !in onToolbarNow && it != "zoom" }
        }

        fun updateQsTokens(transform: (MutableList<String>) -> Unit) {
            val next = latestActiveQsTokens.toMutableList()
            transform(next)
            SpaceManager.updatePreferences { it.copy(quickMenuLayout = next) }
        }

        // 툴바 -> 빠른메뉴로 바로 내리기 칩 행 (크롬 확장 EXT:* 및 커스텀 QS:* 포함 완전 동기화)
        val transferableToolbarKeys = remember(enabledKeys) {
            enabledKeys.filter { resolveQsKeyFromTb(it) != null }
        }
        if (transferableToolbarKeys.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "툴바→빠른메뉴:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transferableToolbarKeys.forEach { tbKey ->
                    val qsKey = resolveQsKeyFromTb(tbKey) ?: return@forEach
                    val meta = qsCatalogMap[qsKey]
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            setToolbarKeyEnabled(tbKey, false)
                            updateQsTokens { list ->
                                list.removeAll { it == "hide:$qsKey" || it.startsWith("$qsKey:") }
                                list.add(1.coerceAtMost(list.size), "$qsKey:1")
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            EgoIcon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp), symbolSize = 12.dp)
                            Text(meta?.second ?: tbKey, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("빠른메뉴 통합 미리보기 (4열)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "길게 눌러 위치 이동 · 우하단 핸들 크기조절 · ↑ 툴바 올리기 · − 보관함",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clickable {
                        updateQsTokens { list ->
                            val divId = "div:d_${System.currentTimeMillis() % 10000}"
                            list.add(divId)
                        }
                    }
                ) {
                    Text(
                        "+ 구분선",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.clickable {
                        SpaceManager.updatePreferences {
                            it.copy(
                                quickMenuLayout = emptyList(),
                                toolbarButtonOrder = defaultOrder
                            )
                        }
                    }
                ) {
                    Text(
                        "기본값",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 실제 사용환경과 동일한 비율·위치 배치: 우측 384.dp 빠른메뉴 + 좌측 빈 공간에 제거한 버튼 보관함
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sideBySide = maxWidth >= 520.dp
            val removedTrayContent: @Composable (Modifier) -> Unit = { trayMod ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = trayMod.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "제거한 버튼 (${qsHiddenKeys.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (qsHiddenKeys.isEmpty()) {
                            Text(
                                text = "제거한 버튼이 여기에 표시됩니다.\n탭하면 빠른메뉴로 복원됩니다.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            qsHiddenKeys.forEach { hKey ->
                                val meta = qsCatalogMap[hKey]
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            updateQsTokens { list ->
                                                list.removeAll { it == "hide:$hKey" }
                                                list.add("$hKey:1")
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        EgoIcon(meta?.third ?: Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp), symbolSize = 16.dp)
                                        Text(meta?.second ?: hKey, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val qsPreviewWidth = if (sideBySide) 384.dp else maxWidth.coerceAtMost(384.dp)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.Top
            ) {
                if (sideBySide) {
                    removedTrayContent(Modifier.weight(1f))
                }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .width(qsPreviewWidth)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val packedRows = remember(qsVisibleEntries) {
                    val out = mutableListOf<List<Pair<Int, String>>>()
                    var curRow = mutableListOf<Pair<Int, String>>()
                    var used = 0
                    qsVisibleEntries.forEachIndexed { idx, tok ->
                        if (tok.startsWith("div:") || tok.startsWith("zoom:")) {
                            if (curRow.isNotEmpty()) {
                                out.add(curRow)
                                curRow = mutableListOf()
                                used = 0
                            }
                            out.add(listOf(idx to tok))
                        } else {
                            val sp = tok.substringAfter(':', "1").toIntOrNull()?.coerceIn(1, 4) ?: 1
                            if (used + sp > 4 && curRow.isNotEmpty()) {
                                out.add(curRow)
                                curRow = mutableListOf()
                                used = 0
                            }
                            curRow.add(idx to tok)
                            used += sp
                            if (used >= 4) {
                                out.add(curRow)
                                curRow = mutableListOf()
                                used = 0
                            }
                        }
                    }
                    if (curRow.isNotEmpty()) out.add(curRow)
                    out
                }

                packedRows.forEach { rowItems ->
                    val first = rowItems.first()
                    if (first.second.startsWith("div:")) {
                        val divTok = first.second
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = EgoDangerRed.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        updateQsTokens { list -> list.removeAll { it == divTok } }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    EgoIcon(Icons.Default.Close, contentDescription = "구분선 삭제", tint = EgoDangerRed, modifier = Modifier.size(13.dp), symbolSize = 13.dp)
                                }
                            }
                        }
                    } else if (first.second.startsWith("zoom:")) {
                        var zDragging by remember { mutableStateOf(false) }
                        var zAccumY by remember { mutableFloatStateOf(0f) }
                        var zLiftY by remember { mutableFloatStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .zIndex(if (zDragging) 20f else 0f)
                                .graphicsLayer {
                                    scaleX = if (zDragging) 1.03f else 1f
                                    scaleY = if (zDragging) 1.03f else 1f
                                    translationY = if (zDragging) zLiftY else 0f
                                }
                                .border(
                                    width = if (zDragging) 2.dp else 1.dp,
                                    color = if (zDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(35.dp)
                                )
                                .pointerInput("settings_zoom_drag") {
                                    val stepY = 44.dp.toPx()
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            zDragging = true; zAccumY = 0f; zLiftY = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragEnd = { zDragging = false; zAccumY = 0f; zLiftY = 0f },
                                        onDragCancel = { zDragging = false; zAccumY = 0f; zLiftY = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            zAccumY += dragAmount.y
                                            zLiftY = (zLiftY + dragAmount.y * 0.35f).coerceIn(-36f, 36f)
                                            if (zAccumY <= -stepY) {
                                                zAccumY = 0f; zLiftY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                updateQsTokens { list ->
                                                    val i = list.indexOfFirst { it.startsWith("zoom:") }
                                                    if (i > 0) {
                                                        val item = list.removeAt(i)
                                                        list.add(i - 1, item)
                                                    }
                                                }
                                            } else if (zAccumY >= stepY) {
                                                zAccumY = 0f; zLiftY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                updateQsTokens { list ->
                                                    val i = list.indexOfFirst { it.startsWith("zoom:") }
                                                    if (i in 0 until list.lastIndex) {
                                                        val item = list.removeAt(i)
                                                        list.add(i + 1, item)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            HorizontalHarmonyCapsuleSlider(
                                valueFraction = 0.5f,
                                valueLabel = "100%",
                                icon = Icons.Default.Search,
                                accentColor = MaterialTheme.colorScheme.primary,
                                lightIntensity = prefs.progressLightIntensity,
                                particleDensity = prefs.progressParticleDensity,
                                darkUseThemeColor = prefs.progressDarkUseThemeColor,
                                dayParticleColor = Color(prefs.lightModeParticleColorHex),
                                interactive = false,
                                iconCenterOffsetDp = 42.dp,
                                onValueChange = {},
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        val rowSpans = rowItems.sumOf { it.second.substringAfter(':', "1").toIntOrNull()?.coerceIn(1, 4) ?: 1 }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (_, tok) ->
                                val key = tok.substringBefore(':')
                                val sp = tok.substringAfter(':', "1").toIntOrNull()?.coerceIn(1, 4) ?: 1
                                val meta = qsCatalogMap[key]
                                val label = meta?.second ?: key
                                val iconVec = meta?.third ?: Icons.Default.Tune
                                val matchingTbKey = if (key != "zoom") resolveTbKeyFromQs(key) else null
                                val isSingle = sp == 1
                                val tileShape = RoundedCornerShape(35.dp)

                                var lifted by remember(key) { mutableStateOf(false) }
                                var liftX by remember(key) { mutableFloatStateOf(0f) }
                                var liftY by remember(key) { mutableFloatStateOf(0f) }
                                var accX by remember(key) { mutableFloatStateOf(0f) }
                                var accY by remember(key) { mutableFloatStateOf(0f) }

                                // 외곽 Box는 clip하지 않음 -> '-' 버튼, '↑' 툴바 이동 버튼, 우하단 크기조절 핸들 잘림 0%!
                                Box(
                                    modifier = Modifier
                                        .weight(sp.toFloat())
                                        .height(70.dp)
                                        .zIndex(if (lifted) 24f else 0f)
                                        .graphicsLayer {
                                            scaleX = if (lifted) 1.07f else 1f
                                            scaleY = if (lifted) 1.07f else 1f
                                            translationX = if (lifted) liftX else 0f
                                            translationY = if (lifted) liftY else 0f
                                        }
                                        .pointerInput("settings_tile_drag_$key") {
                                            val stepX = 52.dp.toPx()
                                            val stepY = 56.dp.toPx()
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    lifted = true; liftX = 0f; liftY = 0f; accX = 0f; accY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragEnd = { lifted = false; liftX = 0f; liftY = 0f; accX = 0f; accY = 0f },
                                                onDragCancel = { lifted = false; liftX = 0f; liftY = 0f; accX = 0f; accY = 0f },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    liftX = (liftX + dragAmount.x * 0.45f).coerceIn(-64f, 64f)
                                                    liftY = (liftY + dragAmount.y * 0.45f).coerceIn(-64f, 64f)
                                                    accX += dragAmount.x
                                                    accY += dragAmount.y
                                                    val delta = when {
                                                        accX >= stepX -> { accX = 0f; liftX = 0f; 1 }
                                                        accX <= -stepX -> { accX = 0f; liftX = 0f; -1 }
                                                        accY >= stepY -> { accY = 0f; liftY = 0f; 2 }
                                                        accY <= -stepY -> { accY = 0f; liftY = 0f; -2 }
                                                        else -> 0
                                                    }
                                                    if (delta != 0) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        updateQsTokens { list ->
                                                            val curIdx = list.indexOfFirst { it.startsWith("$key:") }
                                                            if (curIdx >= 0) {
                                                                val visEnd = list.indexOfLast { !it.startsWith("hide:") }.coerceAtLeast(0)
                                                                val nextIdx = (curIdx + delta).coerceIn(0, visEnd)
                                                                if (nextIdx != curIdx) {
                                                                    val item = list.removeAt(curIdx)
                                                                    list.add(nextIdx, item)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    // 내부 클립된 타일 본체 (설정에서는 탭 ON/OFF 비활성)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(tileShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .then(
                                                if (lifted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, tileShape)
                                                else Modifier
                                            )
                                    ) {
                                        val extItem = if (key.startsWith("ext:")) extById[key.removePrefix("ext:")] else null
                                        if (isSingle) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                if (extItem != null) {
                                                    ExtensionIconBadge(iconPath = extItem.iconPath, tint = MaterialTheme.colorScheme.onSurface, sizeDp = 22)
                                                } else if (key == "profile" && googleAccount.isSignedIn && googleAvatar != null) {
                                                    androidx.compose.foundation.Image(
                                                        bitmap = googleAvatar!!.asImageBitmap(),
                                                        contentDescription = label,
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        modifier = Modifier.size(28.dp).clip(CircleShape)
                                                    )
                                                } else {
                                                    EgoIcon(iconVec, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp), symbolSize = 22.dp)
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(start = 20.dp, end = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                                    if (extItem != null) {
                                                        ExtensionIconBadge(iconPath = extItem.iconPath, tint = MaterialTheme.colorScheme.onSurface, sizeDp = 21)
                                                    } else if (key == "profile" && googleAccount.isSignedIn && googleAvatar != null) {
                                                        androidx.compose.foundation.Image(
                                                            bitmap = googleAvatar!!.asImageBitmap(),
                                                            contentDescription = label,
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                                        )
                                                    } else {
                                                        EgoIcon(iconVec, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp), symbolSize = 21.dp)
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (key == "profile" && googleAccount.isSignedIn) {
                                                        Text(
                                                            text = "Google 계정",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 좌상단 '-' 보관함으로 빼기 (잘림 없음!)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .size(21.dp)
                                            .clip(CircleShape)
                                            .background(EgoDangerRed)
                                            .clickable {
                                                updateQsTokens { list ->
                                                    list.removeAll { it.startsWith("$key:") }
                                                    if ("hide:$key" !in list) list.add("hide:$key")
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EgoIcon(Icons.Default.Remove, contentDescription = "보관함으로 빼기", tint = Color.White, modifier = Modifier.size(13.dp), symbolSize = 13.dp)
                                    }

                                    // 우상단 '↑' 상단 툴바로 바로 올리기 (툴바 호환 버튼만 표시!)
                                    if (matchingTbKey != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(21.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    setToolbarKeyEnabled(matchingTbKey, true)
                                                    updateQsTokens { list ->
                                                        list.removeAll { it.startsWith("$key:") }
                                                        if ("hide:$key" !in list) list.add("hide:$key")
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            EgoIcon(Icons.Default.ArrowUpward, contentDescription = "상단 툴바로 이동", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp), symbolSize = 13.dp)
                                        }
                                    }

                                    // 좌하단 주황색 연필 버튼: 크롬 확장 메뉴 이름 수정 (빠른메뉴 편집과 실시간 동기화)
                                    if (key.startsWith("ext:")) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .size(21.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF8F00))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    renamingExtId = key.removePrefix("ext:")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            EgoIcon(Icons.Default.Edit, contentDescription = "확장 이름 변경", tint = Color.White, modifier = Modifier.size(12.dp), symbolSize = 12.dp)
                                        }
                                    }

                                    // 우하단 두꺼운 호흡 크기조절 핸들 (1->2->3->4->1, 잘림 없음!)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(28.dp)
                                            .clickable {
                                                val nextSp = if (sp >= 4) 1 else sp + 1
                                                updateQsTokens { list ->
                                                    val idx = list.indexOfFirst { it.startsWith("$key:") }
                                                    if (idx >= 0) list[idx] = "$key:$nextSp"
                                                }
                                            },
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Canvas(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(end = 2.dp, bottom = 2.dp)
                                        ) {
                                            val arcSize = size.minDimension * 1.80f
                                            drawArc(
                                                color = Color.White.copy(alpha = 0.78f),
                                                startAngle = 8f,
                                                sweepAngle = 74f,
                                                useCenter = false,
                                                topLeft = Offset(size.width - arcSize, size.height - arcSize),
                                                size = Size(arcSize, arcSize),
                                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowSpans < 4) {
                                Spacer(modifier = Modifier.weight((4 - rowSpans).toFloat()))
                            }
                        }
                    }
                }
            }
        }

            } // end Row (side-by-side removed tray + real-size 384dp Quick Menu)
            if (!sideBySide) {
                removedTrayContent(Modifier.fillMaxWidth())
            }
            }
        } // end BoxWithConstraints
    }

    renamingExtId?.let { eid ->
        extById[eid]?.let { ext ->
            ExtensionRenameDialog(
                ext = ext,
                onDismiss = { renamingExtId = null }
            )
        }
    }
}

@Composable
private fun ThemeColorPalettePickerCard(
    selectedPreset: ThemeColorPreset,
    isDarkPreview: Boolean,
    onSelectPreset: (ThemeColorPreset) -> Unit
) {
    val allPresets = remember { ThemeColorPreset.entries.toList() }
    val activeIdx = allPresets.indexOf(selectedPreset).coerceAtLeast(0)
    val swatch = previewSwatchForPreset(selectedPreset, isDarkPreview)

    fun stepPreset(delta: Int) {
        val next = (activeIdx + delta + allPresets.size) % allPresets.size
        onSelectPreset(allPresets[next])
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = swatch.backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, swatch.accent.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                .pointerInput(activeIdx) {
                    var dragSum = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragSum = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragSum += amount
                        },
                        onDragEnd = {
                            if (dragSum < -42f) stepPreset(+1)
                            else if (dragSum > 42f) stepPreset(-1)
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = swatch.group,
                        modifier = Modifier.size(30.dp).clickable { stepPreset(-1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EgoIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 테마", tint = swatch.accent, modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${selectedPreset.label} (${activeIdx + 1}/${allPresets.size})",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkPreview) Color(0xFFF1F4FA) else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedPreset.subtitle,
                            fontSize = 11.5.sp,
                            color = if (isDarkPreview) Color(0xFFB4BCD0) else Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = swatch.group,
                        modifier = Modifier.size(30.dp).clickable { stepPreset(+1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EgoIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 테마", tint = swatch.accent, modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.width(56.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp, topEnd = 5.dp, bottomEnd = 5.dp)).background(swatch.group))
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(swatch.group).border(1.dp, swatch.accent.copy(alpha = 0.55f), RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(modifier = Modifier.padding(start = 10.dp).width(44.dp).height(6.dp).clip(CircleShape).background(swatch.accent))
                    }
                    Box(modifier = Modifier.width(64.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 13.dp, bottomEnd = 13.dp)).background(swatch.group))
                }
                Text(
                    text = "← 좌우로 드래그해 32종 테마 즉시 전환 →",
                    fontSize = 10.5.sp,
                    color = swatch.accent.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            allPresets.forEach { preset ->
                val isSelected = preset == selectedPreset
                val sw = previewSwatchForPreset(preset, isDarkPreview)
                Column(
                    modifier = Modifier
                        .width(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectPreset(preset) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(sw.backdrop)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp).background(sw.group))
                        Box(
                            modifier = Modifier.size(if (isSelected) 18.dp else 11.dp).clip(CircleShape).background(sw.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                EgoIcon(Icons.Default.Check, contentDescription = "선택됨", modifier = Modifier.size(12.dp), tint = Color(0xFF0B192C), symbolSize = 12.dp)
                            }
                        }
                    }
                    Text(
                        text = preset.label.replace("크롬 ", "").replace("시스템 색상 따르기", "시스템"),
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ShaderTuningSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Normal)
            Text(
                text = valueLabel,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
