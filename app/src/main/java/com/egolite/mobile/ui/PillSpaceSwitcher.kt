package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.AI_SPACE_COLOR_PRESETS
import com.egolite.mobile.model.AiSpaceStatus
import com.egolite.mobile.model.McpActionMotion
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.SpaceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun parseSpaceColor(hex: String, fallback: Color = EgoEmerald): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

/**
 * Material 3 Expressive 7-shape geometric buddy library.
 * Smooth polar-radius sampling enables seamless path morphing between any two shapes.
 */
enum class M3BuddyShape {
    CIRCLE,
    CLOVER4,
    DIAMOND,
    SQUIRCLE,
    SCALLOP,
    PILLOW,
    PEBBLE
}

private val AI_BUDDY_SHAPES = listOf(
    M3BuddyShape.CLOVER4,
    M3BuddyShape.SQUIRCLE,
    M3BuddyShape.SCALLOP,
    M3BuddyShape.DIAMOND,
    M3BuddyShape.PILLOW,
    M3BuddyShape.PEBBLE,
    M3BuddyShape.CIRCLE
)

fun buddyShapeForSpace(spaceId: String, index: Int): M3BuddyShape {
    val hash = (spaceId.hashCode() and 0x7FFFFFFF) + index * 3
    return AI_BUDDY_SHAPES[hash % AI_BUDDY_SHAPES.size]
}

fun morphTargetShape(base: M3BuddyShape): M3BuddyShape {
    val idx = AI_BUDDY_SHAPES.indexOf(base).coerceAtLeast(0)
    return AI_BUDDY_SHAPES[(idx + 2) % AI_BUDDY_SHAPES.size]
}

private fun sampleShapeRadius(shape: M3BuddyShape, theta: Double): Double {
    return when (shape) {
        M3BuddyShape.CIRCLE -> 0.94
        M3BuddyShape.CLOVER4 -> 0.84 + 0.16 * cos(4.0 * theta)
        M3BuddyShape.SCALLOP -> 0.88 + 0.10 * cos(8.0 * theta)
        M3BuddyShape.SQUIRCLE -> {
            val c = abs(cos(theta)).pow(3.6)
            val s = abs(sin(theta)).pow(3.6)
            0.94 / (c + s).pow(1.0 / 3.6)
        }
        M3BuddyShape.DIAMOND -> {
            val c = abs(cos(theta)).pow(1.45)
            val s = abs(sin(theta)).pow(1.45)
            (0.95 / (c + s).pow(1.0 / 1.45)).coerceAtMost(1.02)
        }
        M3BuddyShape.PILLOW -> {
            val base = 0.88 + 0.08 * cos(4.0 * (theta - PI / 4.0))
            base + 0.04 * cos(2.0 * theta)
        }
        M3BuddyShape.PEBBLE -> {
            0.88 + 0.08 * cos(3.0 * theta + 0.4) + 0.04 * sin(2.0 * theta)
        }
    }
}

