package com.egolite.mobile.ui

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.BookmarkItem
import com.egolite.mobile.model.BrowserPreferences
import com.egolite.mobile.model.BrowserTab
import com.egolite.mobile.model.ProgressBarStyle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.sin

/**
 * 좌측 4개 내비게이션 버튼 (홈 · 뒤로 · 앞으로 · 새로고침) — 물리 반응 애니메이션 포함.
 * BrowserScreen() JVM 64KB 메서드 크기 제한 방지를 위해 별도 Composable로 분리.
 */
@Composable
fun LeftNavToolbarButtons(
    showHomeButton: Boolean,
    canBackOrClose: Boolean,
    canGoForward: Boolean,
    activeIconColor: Color,
    mutedIconColor: Color,
    onGoHome: () -> Unit,
    onBackOrClose: () -> Unit,
    onGoForward: () -> Unit,
    onReload: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val navPrimaryColor = MaterialTheme.colorScheme.primary

    val homeJumpY = remember { Animatable(0f) }
    val homeScaleX = remember { Animatable(1f) }
    val homeScaleY = remember { Animatable(1f) }
    val homeWave = remember { Animatable(0f) }
    if (showHomeButton) {
        IconButton(
            onClick = {
                scope.launch {
                    homeScaleX.snapTo(1.16f); homeScaleY.snapTo(0.84f)
                    homeJumpY.animateTo(-5.5f, tween(110))
                    homeScaleX.animateTo(0.92f, tween(80))
                    homeScaleY.animateTo(1.12f, tween(80))
                    homeJumpY.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 460f))
                    homeScaleX.animateTo(1f, EgoMotion.bouncySpring())
                    homeScaleY.animateTo(1f, EgoMotion.bouncySpring())
                }
                scope.launch {
                    homeWave.snapTo(0f)
                    homeWave.animateTo(1f, tween(380))
                    homeWave.snapTo(0f)
                }
                onGoHome()
            },
            modifier = Modifier
                .size(38.dp)
                .drawBehind {
                    val w = homeWave.value
                    if (w > 0.01f && w < 0.99f) {
                        drawCircle(
                            color = navPrimaryColor.copy(alpha = (1f - w) * 0.45f),
                            radius = size.minDimension * (0.24f + 0.34f * w),
                            style = Stroke(width = (2.2f * (1f - w)).dp.toPx())
                        )
                    }
                }
        ) {
            EgoIcon(
                Icons.Default.Home,
                contentDescription = "홈",
                tint = activeIconColor,
                modifier = Modifier.graphicsLayer {
                    translationY = homeJumpY.value * density.density
                    scaleX = homeScaleX.value
                    scaleY = homeScaleY.value
                },
                symbolSize = 20.dp
            )
        }
    }

    val backKickX = remember { Animatable(0f) }
    val backSquashX = remember { Animatable(1f) }
    IconButton(
        onClick = {
            scope.launch {
                backSquashX.snapTo(0.82f)
                backKickX.animateTo(-6.5f, tween(95))
                backSquashX.animateTo(1.10f, tween(90))
                backKickX.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 480f))
                backSquashX.animateTo(1f, EgoMotion.bouncySpring())
            }
            onBackOrClose()
        },
        enabled = canBackOrClose,
        modifier = Modifier.size(38.dp)
    ) {
        EgoIcon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "뒤로",
            tint = if (canBackOrClose) activeIconColor else mutedIconColor.copy(alpha = 0.45f),
            modifier = Modifier.graphicsLayer {
                translationX = backKickX.value * density.density
                scaleX = backSquashX.value
                scaleY = 2f - backSquashX.value.coerceIn(0.85f, 1.15f)
            },
            symbolSize = 20.dp
        )
    }

    val fwdKickX = remember { Animatable(0f) }
    val fwdSquashX = remember { Animatable(1f) }
    IconButton(
        onClick = {
            scope.launch {
                fwdSquashX.snapTo(0.82f)
                fwdKickX.animateTo(6.5f, tween(95))
                fwdSquashX.animateTo(1.10f, tween(90))
                fwdKickX.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 480f))
                fwdSquashX.animateTo(1f, EgoMotion.bouncySpring())
            }
            onGoForward()
        },
        enabled = canGoForward,
        modifier = Modifier.size(38.dp)
    ) {
        EgoIcon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "앞으로",
            tint = if (canGoForward) activeIconColor else mutedIconColor.copy(alpha = 0.45f),
            modifier = Modifier.graphicsLayer {
                translationX = fwdKickX.value * density.density
                scaleX = fwdSquashX.value
                scaleY = 2f - fwdSquashX.value.coerceIn(0.85f, 1.15f)
            },
            symbolSize = 20.dp
        )
    }

    var refreshSpinTurns by remember { mutableIntStateOf(0) }
    val refreshSpinDeg by animateFloatAsState(
        targetValue = refreshSpinTurns * 360f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 210f),
        label = "refreshSpinMorph"
    )
    val refreshPop = remember { Animatable(1f) }
    val refreshWave = remember { Animatable(0f) }
    IconButton(
        onClick = {
            refreshSpinTurns++
            scope.launch {
                refreshPop.snapTo(0.82f)
                refreshPop.animateTo(1.20f, spring(dampingRatio = 0.48f, stiffness = 520f))
                refreshPop.animateTo(1f, EgoMotion.bouncySpring())
            }
            scope.launch {
                refreshWave.snapTo(0f)
                refreshWave.animateTo(1f, tween(420))
                refreshWave.snapTo(0f)
            }
            onReload()
        },
        modifier = Modifier
            .size(38.dp)
            .drawBehind {
                val w = refreshWave.value
                if (w > 0.01f && w < 0.99f) {
                    drawCircle(
                        color = navPrimaryColor.copy(alpha = (1f - w) * 0.42f),
                        radius = size.minDimension * (0.22f + 0.34f * w),
                        style = Stroke(width = (2.0f * (1f - w)).dp.toPx())
                    )
                }
            }
    ) {
        EgoIcon(
            Icons.Default.Refresh,
            contentDescription = "새로고침",
            tint = activeIconColor,
            modifier = Modifier.graphicsLayer {
                rotationZ = refreshSpinDeg
                scaleX = refreshPop.value
                scaleY = refreshPop.value
            },
            symbolSize = 20.dp
        )
    }
}

