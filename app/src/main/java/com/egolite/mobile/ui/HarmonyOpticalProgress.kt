package com.egolite.mobile.ui
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.exp
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

/**
 * 점프 없는 부드러운 지수 감속 + 점진적 멈춤(Asymptotic Creep) 진행률 보간기.
 * 4% -> 30% 급격한 점프도 부드럽게 흘러가며 목표치 근처에서 점진적으로 감속 정지.
 */
@Composable
internal fun rememberSmoothOpticalProgress(
    rawProgress: Float,
    isActive: Boolean,
    isCountdown: Boolean = false
): Pair<Float, Float> {
    val target = rawProgress.coerceIn(0f, 1f)
    if (isCountdown) {
        // 카운트다운 모드(되돌리기 배너 등): 우측 끝(1.0)에서 좌측 끝(0.0)까지 1:1 직선 진행!
        val cdAlpha by animateFloatAsState(
            targetValue = if (isActive && target > 0.005f) 1f else 0f,
            animationSpec = tween(durationMillis = if (isActive) 120 else 280),
            label = "countdownOpticalAlpha"
        )
        return target to cdAlpha
    }

    var displayed by remember { mutableFloatStateOf(if (isActive) target.coerceAtMost(0.08f) else 0f) }
    var activeVisual by remember { mutableStateOf(isActive) }
    var holdAtFullMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            activeVisual = true
            holdAtFullMs = 0f
            if (displayed >= 0.98f || target < displayed - 0.25f) {
                displayed = target.coerceAtMost(0.06f)
            }
        }
    }

    LaunchedEffect(isActive, target) {
        var lastNs = 0L
        while (activeVisual || isActive) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                if (isActive) {
                    holdAtFullMs = 0f
                    // 점진적 멈춤 크립 상한: 현재 target보다 약간 앞까지 서서히 차오르며 감속
                    val creepCeil = (target + 0.14f).coerceAtMost(0.92f)
                    val goal = if (target > displayed) target else creepCeil
                    val rate = if (target > displayed) 3.8f else 0.32f
                    val factor = 1f - exp(-dt * rate)
                    displayed = (displayed + (goal - displayed) * factor).coerceIn(0f, 0.99f)
                } else {
                    // 완료 시 100% 끝까지 부드럽게 도달 -> 100ms 인터벌 유지 -> 부드럽게 페이드아웃!
                    if (target >= 0.95f && displayed < 0.995f) {
                        val factor = 1f - exp(-dt * 8.5f)
                        displayed = (displayed + (1f - displayed) * factor).coerceAtMost(1f)
                    } else if (target >= 0.95f && holdAtFullMs < 100f) {
                        displayed = 1f
                        holdAtFullMs += dt * 1000f
                    } else {
                        displayed = if (target >= 0.95f) 1f else 0f
                        activeVisual = false
                    }
                }
            }
        }
    }

    val effectAlpha by animateFloatAsState(
        targetValue = if (isActive || activeVisual) 1f else 0f,
        animationSpec = tween(durationMillis = if (isActive) 150 else 360),
        label = "smoothOpticalAlpha"
    )
    return displayed to effectAlpha
}