private fun buildMorphedBuddyPath(
    fromShape: M3BuddyShape,
    toShape: M3BuddyShape,
    morphProgress: Float,
    centerX: Float,
    centerY: Float,
    radius: Float
): Path {
    val t = morphProgress.coerceIn(0f, 1f).toDouble()
    val samples = 64
    val path = Path()
    for (i in 0 until samples) {
        val theta = (2.0 * PI * i) / samples
        val r0 = sampleShapeRadius(fromShape, theta)
        val r1 = sampleShapeRadius(toShape, theta)
        val r = (r0 + (r1 - r0) * t) * radius
        val x = centerX + (cos(theta) * r).toFloat()
        val y = centerY + (sin(theta) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Fixed reserved slot width for the top-left M3 buddy strip (34dp each, 3dp resting gap).
 * Adjacent Row 1 page tabs use this fixed width so they never shift left when buddies huddle.
 */
private fun buildRadiiBuddyPath(
    radii: FloatArray,
    centerX: Float,
    centerY: Float,
    radius: Float
): Path {
    val samples = radii.size
    val path = Path()
    for (i in 0 until samples) {
        val theta = (i.toDouble() / samples.toDouble()) * 2.0 * PI
        val r = radius * radii[i]
        val x = centerX + (cos(theta) * r).toFloat()
        val y = centerY + (sin(theta) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

fun buddySlotWidthDp(spaces: List<BrowserSpace>): Dp {
    val visibleCount = spaces.size.coerceIn(1, 5)
    return (visibleCount * 34 + (visibleCount - 1) * 3 + 8).dp
}

/**
 * Single M3 Expressive geometric buddy avatar with minimal vertical-pill eyes (no mouth/blush).
 * Supports smooth path morphing, AI thinking/working motions, natural random blink, and > < squint.
 */
@Composable
fun M3ExpressiveBuddyAvatar(
    color: Color,
    baseShape: M3BuddyShape,
    targetShape: M3BuddyShape = morphTargetShape(baseShape),
    morphProgress: Float = 0f,
    isSquinting: Boolean = false,
    aiStatus: AiSpaceStatus = AiSpaceStatus.IDLE,
    actionMotion: McpActionMotion = McpActionMotion.IDLE,
    size: Dp = 34.dp,
    blinkSeed: Int = 0,
    modifier: Modifier = Modifier
) {
    val isWorking = aiStatus == AiSpaceStatus.WORKING
    val isThinking = aiStatus == AiSpaceStatus.THINKING
    val eyeInk = if (color.luminance() > 0.42f) Color(0xFF0A1F18) else Color(0xFFFFFFFF)

    // Natural random blink when idle/done and not squinting
    var blinkScaleY by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(blinkSeed, isSquinting, isWorking) {
        if (isSquinting) {
            blinkScaleY = 1f
            return@LaunchedEffect
        }
        val baseDelay = 1800L + ((blinkSeed * 733) % 1900).toLong()
        delay(baseDelay)
        while (true) {
            blinkScaleY = 0.12f
            delay(110L)
            blinkScaleY = 1f
            delay(2700L + ((blinkSeed * 521) % 1800).toLong())
        }
    }

    // Status-driven micro-motion (only runs infinite transition when thinking or working)
    val statusPhase = if (isWorking || isThinking) {
        val inf = rememberInfiniteTransition(label = "buddyStatus_$blinkSeed")
        inf.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isWorking) 760 else 1600,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "buddyPhase"
        ).value
    } else {
        0.5f
    }

    val resolvedMotion = when {
        actionMotion != McpActionMotion.IDLE -> actionMotion
        aiStatus == AiSpaceStatus.DONE -> McpActionMotion.DONE
        aiStatus == AiSpaceStatus.TAKEOVER_PAUSED -> McpActionMotion.TAKEOVER
        aiStatus == AiSpaceStatus.THINKING -> McpActionMotion.SNAPSHOT
        aiStatus == AiSpaceStatus.WORKING -> McpActionMotion.EXEC_JS
        else -> McpActionMotion.IDLE
    }
    val motionActive = resolvedMotion != McpActionMotion.IDLE

    // 저전력 연속 스프링 모핑: IDLE 시 프레임 루프 0fps 정지, 활성 명령 시에만 64-정점 셰이프·눈빛 연속 보간
    var motionClock by remember { mutableFloatStateOf(0f) }
    var smoothRot by remember { mutableFloatStateOf(0f) }
    var smoothHop by remember { mutableFloatStateOf(0f) }
    var smoothEyeX by remember { mutableFloatStateOf(0f) }
    var smoothEyeY by remember { mutableFloatStateOf(0f) }
    var smoothSx by remember { mutableFloatStateOf(1f) }
    var smoothSy by remember { mutableFloatStateOf(1f) }
    var smoothErrorEyes by remember { mutableFloatStateOf(0f) }
    var smoothHappyEyes by remember { mutableFloatStateOf(0f) }
    var smoothShutter by remember { mutableFloatStateOf(0f) }
    val latestMotion by rememberUpdatedState(resolvedMotion)

    LaunchedEffect(motionActive) {
        var lastNs = 0L
        while (latestMotion != McpActionMotion.IDLE ||
            abs(smoothRot) > 0.15f || abs(smoothHop) > 0.08f ||
            abs(smoothSx - 1f) > 0.005f || abs(smoothSy - 1f) > 0.005f ||
            smoothErrorEyes > 0.02f || smoothHappyEyes > 0.02f || smoothShutter > 0.02f
        ) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                motionClock += dt
                val now = motionClock
                var tRot = 0f; var tHop = 0f; var tEx = 0f; var tEy = 0f
                var tSx = 1f; var tSy = 1f; var tErr = 0f; var tHap = 0f; var tShut = 0f
                when (latestMotion) {
                    McpActionMotion.CMD_RECV -> {
                        val h = abs(sin(now * 6.0f))
                        tHop = -h * 3.2f; tSx = 0.94f + (1f - h) * 0.12f; tSy = 1.06f + h * 0.08f; tEy = -1.2f
                    }
                    McpActionMotion.NAVIGATE -> {
                        tRot = 10f + sin(now * 5.2f) * 3f; tSx = 1.08f; tSy = 0.93f; tEx = 2.6f
                    }
                    McpActionMotion.READ_MD -> {
                        val cyc = (now * 1.15f) % 1f
                        val scan = if (cyc < 0.82f) (cyc / 0.82f) * 2f - 1f else 1f - ((cyc - 0.82f) / 0.18f) * 2f
                        tEx = scan * 3.0f; tEy = 0.8f; tRot = scan * 3.5f
                    }
                    McpActionMotion.SNAPSHOT -> {
                        tRot = sin(now * 3.2f) * 11f; tEx = sin(now * 3.2f + 0.4f) * 2.8f
                    }
                    McpActionMotion.FILL -> {
                        val tap = abs(sin(now * 8.5f)).pow(2.2f)
                        tHop = tap * 1.8f; tSx = 1f + tap * 0.10f; tSy = 1f - tap * 0.09f; tEx = 1.2f; tEy = 1.1f
                    }
                    McpActionMotion.CLICK -> {
                        val c = (now * 1.45f) % 1f
                        val press = if (c < 0.22f) sin((c / 0.22f) * PI.toFloat()) else 0f
                        val pop = if (c in 0.22f..0.55f) sin(((c - 0.22f) / 0.33f) * PI.toFloat()) else 0f
                        tHop = press * 2.2f - pop * 2.6f; tSx = 1f + press * 0.18f - pop * 0.08f; tSy = 1f - press * 0.16f + pop * 0.12f
                    }
                    McpActionMotion.SCROLL -> {
                        val s = sin(now * 3.1f)
                        tHop = s * 2.2f; tEy = s * 2.2f; tRot = s * 3f
                    }
                    McpActionMotion.EXEC_JS, McpActionMotion.RESEARCH -> {
                        tRot = sin(now * 3.2f) * 8f; tSx = 1f + sin(now * 5.2f) * 0.06f; tSy = 1f - sin(now * 5.2f) * 0.06f
                        tHop = -abs(sin(now * 5.2f)) * 1.8f
                        if (latestMotion == McpActionMotion.RESEARCH) { tEx = cos(now * 3.4f) * 2.6f; tEy = sin(now * 3.4f) * 1.2f }
                    }
                    McpActionMotion.SCREENSHOT -> {
                        val cyc = (now * 0.85f) % 1f
                        val snap = if (cyc in 0.42f..0.62f) sin(((cyc - 0.42f) / 0.20f) * PI.toFloat()) else 0f
                        tSx = 1f + snap * 0.12f; tSy = 1f - snap * 0.10f; tShut = snap
                    }
                    McpActionMotion.DONE -> {
                        val h = abs(sin(now * 4.6f))
                        tHop = -h * 2.8f; tHap = 1f
                    }
                    McpActionMotion.TAKEOVER -> {
                        tSx = 0.91f; tSy = 0.91f; tEx = 0.9f
                    }
                    McpActionMotion.ERROR -> {
                        // 색상 변경 없이 도리도리 + X X 눈으로만 오류 표현!
                        tRot = sin(now * 11.5f) * 7.5f; tErr = 1f
                    }
                    McpActionMotion.IDLE -> Unit
                }
                val k = (dt * 10f).coerceAtMost(1f)
                smoothRot += (tRot - smoothRot) * k
                smoothHop += (tHop - smoothHop) * k
                smoothEyeX += (tEx - smoothEyeX) * k
                smoothEyeY += (tEy - smoothEyeY) * k
                smoothSx += (tSx - smoothSx) * k
                smoothSy += (tSy - smoothSy) * k
                smoothErrorEyes += (tErr - smoothErrorEyes) * k
                smoothHappyEyes += (tHap - smoothHappyEyes) * k
                smoothShutter += (tShut - smoothShutter) * (dt * 14f).coerceAtMost(1f)
            }
        }
    }

    val desiredShape = when {
        morphProgress > 0.5f -> targetShape
        resolvedMotion == McpActionMotion.NAVIGATE || resolvedMotion == McpActionMotion.SCROLL -> M3BuddyShape.PEBBLE
        resolvedMotion == McpActionMotion.READ_MD || resolvedMotion == McpActionMotion.SCREENSHOT -> M3BuddyShape.SQUIRCLE
        resolvedMotion == McpActionMotion.FILL || resolvedMotion == McpActionMotion.CMD_RECV -> M3BuddyShape.PILLOW
        resolvedMotion == McpActionMotion.CLICK || resolvedMotion == McpActionMotion.TAKEOVER -> M3BuddyShape.CIRCLE
        resolvedMotion == McpActionMotion.EXEC_JS || resolvedMotion == McpActionMotion.RESEARCH -> M3BuddyShape.SCALLOP
        resolvedMotion == McpActionMotion.ERROR -> M3BuddyShape.DIAMOND
        else -> baseShape
    }
    // 64-정점 연속 극좌표 버퍼: 어떤 셰이프에서 다른 셰이프로 바뀌어도 절대 끊기지 않고 액체처럼 연속 모핑!
    val liveRadii = remember {
        FloatArray(64) { i ->
            sampleShapeRadius(baseShape, (i.toDouble() / 64.0) * 2.0 * PI).toFloat()
        }
    }
    var radiiFrameTick by remember { mutableLongStateOf(0L) }
    val latestDesiredShape by rememberUpdatedState(desiredShape)
    val latestMorphP by rememberUpdatedState(morphProgress)

    LaunchedEffect(desiredShape, morphProgress > 0.01f) {
        var lastNs = 0L
        while (true) {
            var maxDiff = 0f
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0.016f else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.033f)
                lastNs = ns
                val kShape = (dt * 9.5f).coerceAtMost(1f)
                val targetS = latestDesiredShape
                val mp = latestMorphP.coerceIn(0f, 1f)
                for (i in 0 until 64) {
                    val th = (i.toDouble() / 64.0) * 2.0 * PI
                    val goalR = if (mp in 0.01f..0.99f && !motionActive) {
                        val r0 = sampleShapeRadius(baseShape, th).toFloat()
                        val r1 = sampleShapeRadius(targetShape, th).toFloat()
                        r0 + (r1 - r0) * mp
                    } else {
                        sampleShapeRadius(targetS, th).toFloat()
                    }
                    val diff = goalR - liveRadii[i]
                    if (abs(diff) > maxDiff) maxDiff = abs(diff)
                    liveRadii[i] += diff * kShape
                }
                radiiFrameTick = ns
            }
            if (maxDiff < 0.002f && latestMorphP !in 0.01f..0.99f) break
        }
    }
    val effectiveSquinting = isSquinting || smoothShutter > 0.45f

    val tiltDeg = if (!isSquinting) smoothRot else 0f
    val eyeGlanceX = if (!isSquinting) smoothEyeX else 0f
    val workHopDp = if (!isSquinting) smoothHop else 0f
    val glowPulse = if (isWorking || motionActive) 0.24f + 0.18f * statusPhase else 0f

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = smoothSx
                scaleY = smoothSy
                rotationZ = tiltDeg
                translationY = workHopDp.dp.toPx()
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.47f

        @Suppress("UNUSED_EXPRESSION")
        radiiFrameTick

        // Subtle aura glow when AI is actively working
        if (glowPulse > 0.01f) {
            val auraPath = buildRadiiBuddyPath(liveRadii, cx, cy, r * 1.13f)
            drawPath(path = auraPath, color = color.copy(alpha = glowPulse))
        }

        // Main M3 64-vertex continuously morphed geometric body
        val bodyPath = buildRadiiBuddyPath(liveRadii, cx, cy, r)
        drawPath(path = bodyPath, color = color)

        // Subtle specular top highlight for tactile depth
        drawPath(
            path = bodyPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Minimal vertical-pill eyes (or > < squint during long-press huddle)
        val eyeOffsetX = r * 0.30f
        val eyeCenterY = cy - r * 0.02f
        val glancePx = eyeGlanceX.dp.toPx()
        val glancePy = smoothEyeY.dp.toPx()

        if (smoothErrorEyes > 0.45f) {
            val sz = r * 0.16f
            val sw = (this.size.minDimension * 0.075f).coerceAtLeast(1.8.dp.toPx())
            listOf(-1f, 1f).forEach { s ->
                val ex = cx + s * eyeOffsetX + glancePx
                val ey = eyeCenterY + glancePy
                drawLine(eyeInk, Offset(ex - sz, ey - sz), Offset(ex + sz, ey + sz), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(eyeInk, Offset(ex + sz, ey - sz), Offset(ex - sz, ey + sz), strokeWidth = sw, cap = StrokeCap.Round)
            }
        } else if (smoothHappyEyes > 0.45f) {
            val sz = r * 0.17f
            val sw = (this.size.minDimension * 0.075f).coerceAtLeast(1.8.dp.toPx())
            listOf(-1f, 1f).forEach { s ->
                val ex = cx + s * eyeOffsetX + glancePx
                val ey = eyeCenterY + glancePy
                drawArc(
                    color = eyeInk,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(ex - sz, ey - sz * 0.5f),
                    size = Size(sz * 2f, sz * 2f),
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        } else if (effectiveSquinting) {
            val sw = (this.size.minDimension * 0.078f).coerceAtLeast(1.8.dp.toPx())
            val arm = r * 0.17f
            val lx = cx - eyeOffsetX
            val rx = cx + eyeOffsetX
            // Left eye: '>'
            val leftSquint = Path().apply {
                moveTo(lx - arm * 0.7f, eyeCenterY - arm * 0.75f)
                lineTo(lx + arm * 0.65f, eyeCenterY)
                lineTo(lx - arm * 0.7f, eyeCenterY + arm * 0.75f)
            }
            // Right eye: '<'
            val rightSquint = Path().apply {
                moveTo(rx + arm * 0.7f, eyeCenterY - arm * 0.75f)
                lineTo(rx - arm * 0.65f, eyeCenterY)
                lineTo(rx + arm * 0.7f, eyeCenterY + arm * 0.75f)
            }
            val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(leftSquint, color = eyeInk, style = stroke)
            drawPath(rightSquint, color = eyeInk, style = stroke)
        } else {
            val pillW = (this.size.minDimension * 0.105f).coerceAtLeast(2.6.dp.toPx())
            val pillH = ((this.size.minDimension * 0.24f) * blinkScaleY).coerceAtLeast(1.4.dp.toPx())
            val corner = CornerRadius(pillW / 2f, pillW / 2f)
            drawRoundRect(
                color = eyeInk,
                topLeft = Offset(cx - eyeOffsetX - pillW / 2f + glancePx, eyeCenterY - pillH / 2f),
                size = Size(pillW, pillH),
                cornerRadius = corner
            )
            drawRoundRect(
                color = eyeInk,
                topLeft = Offset(cx + eyeOffsetX - pillW / 2f + glancePx, eyeCenterY - pillH / 2f),
                size = Size(pillW, pillH),
                cornerRadius = corner
            )
        }
    }
}

/**
 * 좌상단 캡슐리스 34dp M3 Expressive 캐릭터 트리거 (Drag-to-Open & Drag-to-Select 지원)
 * - 평소 간격 3dp, 롱프레스/드래그 시 -10dp 다닥다닥 모임 + 20% 확대 + 도형 모프 + > < 눈웃음
 * - 고정 슬롯 너비 유지로 인접 페이지 탭 위치 불변
 * - 공간 선택 좌우 드래그 시 모인 캐릭터들이 예약 영역 범위 내에서 손가락을 자기장처럼 추종
 */
@Composable
fun PillSpaceCapsule(
    activeSpace: BrowserSpace,
    spaces: List<BrowserSpace>,
    isAnyAiWorking: Boolean,
    workingAccentColor: Color = EgoEmerald,
    spaceCount: Int,
    canAddAiSpace: Boolean,
    isExpanded: Boolean,
    isMonitorExpanded: Boolean = false,
    onSetExpanded: (Boolean) -> Unit,
    onToggleAiMonitor: () -> Unit,
    onHoverIndexChanged: (Int) -> Unit,
    onCommitIndex: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val orderedCollapsed = remember(spaces, activeSpace) {
        val rest = spaces.filter { it.id != activeSpace.id }
        (listOf(activeSpace) + rest).take(5)
    }
    // 접힘 <-> 펼침 인터럽트 가능 연속 스프링 모프 (진행 중 다시 눌러도 그 자리에서 즉시 유턴!)
    val monitorMorph by animateFloatAsState(
        targetValue = if (isMonitorExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 270f),
        label = "monitorExpandMorph"
    )
    val collapsedCount = orderedCollapsed.size.coerceAtLeast(1)
    val targetSlotWidthDp = if (isMonitorExpanded) 42.dp else (collapsedCount * 34 + (collapsedCount - 1) * 3 + 8).dp
    val slotWidthDp by animateDpAsState(
        targetValue = targetSlotWidthDp,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f),
        label = "buddySlotWidthMorph"
    )
    val buddyCount = if (isMonitorExpanded) 1 else collapsedCount
    val slotWidthPx = with(density) { slotWidthDp.toPx() }

    val chipWidthPx = with(density) { 112.dp.toPx() }
    val dragThresholdY = with(density) { 26.dp.toPx() }
    val totalSlots = spaceCount + if (canAddAiSpace) 1 else 0

    var isHuddled by remember { mutableStateOf(false) }
    var magneticTargetX by remember { mutableFloatStateOf(0f) }

    // Short-tap organic squash-and-stretch hop synced with panel open/close
    val tapHopScaleX = remember { Animatable(1f) }
    val tapHopScaleY = remember { Animatable(1f) }
    val tapHopOffsetY = remember { Animatable(0f) }

    val huddleActive = isHuddled || isExpanded
    val buddyGapDp by animateDpAsState(
        targetValue = if (huddleActive) (-10).dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "buddyHuddleGap"
    )
    val buddyScale by animateFloatAsState(
        targetValue = if (huddleActive) 1.20f else 1.0f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 400f),
        label = "buddyHuddleScale"
    )
    val morphProgress by animateFloatAsState(
        targetValue = if (huddleActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "buddyMorphProgress"
    )
    val magneticShiftX by animateFloatAsState(
        targetValue = if (huddleActive) magneticTargetX else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 380f),
        label = "buddyMagneticX"
    )

    val latestTotalSlots by rememberUpdatedState(totalSlots)
    val latestSlotWidthPx by rememberUpdatedState(slotWidthPx)
    val latestBuddyCount by rememberUpdatedState(buddyCount)
    val latestSetExpanded by rememberUpdatedState(onSetExpanded)
    val latestMonitorExpanded by rememberUpdatedState(isMonitorExpanded)
    val latestToggleMonitor by rememberUpdatedState(onToggleAiMonitor)
    val latestHoverChanged by rememberUpdatedState(onHoverIndexChanged)
    val latestCommit by rememberUpdatedState(onCommitIndex)

    Box(
        modifier = Modifier
            .width(slotWidthDp)
            .height(38.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragged = false
                    var longPressed = false
                    var lastHover = -1

                    val longPressJob = scope.launch {
                        delay(260L)
                        // 펼침 상태에서는 좌측 상단 캐릭터 롱프레스 공간 전환 잠금
                        if (latestMonitorExpanded) return@launch
                        longPressed = true
                        isHuddled = true
                        latestSetExpanded(true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                longPressJob.cancel()
                                if (dragged || longPressed) {
                                    if (lastHover >= 0) {
                                        latestCommit(lastHover)
                                    }
                                    latestSetExpanded(false)
                                    latestHoverChanged(-1)
                                } else {
                                    // 짧게 눌렀다 떼기: 유기적 스쿼시-홉과 함께 AI 라이브 모니터 토글
                                    latestSetExpanded(false)
                                    latestHoverChanged(-1)
                                    latestToggleMonitor()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        launch {
                                            tapHopScaleX.animateTo(1.08f, tween(70))
                                            tapHopScaleX.animateTo(0.95f, tween(95))
                                            tapHopScaleX.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                        launch {
                                            tapHopScaleY.animateTo(0.88f, tween(70))
                                            tapHopScaleY.animateTo(1.09f, tween(95))
                                            tapHopScaleY.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                        launch {
                                            tapHopOffsetY.animateTo(2.5f, tween(70))
                                            tapHopOffsetY.animateTo(-4.5f, tween(95))
                                            tapHopOffsetY.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                    }
                                }
                                isHuddled = false
                                magneticTargetX = 0f
                                break
                            }

                            val dy = change.position.y - down.position.y
                            val dx = change.position.x
                            val totalBarWidthPx = latestTotalSlots * chipWidthPx
                            val maxBarBottomPx = dragThresholdY + chipWidthPx * 0.9f

                            if (!dragged && (abs(dy) > dragThresholdY * 0.55f || abs(dx - down.position.x) > dragThresholdY * 0.55f)) {
                                longPressJob.cancel()
                                dragged = true
                                isHuddled = true
                                latestSetExpanded(true)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }

                            if (dragged || longPressed) {
                                // 모인 캐릭터들이 예약된 캐릭터 영역(slotWidthPx) 내에서만 손가락 X를 따라감
                                val huddledWidthPx = with(density) {
                                    (latestBuddyCount * 34 + (latestBuddyCount - 1) * (-10)).coerceAtLeast(34).dp.toPx()
                                }
                                val maxShift = ((latestSlotWidthPx - huddledWidthPx) * 0.5f).coerceAtLeast(0f)
                                val desiredOffset = (dx - latestSlotWidthPx * 0.5f) * 0.55f
                                magneticTargetX = desiredOffset.coerceIn(-maxShift, maxShift)

                                val inBarBounds = dy > dragThresholdY && dy <= maxBarBottomPx && dx >= 0f && dx <= totalBarWidthPx
                                if (inBarBounds) {
                                    val idx = (dx / chipWidthPx).toInt()
                                    if (idx in 0 until latestTotalSlots && idx != lastHover) {
                                        lastHover = idx
                                        latestHoverChanged(idx)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                } else if (lastHover != -1) {
                                    lastHover = -1
                                    latestHoverChanged(-1)
                                }
                            }
                        }
                    } finally {
                        longPressJob.cancel()
                        isHuddled = false
                        magneticTargetX = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.graphicsLayer {
                translationX = magneticShiftX
                translationY = tapHopOffsetY.value.dp.toPx()
                scaleX = buddyScale * tapHopScaleX.value
                scaleY = buddyScale * tapHopScaleY.value
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(buddyGapDp)
        ) {
            orderedCollapsed.forEachIndexed { idx, space ->
                val isCompanion = idx > 0
                if (isCompanion && monitorMorph >= 0.99f) return@forEachIndexed
                val isPersonal = space.type == SpaceType.PERSONAL
                val buddyColor = if (isPersonal) EgoCobalt else parseSpaceColor(space.accentColorHex, workingAccentColor)
                val baseShape = if (isPersonal) M3BuddyShape.CIRCLE else buddyShapeForSpace(space.id, idx)
                val targetShape = if (isPersonal) M3BuddyShape.CLOVER4 else morphTargetShape(baseShape)
                val compAlpha = if (isCompanion) (1f - monitorMorph * 1.15f).coerceIn(0f, 1f) else 1f
                val compScale = if (isCompanion) (1f - monitorMorph * 0.42f).coerceIn(0.45f, 1f) else 1f
                val compShiftX = if (isCompanion) (-monitorMorph * (idx * 24f)) else 0f
                val compShiftY = if (isCompanion) (monitorMorph * (28f + idx * 14f)) else 0f
                // 펼침/접힘 도중 활성 버디도 부드러운 M3 셰이프 모프 펄스!
                val activeEdgeMorph = if (!isCompanion) (sin(monitorMorph * PI.toFloat()) * 0.65f).coerceIn(0f, 1f) else monitorMorph
                M3ExpressiveBuddyAvatar(
                    color = buddyColor,
                    baseShape = baseShape,
                    targetShape = targetShape,
                    morphProgress = maxOf(morphProgress, activeEdgeMorph),
                    isSquinting = huddleActive,
                    aiStatus = if (isPersonal) AiSpaceStatus.IDLE else space.aiStatus,
                    actionMotion = if (isPersonal) McpActionMotion.IDLE else space.aiActionMotion,
                    size = 34.dp,
                    blinkSeed = idx + (space.id.hashCode() and 0xFF),
                    modifier = Modifier.graphicsLayer {
                        alpha = compAlpha
                        scaleX = compScale
                        scaleY = compScale
                        translationX = compShiftX.dp.toPx()
                        translationY = compShiftY.dp.toPx()
                    }
                )
            }
        }
    }
}

@Composable
private fun SpaceDotIndicator(color: Color, filled: Boolean) {
    Canvas(modifier = Modifier.size(10.dp)) {
        if (filled) {
            drawCircle(color = color)
        } else {
            drawCircle(color = color, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun ExpandedSpaceSlideBar(
    visible: Boolean,
    spaces: List<BrowserSpace>,
    activeSpaceId: String,
    canAddAiSpace: Boolean,
    hoveredIndex: Int = -1,
    onSelectSpace: (String) -> Unit,
    onCreateAiSpace: () -> Unit,
    onCloseAiSpace: (String) -> Unit,
    onChangeSpaceColor: (String, String) -> Unit = { _, _ -> }
) {
    val activeSpace = spaces.firstOrNull { it.id == activeSpaceId }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(130)) + expandVertically(tween(160)),
        exit = fadeOut(tween(110)) + shrinkVertically(tween(130))
    ) {
        Surface(
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    spaces.forEachIndexed { idx, space ->
                        val selected = space.id == activeSpaceId
                        val isHovered = hoveredIndex == idx
                        val isAi = space.type == SpaceType.AI
                        val dotColor = if (isAi) parseSpaceColor(space.accentColorHex) else EgoCobalt
                        val bgColor = when {
                            isHovered -> dotColor.copy(alpha = 0.28f)
                            selected -> dotColor.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        }

                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isHovered) Modifier.border(1.5.dp, dotColor, CircleShape) else Modifier
                                )
                                .clickable { onSelectSpace(space.id) }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpaceDotIndicator(color = dotColor, filled = selected || isHovered)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = space.name,
                                fontSize = 13.sp,
                                fontWeight = if (selected || isHovered) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isAi && (space.aiStatus == AiSpaceStatus.WORKING || space.aiStatus == AiSpaceStatus.THINKING)) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (space.aiStatus == AiSpaceStatus.THINKING) "생각중" else "작업중",
                                    fontSize = 11.sp,
                                    color = dotColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isAi && spaces.count { it.type == SpaceType.AI } > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                EgoIcon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "공간 닫기",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onCloseAiSpace(space.id) },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    symbolSize = 16.dp
                                )
                            }
                        }
                    }

                    if (canAddAiSpace) {
                        val isAddHovered = hoveredIndex == spaces.size
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isAddHovered) EgoEmerald.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant)
                                .then(if (isAddHovered) Modifier.border(1.5.dp, EgoEmerald, CircleShape) else Modifier)
                                .clickable { onCreateAiSpace() },
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "AI 공간 추가",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                                symbolSize = 18.dp
                            )
                        }
                    }
                }

                // 현재 선택된 공간이 AI Space일 때 하단에 커스텀 색상 팔레트 표시
                if (activeSpace != null && activeSpace.type == SpaceType.AI) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${activeSpace.name} 색상",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        AI_SPACE_COLOR_PRESETS.forEach { hex ->
                            val swatch = parseSpaceColor(hex)
                            val isSelected = activeSpace.accentColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 22.dp else 18.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onChangeSpaceColor(activeSpace.id, hex) }
                            )
                        }
                    }
                }
            }
        }
    }
}