/**
 * 중앙 주소창 캡슐 (방식 B 하모니 광감 진행바 + 0-오버랩 탄성 + 6방향 별빛 버스트 북마크 버튼).
 */
@Composable
fun RowScope.OmniboxCenterCapsule(
    activeTab: BrowserTab,
    activeSpaceId: String,
    isAiSpace: Boolean,
    isEffectiveDark: Boolean,
    showOmniboxOverlay: Boolean,
    prefs: BrowserPreferences,
    existingBookmark: BookmarkItem?,
    groupSurface: Color,
    groupBorder: Color,
    activeIconColor: Color,
    mutedIconColor: Color,
    rootBoxLeftPx: Float,
    rootBoxTopPx: Float,
    tabCount: Int = 1,
    activeTabIndex: Int = 0,
    onElasticPullChanged: (Float) -> Unit = {},
    onTabDragOffsetChanged: (Int?) -> Unit = {},
    onBoundsMeasured: (left: Float, top: Float, width: Float) -> Unit,
    onSwitchTabByOffset: (Int) -> Unit,
    onOpenOmnibox: () -> Unit,
    onClickBookmark: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val m3CenterGroupShape = remember { RoundedCornerShape(6.dp) }
    val omniElasticPull = remember { Animatable(0f) }
    val showOmniOptical = prefs.progressShowInOmnibox && prefs.progressBarStyle != ProgressBarStyle.MATERIAL
    val omniIsLoadingNow = activeTab.isLoading && activeTab.progress in 1..99
    val omniProgFrac = (activeTab.progress / 100f).coerceIn(0f, 1f)
    val omniAccent = if (isAiSpace) EgoEmerald else MaterialTheme.colorScheme.primary
    val omniInvTextColor = if (isEffectiveDark) Color(0xFF090D16) else Color.White

    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .graphicsLayer {
                alpha = if (showOmniboxOverlay) 0f else 1f
            }
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onBoundsMeasured(
                    (pos.x - rootBoxLeftPx).coerceAtLeast(0f),
                    (pos.y - rootBoxTopPx).coerceAtLeast(0f),
                    coords.size.width.toFloat()
                )
            }
            .clip(m3CenterGroupShape)
            .background(groupSurface)
            .harmonyRimGlow(enabled = prefs.rimGlowEnabled, shape = m3CenterGroupShape, glowColor = MaterialTheme.colorScheme.primary)
            .border(1.dp, groupBorder, m3CenterGroupShape)
            .pointerInput(activeSpaceId, prefs.toolbarElasticityEnabled, tabCount, activeTabIndex) {
                var dragSum = 0f
                var lastStep = 0
                val stepPx = 76f * density
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (lastStep != 0) onSwitchTabByOffset(lastStep)
                        dragSum = 0f
                        lastStep = 0
                        onTabDragOffsetChanged(null)
                        onElasticPullChanged(0f)
                        scope.launch { omniElasticPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                    },
                    onDragCancel = {
                        dragSum = 0f
                        lastStep = 0
                        onTabDragOffsetChanged(null)
                        onElasticPullChanged(0f)
                        scope.launch { omniElasticPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                    },
                    onHorizontalDrag = { _, amount ->
                        dragSum += amount
                        // 드래그 거리 비례 멀티 탭 탐색 (왼쪽 드래그 -> 이전 탭(-), 오른쪽 드래그 -> 다음 탭(+))
                        val rawStep = (dragSum / stepPx).toInt()
                        val minStep = -activeTabIndex
                        val maxStep = (tabCount - 1 - activeTabIndex).coerceAtLeast(0)
                        val clampedStep = rawStep.coerceIn(minStep, maxStep)
                        lastStep = clampedStep
                        onTabDragOffsetChanged(if (abs(dragSum) > 22f && tabCount > 1) clampedStep else null)
                        if (prefs.toolbarElasticityEnabled) {
                            val maxPullPx = 38.dp.toPx()
                            val rubber = sign(dragSum) * maxPullPx * (1f - exp(-abs(dragSum) / (maxPullPx * 2.1f)))
                            onElasticPullChanged(rubber)
                        }
                    }
                )
            }
            .clickable { onOpenOmnibox() }
    ) {
        if (showOmniOptical) {
            HarmonyHorizontalProgressCanvas(
                style = prefs.progressBarStyle,
                progress = omniProgFrac,
                isActive = omniIsLoadingNow,
                accentColor = omniAccent,
                lightIntensity = prefs.progressLightIntensity,
                particleDensity = prefs.progressParticleDensity,
                darkUseThemeColor = prefs.progressDarkUseThemeColor,
                cornerRadiusDp = 6.dp,
                modifier = Modifier.fillMaxSize()
            )
        }

        val isNewTab = activeTab.url == NEW_TAB_URL
        OmniboxInnerRowContent(
            activeTab = activeTab,
            isNewTab = isNewTab,
            existingBookmark = existingBookmark,
            lockTint = if (activeTab.isIncognito) MaterialTheme.colorScheme.primary else mutedIconColor,
            urlTint = if (isNewTab) mutedIconColor else activeIconColor,
            starTint = if (existingBookmark != null) MaterialTheme.colorScheme.primary else mutedIconColor,
            onClickBookmark = onClickBookmark
        )

        HarmonyModeBClippedOverlay(
            enabled = showOmniOptical && prefs.progressBarStyle == ProgressBarStyle.HARMONY_OPTICAL,
            progress = omniProgFrac,
            isActive = omniIsLoadingNow
        ,
                    modifier = Modifier.matchParentSize()
                ) {
            OmniboxInnerRowContent(
                activeTab = activeTab,
                isNewTab = isNewTab,
                existingBookmark = existingBookmark,
                lockTint = omniInvTextColor.copy(alpha = 0.85f),
                urlTint = omniInvTextColor,
                starTint = omniInvTextColor,
                onClickBookmark = onClickBookmark
            )
        }
    }
}

