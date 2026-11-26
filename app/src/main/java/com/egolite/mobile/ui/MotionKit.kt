package com.egolite.mobile.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.egolite.mobile.model.EqualizerMorphStyle
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Ego Lite Mobile 공용 Material 3 Expressive 스프링 & 모프 모션 프리셋
 */
object EgoMotion {
    fun <T> bouncySpring() = spring<T>(
        dampingRatio = 0.74f,
        stiffness = 430f
    )

    fun <T> smoothSpring() = spring<T>(
        dampingRatio = 0.84f,
        stiffness = 440f
    )

    fun <T> snappySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 560f
    )

    val intOffsetSpring = spring<IntOffset>(
        dampingRatio = 0.84f,
        stiffness = 440f
    )

    val intSizeSpring = spring<IntSize>(
        dampingRatio = 0.82f,
        stiffness = 420f
    )

    // 눈에 확실히 보이는 M3 Expressive 컨테이너 모프 스프링 (~340ms)
    fun <T> morphSpring() = spring<T>(
        dampingRatio = 0.80f,
        stiffness = 200f
    )

    val morphIntSizeSpring = spring<IntSize>(
        dampingRatio = 0.82f,
        stiffness = 200f
    )
}

/**
 * 툴바 아이콘 버튼용 Micro-Press 탄성 피드백 (누르면 0.90배 축소, 떼면 탄성 복원)
 * - 기존 clickable / combinedClickable / pointerInput 이벤트를 소비하지 않고 시각 피드백만 제공
 */
