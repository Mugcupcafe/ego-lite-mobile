package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.BrowserPreferences
import com.egolite.mobile.model.ProgressBarStyle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 설정 > 디자인: 프로그래스바 스타일 좌우 스와이프 미리보기(머테리얼 · 하단 라이팅 · 하모니 광감)
 * + 상세 옵션 + 전체화면 세로 밝기·볼륨 HUD 라이브 미리보기 및 상세 옵션.
 */
@Composable
fun ProgressEffectSettingsSection(
    prefs: BrowserPreferences,
    settingsCardColor: Color
) {
    val styles = remember { ProgressBarStyle.entries }
    val activeIdx = styles.indexOf(prefs.progressBarStyle).coerceAtLeast(0)
    var slideDirection by remember { mutableIntStateOf(1) }
    val isLightBg = MaterialTheme.colorScheme.background.luminance() >= 0.5f

    fun switchStyle(delta: Int) {
        val nextIdx = (activeIdx + delta + styles.size) % styles.size
        slideDirection = if (delta >= 0) 1 else -1
        SpaceManager.updatePreferences { it.copy(progressBarStyle = styles[nextIdx]) }
    }

    // 라이브 진행바 미리보기 자동 루프 (0% -> 100% 2.6초 + 완료 후 0.9초 정지)
    var previewProg by remember { mutableFloatStateOf(0.54f) }
    var previewActive by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()
        while (true) {
            withFrameNanos { ns ->
                val elapsedMs = ((ns - startNs) / 1_000_000L) % 3600L
                if (elapsedMs < 2600L) {
                    val t = elapsedMs / 2600f
                    previewProg = 1f - (1f - t).pow(1.8f)
                    previewActive = true
                } else {
                    previewProg = 1f
                    previewActive = false
                }
            }
        }
    }

    val currentStyle = prefs.progressBarStyle
    val accent = MaterialTheme.colorScheme.primary
    val stageBg = if (isLightBg) Color(0xFFE7ECF6) else Color(0xFF0E1118)
    val barBg = if (isLightBg) Color(0xFFFFFFFF) else Color(0xFF222733)
    val barBorder = if (isLightBg) Color(0x220F172A) else Color(0x22FFFFFF)
    val baseTextCol = if (isLightBg) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val invTextCol = if (isLightBg) Color.White else Color(0xFF090D16)

    Text(
        text = "프로그래스바 & 로딩 광감",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = settingsCardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. 좌우 스와이프 미리보기 스테이지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(stageBg)
                    .border(1.dp, barBorder, RoundedCornerShape(18.dp))
                    .pointerInput(activeIdx) {
                        var dragSum = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragSum = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragSum += dragAmount },
                            onDragEnd = {
                                if (dragSum < -48f) switchStyle(+1)
                                else if (dragSum > 48f) switchStyle(-1)
                            }
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 상단 스타일 헤더 + 좌/우 화살표
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { switchStyle(-1) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                EgoIcon(Icons.Default.ChevronLeft, contentDescription = "이전 스타일", modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                            }
                        }

                        AnimatedContent(
                            targetState = currentStyle,
                            transitionSpec = {
                                (slideInHorizontally(tween(220)) { it * slideDirection / 2 } + fadeIn(tween(180))) togetherWith
                                    (slideOutHorizontally(tween(180)) { -it * slideDirection / 2 } + fadeOut(tween(140)))
                            },
                            label = "progressStyleTitle",
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) { st ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = st.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = baseTextCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = st.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { switchStyle(+1) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                EgoIcon(Icons.Default.ChevronRight, contentDescription = "다음 스타일", modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                            }
                        }
                    }

                    // 미니 주소창 라이브 미리보기
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(barBg)
                                .border(1.dp, barBorder, RoundedCornerShape(10.dp))
                        ) {
                            HarmonyHorizontalProgressCanvas(
                                style = currentStyle,
                                progress = previewProg,
                                isActive = previewActive,
                                accentColor = accent,
                                lightIntensity = prefs.progressLightIntensity,
                                particleDensity = prefs.progressParticleDensity,
                                cornerRadiusDp = 10.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EgoIcon(Icons.Default.Lock, contentDescription = null, tint = baseTextCol.copy(alpha = 0.72f), modifier = Modifier.size(14.dp), symbolSize = 14.dp)
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "egolite.browser / preview",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = baseTextCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(previewProg * 100).roundToInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = baseTextCol.copy(alpha = 0.8f)
                                )
                            }
                            HarmonyModeBClippedOverlay(
                                enabled = currentStyle == ProgressBarStyle.HARMONY_OPTICAL,
                                progress = previewProg,
                                isActive = previewActive
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EgoIcon(Icons.Default.Lock, contentDescription = null, tint = invTextCol.copy(alpha = 0.85f), modifier = Modifier.size(14.dp), symbolSize = 14.dp)
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = "egolite.browser / preview",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = invTextCol,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${(previewProg * 100).roundToInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = invTextCol
                                    )
                                }
                            }
                        }
                        if (currentStyle == ProgressBarStyle.MATERIAL && previewActive) {
                            LinearProgressIndicator(
                                progress = { previewProg },
                                modifier = Modifier.fillMaxWidth().height(2.5.dp).clip(CircleShape),
                                color = accent
                            )
                        }
                    }

                    // 미니 다운로드 배너 라이브 미리보기
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(barBg)
                            .border(1.dp, barBorder, RoundedCornerShape(22.dp))
                    ) {
                        HarmonyHorizontalProgressCanvas(
                            style = currentStyle,
                            progress = previewProg,
                            isActive = previewActive,
                            accentColor = accent,
                            lightIntensity = prefs.progressLightIntensity,
                            particleDensity = prefs.progressParticleDensity,
                            cornerRadiusDp = 22.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EgoIcon(Icons.Default.Download, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ego-lite-mobile.zip", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = baseTextCol, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (previewActive) "다운로드 중 · ${(previewProg * 100).roundToInt()}%" else "다운로드 완료",
                                    fontSize = 10.5.sp,
                                    color = baseTextCol.copy(alpha = 0.72f)
                                )
                            }
                        }
                        HarmonyModeBClippedOverlay(
                            enabled = currentStyle == ProgressBarStyle.HARMONY_OPTICAL,
                            progress = previewProg,
                            isActive = previewActive
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                EgoIcon(Icons.Default.Download, contentDescription = null, tint = invTextCol, modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ego-lite-mobile.zip", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = invTextCol, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (previewActive) "다운로드 중 · ${(previewProg * 100).roundToInt()}%" else "다운로드 완료",
                                        fontSize = 10.5.sp,
                                        color = invTextCol.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                        if (currentStyle == ProgressBarStyle.MATERIAL && previewActive) {
                            LinearProgressIndicator(
                                progress = { previewProg },
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.5.dp),
                                color = accent
                            )
                        }
                    }

                    // 페이지 도트 인디케이터 + 좌우 스와이프 안내
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        styles.forEachIndexed { idx, st ->
                            val selected = idx == activeIdx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(7.dp)
                                    .width(if (selected) 20.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                    .clickable {
                                        slideDirection = if (idx >= activeIdx) 1 else -1
                                        SpaceManager.updatePreferences { it.copy(progressBarStyle = st) }
                                    }
                            )
                        }
                    }
                    Text(
                        text = "← 좌우로 드래그해 머테리얼 · 하단 라이팅 · 하모니 광감 전환 →",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // 2. 프로그래스바 상세 옵션
            if (currentStyle != ProgressBarStyle.MATERIAL) {
                ProgressSliderRow(
                    label = "내부 순백 빛 세기",
                    valueText = String.format("%.1fx", prefs.progressLightIntensity),
                    value = prefs.progressLightIntensity.coerceIn(0.8f, 2.4f),
                    range = 0.8f..2.4f,
                    onValueChange = { v -> SpaceManager.updatePreferences { it.copy(progressLightIntensity = v) } }
                )
            }
            if (currentStyle == ProgressBarStyle.HARMONY_OPTICAL) {
                ProgressSliderRow(
                    label = "단면 양방향 입자 밀도",
                    valueText = String.format("%.1fx", prefs.progressParticleDensity),
                    value = prefs.progressParticleDensity.coerceIn(0.2f, 2.2f),
                    range = 0.2f..2.2f,
                    onValueChange = { v -> SpaceManager.updatePreferences { it.copy(progressParticleDensity = v) } }
                )
            }

            ProgressSwitchRow(
                title = "주소창 로딩에 적용",
                subtitle = "페이지 로딩 시 주소창에 선택한 프로그래스바 효과 표시",
                checked = prefs.progressShowInOmnibox,
                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(progressShowInOmnibox = c) } }
            )
            ProgressSwitchRow(
                title = "다운로드 팝업에 적용",
                subtitle = "상단 다운로드 진행 배너에 실시간 진행률과 광감 표시",
                checked = prefs.progressShowInDownload,
                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(progressShowInDownload = c) } }
            )
            ProgressSwitchRow(
                title = "주소창 0-오버랩 고무줄 탄성",
                subtitle = "주소창 좌우 스와이프 시 인접 버튼이 겹치지 않고 탄성 압축",
                checked = prefs.toolbarElasticityEnabled,
                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(toolbarElasticityEnabled = c) } }
            )
        }
    }

    // 3. 영상 플레이어 전체화면 세로 밝기·볼륨 HUD 섹션 + 직접 드래그 미리보기
    Text(
        text = "전체화면 영상 세로 밝기·볼륨 HUD",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = settingsCardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProgressSwitchRow(
                title = "세로 밝기·볼륨 광감 HUD",
                subtitle = "전체화면 동영상 좌측(밝기)·우측(볼륨) 상하 스와이프 시 하모니OS 7 세로 캡슐 표시",
                checked = prefs.videoVerticalHudEnabled,
                onCheckedChange = { c -> SpaceManager.updatePreferences { it.copy(videoVerticalHudEnabled = c) } }
            )

            if (prefs.videoVerticalHudEnabled) {
                val scope = rememberCoroutineScope()
                var demoBright by remember { mutableFloatStateOf(0.62f) }
                var demoVol by remember { mutableFloatStateOf(0.48f) }
                var bDrag by remember { mutableStateOf(false) }
                var vDrag by remember { mutableStateOf(false) }
                var bSpd by remember { mutableFloatStateOf(0f) }
                var vSpd by remember { mutableFloatStateOf(0f) }
                val bOver = remember { Animatable(0f) }
                val vOver = remember { Animatable(0f) }
                val bEnergy by animateFloatAsState(if (bDrag) 1f else 0.85f, tween(240), label = "demoBE")
                val vEnergy by animateFloatAsState(if (vDrag) 1f else 0.85f, tween(240), label = "demoVE")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .height(238.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0E1629), Color(0xFF1A1C30), Color(0xFF241724))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 좌측 밝기 캡슐 미리보기 (직접 상하 드래그)
                        Box(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = { bDrag = true },
                                        onDragEnd = {
                                            bDrag = false; bSpd = 0f
                                            scope.launch { bOver.animateTo(0f, EgoMotion.responsiveSpring()) }
                                        },
                                        onDragCancel = {
                                            bDrag = false; bSpd = 0f
                                            scope.launch { bOver.animateTo(0f, EgoMotion.responsiveSpring()) }
                                        },
                                        onVerticalDrag = { change, amount ->
                                            change.consume()
                                            val d = -amount / 320f
                                            bSpd = (abs(d) * 20f).coerceIn(0f, 1.4f)
                                            val raw = demoBright + d
                                            when {
                                                raw < 0f -> { demoBright = 0f; scope.launch { bOver.snapTo((bOver.value + amount * 0.25f).coerceIn(0f, 28f)) } }
                                                raw > 1f -> { demoBright = 1f; scope.launch { bOver.snapTo((bOver.value + amount * 0.25f).coerceIn(-28f, 0f)) } }
                                                else -> { demoBright = raw; scope.launch { bOver.snapTo(0f) } }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            VerticalHarmonyCapsuleSlider(
                                value = demoBright,
                                isBrightness = true,
                                activeEnergy = bEnergy,
                                speed = bSpd,
                                overPullPx = bOver.value,
                                lightIntensity = prefs.videoHudLightIntensity,
                                particleDensity = prefs.videoHudParticleDensity
                            )
                        }

                        // 우측 볼륨 캡슐 미리보기 (직접 상하 드래그)
                        Box(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = { vDrag = true },
                                        onDragEnd = {
                                            vDrag = false; vSpd = 0f
                                            scope.launch { vOver.animateTo(0f, EgoMotion.responsiveSpring()) }
                                        },
                                        onDragCancel = {
                                            vDrag = false; vSpd = 0f
                                            scope.launch { vOver.animateTo(0f, EgoMotion.responsiveSpring()) }
                                        },
                                        onVerticalDrag = { change, amount ->
                                            change.consume()
                                            val d = -amount / 320f
                                            vSpd = (abs(d) * 20f).coerceIn(0f, 1.4f)
                                            val raw = demoVol + d
                                            when {
                                                raw < 0f -> { demoVol = 0f; scope.launch { vOver.snapTo((vOver.value + amount * 0.25f).coerceIn(0f, 28f)) } }
                                                raw > 1f -> { demoVol = 1f; scope.launch { vOver.snapTo((vOver.value + amount * 0.25f).coerceIn(-28f, 0f)) } }
                                                else -> { demoVol = raw; scope.launch { vOver.snapTo(0f) } }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            VerticalHarmonyCapsuleSlider(
                                value = demoVol,
                                isBrightness = false,
                                activeEnergy = vEnergy,
                                speed = vSpd,
                                overPullPx = vOver.value,
                                lightIntensity = prefs.videoHudLightIntensity,
                                particleDensity = prefs.videoHudParticleDensity
                            )
                        }
                    }

                    Text(
                        text = "좌(밝기 ${(demoBright * 100).roundToInt()}%) · 우(볼륨 ${(demoVol * 100).roundToInt()}%) 위아래로 드래그해 테스트",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    )
                }

                ProgressSliderRow(
                    label = "HUD 순백 빛 세기",
                    valueText = String.format("%.1fx", prefs.videoHudLightIntensity),
                    value = prefs.videoHudLightIntensity.coerceIn(0.8f, 2.4f),
                    range = 0.8f..2.4f,
                    onValueChange = { v -> SpaceManager.updatePreferences { it.copy(videoHudLightIntensity = v) } }
                )
                ProgressSliderRow(
                    label = "HUD 단면 입자 밀도",
                    valueText = String.format("%.1fx", prefs.videoHudParticleDensity),
                    value = prefs.videoHudParticleDensity.coerceIn(0.2f, 2.2f),
                    range = 0.2f..2.2f,
                    onValueChange = { v -> SpaceManager.updatePreferences { it.copy(videoHudParticleDensity = v) } }
                )
            }
        }
    }
}

@Composable
private fun ProgressSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(valueText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ProgressSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
