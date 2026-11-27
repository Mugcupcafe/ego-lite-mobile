package com.egolite.mobile.ui

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.ProgressBarStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 개별 입자 풀(True Particle Pool) — 격자 스크롤 없이 19012.mp4와 동일하게
 * 단면(cutX / cutY)에서 양방향으로 방출되어 유기적 난류를 타며 멀어질수록 소멸.
 */
internal class OpticalParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var isBokeh: Boolean,
    var life: Float,
    var maxLife: Float,
    var maxDist: Float,
    var phase: Float,
    var freq: Float,
    var swirl: Float
)

internal class OpticalParticlePool(private val maxCount: Int = 220) {
    val particles = ArrayList<OpticalParticle>(maxCount)
    private var spawnAcc = 0f
    private val rng = Random(System.nanoTime())

    fun clear() {
        particles.clear()
        spawnAcc = 0f
    }

    fun stepHorizontal(
        dt: Float,
        cutX: Float,
        barW: Float,
        barH: Float,
        speed: Float,
        densityMul: Float,
        spawning: Boolean
    ) {
        if (spawning && barW > 20f && barH > 6f) {
            val rate = (54f + speed * 140f) * densityMul.coerceIn(0f, 2.5f)
            spawnAcc += rate * dt
            while (spawnAcc >= 1f) {
                spawnAcc -= 1f
                if (particles.size >= maxCount) particles.removeAt(0)
                val side = if (rng.nextFloat() < 0.48f) -1f else 1f
                val tier = rng.nextFloat()
                val radius: Float
                val bokeh: Boolean
                when {
                    tier < 0.65f -> { radius = 1.4f + rng.nextFloat() * 1.1f; bokeh = false }
                    tier < 0.92f -> { radius = 2.6f + rng.nextFloat() * 1.8f; bokeh = false }
                    else -> { radius = 4.2f + rng.nextFloat() * 2.4f; bokeh = true }
                }
                val maxDist = barW * (0.17f + rng.nextFloat() * 0.14f) * (1f + speed * 0.35f)
                val vx = side * (48f + rng.nextFloat() * 105f + speed * 170f)
                val vy = (rng.nextFloat() - 0.5f) * (32f + speed * 36f)
                particles.add(
                    OpticalParticle(
                        x = cutX + side * (rng.nextFloat() * 12f),
                        y = 4f + rng.nextFloat() * (barH - 8f).coerceAtLeast(4f),
                        vx = vx,
                        vy = vy,
                        radius = radius,
                        isBokeh = bokeh,
                        life = 0f,
                        maxLife = 0.42f + rng.nextFloat() * 0.66f,
                        maxDist = maxDist,
                        phase = rng.nextFloat() * (PI.toFloat() * 2f),
                        freq = 7f + rng.nextFloat() * 11f,
                        swirl = (rng.nextFloat() - 0.5f) * 44f
                    )
                )
            }
        }
        val drag = 0.28f.pow(dt)
        var i = particles.size - 1
        while (i >= 0) {
            val pt = particles[i]
            pt.life += dt
            val t = pt.life / pt.maxLife
            if (t >= 1f || abs(pt.x - cutX) > pt.maxDist) {
                particles.removeAt(i)
            } else {
                pt.vx *= drag
                pt.x += pt.vx * dt
                pt.y += (pt.vy + sin(pt.life * 6.5f + pt.phase) * pt.swirl) * dt
            }
            i--
        }
    }