@Composable
private fun OmniboxInnerRowContent(
    activeTab: BrowserTab,
    isNewTab: Boolean,
    existingBookmark: BookmarkItem?,
    lockTint: Color,
    urlTint: Color,
    starTint: Color,
    onClickBookmark: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EgoIcon(
            imageVector = if (activeTab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Lock,
            contentDescription = if (activeTab.isIncognito) "시크릿 모드" else null,
            modifier = Modifier.size(16.dp),
            tint = lockTint,
            symbolSize = 16.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isNewTab) "Google 검색 또는 URL 입력" else activeTab.url,
            fontSize = 14.sp,
            color = urlTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val starBurst = remember { Animatable(0f) }
        val starSpin = remember { Animatable(0f) }
        val starSparkColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(34.dp)
                .drawBehind {
                    val b = starBurst.value
                    if (b > 0.01f && b < 0.99f) {
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        val alpha = (1f - b) * 0.85f
                        for (ray in 0 until 6) {
                            val ang = (ray * 60.0 + 15.0) * Math.PI / 180.0
                            val r1 = size.minDimension * (0.18f + 0.30f * b)
                            val r2 = size.minDimension * (0.28f + 0.36f * b)
                            val cosA = cos(ang).toFloat()
                            val sinA = sin(ang).toFloat()
                            drawLine(
                                color = starSparkColor.copy(alpha = alpha),
                                start = Offset(cx + cosA * r1, cy + sinA * r1),
                                end = Offset(cx + cosA * r2, cy + sinA * r2),
                                strokeWidth = (2.0f * (1f - b * 0.6f)).dp.toPx()
                            )
                        }
                    }
                }
                .clip(CircleShape)
                .clickable {
                    scope.launch {
                        starSpin.snapTo(0f)
                        starSpin.animateTo(360f, spring(dampingRatio = 0.55f, stiffness = 320f))
                    }
                    scope.launch {
                        starBurst.snapTo(0f)
                        starBurst.animateTo(1f, tween(420))
                        starBurst.snapTo(0f)
                    }
                    onClickBookmark()
                },
            contentAlignment = Alignment.Center
        ) {
            val starPopScale by animateFloatAsState(
                targetValue = if (existingBookmark != null) 1.14f else 1.0f,
                animationSpec = EgoMotion.bouncySpring(),
                label = "bookmarkStarPop"
            )
            EgoIcon(
                imageVector = if (existingBookmark != null) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "북마크 추가/수정",
                tint = starTint,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        val pulse = 1f + 0.22f * sin(starBurst.value * Math.PI).toFloat()
                        scaleX = starPopScale * pulse
                        scaleY = starPopScale * pulse
                        rotationZ = starSpin.value
                    },
                symbolSize = 20.dp
            )
        }
    }
}

