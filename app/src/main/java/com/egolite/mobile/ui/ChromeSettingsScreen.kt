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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
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
            .fillMaxSize()
            .graphicsLayer {
                translationX = dismissSwipeX.value
                alpha = (1f - (dismissSwipeX.value / 1100f)).coerceIn(0.65f, 1f)
            }
            .pointerInput(Unit) {
                val vt = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = { vt.resetTracking() },
                    onHorizontalDrag = { change, dragAmount ->
                        if (dragAmount > 0f || dismissSwipeX.value > 0f) {
                            change.consume()
                            vt.addPosition(change.uptimeMillis, change.position)
                            val next = (dismissSwipeX.value + dragAmount).coerceAtLeast(0f)
                            csScope.launch { dismissSwipeX.snapTo(next) }
                        }
                    },
                    onDragEnd = {
                        val vx = vt.calculateVelocity().x
                        csScope.launch {
                            if (dismissSwipeX.value > 220f || vx > 1100f) {
                                dismissSwipeX.animateTo(
                                    targetValue = 1200f,
                                    animationSpec = EgoMotion.responsiveSpring(),
                                    initialVelocity = vx
                                )
                                onDismiss()
                            } else {
                                dismissSwipeX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = EgoMotion.responsiveSpring(),
                                    initialVelocity = vx
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        csScope.launch {
                            dismissSwipeX.animateTo(0f, animationSpec = EgoMotion.responsiveSpring())
                        }
                    }
                )
            },
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
                                                OutlinedTextField(
                                                    value = clientIdInput,
                                                    onValueChange = { clientIdInput = it },
                                                    singleLine = true,
                                                    placeholder = { Text("xxxx.apps.googleusercontent.com", fontSize = 13.sp) },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
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
                                            Text("동시 활성 AI Space 최대 개수: ${prefs.maxAiSpaces}개", fontSize = 16.sp, fontWeight = FontWeight.Normal)
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
                                        text = "주소창 툴바 버튼 커스텀",
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
                                            ChromeSwitchItem(
                                                title = "MCP 상태 배지",
                                                subtitle = "주소창에 MCP 노드 허브 연결 상태 아이콘 표시",
                                                checked = prefs.showMcpBadgeButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showMcpBadgeButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "AI 명령 버튼",
                                                subtitle = "MCP 연결 시 AI 명령 팝업 아이콘 표시",
                                                checked = prefs.showAiCommandButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showAiCommandButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "광고 차단 방패",
                                                subtitle = "현재 사이트 광고 차단 ON/OFF 및 카운터",
                                                checked = prefs.showAdBlockButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showAdBlockButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "자동 번역",
                                                subtitle = "원탭 페이지 번역 및 길게 눌러 번역 설정",
                                                checked = prefs.showTranslateButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showTranslateButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "다크모드 빠른 전환",
                                                subtitle = "주간 / 야간 테마 원탭 전환",
                                                checked = prefs.showDarkModeButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showDarkModeButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "Google 프로필 아바타",
                                                subtitle = "주소창 우측에 구글 프로필 사진 표시 (탭하여 계정 설정)",
                                                checked = prefs.showProfileAvatarButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showProfileAvatarButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "새 탭 바로 열기 (+)",
                                                subtitle = "주소창 옆에서 즉시 새 탭 생성",
                                                checked = prefs.showNewTabToolbarButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showNewTabToolbarButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "현재 페이지 공유",
                                                subtitle = "현재 웹페이지 링크를 다른 앱으로 바로 공유",
                                                checked = prefs.showShareButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showShareButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "페이지 내 찾기",
                                                subtitle = "현재 페이지 본문 텍스트 검색 바 열기",
                                                checked = prefs.showFindInPageButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showFindInPageButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "데스크톱 / 모바일 사이트 전환",
                                                subtitle = "PC 화면과 모바일 화면 즉시 전환",
                                                checked = prefs.showDesktopSiteButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showDesktopSiteButton = c) } }
                                            )
                                        }
                                        item {
                                            ChromeSwitchItem(
                                                title = "리더(간단히 보기) 모드",
                                                subtitle = "기사 본문만 깔끔하게 추출해 읽기",
                                                checked = prefs.showReaderModeButton,
                                                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(showReaderModeButton = c) } }
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
    val defaultOrder = listOf("MCP", "AI", "ADBLOCK", "TRANSLATE", "DARKMODE", "NEW_TAB", "SHARE", "FIND", "DESKTOP", "READER", "PROFILE")
    val normalizedOrder = remember(prefs.toolbarButtonOrder) {
        val known = prefs.toolbarButtonOrder.filter { it in defaultOrder }
        known + defaultOrder.filter { it !in known }
    }
    val enabledKeys = remember(normalizedOrder, prefs) {
        normalizedOrder.filter { key ->
            when (key) {
                "MCP" -> prefs.showMcpBadgeButton
                "AI" -> prefs.showAiCommandButton
                "ADBLOCK" -> prefs.showAdBlockButton
                "TRANSLATE" -> prefs.showTranslateButton
                "DARKMODE" -> prefs.showDarkModeButton
                "NEW_TAB" -> prefs.showNewTabToolbarButton
                "SHARE" -> prefs.showShareButton
                "FIND" -> prefs.showFindInPageButton
                "DESKTOP" -> prefs.showDesktopSiteButton
                "READER" -> prefs.showReaderModeButton
                "PROFILE" -> prefs.showProfileAvatarButton
                else -> false
            }
        }
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
                                                    val disabledRest = latestFullOrder.filter { it !in reorderedEnabled }
                                                    onReorder(reorderedEnabled + disabledRest)
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
                                } else {
                                    EgoIcon(
                                        imageVector = iconVec,
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
    }
}

@Composable
private fun ThemeColorPalettePickerCard(
    selectedPreset: ThemeColorPreset,
    isDarkPreview: Boolean,
    onSelectPreset: (ThemeColorPreset) -> Unit
) {
    val allPresets = remember { ThemeColorPreset.entries.toList() }
    val rows = remember(allPresets) { allPresets.chunked(5) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = selectedPreset.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selectedPreset.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selectedPreset.isSystem) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Material You",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPresets.forEach { preset ->
                        val isSelected = preset == selectedPreset
                        val swatch = previewSwatchForPreset(preset, isDarkPreview)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPreset(preset) }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(swatch.backdrop)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // 하단 절반: 선택 탭 & 3-그룹 툴바 색상 프리뷰
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(22.dp)
                                        .background(swatch.group)
                                )
                                // 중앙 포인트 액센트 또는 선택 체크 배지
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 20.dp else 12.dp)
                                        .clip(CircleShape)
                                        .background(swatch.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        EgoIcon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "선택됨",
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF0B192C),
                                            symbolSize = 13.dp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = preset.label.replace("크롬 ", "").replace("시스템 색상 따르기", "시스템"),
                                fontSize = 11.sp,
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