@Composable
fun HarmonyHorizontalProgressCanvas(
    style: ProgressBarStyle,
    progress: Float,
    isActive: Boolean,
    accentColor: Color,
    lightIntensity: Float = 1.6f,
    particleDensity: Float = 1.4f,
    cornerRadiusDp: Dp = 6.dp,
    darkUseThemeColor: Boolean = false,
    isCountdown: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (style == ProgressBarStyle.MATERIAL) return

    val isLightTheme = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val (smoothProg, effectAlpha) = rememberSmoothOpticalProgress(
        rawProgress = progress,
        isActive = isActive,
        isCountdown = isCountdown
    )

    val pool = remember { OpticalParticlePool(200) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var prevProg by remember { mutableFloatStateOf(smoothProg) }
    var smoothSpeed by remember { mutableFloatStateOf(0f) }
    var lastSize by remember { mutableStateOf(Size.Zero) }

    val latestSmoothProg by rememberUpdatedState(smoothProg)
    val latestEffectAlpha by rememberUpdatedState(effectAlpha)
    val latestParticleDensity by rememberUpdatedState(particleDensity)

    LaunchedEffect(style, effectAlpha > 0.01f) {
        if (style != ProgressBarStyle.HARMONY_OPTICAL) {
            pool.clear()
            return@LaunchedEffect
        }
        var lastNs = 0L
        while (latestEffectAlpha > 0.01f || pool.particles.isNotEmpty()) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                val curProg = latestSmoothProg
                val curAlpha = latestEffectAlpha
                val rawSpd = (abs(curProg - prevProg) / dt * 1.35f).coerceAtMost(1.5f)
                prevProg = curProg
                smoothSpeed += (rawSpd - smoothSpeed) * (dt * 10f).coerceAtMost(1f)
                val w = lastSize.width
                val h = lastSize.height
                val cutX = w * curProg
                pool.stepHorizontal(
                    dt = dt,
                    cutX = cutX,
                    barW = w,
                    barH = h,
                    speed = smoothSpeed,
                    densityMul = latestParticleDensity,
                    spawning = curAlpha > 0.08f && curProg in 0.01f..0.992f
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
                    val blColor = if (isLightTheme || darkUseThemeColor) accentColor else Color(0xFFEAF2FF)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                blColor.copy(alpha = (0.34f * lightIntensity * effectAlpha).coerceIn(0f, 0.65f))
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
                                blColor.copy(alpha = 0.45f * effectAlpha),
                                blColor.copy(alpha = 0.92f * effectAlpha),
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
                                blColor.copy(alpha = (0.55f * effectAlpha).coerceIn(0f, 1f)),
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
                    val silverBase = if (isLightTheme || darkUseThemeColor) {
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
                    val curtainStartCol = if (isLightTheme || darkUseThemeColor) accentColor.copy(alpha = 0f) else Color.White.copy(alpha = 0f)
                    val curtainMidCol = if (isLightTheme || darkUseThemeColor) Color.White.copy(alpha = peakAlpha * 0.42f) else Color(0xFFEAF2FF).copy(alpha = peakAlpha * 0.52f)
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
    isCountdown: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (!enabled) return
    val (smoothProg, alpha) = rememberSmoothOpticalProgress(
        rawProgress = progress,
        isActive = isActive,
        isCountdown = isCountdown
    )
    if (alpha <= 0.01f) return
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
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

    val latestEnergy by rememberUpdatedState(activeEnergy)
    val latestVal by rememberUpdatedState(clampedVal)
    val latestSpeed by rememberUpdatedState(speed)
    val latestDensity by rememberUpdatedState(particleDensity)

    LaunchedEffect(activeEnergy > 0.01f) {
        var lastNs = 0L
        while (latestEnergy > 0.01f || pool.particles.isNotEmpty()) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                val w = lastSize.width
                val h = lastSize.height
                val eNow = latestEnergy
                val vNow = latestVal
                val cutY = h * (1f - vNow)
                pool.stepVertical(
                    dt = dt,
                    cutY = cutY,
                    barW = w,
                    barH = h,
                    speed = latestSpeed,
                    densityMul = (latestDensity * eNow).coerceAtLeast(if (eNow > 0.08f) 0.65f else 0f),
                    spawning = eNow > 0.08f && vNow in 0.01f..0.992f
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

                // 2. 비터치 시 19108 스타일 5.5dp 내장형 반투명 바 -> 드래그 시 0dp 풀폭으로 자연 확장
                val insetPx = (5.5.dp.toPx() * (1f - activeEnergy)).coerceAtLeast(0f)
                val iw = (w - insetPx * 2f).coerceAtLeast(4f)
                val ih = (h - insetPx * 2f).coerceAtLeast(4f)
                val ir = (rad - insetPx).coerceAtLeast(2f)
                val innerPath = Path().apply {
                    addRoundRect(RoundRect(Rect(insetPx, insetPx, insetPx + iw, insetPx + ih), CornerRadius(ir, ir)))
                }
                val innerFillH = (ih * clampedVal).coerceAtLeast(0f)
                val innerCutY = insetPx + ih - innerFillH
                if (innerFillH > 0.5f) {
                    clipPath(innerPath) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFDAE4F6).copy(alpha = 0.18f + 0.34f * activeEnergy),
                                    Color(0xFFB0C0DA).copy(alpha = 0.13f + 0.22f * activeEnergy)
                                ),
                                startY = innerCutY,
                                endY = insetPx + ih
                            ),
                            topLeft = Offset(insetPx, innerCutY),
                            size = Size(iw, innerFillH)
                        )
                        if (activeEnergy < 0.35f) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.16f * (1f - activeEnergy)),
                                topLeft = Offset(insetPx, innerCutY),
                                size = Size(iw, 1.dp.toPx())
                            )
                        }
                    }
                }
                val fillH = (h - cutY).coerceAtLeast(0f)

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
    syncSystemBrightness: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()

    var hudOpen by remember { mutableStateOf(false) }
    var hudOnLeft by remember { mutableStateOf(true) }
    var activeBar by remember { mutableStateOf<String?>(null) } // "bright" | "vol" | null
    var brightnessVal by remember { mutableFloatStateOf(0.58f) }
    var volumeVal by remember { mutableFloatStateOf(0.52f) }
    var brightSpeed by remember { mutableFloatStateOf(0f) }
    var volSpeed by remember { mutableFloatStateOf(0f) }
    val brightOverPull = remember { Animatable(0f) }
    val volOverPull = remember { Animatable(0f) }
    var hideToken by remember { mutableIntStateOf(0) }

    // Apple Liquid Glass 단계별 스프링 모핑 상태 (베젤 앵커 -> 메인 물방울 이탈 -> 짝꿍 캡슐 분리)
    val edgeBulge = remember { Animatable(0f) }
    val primDetach = remember { Animatable(0f) }
    val primInflate = remember { Animatable(0f) }
    val compSep = remember { Animatable(0f) }
    val compInflate = remember { Animatable(0f) }

    LaunchedEffect(hudOpen, hudOnLeft) {
        if (hudOpen) {
            launch {
                edgeBulge.snapTo(0f)
                edgeBulge.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 340f))
                delay(150L)
                edgeBulge.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 280f))
            }
            launch {
                delay(15L)
                primDetach.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 230f))
            }
            launch {
                delay(95L)
                primInflate.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 240f))
            }
            launch {
                delay(165L)
                compSep.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 210f))
            }
            launch {
                delay(195L)
                compInflate.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 225f))
            }
        } else {
            launch { compInflate.animateTo(0f, tween(210)) }
            launch { compSep.animateTo(0f, tween(240)) }
            launch { primInflate.animateTo(0f, tween(240)) }
            launch {
                delay(40L)
                primDetach.animateTo(0f, tween(280))
            }
            launch {
                delay(70L)
                edgeBulge.animateTo(0.75f, tween(110))
                edgeBulge.animateTo(0f, tween(170))
            }
        }
    }

    val brightEnergy by animateFloatAsState(
        targetValue = if (activeBar == "bright") 1f else 0f,
        animationSpec = tween(durationMillis = if (activeBar == "bright") 110 else 360),
        label = "dualBrightEnergy"
    )
    val volEnergy by animateFloatAsState(
        targetValue = if (activeBar == "vol") 1f else 0f,
        animationSpec = tween(durationMillis = if (activeBar == "vol") 110 else 360),
        label = "dualVolEnergy"
    )

    LaunchedEffect(hideToken, activeBar, hudOpen) {
        if (hudOpen && activeBar == null) {
            delay(2300L)
            hudOpen = false
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

    fun resetWindowBrightnessOverride() {
        activity?.window?.let { win ->
            val lp = win.attributes
            if (lp.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }
        }
    }

    // 전체화면 종료 시 앱 창 밝기 오버라이드 자동 복원 + 상태창 퀵패널 밝기 변경 실시간 감지
    DisposableEffect(activity, syncSystemBrightness) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (activeBar != "bright") {
                    resetWindowBrightnessOverride()
                    brightnessVal = readCurrentBrightness()
                }
            }
        }
        runCatching {
            resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false,
                observer
            )
        }
        onDispose {
            runCatching { resolver.unregisterContentObserver(observer) }
            resetWindowBrightnessOverride()
        }
    }

    fun applyBrightness(v: Float) {
        val clamped = v.coerceIn(0.02f, 1f)
        val canWriteSys = syncSystemBrightness && runCatching { Settings.System.canWrite(context) }.getOrDefault(false)
        if (canWriteSys) {
            runCatching {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (clamped * 255f).roundToInt().coerceIn(4, 255)
                )
            }
            resetWindowBrightnessOverride()
        } else {
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = clamped
                win.attributes = lp
            }
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

    fun handleBarDrag(kind: String, dragAmount: Float, travelPx: Float) {
        val delta = -dragAmount / travelPx.coerceAtLeast(220f)
        val spd = (abs(delta) * 24f).coerceIn(0f, 1.5f)
        if (kind == "bright") {
            brightSpeed = spd
            val raw = brightnessVal + delta
            when {
                raw < 0f -> {
                    brightnessVal = 0f
                    scope.launch { brightOverPull.snapTo((brightOverPull.value + dragAmount * 0.25f).coerceIn(0f, 32f)) }
                }
                raw > 1f -> {
                    brightnessVal = 1f
                    scope.launch { brightOverPull.snapTo((brightOverPull.value + dragAmount * 0.25f).coerceIn(-32f, 0f)) }
                }
                else -> {
                    brightnessVal = raw
                    scope.launch { brightOverPull.snapTo(0f) }
                }
            }
            applyBrightness(brightnessVal)
        } else {
            volSpeed = spd
            val raw = volumeVal + delta
            when {
                raw < 0f -> {
                    volumeVal = 0f
                    scope.launch { volOverPull.snapTo((volOverPull.value + dragAmount * 0.25f).coerceIn(0f, 32f)) }
                }
                raw > 1f -> {
                    volumeVal = 1f
                    scope.launch { volOverPull.snapTo((volOverPull.value + dragAmount * 0.25f).coerceIn(-32f, 0f)) }
                }
                else -> {
                    volumeVal = raw
                    scope.launch { volOverPull.snapTo(0f) }
                }
            }
            applyVolume(volumeVal)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalW = with(density) { maxWidth.toPx() }
        val totalH = with(density) { maxHeight.toPx() }
        val capW = with(density) { 70.dp.toPx() }
        val capH = with(density) { 194.dp.toPx() }
        val capGap = with(density) { 22.dp.toPx() }
        val edgeMargin = with(density) { 30.dp.toPx() }

        // 좌측 세로 스와이프 제스처 영역 -> 좌측에서 밝기(활성) + 볼륨(짝꿍) 함께 물방울 모핑 등장
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.82f)
                .width(120.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            brightnessVal = readCurrentBrightness()
                            volumeVal = readCurrentVolume()
                            if (!hudOpen || !hudOnLeft) {
                                hudOnLeft = true
                                hudOpen = true
                            }
                            activeBar = "bright"
                        },
                        onDragEnd = {
                            activeBar = null
                            brightSpeed = 0f
                            hideToken++
                            scope.launch { brightOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onDragCancel = {
                            activeBar = null
                            brightSpeed = 0f
                            hideToken++
                            scope.launch { brightOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            handleBarDrag("bright", dragAmount, size.height * 0.72f)
                        }
                    )
                }
        )

        // 우측 세로 스와이프 제스처 영역 -> 우측에서 볼륨(활성) + 밝기(짝꿍) 함께 물방울 모핑 등장
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.82f)
                .width(120.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            brightnessVal = readCurrentBrightness()
                            volumeVal = readCurrentVolume()
                            if (!hudOpen || hudOnLeft) {
                                hudOnLeft = false
                                hudOpen = true
                            }
                            activeBar = "vol"
                        },
                        onDragEnd = {
                            activeBar = null
                            volSpeed = 0f
                            hideToken++
                            scope.launch { volOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onDragCancel = {
                            activeBar = null
                            volSpeed = 0f
                            hideToken++
                            scope.launch { volOverPull.animateTo(0f, EgoMotion.responsiveSpring()) }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            handleBarDrag("vol", dragAmount, size.height * 0.72f)
                        }
                    )
                }
        )

        val pDet = primDetach.value
        val pInf = primInflate.value
        val cSep = compSep.value
        val cInf = compInflate.value
        val eBlg = edgeBulge.value

        if (pDet > 0.01f || eBlg > 0.01f) {
            val cy = totalH * 0.5f
            val primFinalCx = if (hudOnLeft) (edgeMargin + capW * 0.5f) else (totalW - edgeMargin - capW * 0.5f)
            val compFinalCx = if (hudOnLeft) (primFinalCx + capW + capGap) else (primFinalCx - capW - capGap)
            val startCx = if (hudOnLeft) -16f else (totalW + 16f)

            val pCx = startCx + (primFinalCx - startCx) * pDet
            val pW = (34f + (capW - 34f) * pInf.coerceIn(0f, 1f)) * (pDet * 3f).coerceIn(0f, 1f)
            val pH = (44f + (capH - 44f) * pInf.coerceAtLeast(0f)) * (pDet * 3f).coerceIn(0f, 1f)

            val cCx = pCx + (compFinalCx - pCx) * cSep
            val cVis = (cSep * 2.5f).coerceIn(0f, 1f)
            val cW = (34f + (capW - 34f) * cInf.coerceIn(0f, 1f)) * cVis
            val cH = (44f + (capH - 44f) * cInf.coerceAtLeast(0f)) * cVis

            // 분리 완료 시(cSep > 0.68) 메타볼 블러를 0으로 넘겨 두 캡슐이 물갈퀴 없이 칼같이 독립 마감!
            val crispAlpha = ((cSep - 0.68f) / 0.26f).coerceIn(0f, 1f)
            val metaAlpha = (1f - crispAlpha).coerceIn(0f, 1f)

            // 1) Android 13+ 실제 메타볼(Blur + Alpha Threshold ColorMatrix) 표면장력 융합/분리 레이어
            if (metaAlpha > 0.02f) {
                val metaballEffect = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blur = AndroidRenderEffect.createBlurEffect(22f, 22f, Shader.TileMode.DECAL)
                        val cm = ColorMatrix(
                            floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,
                                0f, 1f, 0f, 0f, 0f,
                                0f, 0f, 1f, 0f, 0f,
                                0f, 0f, 0f, 26f, -11.5f * 255f
                            )
                        )
                        val threshold = AndroidRenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm), blur)
                        threshold.asComposeRenderEffect()
                    } else null
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = metaAlpha * 0.72f
                            if (metaballEffect != null) renderEffect = metaballEffect
                        }
                ) {
                    val eW = (28.dp.toPx() * eBlg).coerceAtLeast(0f)
                    val eH = (96.dp.toPx() * eBlg).coerceAtLeast(0f)
                    val eX = if (hudOnLeft) -12f else (size.width - eW + 12f)
                    val metaCol = Color(0xFF28354A)

                    if (eW > 1f && eH > 1f) {
                        drawRoundRect(
                            color = metaCol,
                            topLeft = Offset(eX, cy - eH * 0.5f),
                            size = Size(eW, eH),
                            cornerRadius = CornerRadius(eW * 0.5f, eW * 0.5f)
                        )
                    }
                    if (pW > 4f && pH > 4f) {
                        drawRoundRect(
                            color = metaCol,
                            topLeft = Offset(pCx - pW * 0.5f, cy - pH * 0.5f),
                            size = Size(pW, pH),
                            cornerRadius = CornerRadius(pW * 0.5f, pW * 0.5f)
                        )
                    }
                    if (cW > 4f && cH > 4f) {
                        drawRoundRect(
                            color = metaCol,
                            topLeft = Offset(cCx - cW * 0.5f, cy - cH * 0.5f),
                            size = Size(cW, cH),
                            cornerRadius = CornerRadius(cW * 0.5f, cW * 0.5f)
                        )
                    }
                }
            }

            // 2) 독립 마감된 밝기·볼륨 듀얼 캡슐 (손을 떼고 짝꿍 캡슐을 바로 터치·드래그 가능!)
            val primaryIsBright = hudOnLeft
            val primScaleX = (pW / capW).coerceIn(0.35f, 1.15f)
            val primScaleY = (pH / capH).coerceIn(0.25f, 1.15f)
            val compScaleX = (cW / capW).coerceIn(0.35f, 1.15f)
            val compScaleY = (cH / capH).coerceIn(0.25f, 1.15f)

            val primReveal = ((pInf - 0.16f) / 0.58f).coerceIn(0f, 1f)
            val compReveal = ((cInf - 0.18f) / 0.58f).coerceIn(0f, 1f)

            // 메인 캡슐
            if (primReveal > 0.02f) {
                val pKind = if (primaryIsBright) "bright" else "vol"
                Box(
                    modifier = Modifier
                        .offset { IntOffset((pCx - capW * 0.5f).roundToInt(), (cy - capH * 0.5f).roundToInt()) }
                        .graphicsLayer {
                            scaleX = primScaleX
                            scaleY = primScaleY
                            alpha = primReveal
                        }
                        .pointerInput(pKind) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    activeBar = pKind
                                    hudOpen = true
                                },
                                onDragEnd = {
                                    activeBar = null
                                    brightSpeed = 0f; volSpeed = 0f
                                    hideToken++
                                    scope.launch {
                                        brightOverPull.animateTo(0f, EgoMotion.responsiveSpring())
                                        volOverPull.animateTo(0f, EgoMotion.responsiveSpring())
                                    }
                                },
                                onDragCancel = {
                                    activeBar = null
                                    brightSpeed = 0f; volSpeed = 0f
                                    hideToken++
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    handleBarDrag(pKind, dragAmount, capH * 1.15f)
                                }
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    val pctVal = if (primaryIsBright) brightnessVal else volumeVal
                    Text(
                        text = "${(pctVal * 100).roundToInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f * crispAlpha),
                        modifier = Modifier.offset(y = (-24).dp)
                    )
                    VerticalHarmonyCapsuleSlider(
                        value = pctVal,
                        isBrightness = primaryIsBright,
                        activeEnergy = if (primaryIsBright) brightEnergy else volEnergy,
                        speed = if (primaryIsBright) brightSpeed else volSpeed,
                        overPullPx = if (primaryIsBright) brightOverPull.value else volOverPull.value,
                        lightIntensity = lightIntensity,
                        particleDensity = particleDensity
                    )
                }
            }

            // 짝꿍 캡슐 (손 떼고 바로 터치·드래그로 조절 가능!)
            if (compReveal > 0.02f) {
                val cKind = if (primaryIsBright) "vol" else "bright"
                Box(
                    modifier = Modifier
                        .offset { IntOffset((cCx - capW * 0.5f).roundToInt(), (cy - capH * 0.5f).roundToInt()) }
                        .graphicsLayer {
                            scaleX = compScaleX
                            scaleY = compScaleY
                            alpha = compReveal
                        }
                        .pointerInput(cKind) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    activeBar = cKind
                                    hudOpen = true
                                },
                                onDragEnd = {
                                    activeBar = null
                                    brightSpeed = 0f; volSpeed = 0f
                                    hideToken++
                                    scope.launch {
                                        brightOverPull.animateTo(0f, EgoMotion.responsiveSpring())
                                        volOverPull.animateTo(0f, EgoMotion.responsiveSpring())
                                    }
                                },
                                onDragCancel = {
                                    activeBar = null
                                    brightSpeed = 0f; volSpeed = 0f
                                    hideToken++
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    handleBarDrag(cKind, dragAmount, capH * 1.15f)
                                }
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    val pctVal = if (primaryIsBright) volumeVal else brightnessVal
                    Text(
                        text = "${(pctVal * 100).roundToInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f * crispAlpha),
                        modifier = Modifier.offset(y = (-24).dp)
                    )
                    VerticalHarmonyCapsuleSlider(
                        value = pctVal,
                        isBrightness = !primaryIsBright,
                        activeEnergy = if (primaryIsBright) volEnergy else brightEnergy,
                        speed = if (primaryIsBright) volSpeed else brightSpeed,
                        overPullPx = if (primaryIsBright) volOverPull.value else brightOverPull.value,
                        lightIntensity = lightIntensity,
                        particleDensity = particleDensity
                    )
                }
            }
        }
    }
}