    fun stepVertical(
        dt: Float,
        cutY: Float,
        barW: Float,
        barH: Float,
        speed: Float,
        densityMul: Float,
        spawning: Boolean
    ) {
        if (spawning && barW > 12f && barH > 20f) {
            val rate = (68f + speed * 185f) * densityMul.coerceIn(0f, 2.5f)
            spawnAcc += rate * dt
            while (spawnAcc >= 1f) {
                spawnAcc -= 1f
                if (particles.size >= maxCount) particles.removeAt(0)
                val side = if (rng.nextFloat() < 0.48f) -1f else 1f
                val tier = rng.nextFloat()
                val radius: Float
                val bokeh: Boolean
                when {
                    tier < 0.62f -> { radius = 1.5f + rng.nextFloat() * 1.2f; bokeh = false }
                    tier < 0.90f -> { radius = 2.8f + rng.nextFloat() * 2.0f; bokeh = false }
                    else -> { radius = 4.6f + rng.nextFloat() * 2.8f; bokeh = true }
                }
                val maxDist = barH * (0.20f + rng.nextFloat() * 0.16f) * (1f + speed * 0.35f)
                val vy = side * (52f + rng.nextFloat() * 115f + speed * 185f)
                val vx = (rng.nextFloat() - 0.5f) * (36f + speed * 42f)
                particles.add(
                    OpticalParticle(
                        x = 6f + rng.nextFloat() * (barW - 12f).coerceAtLeast(4f),
                        y = cutY + side * (rng.nextFloat() * 14f),
                        vx = vx,
                        vy = vy,
                        radius = radius,
                        isBokeh = bokeh,
                        life = 0f,
                        maxLife = 0.45f + rng.nextFloat() * 0.70f,
                        maxDist = maxDist,
                        phase = rng.nextFloat() * (PI.toFloat() * 2f),
                        freq = 7f + rng.nextFloat() * 11f,
                        swirl = (rng.nextFloat() - 0.5f) * 42f
                    )
                )
            }
        }
        val drag = 0.26f.pow(dt)
        var i = particles.size - 1
        while (i >= 0) {
            val pt = particles[i]
            pt.life += dt
            val t = pt.life / pt.maxLife
            if (t >= 1f || abs(pt.y - cutY) > pt.maxDist) {
                particles.removeAt(i)
            } else {
                pt.vy *= drag
                pt.y += pt.vy * dt
                pt.x += (pt.vx + sin(pt.life * 6.0f + pt.phase) * pt.swirl) * dt
            }
            i--
        }
    }
}

/**
 * 주소창 · 다운로드 배너 내부에 그려지는 통합 광감/하단 라이팅 오버레이.
 * - ProgressBarStyle.BOTTOM_LIGHTING: 하단 테두리 발광 스트립 + 코멧 헤드
 * - ProgressBarStyle.HARMONY_OPTICAL: 방식 B (실버 베이스 + 단면 100% 순백 커튼 + 양방향 개별 입자 풀)
 * - 완료(progress >= 1f 또는 !isActive) 시 부드럽게 0으로 페이드아웃
 */
