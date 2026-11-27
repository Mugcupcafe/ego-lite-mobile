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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
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
    // 인터럽트 반응형 스프링: 방향 전환 시 현재 속도를 그대로 이어받아 매끄럽게 수렴
    fun <T> responsiveSpring() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = 380f
    )

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

/**
 * 하모니OS 7 터치 광학 테두리 발광 (Dynamic Rim Light)
 * - 손가락이 닿은 좌표 주변 테두리와 표면에만 은은한 빛이 번졌다 잔광과 함께 소멸
 * - 기존 clickable/drag 이벤트를 소비하지 않고 시각 효과만 중첩
 */
val LocalRimGlowIntensity = compositionLocalOf { 1.35f }

fun Modifier.harmonyRimGlow(
    enabled: Boolean = true,
    cornerRadius: Dp = 18.dp,
    glowColor: Color = Color(0xFFBBD6FF),
    strokeWidth: Dp = 1.6.dp,
    shape: Shape? = null,
    intensity: Float? = null
): Modifier = composed {
    if (!enabled) return@composed this
    val effectiveIntensity = (intensity ?: LocalRimGlowIntensity.current).coerceIn(0.4f, 2.6f)
    var touchPos by remember { mutableStateOf(Offset.Zero) }
    var isTouching by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = tween(durationMillis = if (isTouching) 75 else 380),
        label = "harmonyRimGlowAlpha"
    )
    val resolvedShape = shape ?: RoundedCornerShape(cornerRadius)
    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchPos = down.position
                    isTouching = true
                    while (true) {
                        val event = awaitPointerEvent()
                        val cur = event.changes.firstOrNull { it.id == down.id }
                        if (cur == null || !cur.pressed) break
                        touchPos = cur.position
                    }
                    isTouching = false
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (glowAlpha > 0.01f) {
                val radiusPx = (size.maxDimension * 0.56f).coerceIn(130f, 380f)
                val outline = resolvedShape.createOutline(size, layoutDirection, this)
                val surfCore = (0.16f * effectiveIntensity * glowAlpha).coerceAtMost(0.42f)
                val surfMid = (0.24f * effectiveIntensity * glowAlpha).coerceAtMost(0.55f)
                // 1) 광수지동(光随指动): 정확한 컴포넌트 윤곽 내부 표면 물리 광장
                drawOutline(
                    outline = outline,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = surfCore),
                            glowColor.copy(alpha = surfMid),
                            Color.Transparent
                        ),
                        center = touchPos,
                        radius = radiusPx
                    ),
                    style = Fill
                )
                // 2) 광선구륵(光线勾勒): 비대칭 22dp/6dp 코너까지 픽셀 정밀도로 따라가는 테두리 스페큘러 윤곽선
                val rimPeak = (0.94f * effectiveIntensity * glowAlpha).coerceAtMost(1.0f)
                val rimMid = (0.58f * effectiveIntensity * glowAlpha).coerceAtMost(0.95f)
                val strokePx = (strokeWidth.toPx() * (0.85f + 0.30f * effectiveIntensity)).coerceAtMost(3.2.dp.toPx())
                drawOutline(
                    outline = outline,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = rimPeak),
                            glowColor.copy(alpha = rimMid),
                            glowColor.copy(alpha = (0.18f * effectiveIntensity * glowAlpha).coerceAtMost(0.5f)),
                            Color.Transparent
                        ),
                        center = touchPos,
                        radius = radiusPx * 0.92f
                    ),
                    style = Stroke(width = strokePx)
                )
            }
        }
}

/**
 * 하모니OS 7 아이콘 하단 앵커 + 일경도저 비선형 모핑 컨테이너
 * - 버튼의 실제 화면 좌표(anchorRect) 바로 아래에 카드를 배치하고
 * - 아이콘 중심을 Pivot으로 삼아 알약에서 카드로 매끄럽게 모핑 전개
 */