/**
 * 하모니OS 7 가로형 광감 캡슐 슬라이더 (빠른메뉴 페이지 확대바용)
 * - 미터치(19278): 5.5dp 네스티드 인셋 차분한 반투명 글래스 채움 (광감 0%, 입자 0%)
 * - 터치/드래그(19279): 실버/테마색 HDR 순백 단면 빛 커튼 + 양방향 개별 입자 풀 점등
 * - 좌측 끝(0%) ~ 우측 끝(100%) 풀 레인지 + 더블탭 100% 초기화
 */
@Composable
fun HorizontalHarmonyCapsuleSlider(
    valueFraction: Float,
    valueLabel: String = "",
    icon: ImageVector,
    accentColor: Color,
    lightIntensity: Float = 1.8f,
    particleDensity: Float = 1.6f,
    darkUseThemeColor: Boolean = false,
    dayParticleColor: Color? = null,
    interactive: Boolean = true,
    iconCenterOffsetDp: Dp = 42.dp,
    onValueChange: (Float) -> Unit,
    onDoubleTapReset: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pool = remember { OpticalParticlePool(180) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(valueFraction.coerceIn(0f, 1f)) }
    var prevFrac by remember { mutableFloatStateOf(valueFraction.coerceIn(0f, 1f)) }
    var smoothSpeed by remember { mutableFloatStateOf(0f) }
    var lastSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(valueFraction, isDragging) {
        if (!isDragging) {
            dragFrac = valueFraction.coerceIn(0f, 1f)
        }
    }

    val activeEnergy by animateFloatAsState(
        targetValue = if (isDragging && interactive) 1f else 0f,
        animationSpec = tween(durationMillis = if (isDragging && interactive) 140 else 360),
        label = "horizHarmonyEnergy"
    )
    val latestEnergy by rememberUpdatedState(activeEnergy)
    val latestFrac by rememberUpdatedState(if (isDragging) dragFrac else valueFraction.coerceIn(0f, 1f))
    val latestDensity by rememberUpdatedState(particleDensity)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnReset by rememberUpdatedState(onDoubleTapReset)

    LaunchedEffect(activeEnergy > 0.01f) {
        var lastNs = 0L
        while (latestEnergy > 0.01f || pool.particles.isNotEmpty()) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                val fNow = latestFrac
                val eNow = latestEnergy
                val rawSpd = (abs(fNow - prevFrac) / dt * 1.4f).coerceAtMost(1.8f)
                prevFrac = fNow
                smoothSpeed += (rawSpd - smoothSpeed) * (dt * 10f).coerceAtMost(1f)
                val w = lastSize.width
                val h = lastSize.height
                val cutX = w * fNow
                pool.stepHorizontal(
                    dt = dt,
                    cutX = cutX,
                    barW = w,
                    barH = h,
                    speed = smoothSpeed,
                    densityMul = (latestDensity * eNow).coerceAtLeast(if (eNow > 0.08f) 0.65f else 0f),
                    spawning = eNow > 0.08f && fNow in 0.005f..0.995f
                )
                frameTick = ns
            }
        }
    }

    val isLightTheme = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val m3TrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val clamped = latestFrac.coerceIn(0f, 1f)
    val leftIconCovered = activeEnergy > 0.32f && clamped > 0.14f
    val resolvedDayParticle = dayParticleColor ?: accentColor

    val gestureMod = if (interactive) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { latestOnReset() }
                )
            }
            .pointerInput(Unit) {
                // 상대 드래그 방식: 어느 지점을 터치해도 값이 튀지 않고 현재 값에서 드래그량만큼 부드럽게 가감
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragFrac = latestFrac
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (size.width > 0) {
                            dragFrac = (dragFrac + dragAmount / size.width.toFloat()).coerceIn(0f, 1f)
                            latestOnChange(dragFrac)
                        }
                    }
                )
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(35.dp))
            .then(gestureMod)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            lastSize = size
            @Suppress("UNUSED_EXPRESSION")
            frameTick

            val w = size.width
            val h = size.height
            val rPx = h * 0.5f
            val e = activeEnergy.coerceIn(0f, 1f)
            val clipShape = Path().apply {
                addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(rPx, rPx)))
            }

            clipPath(clipShape) {
                // 1) M3 버튼 톤과 동일한 캡슐 트랙 배경
                drawRect(color = m3TrackColor)

                // 2) 미터치 상태(90도 회전 하모니 캡슐): 5.5dp 동심 인셋 + 좌측 둥근 끝 + 우측 직선 절단면
                val insetPx = 5.5.dp.toPx()
                val innerH = (h - insetPx * 2f).coerceAtLeast(1f)
                val innerR = innerH * 0.5f
                val cutX = (w * clamped).coerceIn(0f, w)
                val fillW = (cutX - insetPx).coerceAtLeast(0f)
                val calmAlpha = (1f - e * 0.92f).coerceIn(0f, 1f)
                if (fillW > 1f && calmAlpha > 0.01f) {
                    val flatRightPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(insetPx, insetPx, insetPx + fillW, insetPx + innerH),
                                topLeft = CornerRadius(innerR, innerR),
                                bottomLeft = CornerRadius(innerR, innerR),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius.Zero
                            )
                        )
                    }
                    drawPath(
                        path = flatRightPath,
                        brush = Brush.horizontalGradient(
                            0.0f to accentColor.copy(alpha = 0.16f * calmAlpha),
                            1.0f to accentColor.copy(alpha = 0.30f * calmAlpha),
                            startX = insetPx,
                            endX = (insetPx + fillW).coerceAtLeast(insetPx + 1f)
                        )
                    )
                    val edgeW = 1.5.dp.toPx()
                    drawRect(
                        color = accentColor.copy(alpha = (if (isLightTheme) 0.68f else 0.62f) * calmAlpha),
                        topLeft = Offset((insetPx + fillW - edgeW).coerceAtLeast(insetPx), insetPx),
                        size = Size(edgeW, innerH)
                    )
                }

                // 3) 터치/드래그 상태(140ms 점등 -> 360ms 잔광 복귀): 전체 높이 M3 톤 + HDR 광감 커튼 + 1.5dp 경계선
                if (e > 0.01f && cutX > 1f) {
                    val baseFill = if (isLightTheme || darkUseThemeColor) {
                        accentColor.copy(alpha = 0.86f * e)
                    } else {
                        accentColor.copy(alpha = 0.68f * e)
                    }
                    drawRect(color = baseFill, topLeft = Offset.Zero, size = Size(cutX, h))

                    val curtainW = (w * (0.28f + smoothSpeed.coerceIn(0f, 1f) * 0.14f) * (0.82f + 0.18f * lightIntensity))
                        .coerceIn(22f, w * 0.52f)
                    val startX = (cutX - curtainW).coerceAtLeast(0f)
                    val peakAlpha = (0.96f * e * (0.82f + 0.18f * lightIntensity)).coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.0f to Color.Transparent,
                            0.52f to Color(0xFFEAF2FF).copy(alpha = peakAlpha * (if (isLightTheme) 0.42f else 0.54f)),
                            0.85f to Color.White.copy(alpha = peakAlpha * (if (isLightTheme) 0.84f else 0.96f)),
                            1.0f to Color.White.copy(alpha = peakAlpha),
                            startX = startX,
                            endX = cutX.coerceAtLeast(startX + 1f)
                        ),
                        topLeft = Offset(startX, 0f),
                        size = Size(cutX - startX, h)
                    )
                    val cutEdgeW = 1.5.dp.toPx()
                    drawRect(
                        color = Color.White.copy(alpha = (0.98f * e).coerceIn(0f, 1f)),
                        topLeft = Offset((cutX - cutEdgeW).coerceAtLeast(0f), 0f),
                        size = Size(cutEdgeW, h)
                    )
                }

                // 4) 양방향 개별 입자 풀 (야간 순백 가산발광 / 주간 파티클 색감 SrcOver)
                val blend = if (isLightTheme) BlendMode.SrcOver else BlendMode.Plus
                val pColor = if (isLightTheme) resolvedDayParticle else Color.White
                for (i in 0 until pool.particles.size) {
                    val pt = pool.particles[i]
                    val t = (pt.life / pt.maxLife).coerceIn(0f, 1f)
                    val distNorm = (abs(pt.x - cutX) / pt.maxDist).coerceIn(0f, 1f)
                    val env = sin((t * 4f).coerceAtMost(1f) * (PI.toFloat() * 0.5f)) *
                        (1f - t).pow(1.35f) * (1f - distNorm).pow(1.65f)
                    val tw = 0.45f + 0.55f * sin(pt.life * pt.freq + pt.phase)
                    val a = (env * tw * (0.35f + 0.65f * e)).coerceIn(0f, 1f)
                    if (a < 0.025f) continue
                    drawCircle(
                        color = pColor.copy(alpha = a),
                        radius = pt.radius,
                        center = Offset(pt.x, pt.y),
                        blendMode = blend
                    )
                }

                // 5) 평상시(미터치 e <= 0.02f)에는 일반 버튼처럼 정적 테두리선 제거; 터치/광감 시에만 발광!
                if (e > 0.02f) {
                    val rimColor = if (isLightTheme) {
                        accentColor.copy(alpha = (0.48f * e).coerceIn(0f, 0.78f))
                    } else {
                        Color.White.copy(alpha = (0.68f * e).coerceIn(0f, 0.82f))
                    }
                    drawRoundRect(
                        color = rimColor,
                        cornerRadius = CornerRadius(rPx, rPx),
                        style = Stroke(width = 1.35.dp.toPx())
                    )
                }
            }
        }

        // 좌측 확대경 아이콘 (1열 중심 수직축 0dp 정밀 정렬) + 우측 퍼센티지 표시
        val iconStartPad = (iconCenterOffsetDp - 11.dp).coerceAtLeast(12.dp)
        val iconTint = when {
            leftIconCovered && isLightTheme -> Color.White
            leftIconCovered && !isLightTheme -> Color(0xFF090D16)
            else -> MaterialTheme.colorScheme.onSurface
        }
        val rightLabelCovered = activeEnergy > 0.32f && clamped > 0.82f
        val rightLabelTint = when {
            rightLabelCovered && isLightTheme -> Color.White
            rightLabelCovered && !isLightTheme -> Color(0xFF090D16)
            else -> MaterialTheme.colorScheme.onSurface
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = iconStartPad, end = 18.dp)
        ) {
            EgoIcon(
                imageVector = icon,
                contentDescription = "페이지 확대",
                tint = iconTint,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(22.dp),
                symbolSize = 22.dp
            )
            if (valueLabel.isNotBlank()) {
                Text(
                    text = valueLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = rightLabelTint,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}