@Composable
fun HarmonyHorizontalProgressCanvas(
    style: ProgressBarStyle,
    progress: Float,
    isActive: Boolean,
    accentColor: Color,
    lightIntensity: Float = 1.6f,
    particleDensity: Float = 1.4f,
    cornerRadiusDp: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    if (style == ProgressBarStyle.MATERIAL) return

    val isLightTheme = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val clampedProg = progress.coerceIn(0f, 1f)
    val smoothProg by animateFloatAsState(
        targetValue = if (isActive) clampedProg else if (clampedProg >= 0.99f) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "opticalProg"
    )
    val effectAlpha by animateFloatAsState(
        targetValue = if (isActive && clampedProg in 0.005f..0.995f) 1f else 0f,
        animationSpec = tween(durationMillis = if (isActive) 120 else 280),
        label = "opticalAlpha"
    )

    val pool = remember { OpticalParticlePool(200) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var prevProg by remember { mutableFloatStateOf(smoothProg) }
    var smoothSpeed by remember { mutableFloatStateOf(0f) }
    var lastSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(style, effectAlpha > 0.01f) {
        if (style != ProgressBarStyle.HARMONY_OPTICAL) {
            pool.clear()
            return@LaunchedEffect
        }
        var lastNs = 0L
        while (effectAlpha > 0.01f || pool.particles.isNotEmpty()) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                val rawSpd = (abs(smoothProg - prevProg) / dt * 1.35f).coerceAtMost(1.5f)
                prevProg = smoothProg
                smoothSpeed += (rawSpd - smoothSpeed) * (dt * 10f).coerceAtMost(1f)
                val w = lastSize.width
                val h = lastSize.height
                val cutX = w * smoothProg
                pool.stepHorizontal(
                    dt = dt,
                    cutX = cutX,
                    barW = w,
                    barH = h,
                    speed = smoothSpeed,
                    densityMul = particleDensity,
                    spawning = effectAlpha > 0.08f && smoothProg in 0.01f..0.992f
                )
                frameTick = ns
            }
        }
    }

    Canvas(modifier = modifier) {
        lastSize = size
        @Suppress("UNUSED_EXPRESSION")
        frameTick
        if (effectAlpha <= 0.008f && pool.particles.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val cutX = (w * smoothProg).coerceIn(0f, w)
        val rPx = cornerRadiusDp.toPx().coerceAtMost(h * 0.5f)
        val clipShape = Path().apply {
            addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(rPx, rPx)))
        }

        clipPath(clipShape) {
            if (style == ProgressBarStyle.BOTTOM_LIGHTING) {
                val stripH = 3.2.dp.toPx()
                val glowH = 14.dp.toPx()
                // Soft upward ambient glow along bottom edge
                if (cutX > 2f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = (0.34f * lightIntensity * effectAlpha).coerceIn(0f, 0.65f))
                            ),
                            startY = h - glowH,
                            endY = h
                        ),
                        topLeft = Offset(0f, h - glowH),
                        size = Size(cutX, glowH)
                    )
                    // Sharp luminous bottom bar
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.45f * effectAlpha),
                                accentColor.copy(alpha = 0.92f * effectAlpha),
                                Color.White.copy(alpha = (0.98f * effectAlpha).coerceIn(0f, 1f))
                            ),
                            startX = 0f,
                            endX = cutX.coerceAtLeast(1f)
                        ),
                        topLeft = Offset(0f, h - stripH),
                        size = Size(cutX, stripH)
                    )
                    // Bright comet head at cutX
                    val headR = 16.dp.toPx() * (0.75f + 0.25f * lightIntensity)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (0.95f * effectAlpha).coerceIn(0f, 1f)),
                                accentColor.copy(alpha = (0.55f * effectAlpha).coerceIn(0f, 1f)),
                                Color.Transparent
                            ),
                            center = Offset(cutX, h - stripH * 0.5f),
                            radius = headR
                        ),
                        radius = headR,
                        center = Offset(cutX, h - stripH * 0.5f)
                    )
                }
            } else if (style == ProgressBarStyle.HARMONY_OPTICAL) {
                if (cutX > 1f && effectAlpha > 0.01f) {
                    // 1. 방식 B: 지나간 구간(0..cutX) 실버 베이스(야간 기준 검정 글자 선명 유지)
                    val silverBase = if (isLightTheme) {
                        accentColor.copy(alpha = 0.88f * effectAlpha)
                    } else {
                        Color(0xFF7A8396).copy(alpha = 0.94f * effectAlpha)
                    }
                    drawRect(
                        color = silverBase,
                        topLeft = Offset.Zero,
                        size = Size(cutX, h)
                    )

                    // 2. 단면 직전 100% 순백 불투명 HDR 빛 커튼 (19012.mp4 정밀 비율)
                    val curtainW = (w * (0.21f + smoothSpeed.coerceIn(0f, 1f) * 0.11f) * (0.82f + 0.18f * lightIntensity))
                        .coerceIn(18f, w * 0.45f)
                    val startX = (cutX - curtainW).coerceAtLeast(0f)
                    val peakAlpha = (0.96f * effectAlpha * (0.82f + 0.18f * lightIntensity)).coerceIn(0f, 1f)
                    val curtainStartCol = if (isLightTheme) accentColor.copy(alpha = 0f) else Color.White.copy(alpha = 0f)
                    val curtainMidCol = if (isLightTheme) Color.White.copy(alpha = peakAlpha * 0.35f) else Color(0xFFEAF2FF).copy(alpha = peakAlpha * 0.52f)
                    val curtainCoreCol = Color.White.copy(alpha = peakAlpha)

                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.0f to curtainStartCol,
                            0.52f to curtainMidCol,
                            0.84f to curtainCoreCol,
                            1.0f to curtainCoreCol,
                            startX = startX,
                            endX = cutX.coerceAtLeast(startX + 1f)
                        ),
                        topLeft = Offset(startX, 0f),
                        size = Size(cutX - startX, h)
                    )

                    // 3. 1.5dp 순백 단면 경계선
                    drawRect(
                        color = Color.White.copy(alpha = (0.98f * effectAlpha).coerceIn(0f, 1f)),
                        topLeft = Offset((cutX - 1.5.dp.toPx()).coerceAtLeast(0f), 0f),
                        size = Size(1.5.dp.toPx(), h)
                    )
                }

                // 4. 양방향 개별 입자 풀 (단면 주변 밀집, 멀어질수록 소멸)
                val blend = if (isLightTheme) BlendMode.SrcOver else BlendMode.Plus
                for (i in 0 until pool.particles.size) {
                    val pt = pool.particles[i]
                    val t = (pt.life / pt.maxLife).coerceIn(0f, 1f)
                    val distNorm = (abs(pt.x - cutX) / pt.maxDist).coerceIn(0f, 1f)
                    val env = sin((t * 4f).coerceAtMost(1f) * (PI.toFloat() * 0.5f)) *
                        (1f - t).pow(1.35f) * (1f - distNorm).pow(1.65f)
                    val tw = 0.45f + 0.55f * sin(pt.life * pt.freq + pt.phase)
                    val a = (env * tw * effectAlpha * (if (isLightTheme) 0.78f else 1.0f)).coerceIn(0f, 1f)
                    if (a < 0.025f) continue

                    if (pt.isBokeh) {
                        val bR = pt.radius * 2.4f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = a * 0.96f),
                                    Color(0xFFDCEEFF).copy(alpha = a * 0.45f),
                                    Color.Transparent
                                ),
                                center = Offset(pt.x, pt.y),
                                radius = bR
                            ),
                            radius = bR,
                            center = Offset(pt.x, pt.y),
                            blendMode = blend
                        )
                    } else {
                        drawCircle(
                            color = Color.White.copy(alpha = a),
                            radius = pt.radius,
                            center = Offset(pt.x, pt.y),
                            blendMode = blend
                        )
                    }
                }

                // 5. 캡슐 내부 1dp 글래스 림 (외부 블룸 0%)
                if (effectAlpha > 0.02f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = (0.48f * effectAlpha).coerceIn(0f, 0.85f)),
                        cornerRadius = CornerRadius(rPx, rPx),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * 방식 B 전용: 0..prog 구간을 검은색(주간은 흰색) 오버레이 레이어로 정확히 클리핑.
 * 진행 중 글자가 다시 흰색으로 바뀌지 않으며, 완료 시(alpha -> 0) 자연스럽게 원래 글자색으로 복귀.
 */