/**
 * 우측 선택형 5개 툴바 버튼 (NEW_TAB · SHARE · FIND · DESKTOP · READER) — 물리 반응 애니메이션 포함.
 */
@Composable
fun RightExtraToolbarButton(
    tbKey: String,
    prefs: BrowserPreferences,
    isWebPage: Boolean,
    showFindBar: Boolean,
    activeIconColor: Color,
    mutedIconColor: Color,
    activeTabTitle: String,
    activeTabUrl: String,
    onCreateTab: () -> Unit,
    onToggleFindBar: () -> Unit,
    onToggleDesktop: () -> Unit,
    onOpenReader: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    when (tbKey) {
        "NEW_TAB" -> if (prefs.showNewTabToolbarButton) {
            val ntSpin = remember { Animatable(0f) }
            val ntPop = remember { Animatable(1f) }
            val ntWave = remember { Animatable(0f) }
            val ntRingCol = MaterialTheme.colorScheme.primary
            IconButton(
                onClick = {
                    scope.launch {
                        ntSpin.snapTo(0f)
                        ntSpin.animateTo(90f, spring(dampingRatio = 0.55f, stiffness = 360f))
                    }
                    scope.launch {
                        ntPop.snapTo(0.80f)
                        ntPop.animateTo(1.22f, spring(dampingRatio = 0.48f, stiffness = 540f))
                        ntPop.animateTo(1f, EgoMotion.bouncySpring())
                    }
                    scope.launch {
                        ntWave.snapTo(0f)
                        ntWave.animateTo(1f, tween(380))
                        ntWave.snapTo(0f)
                    }
                    onCreateTab()
                },
                modifier = Modifier
                    .size(38.dp)
                    .drawBehind {
                        val w = ntWave.value
                        if (w > 0.01f && w < 0.99f) {
                            drawCircle(
                                color = ntRingCol.copy(alpha = (1f - w) * 0.48f),
                                radius = size.minDimension * (0.22f + 0.34f * w),
                                style = Stroke(width = (2.0f * (1f - w)).dp.toPx())
                            )
                        }
                    }
            ) {
                EgoIcon(
                    Icons.Default.Add,
                    contentDescription = "새 탭",
                    tint = activeIconColor,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = ntSpin.value
                        scaleX = ntPop.value
                        scaleY = ntPop.value
                    },
                    symbolSize = 20.dp
                )
            }
        }
        "SHARE" -> if (prefs.showShareButton && isWebPage) {
            val shLaunch = remember { Animatable(0f) }
            val shWave = remember { Animatable(0f) }
            val shRingCol = MaterialTheme.colorScheme.primary
            IconButton(
                onClick = {
                    scope.launch {
                        shLaunch.snapTo(0f)
                        shLaunch.animateTo(1f, tween(120))
                        shLaunch.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 440f))
                    }
                    scope.launch {
                        shWave.snapTo(0f)
                        shWave.animateTo(1f, tween(400))
                        shWave.snapTo(0f)
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, activeTabTitle)
                        putExtra(Intent.EXTRA_TEXT, activeTabUrl)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "링크 공유"))
                },
                modifier = Modifier
                    .size(38.dp)
                    .drawBehind {
                        val w = shWave.value
                        if (w > 0.01f && w < 0.99f) {
                            drawCircle(
                                color = shRingCol.copy(alpha = (1f - w) * 0.46f),
                                radius = size.minDimension * (0.22f + 0.36f * w),
                                style = Stroke(width = (2.0f * (1f - w)).dp.toPx())
                            )
                        }
                    }
            ) {
                val l = shLaunch.value
                EgoIcon(
                    Icons.Default.Share,
                    contentDescription = "공유",
                    tint = activeIconColor,
                    modifier = Modifier.graphicsLayer {
                        translationX = (4.2f * l) * density.density
                        translationY = (-4.5f * l) * density.density
                        rotationZ = -14f * l
                        scaleX = 1f + 0.16f * l
                        scaleY = 1f + 0.16f * l
                    },
                    symbolSize = 20.dp
                )
            }
        }
        "FIND" -> if (prefs.showFindInPageButton && isWebPage) {
            val findTilt = remember { Animatable(0f) }
            val findZoom = remember { Animatable(1f) }
            IconButton(
                onClick = {
                    scope.launch {
                        findTilt.snapTo(0f)
                        findTilt.animateTo(-18f, tween(85))
                        findTilt.animateTo(14f, tween(110))
                        findTilt.animateTo(0f, spring(dampingRatio = 0.50f, stiffness = 420f))
                    }
                    scope.launch {
                        findZoom.snapTo(0.84f)
                        findZoom.animateTo(1.24f, spring(dampingRatio = 0.48f, stiffness = 520f))
                        findZoom.animateTo(1f, EgoMotion.bouncySpring())
                    }
                    onToggleFindBar()
                },
                modifier = Modifier.size(38.dp)
            ) {
                EgoIcon(
                    Icons.Default.FindInPage,
                    contentDescription = "페이지 내 찾기",
                    tint = if (showFindBar) MaterialTheme.colorScheme.primary else activeIconColor,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = findTilt.value
                        scaleX = findZoom.value
                        scaleY = findZoom.value
                    },
                    symbolSize = 20.dp
                )
            }
        }
        "DESKTOP" -> if (prefs.showDesktopSiteButton) {
            val dtFlipY = remember { Animatable(0f) }
            val dtStretch = remember { Animatable(0f) }
            IconButton(
                onClick = {
                    scope.launch {
                        dtFlipY.snapTo(0f)
                        dtFlipY.animateTo(360f, spring(dampingRatio = 0.62f, stiffness = 300f))
                    }
                    scope.launch {
                        dtStretch.snapTo(0f)
                        dtStretch.animateTo(1f, tween(110))
                        dtStretch.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 440f))
                    }
                    onToggleDesktop()
                },
                modifier = Modifier.size(38.dp)
            ) {
                val st = dtStretch.value
                EgoIcon(
                    Icons.Default.DesktopWindows,
                    contentDescription = "데스크톱 사이트 전환",
                    tint = if (prefs.desktopModeEnabled) MaterialTheme.colorScheme.primary else mutedIconColor,
                    filled = prefs.desktopModeEnabled,
                    modifier = Modifier.graphicsLayer {
                        rotationY = dtFlipY.value
                        scaleX = 1f + 0.22f * st
                        scaleY = 1f - 0.14f * st
                        cameraDistance = 12f * density.density
                    },
                    symbolSize = 20.dp
                )
            }
        }
        "READER" -> if (prefs.showReaderModeButton && isWebPage) {
            val rdTiltX = remember { Animatable(0f) }
            val rdPop = remember { Animatable(1f) }
            IconButton(
                onClick = {
                    scope.launch {
                        rdTiltX.snapTo(0f)
                        rdTiltX.animateTo(-28f, tween(110))
                        rdTiltX.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 420f))
                    }
                    scope.launch {
                        rdPop.snapTo(0.84f)
                        rdPop.animateTo(1.20f, spring(dampingRatio = 0.48f, stiffness = 500f))
                        rdPop.animateTo(1f, EgoMotion.bouncySpring())
                    }
                    onOpenReader()
                },
                modifier = Modifier.size(38.dp)
            ) {
                EgoIcon(
                    Icons.Default.MenuBook,
                    contentDescription = "읽기 모드",
                    tint = activeIconColor,
                    modifier = Modifier.graphicsLayer {
                        rotationX = rdTiltX.value
                        translationY = (rdTiltX.value * 0.10f) * density.density
                        scaleX = rdPop.value
                        scaleY = rdPop.value
                        cameraDistance = 12f * density.density
                    },
                    symbolSize = 20.dp
                )
            }
        }
    }
}