@Composable
fun HarmonyAnchoredMorphContainer(
    anchorRect: Rect?,
    cardWidth: Dp,
    fallbackTopPadding: Dp = 96.dp,
    fallbackEndPadding: Dp = 24.dp,
    content: @Composable (morphCorner: Dp) -> Unit
) {
    val density = LocalDensity.current
    var containerRect by remember { mutableStateOf<Rect?>(null) }
    val morphAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        morphAnim.snapTo(0f)
        morphAnim.animateTo(1f, animationSpec = EgoMotion.morphSpring())
    }

    val p = morphAnim.value.coerceIn(0f, 1.15f)
    val clampedP = p.coerceIn(0f, 1f)
    val morphCorner = (32f - 8f * clampedP).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords -> containerRect = coords.boundsInRoot() }
    ) {
        val cRect = containerRect
        val aRect = anchorRect
        if (cRect != null && aRect != null && aRect.width > 4f) {
            val cardWidthPx = with(density) { cardWidth.toPx() }
            val marginPx = with(density) { 12.dp.toPx() }
            val gapPx = with(density) { 8.dp.toPx() }
            val iconCenterX = aRect.center.x - cRect.left
            val maxLeft = (cRect.width - cardWidthPx - marginPx).coerceAtLeast(marginPx)
            val leftPx = (iconCenterX - cardWidthPx * 0.5f).coerceIn(marginPx, maxLeft)
            val topPx = (aRect.bottom - cRect.top + gapPx).coerceAtLeast(with(density) { 52.dp.toPx() })
            val pivotX = ((iconCenterX - leftPx) / cardWidthPx).coerceIn(0.10f, 0.90f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx.toInt(), topPx.toInt()) }
                    .width(cardWidth)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(pivotX, 0f)
                        scaleX = 0.24f + 0.76f * p
                        scaleY = 0.16f + 0.84f * p
                        translationY = with(density) { (-14.dp.toPx()) * (1f - clampedP) }
                        alpha = (clampedP * 1.55f).coerceIn(0f, 1f)
                    }
            ) {
                content(morphCorner)
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = fallbackTopPadding, end = fallbackEndPadding)
                    .width(cardWidth)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.88f, 0f)
                        scaleX = 0.28f + 0.72f * p
                        scaleY = 0.20f + 0.80f * p
                        translationY = with(density) { (-12.dp.toPx()) * (1f - clampedP) }
                        alpha = (clampedP * 1.55f).coerceIn(0f, 1f)
                    }
            ) {
                content(morphCorner)
            }
        }
    }
}

/**
 * 하모니OS 7 오도미터 스타일 숫자 롤링 카운터
 */
@Composable
fun RollingCounterText(
    count: Int,
    textStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState >= initialState) {
                (slideInVertically(tween(210)) { it } + fadeIn(tween(170)))
                    .togetherWith(slideOutVertically(tween(180)) { -it } + fadeOut(tween(140)))
            } else {
                (slideInVertically(tween(210)) { -it } + fadeIn(tween(170)))
                    .togetherWith(slideOutVertically(tween(180)) { it } + fadeOut(tween(140)))
            }
        },
        modifier = modifier,
        label = "rollingCounter"
    ) { target ->
        Text(
            text = target.toString(),
            style = textStyle,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * 하모니OS 7 탄성 고무줄 간격 오버스크롤 (Elastic Gap Stretch)
 * - 스크롤 끝에 도달해 더 당기면 요소 간 간격이 탄성적으로 벌어졌다가 놓으면 과감쇠 스프링으로 복원
 */
class ElasticOverscrollState(
    val pullPx: Float,
    val extraSpacingDp: Dp,
    val nestedScrollConnection: NestedScrollConnection,
    val containerModifier: Modifier
)

@Composable
fun rememberElasticOverscroll(
    isVertical: Boolean,
    maxPullPx: Float = 88f
): ElasticOverscrollState {
    val scope = rememberCoroutineScope()
    val pullAnim = remember { Animatable(0f) }

    val connection = remember(isVertical, maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = if (isVertical) available.y else available.x
                val cur = pullAnim.value
                if (source == NestedScrollSource.UserInput && cur != 0f && delta.sign != cur.sign) {
                    val consumed = if (abs(delta) > abs(cur)) -cur else delta
                    scope.launch { pullAnim.snapTo(cur + consumed) }
                    return if (isVertical) Offset(0f, consumed) else Offset(consumed, 0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val leftover = if (isVertical) available.y else available.x
                if (source == NestedScrollSource.UserInput && abs(leftover) > 0.5f) {
                    val cur = pullAnim.value
                    val resistance = (1f - (abs(cur) / maxPullPx).coerceIn(0f, 0.88f)) * 0.48f
                    val next = (cur + leftover * resistance).coerceIn(-maxPullPx, maxPullPx)
                    scope.launch { pullAnim.snapTo(next) }
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (abs(pullAnim.value) > 0.5f) {
                    val v = if (isVertical) available.y else available.x
                    pullAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = EgoMotion.responsiveSpring(),
                        initialVelocity = v * 0.35f
                    )
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (abs(pullAnim.value) > 0.5f) {
                    pullAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = EgoMotion.responsiveSpring()
                    )
                }
                return Velocity.Zero
            }
        }
    }

    val pull = pullAnim.value
    val extraDp = (abs(pull) * 0.14f).coerceAtMost(13f).dp
    val mod = Modifier
        .nestedScroll(connection)
        .graphicsLayer {
            if (isVertical) translationY = pull * 0.36f
            else translationX = pull * 0.36f
        }

    return ElasticOverscrollState(
        pullPx = pull,
        extraSpacingDp = extraDp,
        nestedScrollConnection = connection,
        containerModifier = mod
    )
}