@Composable
fun HarmonyModeBClippedOverlay(
    enabled: Boolean,
    progress: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (!enabled) return
    val clampedProg = progress.coerceIn(0f, 1f)
    val smoothProg by animateFloatAsState(
        targetValue = if (isActive) clampedProg else if (clampedProg >= 0.99f) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "clipProg"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isActive && clampedProg in 0.005f..0.995f) 1f else 0f,
        animationSpec = tween(durationMillis = if (isActive) 120 else 280),
        label = "clipAlpha"
    )
    if (alpha <= 0.01f) return
    Box(
        modifier = modifier
            .drawWithContent {
                val rightX = size.width * smoothProg
                clipRect(left = 0f, top = 0f, right = rightX, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
            },
        content = content
    )
}

/**
 * 19012.mp4 1:1 세로형 밝기·볼륨 캡슐 슬라이더 (영상 플레이어 전체화면 HUD & 설정 미리보기 공용)
 */
@Composable
fun VerticalHarmonyCapsuleSlider(
    value: Float,
    isBrightness: Boolean,
    activeEnergy: Float,
    speed: Float,
    overPullPx: Float,
    lightIntensity: Float = 1.6f,
    particleDensity: Float = 1.4f,
    modifier: Modifier = Modifier
) {
    val pool = remember { OpticalParticlePool(210) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var lastSize by remember { mutableStateOf(Size.Zero) }
    val clampedVal = value.coerceIn(0f, 1f)

    LaunchedEffect(activeEnergy > 0.01f) {
        var lastNs = 0L
        while (activeEnergy > 0.01f || pool.particles.isNotEmpty()) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                val w = lastSize.width
                val h = lastSize.height
                val cutY = h * (1f - clampedVal)
                pool.stepVertical(
                    dt = dt,
                    cutY = cutY,
                    barW = w,
                    barH = h,
                    speed = speed,
                    densityMul = particleDensity * activeEnergy,
                    spawning = activeEnergy > 0.08f && clampedVal in 0.01f..0.992f
                )
                frameTick = ns
            }
        }
    }

    val density = LocalDensity.current
    val baseW = 70.dp
    val baseH = 194.dp
    val stretchH = with(density) { (baseH.toPx() + abs(overPullPx) * 0.55f).toDp() }
    val stretchW = with(density) { (baseW.toPx() - abs(overPullPx) * 0.16f).coerceAtLeast(58.dp.toPx()).toDp() }
    val shiftY = if (overPullPx < 0f) (overPullPx * 0.55f).roundToInt() else 0

    Box(
        modifier = modifier
            .offset { IntOffset(0, shiftY) }
            .width(stretchW)
            .height(stretchH),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            lastSize = size
            @Suppress("UNUSED_EXPRESSION")
            frameTick
            val w = size.width
            val h = size.height
            val rad = w * 0.5f
            val cutY = h * (1f - clampedVal)
            val capsulePath = Path().apply {
                addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(rad, rad)))
            }

            clipPath(capsulePath) {
                // 1. 반투명 다크 프로스트 유리 베이스
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xA61A2030), Color(0xBA121622)),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    )
                )
                val ambientTint = if (isBrightness) Color(0x2482AFFF) else Color(0x24FF8291)
                drawRect(color = ambientTint)

                // 2. 유휴 상태 채움 트랙 (19012.mp4 3.75s)
                val fillH = (h - cutY).coerceAtLeast(0f)
                if (fillH > 0.5f) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.13f + 0.05f * activeEnergy),
                        topLeft = Offset(0f, cutY),
                        size = Size(w, fillH)
                    )
                }

                // 3. 활성 시 19012.mp4 1:1 순백 HDR 빛 커튼 (단면 아래만 강렬, 멀어지면 0%로 소멸)
                if (activeEnergy > 0.01f && clampedVal > 0.005f) {
                    val curtainH = (h * (0.28f + speed.coerceIn(0f, 1f) * 0.16f) * (0.84f + 0.16f * lightIntensity))
                        .coerceIn(24f, h * 0.62f)
                    val peakA = (activeEnergy * (0.94f + 0.06f * lightIntensity)).coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = peakA),
                            0.18f to Color.White.copy(alpha = peakA * 0.94f),
                            0.48f to Color(0xFFE8F2FF).copy(alpha = peakA * 0.42f),
                            0.80f to Color(0xFFD2E4FF).copy(alpha = peakA * 0.10f),
                            1.0f to Color.Transparent,
                            startY = cutY,
                            endY = cutY + curtainH
                        ),
                        topLeft = Offset(0f, cutY),
                        size = Size(w, fillH.coerceAtMost(curtainH))
                    )
                    // 1.5dp 순백 단면선
                    drawRect(
                        color = Color.White.copy(alpha = activeEnergy.coerceIn(0f, 1f)),
                        topLeft = Offset(0f, cutY),
                        size = Size(w, 1.8.dp.toPx())
                    )
                }

                // 4. 위·아래 양방향 개별 입자 풀
                for (i in 0 until pool.particles.size) {
                    val pt = pool.particles[i]
                    val t = (pt.life / pt.maxLife).coerceIn(0f, 1f)
                    val distNorm = (abs(pt.y - cutY) / pt.maxDist).coerceIn(0f, 1f)
                    val env = sin((t * 4.2f).coerceAtMost(1f) * (PI.toFloat() * 0.5f)) *
                        (1f - t).pow(1.3f) * (1f - distNorm).pow(1.55f)
                    val tw = 0.42f + 0.58f * sin(pt.life * pt.freq + pt.phase)
                    val a = (env * tw * activeEnergy).coerceIn(0f, 1f)
                    if (a < 0.025f) continue

                    if (pt.isBokeh) {
                        val bR = pt.radius * 2.5f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = a * 0.96f),
                                    Color(0xFFDCEEFF).copy(alpha = a * 0.45f),
                                    Color.Transparent
                                ),
                                center = Offset(pt.x, pt.y),
                                radius = bR
                            ),
                            radius = bR,
                            center = Offset(pt.x, pt.y),
                            blendMode = BlendMode.Plus
                        )
                    } else {
                        drawCircle(
                            color = Color.White.copy(alpha = a),
                            radius = pt.radius,
                            center = Offset(pt.x, pt.y),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            }

            // 5. 캡슐 내부 1.35dp 글래스 림 (외부 블룸 0%)
            val cutNorm = (cutY / h).coerceIn(0.04f, 0.96f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.26f),
                    (cutNorm - 0.14f).coerceAtLeast(0.01f) to Color.White.copy(alpha = 0.28f + 0.25f * activeEnergy),
                    cutNorm to Color.White.copy(alpha = 0.35f + 0.63f * activeEnergy),
                    (cutNorm + 0.18f).coerceAtMost(0.99f) to Color.White.copy(alpha = 0.28f + 0.35f * activeEnergy),
                    1.0f to Color.White.copy(alpha = 0.28f)
                ),
                cornerRadius = CornerRadius(rad, rad),
                style = Stroke(width = 1.35.dp.toPx())
            )
        }

        // 하단 아이콘 (19012.mp4처럼 빛 커튼 활성 + 채워짐 시 검정, 유휴 시 흰색)
        val icoCovered = clampedVal > 0.18f && activeEnergy > 0.28f
        EgoIcon(
            imageVector = if (isBrightness) Icons.Default.Brightness6 else Icons.Default.VolumeUp,
            contentDescription = if (isBrightness) "밝기" else "볼륨",
            tint = if (icoCovered) Color(0xFF0C1018) else Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .padding(bottom = 18.dp)
                .size(26.dp),
            symbolSize = 26.dp
        )
    }
}