fun Modifier.pressBounceScale(pressedScale: Float = 0.90f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = EgoMotion.bouncySpring(),
        label = "pressBounceScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}


/**
 * 자체 제작 미니 이퀄라이저 ↔ 음소거/중단 유기적 모프 아이콘
 * - 재생 중: 3개 막대가 서로 다른 위상으로 부드럽게 웨이브
 * - 중단/음소거: 막대가 탄성 있게 낮은 점(3dp)으로 가라앉으며 대각선 슬래시가 스르륵 그려짐
 * - 재개: 슬래시가 걷히며 3개 막대가 스프링처럼 톡 튀어 올라 다시 파형 시작
 */
@Composable
fun TabEqualizerIcon(
    isPlaying: Boolean,
    isMuted: Boolean,
    activeColor: Color,
    mutedColor: Color,
    cutoutBgColor: Color,
    morphStyle: EqualizerMorphStyle = EqualizerMorphStyle.DOTS_SLASH,
    onClick: () -> Unit
) {
    val activeProgress by animateFloatAsState(
        targetValue = if (isPlaying && !isMuted) 1f else 0f,
        animationSpec = EgoMotion.smoothSpring(),
        label = "eqActiveMorph"
    )
    val slashProgress by animateFloatAsState(
        targetValue = if (isMuted && morphStyle != EqualizerMorphStyle.PAUSE_MORPH) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "eqMuteSlash"
    )
    val barColor by animateColorAsState(
        targetValue = if (isPlaying && !isMuted) activeColor else mutedColor,
        animationSpec = tween(durationMillis = 220),
        label = "eqBarColor"
    )
    // 발열 최적화: 실제 오디오가 재생 중이고 음소거가 아닐 때만 파형 루프 구동
    val isActivelyWaving = isPlaying && !isMuted
    val phase = if (isActivelyWaving) {
        val infinite = rememberInfiniteTransition(label = "eqWaveLoop")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1050, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "eqPhase"
        ).value
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .pressBounceScale(0.84f)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val u = w / 14f
            val barW = 2.4f * u
            val gap = 2.2f * u
            val totalW = barW * 3f + gap * 2f
            val startX = (w - totalW) / 2f
            val minBarH = 2.8f * u
            val maxBarH = h * 0.90f
            val baselineY = h * 0.93f
            val clampedActive = activeProgress.coerceIn(0f, 1f)
            val muteProg = 1f - clampedActive

            val waveRatios = floatArrayOf(
                0.38f + 0.54f * ((sin(phase) + 1f) * 0.5f),
                0.48f + 0.52f * ((sin(phase * 1.25f + 1.4f) + 1f) * 0.5f),
                0.34f + 0.58f * ((sin(phase * 0.9f + 2.8f) + 1f) * 0.5f)
            )

            when (morphStyle) {
                EqualizerMorphStyle.DOTS_SLASH -> {
                    // A안: 3단 바가 3개의 둥근 점으로 탄성 수축 + 대각선 컷아웃 사선
                    val dotCenterY = h * 0.56f + minBarH * 0.5f
                    val yBase = baselineY + (dotCenterY - baselineY) * muteProg * 0.45f
                    for (i in 0 until 3) {
                        val targetH = minBarH + (maxBarH - minBarH) * waveRatios[i] * clampedActive
                        val x = startX + i * (barW + gap)
                        val topY = yBase - targetH
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, topY),
                            size = Size(barW, targetH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }
                EqualizerMorphStyle.FLAT_MERGE -> {
                    // B안: 3단 바가 중앙 수평선으로 납작하게 병합 + 대각선 컷아웃 사선
                    val midY = h * 0.52f
                    val flatH = 2.1f * u
                    for (i in 0 until 3) {
                        val playH = minBarH + (maxBarH - minBarH) * waveRatios[i]
                        val curH = flatH + (playH - flatH) * clampedActive
                        val extraW = gap * 1.15f * muteProg
                        val curW = barW + extraW
                        val x = startX + i * (barW + gap) - extraW / 2f
                        val playTop = baselineY - playH
                        val flatTop = midY - flatH / 2f
                        val topY = flatTop + (playTop - flatTop) * clampedActive
                        val rad = min(curW, curH) / 2f
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, topY),
                            size = Size(curW, curH),
                            cornerRadius = CornerRadius(rad, rad)
                        )
                    }
                }
                EqualizerMorphStyle.PAUSE_MORPH -> {
                    // C안: 가운데 바가 부드럽게 사라지고 양옆 바가 일시정지(II)로 중앙 정렬
                    val pauseH = h * 0.68f
                    val pauseTop = (h - pauseH) / 2f
                    val pauseW = 2.8f * u
                    val leftPauseX = w * 0.5f - pauseW - 1.3f * u
                    val rightPauseX = w * 0.5f + 1.3f * u
                    for (i in 0 until 3) {
                        val playH = minBarH + (maxBarH - minBarH) * waveRatios[i]
                        val playX = startX + i * (barW + gap)
                        val playTop = baselineY - playH
                        if (i == 1) {
                            val alpha = (1f - muteProg * 1.45f).coerceIn(0f, 1f)
                            if (alpha > 0.01f) {
                                val curH = max(1.2f * u, playH * (1f - muteProg * 0.85f))
                                val curW = barW * (1f - muteProg * 0.4f)
                                val topY = playTop + (h * 0.5f - playTop) * muteProg
                                drawRoundRect(
                                    color = barColor.copy(alpha = barColor.alpha * alpha),
                                    topLeft = Offset(playX + (barW - curW) / 2f, topY),
                                    size = Size(curW, curH),
                                    cornerRadius = CornerRadius(curW / 2f, curW / 2f)
                                )
                            }
                        } else {
                            val targetX = if (i == 0) leftPauseX else rightPauseX
                            val curX = playX + (targetX - playX) * muteProg
                            val curW = barW + (pauseW - barW) * muteProg
                            val curH = playH + (pauseH - playH) * muteProg
                            val topY = playTop + (pauseTop - playTop) * muteProg
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(curX, topY),
                                size = Size(curW, curH),
                                cornerRadius = CornerRadius(curW / 2f, curW / 2f)
                            )
                        }
                    }
                }
            }

            if (slashProgress > 0.01f && morphStyle != EqualizerMorphStyle.PAUSE_MORPH) {
                val p0 = Offset(w * 0.12f, h * 0.14f)
                val p1 = Offset(w * 0.88f, h * 0.86f)
                val curEnd = Offset(
                    p0.x + (p1.x - p0.x) * slashProgress,
                    p0.y + (p1.y - p0.y) * slashProgress
                )
                drawLine(
                    color = cutoutBgColor,
                    start = p0,
                    end = curEnd,
                    strokeWidth = 3.2f * u,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = barColor,
                    start = p0,
                    end = curEnd,
                    strokeWidth = 1.6f * u,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
