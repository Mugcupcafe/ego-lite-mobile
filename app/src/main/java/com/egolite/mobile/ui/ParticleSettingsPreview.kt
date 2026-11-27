package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.BrowserPreferences
import com.egolite.mobile.model.CardVanishStyle
import com.egolite.mobile.model.VanishDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private enum class PreviewAnimState { IDLE, VANISHING, RETURNING }

private data class PreviewWinRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val holes: Int
)

private data class PreviewParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val delay: Float,
    val life: Float,
    val phase: Float,
    val swirl: Float,
    val size: Float,
    val color: Color
)

private data class LightColorPreset(
    val label: String,
    val hex: Long
)

private val LIGHT_PARTICLE_PRESETS = listOf(
    LightColorPreset("테마 슬레이트", 0xFF2F436EL),
    LightColorPreset("딥 실버 (무채색)", 0xFF5C6472L),
    LightColorPreset("딥 그라파이트", 0xFF373D47L),
    LightColorPreset("코발트 잉크", 0xFF1D4ED8L),
    LightColorPreset("로즈 와인", 0xFF9D174DL),
    LightColorPreset("에메랄드", 0xFF047857L)
)

private fun generateSpacedRandomLayout(
    stageW: Float,
    stageH: Float,
    density: Float,
    rng: Random
): List<PreviewWinRect> {
    val padX = 14f * density
    val padTop = 14f * density
    val padBot = 38f * density
    val usableW = (stageW - padX * 2f).coerceAtLeast(180f * density)
    val usableH = (stageH - padTop - padBot).coerceAtLeast(120f * density)
    val minGap = 14f * density

    val specs = listOf(
        Triple((96f * density).coerceAtMost(usableW * 0.36f), 32f * density, 1),
        Triple((138f * density).coerceAtMost(usableW * 0.44f), 74f * density, 2),
        Triple((174f * density).coerceAtMost(usableW * 0.50f), 100f * density, 3)
    )

    val order = listOf(2, 1, 0)
    repeat(80) {
        val placed = arrayOfNulls<PreviewWinRect>(3)
        val rects = mutableListOf<PreviewWinRect>()
        var ok = true
        for (idx in order) {
            val (w, h, holes) = specs[idx]
            var found: PreviewWinRect? = null
            for (t in 0 until 40) {
                val maxX = (usableW - w).coerceAtLeast(1f)
                val maxY = (usableH - h).coerceAtLeast(1f)
                val x = padX + rng.nextFloat() * maxX
                val y = padTop + rng.nextFloat() * maxY
                val overlaps = rects.any { o ->
                    !(x + w + minGap < o.x || x > o.x + o.w + minGap || y + h + minGap < o.y || y > o.y + o.h + minGap)
                }
                if (!overlaps) {
                    found = PreviewWinRect(x, y, w, h, holes)
                    break
                }
            }
            if (found == null) {
                ok = false
                break
            }
            rects.add(found)
            placed[idx] = found
        }
        if (ok) return placed.filterNotNull()
    }

    val colW = usableW / 3f
    return listOf(
        PreviewWinRect(padX + 4f, padTop + 26f * density, (colW - 10f).coerceAtLeast(70f), 32f * density, 1),
        PreviewWinRect(padX + colW + 4f, padTop + 14f * density, (colW - 10f).coerceAtLeast(90f), 72f * density, 2),
        PreviewWinRect(padX + colW * 2f + 4f, padTop + 8f * density, (colW - 10f).coerceAtLeast(100f), 94f * density, 3)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParticleVanishSettingsSection(
    prefs: BrowserPreferences,
    settingsCardColor: Color
) {
    val styles = remember { CardVanishStyle.entries }
    val activeIdx = styles.indexOf(prefs.cardVanishStyle).coerceAtLeast(0)
    val isAppLight = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    var previewLightMode by remember(isAppLight) { mutableStateOf(isAppLight) }
    var slideDirection by remember { mutableIntStateOf(1) }
    var advancedExpanded by remember { mutableStateOf(false) }

    var animState by remember { mutableStateOf(PreviewAnimState.IDLE) }
    var vanishTick by remember { mutableIntStateOf(0) }
    var morphTrigger by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val rng = remember { Random(System.currentTimeMillis()) }

    var stageSize by remember { mutableStateOf(Size.Zero) }
    var prevLayout by remember { mutableStateOf<List<PreviewWinRect>>(emptyList()) }
    var curLayout by remember { mutableStateOf<List<PreviewWinRect>>(emptyList()) }
    var flyOffsets by remember { mutableStateOf<List<Pair<Offset, Float>>>(emptyList()) }

    val morphAnim = remember { Animatable(1f) }
    val flyInAnim = remember { Animatable(1f) }
    var vanishProgress by remember { mutableFloatStateOf(0f) }
    var previewParticles by remember { mutableStateOf<List<PreviewParticle>>(emptyList()) }

    fun switchStyle(delta: Int) {
        val nextIdx = (activeIdx + delta + styles.size) % styles.size
        slideDirection = if (delta >= 0) 1 else -1
        SpaceManager.updatePreferences { it.copy(cardVanishStyle = styles[nextIdx]) }
        // 창이 소멸/복귀(생성) 중에는 추가 모프 애니메이션 생략
        if (animState == PreviewAnimState.IDLE && stageSize.width > 50f) {
            prevLayout = curLayout
            curLayout = generateSpacedRandomLayout(stageSize.width, stageSize.height, density.density, rng)
            morphTrigger++
        }
    }

    LaunchedEffect(morphTrigger) {
        if (morphTrigger > 0 && animState == PreviewAnimState.IDLE) {
            morphAnim.snapTo(0f)
            morphAnim.animateTo(1f, animationSpec = EgoMotion.morphSpring())
        }
    }

    LaunchedEffect(vanishTick) {
        if (vanishTick == 0 || curLayout.size < 3) return@LaunchedEffect
        val style = prefs.cardVanishStyle
        animState = PreviewAnimState.VANISHING
        vanishProgress = 0f

        if (style == CardVanishStyle.OFF) {
            vanishProgress = 1f
            delay(220L)
        } else if (style == CardVanishStyle.SCALE_FADE) {
            val durMs = (prefs.vanishDurationMs * 0.42f).toLong().coerceIn(220L, 420L)
            val startNanos = withFrameNanos { it }
            while (true) {
                val t = ((withFrameNanos { it } - startNanos) / 1_000_000f) / durMs.toFloat()
                vanishProgress = t.coerceIn(0f, 1f)
                if (t >= 1f) break
            }
            delay(140L)
        } else {
            val targetRgb = if (previewLightMode) (prefs.lightModeParticleColorHex and 0xFFFFFFL).toInt() else 0xF4F8FF
            val tr = (targetRgb shr 16) and 0xFF
            val tg = (targetRgb shr 8) and 0xFF
            val tb = targetRgb and 0xFF
            val br = if (previewLightMode) 212 else 64
            val bg = if (previewLightMode) 222 else 70
            val bb = if (previewLightMode) 236 else 88

            val totalBudget = if (style == CardVanishStyle.LIGHT_PARTICLES) 360
            else (prefs.vanishParticleCount * 0.24f * prefs.sandRibbonDensity.coerceIn(0.5f, 1.8f)).toInt().coerceIn(320, 1400)
            val weights = if (prefs.vanishSizeAdaptiveCount) listOf(0.16f, 0.33f, 0.51f) else listOf(0.33f, 0.34f, 0.33f)
            val sizeBoost = if (previewLightMode && prefs.lightModeParticleSizeBoost) 1.2f else 1.0f
            val built = ArrayList<PreviewParticle>(totalBudget)

            curLayout.forEachIndexed { idx, r ->
                val cnt = (totalBudget * weights[idx]).toInt().coerceAtLeast(36)
                val holes = if (prefs.vanishSizeAdaptiveDuration) r.holes else 2
                val seeds = List(holes) { Offset(0.18f + rng.nextFloat() * 0.64f, 0.20f + rng.nextFloat() * 0.60f) }
                for (i in 0 until cnt) {
                    val u = rng.nextFloat()
                    val v = rng.nextFloat()
                    var minD = 9f
                    for (sd in seeds) {
                        minD = min(minD, hypot(u - sd.x, v - sd.y))
                    }
                    val emitT = when (style) {
                        CardVanishStyle.HARMONY_7_SAND_WAVE -> (minD / 0.95f).coerceIn(0f, 1f).pow(0.9f) * 0.62f
                        CardVanishStyle.HARMONY_GPU_SHADER -> hypot(u - 0.5f, v - 0.5f) * 0.55f
                        else -> rng.nextFloat() * 0.34f
                    }
                    val mix = (prefs.sandSilverRatio + (rng.nextFloat() - 0.5f) * 0.22f).coerceIn(0.28f, 0.96f)
                    val cr = (br + (tr - br) * mix).toInt().coerceIn(0, 255)
                    val cg = (bg + (tg - bg) * mix).toInt().coerceIn(0, 255)
                    val cb = (bb + (tb - bb) * mix).toInt().coerceIn(0, 255)
                    val wind = prefs.sandWindStrength.coerceIn(0.4f, 2.0f)
                    val vx: Float
                    val vy: Float
                    if (style == CardVanishStyle.HARMONY_7_SAND_WAVE) {
                        vx = (58f + rng.nextFloat() * 96f) * wind * density.density * 0.6f
                        vy = (-44f - rng.nextFloat() * 82f) * wind * density.density * 0.6f
                    } else if (style == CardVanishStyle.HARMONY_GPU_SHADER) {
                        val ang = atan2(v - 0.5f, u - 0.5f) + (rng.nextFloat() - 0.5f) * 0.5f
                        val spd = (48f + rng.nextFloat() * 98f) * wind * density.density * 0.6f
                        vx = cos(ang) * spd
                        vy = sin(ang) * spd - 14f * density.density
                    } else {
                        vx = (32f + rng.nextFloat() * 76f) * wind * density.density * 0.6f
                        vy = (-26f - rng.nextFloat() * 62f) * wind * density.density * 0.6f
                    }
                    built.add(
                        PreviewParticle(
                            x = r.x + u * r.w,
                            y = r.y + v * r.h,
                            vx = vx,
                            vy = vy,
                            delay = emitT,
                            life = 0.32f + rng.nextFloat() * 0.30f,
                            phase = rng.nextFloat() * 6.283f,
                            swirl = (10f + rng.nextFloat() * 14f) * prefs.sandSwirlStrength.coerceIn(0.2f, 2.0f) * density.density * 0.55f,
                            size = (1.35f + rng.nextFloat() * 1.45f) * sizeBoost * density.density,
                            color = Color(cr, cg, cb)
                        )
                    )
                }
            }
            previewParticles = built

            val durMs = prefs.vanishDurationMs.coerceIn(450, 2400).toFloat()
            val startNanos = withFrameNanos { it }
            while (true) {
                val t = ((withFrameNanos { it } - startNanos) / 1_000_000f) / durMs
                vanishProgress = t.coerceIn(0f, 1.08f)
                if (t >= 1.06f) break
            }
            previewParticles = emptyList()
            delay(120L)
        }

        // 자동 복귀: 사방 랜덤 위치에서 날아와 겹치지 않는 새 랜덤 위치에 착지
        animState = PreviewAnimState.RETURNING
        val nextLayout = generateSpacedRandomLayout(stageSize.width, stageSize.height, density.density, rng)
        curLayout = nextLayout
        prevLayout = nextLayout
        flyOffsets = nextLayout.map { r ->
            val edge = rng.nextInt(4)
            val dx: Float
            val dy: Float
            when (edge) {
                0 -> { dx = -r.x - r.w - 36f * density.density; dy = (rng.nextFloat() - 0.5f) * 60f * density.density }
                1 -> { dx = stageSize.width - r.x + 36f * density.density; dy = (rng.nextFloat() - 0.5f) * 60f * density.density }
                2 -> { dx = (rng.nextFloat() - 0.5f) * 90f * density.density; dy = -r.y - r.h - 36f * density.density }
                else -> { dx = (rng.nextFloat() - 0.5f) * 90f * density.density; dy = stageSize.height - r.y + 36f * density.density }
            }
            Offset(dx, dy) to ((rng.nextFloat() - 0.5f) * 16f)
        }
        vanishProgress = 0f
        flyInAnim.snapTo(0f)
        flyInAnim.animateTo(1f, animationSpec = EgoMotion.morphSpring())
        animState = PreviewAnimState.IDLE
    }

    Text(
        text = "카드 삭제 애니메이션 (스와이프 선택 · 터치 미리보기)",
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )

    // 1) 상단 인터랙티브 스와이프 미리보기 카드 (긴 라디오 목록 대체)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = settingsCardColor,
        modifier = Modifier
            .fillMaxWidth()
            .harmonyRimGlow(enabled = prefs.rimGlowEnabled, shape = RoundedCornerShape(24.dp), glowColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 스타일 헤더 + 좌우 버튼 + 주간/야간 미리보기 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = prefs.cardVanishStyle,
                    transitionSpec = {
                        val dir = slideDirection
                        (slideInHorizontally(tween(240)) { w -> if (dir >= 0) w / 2 else -w / 2 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally(tween(200)) { w -> if (dir >= 0) -w / 2 else w / 2 } + fadeOut(tween(150)))
                    },
                    modifier = Modifier.weight(1f),
                    label = "vanishTitleSlide"
                ) { st ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = st.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "적용됨",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            listOf(false to "야간", true to "주간").forEach { (isLight, lbl) ->
                                val sel = previewLightMode == isLight
                                Surface(
                                    shape = CircleShape,
                                    color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.clickable { previewLightMode = isLight }
                                ) {
                                    Text(
                                        text = lbl,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(28.dp).clickable { switchStyle(-1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EgoIcon(Icons.Default.ChevronLeft, contentDescription = "이전", modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(28.dp).clickable { switchStyle(1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EgoIcon(Icons.Default.ChevronRight, contentDescription = "다음", modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                        }
                    }
                }
            }

            // 인터랙티브 3창 동시 미리보기 스테이지 (작은 · 중간 · 큰 창)
            val stageBgBrush = if (previewLightMode) {
                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEEF2F8)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF202430), Color(0xFF12141A)))
            }
            val stageBorderColor = if (previewLightMode) Color(0x220F172A) else Color(0x1CFFFFFF)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(242.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(stageBgBrush)
                    .border(1.dp, stageBorderColor, RoundedCornerShape(18.dp))
                    .pointerInput(prefs.cardVanishStyle, animState) {
                        var dragAccum = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccum = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                            onDragEnd = {
                                if (dragAccum < -48f) switchStyle(1)
                                else if (dragAccum > 48f) switchStyle(-1)
                            }
                        )
                    }
                    .pointerInput(prefs.cardVanishStyle, animState) {
                        detectTapGestures {
                            if (animState == PreviewAnimState.IDLE) {
                                vanishTick++
                            }
                        }
                    }
            ) {
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                LaunchedEffect(wPx, hPx) {
                    if (wPx > 50f && hPx > 50f) {
                        stageSize = Size(wPx, hPx)
                        if (curLayout.isEmpty()) {
                            val init = generateSpacedRandomLayout(wPx, hPx, density.density, rng)
                            curLayout = init
                            prevLayout = init
                        }
                    }
                }

                val mProg = morphAnim.value.coerceIn(0f, 1.1f)
                val flyProg = flyInAnim.value.coerceIn(0f, 1.1f)
                val winCardBg = if (previewLightMode) Color(0xFFFFFFFF) else Color(0xFF2B303E)
                val winCardBorder = if (previewLightMode) Color(0x220F172A) else Color(0x24FFFFFF)
                val winTextColor = if (previewLightMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                val winChipBg = if (previewLightMode) Color(0x1F1D4ED8) else MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                val winChipFg = if (previewLightMode) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.primary

                // 3개 모의 창 렌더링
                if (curLayout.size == 3) {
                    curLayout.forEachIndexed { idx, toRect ->
                        val fromRect = prevLayout.getOrNull(idx) ?: toRect
                        val cx = if (animState == PreviewAnimState.IDLE) fromRect.x + (toRect.x - fromRect.x) * mProg else toRect.x
                        val cy = if (animState == PreviewAnimState.IDLE) fromRect.y + (toRect.y - fromRect.y) * mProg else toRect.y
                        val flyPair = flyOffsets.getOrNull(idx)
                        val tx = if (animState == PreviewAnimState.RETURNING && flyPair != null) flyPair.first.x * (1f - flyProg) else 0f
                        val ty = if (animState == PreviewAnimState.RETURNING && flyPair != null) flyPair.first.y * (1f - flyProg) else 0f
                        val rot = when (animState) {
                            PreviewAnimState.RETURNING -> (flyPair?.second ?: 0f) * (1f - flyProg)
                            PreviewAnimState.IDLE -> sin(mProg.coerceIn(0f, 1f) * PI).toFloat() * (if (slideDirection >= 0) -4.5f else 4.5f) * (if (idx % 2 == 0) 1f else -0.7f)
                            else -> 0f
                        }
                        val cardAlpha = when (animState) {
                            PreviewAnimState.VANISHING -> when (prefs.cardVanishStyle) {
                                CardVanishStyle.OFF -> 0f
                                CardVanishStyle.SCALE_FADE -> (1f - vanishProgress).coerceIn(0f, 1f)
                                else -> (1f - vanishProgress * 2.4f).coerceIn(0f, 1f)
                            }
                            PreviewAnimState.RETURNING -> (flyProg * 2.0f).coerceIn(0f, 1f)
                            PreviewAnimState.IDLE -> 1f
                        }
                        val cardScale = when (animState) {
                            PreviewAnimState.VANISHING -> if (prefs.cardVanishStyle == CardVanishStyle.SCALE_FADE) (1f - vanishProgress * 0.22f) else 1f
                            PreviewAnimState.RETURNING -> 0.72f + 0.28f * flyProg
                            PreviewAnimState.IDLE -> 1f + 0.04f * sin(mProg.coerceIn(0f, 1f) * PI).toFloat()
                        }

                        val shape = when (idx) {
                            0 -> CircleShape
                            1 -> RoundedCornerShape(14.dp)
                            else -> RoundedCornerShape(16.dp)
                        }

                        Surface(
                            shape = shape,
                            color = winCardBg,
                            shadowElevation = if (previewLightMode) 5.dp else 8.dp,
                            modifier = Modifier
                                .offset { IntOffset(cx.roundToInt(), cy.roundToInt()) }
                                .width(with(density) { toRect.w.toDp() })
                                .height(with(density) { toRect.h.toDp() })
                                .graphicsLayer {
                                    translationX = tx
                                    translationY = ty
                                    rotationZ = rot
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    alpha = cardAlpha
                                }
                                .border(1.dp, winCardBorder, shape)
                        ) {
                            when (idx) {
                                0 -> Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    EgoIcon(Icons.Default.Public, contentDescription = null, tint = winChipFg, modifier = Modifier.size(13.dp), symbolSize = 13.dp)
                                    Text("작은 탭", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = winTextColor, maxLines = 1)
                                }
                                1 -> Column(
                                    modifier = Modifier.fillMaxSize().padding(9.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = winChipBg) {
                                        Text("중간 · 구멍 2", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = winChipFg, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                    }
                                    Text("번역 / 광고차단", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = winTextColor, maxLines = 1)
                                    Box(modifier = Modifier.fillMaxWidth(0.75f).height(4.dp).clip(CircleShape).background(winTextColor.copy(alpha = 0.22f)))
                                }
                                else -> Column(
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = winChipBg) {
                                        Text("큰 카드 · 구멍 3", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = winChipFg, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                                    }
                                    Text("AI 워크스페이스 창", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = winTextColor, maxLines = 1)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth(0.9f).height(4.dp).clip(CircleShape).background(winTextColor.copy(alpha = 0.22f)))
                                        Box(modifier = Modifier.fillMaxWidth(0.62f).height(4.dp).clip(CircleShape).background(winTextColor.copy(alpha = 0.16f)))
                                    }
                                }
                            }
                        }
                    }
                }

                // 실시간 미리보기 파티클 캔버스
                if (previewParticles.isNotEmpty() && animState == PreviewAnimState.VANISHING) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val t = vanishProgress
                        for (p in previewParticles) {
                            val lt = (t - p.delay) / p.life
                            if (lt <= 0f || lt >= 1f) continue
                            val drag = 1f - exp(-2.2f * lt)
                            val px = p.x + p.vx * drag + sin(p.phase + t * 5.2f) * p.swirl * lt
                            val py = p.y + p.vy * drag + cos(p.phase + t * 4.4f) * p.swirl * 0.75f * lt
                            val a = (1f - lt).pow(1.35f).coerceIn(0f, 1f)
                            val sz = (p.size * (1.05f - lt * 0.42f)).coerceAtLeast(1.2f)
                            drawRect(
                                color = p.color.copy(alpha = a),
                                topLeft = Offset(px, py),
                                size = Size(sz, sz)
                            )
                        }
                    }
                }

                // 하단 안내 배지 + 상태 배지
                Surface(
                    shape = CircleShape,
                    color = if (previewLightMode) Color(0xD8FFFFFF) else Color(0xB0101218),
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                ) {
                    Text(
                        text = "터치하여 3개 창 동시 재생 · 좌우 스와이프 변경",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (previewLightMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                val statusLabel = when (animState) {
                    PreviewAnimState.IDLE -> "대기 중"
                    PreviewAnimState.VANISHING -> "소멸 중"
                    PreviewAnimState.RETURNING -> "랜덤 복귀"
                }
                Surface(
                    shape = CircleShape,
                    color = if (previewLightMode) Color(0x1F1D4ED8) else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (previewLightMode) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 페이지 도트 + 슬라이드 설명
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                styles.forEachIndexed { i, st ->
                    val sel = i == activeIdx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.5.dp)
                            .height(7.dp)
                            .width(if (sel) 22.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            .clickable {
                                if (i != activeIdx) switchStyle(if (i > activeIdx) 1 else -1)
                            }
                    )
                }
            }
            AnimatedContent(
                targetState = prefs.cardVanishStyle,
                transitionSpec = {
                    val dir = slideDirection
                    (slideInHorizontally(tween(240)) { w -> if (dir >= 0) w / 3 else -w / 3 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(180)) { w -> if (dir >= 0) -w / 3 else w / 3 } + fadeOut(tween(140)))
                },
                label = "vanishDescSlide"
            ) { st ->
                Text(
                    text = st.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 2) 하단 초보자 친화 기본 조절 카드 (스타일별 동적 표시)
    val style = prefs.cardVanishStyle
    val usesParticles = style != CardVanishStyle.SCALE_FADE && style != CardVanishStyle.OFF
    val hasDensityAndFps = style == CardVanishStyle.HARMONY_7_SAND_WAVE || style == CardVanishStyle.HARMONY_GPU_SHADER || style == CardVanishStyle.HARMONY_DUST
    val hasAdvanced = style == CardVanishStyle.HARMONY_7_SAND_WAVE || style == CardVanishStyle.HARMONY_GPU_SHADER

    M3SettingsGroup(settingsCardColor) {
        if (!usesParticles) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (style == CardVanishStyle.OFF) "애니메이션 없음" else "입자 세부 옵션 없음",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "선택한 스타일은 파티클을 사용하지 않아 추가 조절 없이 가장 가볍고 빠르게 동작합니다.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 주간모드 파티클 색감 커스텀
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val curPresetName = LIGHT_PARTICLE_PRESETS.firstOrNull { it.hex == prefs.lightModeParticleColorHex }?.label ?: "커스텀"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("주간모드 파티클 색감", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(curPresetName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "야간모드는 은빛·순백을 유지하며, 주간모드에서만 흰 배경 대비 선명한 색감으로 자동 전환됩니다.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LIGHT_PARTICLE_PRESETS.forEach { preset ->
                            val sel = prefs.lightModeParticleColorHex == preset.hex
                            Surface(
                                shape = CircleShape,
                                color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .border(
                                        width = if (sel) 1.dp else 0.dp,
                                        color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        SpaceManager.updatePreferences { it.copy(lightModeParticleColorHex = preset.hex) }
                                        previewLightMode = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.hex))
                                            .border(0.8.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                    )
                                    Text(
                                        text = preset.label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (hasDensityAndFps) {
            item {
                val curCount = prefs.vanishParticleCount.coerceIn(1000, 14000)
                val isRec = curCount in 2500..4500
                val isHigh = curCount > 4500 && curCount <= 8000
                val isExtreme = curCount > 8000
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("입자 밀도", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${"%,d".format(curCount)}개" + if (isRec) " · ★ 권장" else "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isExtreme -> Color(0xFFEF4444)
                                isHigh -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Slider(
                        value = curCount.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 500f).roundToInt() * 500).coerceIn(1000, 14000)
                            SpaceManager.updatePreferences { it.copy(vanishParticleCount = snapped) }
                        },
                        valueRange = 1000f..14000f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf(1500 to "1,500", 3500 to "★ 3,500", 6000 to "6,000", 10000 to "10,000", 14000 to "14,000").forEach { (cnt, lbl) ->
                            val sel = curCount == cnt
                            Surface(
                                shape = CircleShape,
                                color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f).clickable {
                                    SpaceManager.updatePreferences { it.copy(vanishParticleCount = cnt) }
                                }
                            ) {
                                Text(
                                    text = lbl,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                )
                            }
                        }
                    }
                    if (isHigh || isExtreme) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isExtreme) Color(0x26EF4444) else Color(0x24F59E0B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isExtreme) Color(0x70EF4444) else Color(0x66F59E0B), RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = if (isExtreme) {
                                    "⚠ 초고밀도 경고(8,000개 초과): 기기 발열, 배터리 소모 증가 및 순간 프레임 드롭이 발생할 수 있습니다."
                                } else {
                                    "⚠ 권장 범위(2,500~4,500개) 초과: 소멸 순간 GPU/CPU 부하와 소비 전력이 다소 늘어날 수 있습니다."
                                },
                                fontSize = 11.5.sp,
                                color = if (isExtreme) Color(0xFFFCA5A5) else Color(0xFFFCD34D),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val smEst = (curCount * 0.42f).roundToInt().coerceIn(400, 14000)
                    val lgEst = (curCount * 1.48f).roundToInt().coerceIn(400, 14000)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                SpaceManager.updatePreferences { it.copy(vanishSizeAdaptiveCount = !it.vanishSizeAdaptiveCount) }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("크기 비례 입자 수량 자동 조절", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (prefs.vanishSizeAdaptiveCount) {
                                    "작은 탭 약 ${"%,d".format(smEst)} · 중간 ${"%,d".format(curCount)} · 큰 창 약 ${"%,d".format(lgEst)}개"
                                } else {
                                    "크기와 상관없이 모든 창에 ${"%,d".format(curCount)}개 동일 적용"
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.vanishSizeAdaptiveCount,
                            onCheckedChange = { c ->
                                SpaceManager.updatePreferences { it.copy(vanishSizeAdaptiveCount = c) }
                            }
                        )
                    }
                }
            }
        }

        if (style != CardVanishStyle.OFF) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("소멸 속도", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("${prefs.vanishDurationMs} ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = prefs.vanishDurationMs.coerceIn(450, 2400).toFloat(),
                        onValueChange = { v ->
                            val snapped = ((v / 50f).roundToInt() * 50).coerceIn(450, 2400)
                            SpaceManager.updatePreferences { it.copy(vanishDurationMs = snapped) }
                        },
                        valueRange = 450f..2400f
                    )
                }
            }
        }

        if (hasDensityAndFps) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("프레임 상한", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(60 to "60fps (절전)", 90 to "90fps (부드러움)", 0 to "기기 최대 (120Hz+)").forEach { (fps, lbl) ->
                            val sel = prefs.vanishMaxFps == fps
                            Surface(
                                shape = CircleShape,
                                color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f).clickable {
                                    SpaceManager.updatePreferences { it.copy(vanishMaxFps = fps) }
                                }
                            ) {
                                Text(
                                    text = lbl,
                                    fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 3) 접이식 고급 세부 조절 아코디언 (선택한 스타일 전용 옵션만 깔끔하게 노출)
    if (hasAdvanced) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = settingsCardColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (style == CardVanishStyle.HARMONY_7_SAND_WAVE) {
                            "고급 세부 조절 (모래 띠 · 바람 · 소용돌이 · 주간 보정)"
                        } else {
                            "고급 세부 조절 (소멸 방향 · 발광 · 충격파 · 공기 저항)"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    EgoIcon(
                        imageVector = if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                        symbolSize = 20.dp
                    )
                }

                AnimatedVisibility(
                    visible = advancedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        if (style == CardVanishStyle.HARMONY_7_SAND_WAVE) {
                            CompactSliderRow("모래 띠 밀도", "${"%.1f".format(prefs.sandRibbonDensity)}x", prefs.sandRibbonDensity, 0.4f..1.8f) { v ->
                                SpaceManager.updatePreferences { it.copy(sandRibbonDensity = v) }
                            }
                            CompactSliderRow("은빛/잉크 혼합 비율", "${(prefs.sandSilverRatio * 100).toInt()}%", prefs.sandSilverRatio, 0.2f..0.95f) { v ->
                                SpaceManager.updatePreferences { it.copy(sandSilverRatio = v) }
                            }
                            CompactSliderRow("바람 기류 세기", "${"%.1f".format(prefs.sandWindStrength)}x", prefs.sandWindStrength, 0.3f..2.0f) { v ->
                                SpaceManager.updatePreferences { it.copy(sandWindStrength = v) }
                            }
                            CompactSliderRow("소용돌이 (Swirl)", "${"%.1f".format(prefs.sandSwirlStrength)}x", prefs.sandSwirlStrength, 0f..2.0f) { v ->
                                SpaceManager.updatePreferences { it.copy(sandSwirlStrength = v) }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("소멸 방향", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    VanishDirection.entries.forEach { dir ->
                                        val sel = prefs.vanishDirection == dir
                                        Surface(
                                            shape = CircleShape,
                                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.weight(1f).clickable {
                                                SpaceManager.updatePreferences { it.copy(vanishDirection = dir) }
                                            }
                                        ) {
                                            Text(
                                                text = dir.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            CompactSliderRow("경계 광학 발광", "${"%.1f".format(prefs.vanishEdgeGlow)}x", prefs.vanishEdgeGlow, 0f..2.0f) { v ->
                                SpaceManager.updatePreferences { it.copy(vanishEdgeGlow = v) }
                            }
                            CompactSliderRow("충격파 세기", "${"%.1f".format(prefs.vanishShockwaveStrength)}x", prefs.vanishShockwaveStrength, 0f..2.0f) { v ->
                                SpaceManager.updatePreferences { it.copy(vanishShockwaveStrength = v) }
                            }
                            CompactSliderRow("공기 저항 (Drag)", "${"%.1f".format(prefs.vanishAirDrag)}", prefs.vanishAirDrag, 1.5f..7.5f) { v ->
                                SpaceManager.updatePreferences { it.copy(vanishAirDrag = v) }
                            }
                            CompactSliderRow("소용돌이 (Curl)", "${"%.1f".format(prefs.vanishCurlStrength)}x", prefs.vanishCurlStrength, 0f..2.0f) { v ->
                                SpaceManager.updatePreferences { it.copy(vanishCurlStrength = v) }
                            }
                            CompactSliderRow("Z축 원근감", "${"%.1f".format(prefs.vanishZPerspective)}x", prefs.vanishZPerspective, 0f..2.0f) { v ->
                                SpaceManager.updatePreferences { it.copy(vanishZPerspective = v) }
                            }
                            CompactToggleRow("가산 발광 (Additive Glow)", "야간모드에서 입자가 겹칠 때 더 밝게 빛납니다.", prefs.vanishAdditiveGlow) { c ->
                                SpaceManager.updatePreferences { it.copy(vanishAdditiveGlow = c) }
                            }
                        }

                        CompactToggleRow(
                            "주간모드 입자 크기 자동 보정 (+18%)",
                            "흰 배경에서 입자가 가늘어 보이는 착시를 보정합니다.",
                            prefs.lightModeParticleSizeBoost
                        ) { c ->
                            SpaceManager.updatePreferences { it.copy(lightModeParticleSizeBoost = c) }
                        }
                        CompactToggleRow(
                            "요소 크기 비례 속도 · 구멍 자동 보정",
                            "작은 탭(구멍 1) · 중간 팝업(2) · 큰 카드(3)에 맞춰 자동 조절합니다.",
                            prefs.vanishSizeAdaptiveDuration
                        ) { c ->
                            SpaceManager.updatePreferences { it.copy(vanishSizeAdaptiveDuration = c) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SpaceManager.updatePreferences {
                                        it.copy(
                                            vanishParticleCount = 3500,
                                            vanishDurationMs = 1250,
                                            sandRibbonDensity = 1.15f,
                                            sandSilverRatio = 0.78f,
                                            sandWindStrength = 1.05f,
                                            sandSwirlStrength = 1.05f,
                                            vanishEdgeGlow = 1.0f,
                                            vanishCurlStrength = 1.0f,
                                            vanishShockwaveStrength = 1.0f,
                                            vanishAirDrag = 4.2f,
                                            vanishAdditiveGlow = true,
                                            vanishZPerspective = 1.0f,
                                            vanishSizeAdaptiveDuration = true,
                                            lightModeParticleColorHex = 0xFF2F436EL,
                                            lightModeParticleSizeBoost = true,
                                            vanishSizeAdaptiveCount = true
                                        )
                                    }
                                }
                                .padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "기본 권장값으로 복원",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(valueText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun CompactToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