/**
 * 주소창 좌우 드래그 시 어느 탭으로 이동할지 실시간으로 보여주는 시각적 힌트 캡슐 (#7).
 */
@Composable
fun OmniboxTabSwipeHintPill(
    tabs: List<BrowserTab>,
    activeTabIndex: Int,
    dragStepOffset: Int?,
    modifier: Modifier = Modifier
) {
    if (dragStepOffset == null || tabs.size <= 1) return
    val targetIdx = (activeTabIndex + dragStepOffset).coerceIn(0, tabs.lastIndex)
    val targetTab = tabs[targetIdx]
    val directionText = when {
        dragStepOffset < 0 -> "← ${-dragStepOffset}칸 이전 탭"
        dragStepOffset > 0 -> "다음 탭 ${dragStepOffset}칸 →"
        else -> "현재 탭 유지"
    }
    androidx.compose.material3.Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 10.dp,
        modifier = modifier
            .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = directionText,
                fontSize = 11.5.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = targetTab.title.ifBlank { targetTab.url }.take(28),
                fontSize = 12.5.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${targetIdx + 1}/${tabs.size}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/**
 * 상단 툴바 AI 명령 버튼 — 원링 제거, 실시간 눌림(Press-down) 탄성 애니메이션 + 클릭 리플 +
 * 확장 시 AutoAwesome -> Close 회전 크로스 모핑 동적 전환.
 */
@Composable
fun AiCommandToolbarButton(
    expanded: Boolean,
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.78f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 680f),
        label = "aiBtnPressScale"
    )
    val pressDropPx by animateFloatAsState(
        targetValue = if (isPressed) with(density) { 1.6.dp.toPx() } else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "aiBtnPressDrop"
    )
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = EgoMotion.morphSpring(),
        label = "aiBtnExpandMorph"
    )
    val tapPop = remember { Animatable(1f) }
    val tapHalo = remember { Animatable(0f) }
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .drawBehind {
                val h = tapHalo.value
                if (h in 0.01f..0.99f) {
                    val radius = size.minDimension * (0.20f + 0.34f * h)
                    drawCircle(
                        color = accentColor.copy(alpha = (1f - h) * 0.42f),
                        radius = radius,
                        style = Stroke(width = (2.0f * (1f - h * 0.6f)).dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                scope.launch {
                    tapPop.snapTo(0.84f)
                    tapPop.animateTo(1.16f, animationSpec = spring(dampingRatio = 0.42f, stiffness = 620f))
                    tapPop.animateTo(1f, animationSpec = spring(dampingRatio = 0.60f, stiffness = 520f))
                }
                scope.launch {
                    tapHalo.snapTo(0f)
                    tapHalo.animateTo(1f, animationSpec = tween(320))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        val combinedScale = pressScale * tapPop.value

        // 기본 아이콘: AutoAwesome (확장 시 90도 회전하며 축소 페이드아웃)
        val idleAlpha = (1f - expandProgress * 1.45f).coerceIn(0f, 1f)
        if (idleAlpha > 0.01f) {
            EgoIcon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI 명령창 열기",
                tint = accentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        val s = combinedScale * (1f - 0.32f * expandProgress)
                        scaleX = s
                        scaleY = s
                        translationY = pressDropPx
                        rotationZ = expandProgress * 95f
                        alpha = idleAlpha
                    },
                symbolSize = 20.dp
            )
        }

        // 확장 아이콘: Close (확장 시 -90도 -> 0도 회전하며 탄성 등장)
        val openAlpha = ((expandProgress - 0.28f) / 0.72f).coerceIn(0f, 1f)
        if (openAlpha > 0.01f) {
            EgoIcon(
                imageVector = Icons.Default.Close,
                contentDescription = "AI 명령창 닫기",
                tint = accentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        val s = combinedScale * (0.68f + 0.32f * expandProgress)
                        scaleX = s
                        scaleY = s
                        translationY = pressDropPx
                        rotationZ = -90f * (1f - expandProgress)
                        alpha = openAlpha
                    },
                symbolSize = 20.dp
            )
        }

        if (pendingCount > 0 && !expanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(EgoEmerald)
            )
        }
    }
}