/**
 * 전체화면 동영상 플레이어 좌측(밝기) · 우측(볼륨) 세로 스와이프 HUD 오버레이.
 */
@Composable
fun FullscreenVideoVerticalHudOverlay(
    enabled: Boolean,
    lightIntensity: Float,
    particleDensity: Float,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()

    var showLeftHud by remember { mutableStateOf(false) }
    var showRightHud by remember { mutableStateOf(false) }
    var brightnessVal by remember { mutableFloatStateOf(0.55f) }
    var volumeVal by remember { mutableFloatStateOf(0.5f) }
    var brightDragging by remember { mutableStateOf(false) }
    var volDragging by remember { mutableStateOf(false) }
    var brightSpeed by remember { mutableFloatStateOf(0f) }
    var volSpeed by remember { mutableFloatStateOf(0f) }
    val brightOverPull = remember { Animatable(0f) }
    val volOverPull = remember { Animatable(0f) }
    var hideLeftToken by remember { mutableIntStateOf(0) }
    var hideRightToken by remember { mutableIntStateOf(0) }

    val brightEnergy by animateFloatAsState(
        targetValue = if (brightDragging) 1f else 0f,
        animationSpec = tween(durationMillis = if (brightDragging) 110 else 360),
        label = "brightEnergy"
    )
    val volEnergy by animateFloatAsState(
        targetValue = if (volDragging) 1f else 0f,
        animationSpec = tween(durationMillis = if (volDragging) 110 else 360),
        label = "volEnergy"
    )

    LaunchedEffect(hideLeftToken, brightDragging) {
        if (showLeftHud && !brightDragging) {
            delay(1150L)
            showLeftHud = false
        }
    }
    LaunchedEffect(hideRightToken, volDragging) {
        if (showRightHud && !volDragging) {
            delay(1150L)
            showRightHud = false
        }
    }

    fun readCurrentBrightness(): Float {
        val lpVal = activity?.window?.attributes?.screenBrightness ?: -1f
        if (lpVal in 0f..1f) return lpVal
        val sys = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 140)
        }.getOrDefault(140)
        return (sys / 255f).coerceIn(0.05f, 1f)
    }

    fun applyBrightness(v: Float) {
        val clamped = v.coerceIn(0.02f, 1f)
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = clamped
            win.attributes = lp
        }
    }

    fun readCurrentVolume(): Float {
        val am = audioManager ?: return 0.5f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }

    fun applyVolume(v: Float) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (v.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 좌측 35% 영역: 세로 드래그로 화면 밝기 조절
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.80f)
                .width(120.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            brightnessVal = readCurrentBrightness()
                            brightDragging = true
                            showLeftHud = true
                        },
                        onDragEnd = {
                            brightDragging = false
                            brightSpeed = 0f
                            hideLeftToken++
                            scope.launch { brightOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onDragCancel = {
                            brightDragging = false
                            brightSpeed = 0f
                            hideLeftToken++
                            scope.launch { brightOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / (size.height.coerceAtLeast(240).toFloat() * 0.72f)
                            val raw = brightnessVal + delta
                            brightSpeed = (abs(delta) * 24f).coerceIn(0f, 1.5f)
                            when {
                                raw < 0f -> {
                                    brightnessVal = 0f
                                    scope.launch { brightOverPull.snapTo((brightOverPull.value + dragAmount * 0.25f).coerceIn(0f, 36f)) }
                                }
                                raw > 1f -> {
                                    brightnessVal = 1f
                                    scope.launch { brightOverPull.snapTo((brightOverPull.value + dragAmount * 0.25f).coerceIn(-36f, 0f)) }
                                }
                                else -> {
                                    brightnessVal = raw
                                    scope.launch { brightOverPull.snapTo(0f) }
                                }
                            }
                            applyBrightness(brightnessVal)
                        }
                    )
                }
        )

        // 우측 35% 영역: 세로 드래그로 미디어 볼륨 조절
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.80f)
                .width(120.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            volumeVal = readCurrentVolume()
                            volDragging = true
                            showRightHud = true
                        },
                        onDragEnd = {
                            volDragging = false
                            volSpeed = 0f
                            hideRightToken++
                            scope.launch { volOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onDragCancel = {
                            volDragging = false
                            volSpeed = 0f
                            hideRightToken++
                            scope.launch { volOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / (size.height.coerceAtLeast(240).toFloat() * 0.72f)
                            val raw = volumeVal + delta
                            volSpeed = (abs(delta) * 24f).coerceIn(0f, 1.5f)
                            when {
                                raw < 0f -> {
                                    volumeVal = 0f
                                    scope.launch { volOverPull.snapTo((volOverPull.value + dragAmount * 0.25f).coerceIn(0f, 36f)) }
                                }
                                raw > 1f -> {
                                    volumeVal = 1f
                                    scope.launch { volOverPull.snapTo((volOverPull.value + dragAmount * 0.25f).coerceIn(-36f, 0f)) }
                                }
                                else -> {
                                    volumeVal = raw
                                    scope.launch { volOverPull.snapTo(0f) }
                                }
                            }
                            applyVolume(volumeVal)
                        }
                    )
                }
        )

        // 좌측 세로 밝기 캡슐 HUD
        AnimatedVisibility(
            visible = showLeftHud,
            enter = fadeIn(tween(130)),
            exit = fadeOut(tween(240)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                Text(
                    text = "${(brightnessVal * 100).roundToInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.offset(y = (-24).dp)
                )
                VerticalHarmonyCapsuleSlider(
                    value = brightnessVal,
                    isBrightness = true,
                    activeEnergy = brightEnergy,
                    speed = brightSpeed,
                    overPullPx = brightOverPull.value,
                    lightIntensity = lightIntensity,
                    particleDensity = particleDensity
                )
            }
        }

        // 우측 세로 볼륨 캡슐 HUD
        AnimatedVisibility(
            visible = showRightHud,
            enter = fadeIn(tween(130)),
            exit = fadeOut(tween(240)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                Text(
                    text = "${(volumeVal * 100).roundToInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.offset(y = (-24).dp)
                )
                VerticalHarmonyCapsuleSlider(
                    value = volumeVal,
                    isBrightness = false,
                    activeEnergy = volEnergy,
                    speed = volSpeed,
                    overPullPx = volOverPull.value,
                    lightIntensity = lightIntensity,
                    particleDensity = particleDensity
                )
            }
        }
    }
}
