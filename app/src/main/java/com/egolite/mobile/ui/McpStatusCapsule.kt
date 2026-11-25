package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.model.AiSpaceStatus

/**
 * young-newt-20 스타일 하단 MCP 동작 표기 캡슐 HUD
 * - 주간모드(화이트 프로스트 글래스) / 야간모드(#101010 글로시 다크) 자동 대응
 * - AI Space 커스텀 색상과 하단 글로우·스파클·쉬머 실시간 연동
 * - 대사 길이에 맞춰 최대 380dp까지 부드럽게 늘어나고 초과 시 ... 말줄임
 * - 비작동 중(IDLE)일 때는 자동 숨김으로 웹 화면 시야 확보
 */
@Composable
fun McpStatusCapsuleHud(
    visible: Boolean,
    status: AiSpaceStatus,
    message: String,
    accentColor: Color,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val isThinking = status == AiSpaceStatus.THINKING
    val isPaused = status == AiSpaceStatus.TAKEOVER_PAUSED
    val displayText = message.ifBlank {
        when (status) {
            AiSpaceStatus.WORKING -> "AI가 작업을 처리하는 중..."
            AiSpaceStatus.THINKING -> "다음 행동을 생각하고 있어요..."
            AiSpaceStatus.TAKEOVER_PAUSED -> "직접 조작 중 · 끝나면 AI 재개 가능"
            AiSpaceStatus.DONE -> "작업 완료"
            AiSpaceStatus.IDLE -> ""
        }
    }

    val infinite = rememberInfiniteTransition(label = "mcpCapsule")
    val shimmerProgress by infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 2400 else 1850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val sparkleAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 1050 else 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = if (isThinking) 0.35f else 0.72f,
        targetValue = if (isThinking) 0.72f else 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val capsuleBg = if (darkTheme) Color(0xFF101010) else Color(0xF5FFFFFF)
    val outerBezelBg = if (darkTheme) Color(0xAA050508) else Color(0x140F172A)
    val borderStrokeColor = if (darkTheme) accentColor.copy(alpha = 0.48f) else accentColor.copy(alpha = 0.44f)
    val sparkleColor = if (darkTheme) Color(0xFFF3F4F6) else accentColor

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxTextWidth = (maxWidth * 0.86f - 66.dp).coerceIn(140.dp, 380.dp)

        AnimatedVisibility(
            visible = visible && displayText.isNotBlank(),
            enter = fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 2 },
            exit = fadeOut(tween(220)) + slideOutVertically(tween(240)) { it / 2 }
        ) {
            // 외곽 입체 베젤 (::before)
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (darkTheme) 18.dp else 12.dp,
                        shape = CircleShape,
                        ambientColor = accentColor.copy(alpha = 0.45f),
                        spotColor = accentColor.copy(alpha = 0.45f)
                    )
                    .clip(CircleShape)
                    .background(outerBezelBg)
                    .padding(4.dp)
            ) {
                // 캡슐 본체 + 하단 컬러 글로우(::after) + 부드러운 폭 애니메이션
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(capsuleBg)
                        .border(1.dp, borderStrokeColor, CircleShape)
                        .drawBehind {
                            val r = size.height / 2f
                            // 상단 미세 유리 하이라이트
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (darkTheme) 0.18f else 0.9f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.45f
                                ),
                                cornerRadius = CornerRadius(r, r)
                            )
                            // 하단에서 올라오는 공간 커스텀 컬러 글로우
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        accentColor.copy(alpha = (if (darkTheme) 0.28f else 0.16f) * glowPulse),
                                        accentColor.copy(alpha = (if (darkTheme) 0.72f else 0.36f) * glowPulse),
                                        if (darkTheme) Color.White.copy(alpha = 0.55f * glowPulse)
                                        else accentColor.copy(alpha = 0.45f * glowPulse)
                                    ),
                                    startY = size.height * 0.42f,
                                    endY = size.height
                                ),
                                cornerRadius = CornerRadius(r, r)
                            )
                        }
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .padding(start = 13.dp, end = 17.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 좌측 3-별 스파클 아이콘 (flicker 펄스)
                    ThreeStarSparkleIcon(
                        color = sparkleColor,
                        glowColor = accentColor,
                        alpha = if (isPaused) 0.65f else sparkleAlpha,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))

                    // 문구 전환 애니메이션 (블러-스케일 느낌의 부드러운 크로스페이드) + 빛 물결 쉬머
                    AnimatedContent(
                        targetState = displayText,
                        transitionSpec = {
                            (fadeIn(tween(380, delayMillis = 80)) + scaleIn(tween(380), initialScale = 0.93f)) togetherWith
                                (fadeOut(tween(260)) + scaleOut(tween(260), targetScale = 1.06f))
                        },
                        label = "mcpPhrase"
                    ) { phrase ->
                        val baseColor = if (darkTheme) Color.White.copy(alpha = 0.52f) else Color(0xFF1E293B)
                        val peakColor = if (darkTheme) Color.White else accentColor
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(baseColor, peakColor, baseColor),
                            start = Offset(shimmerProgress * 320f - 90f, 0f),
                            end = Offset(shimmerProgress * 320f + 90f, 36f)
                        )
                        Text(
                            text = phrase,
                            style = TextStyle(
                                brush = shimmerBrush,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.2).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = maxTextWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreeStarSparkleIcon(
    color: Color,
    glowColor: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    ) {
        val w = size.width
        val h = size.height

        fun drawFourPointStar(cx: Float, cy: Float, r: Float) {
            val inner = r * 0.28f
            val path = Path().apply {
                moveTo(cx, cy - r)
                quadraticTo(cx + inner * 0.5f, cy - inner * 0.5f, cx + r, cy)
                quadraticTo(cx + inner * 0.5f, cy + inner * 0.5f, cx, cy + r)
                quadraticTo(cx - inner * 0.5f, cy + inner * 0.5f, cx - r, cy)
                quadraticTo(cx - inner * 0.5f, cy - inner * 0.5f, cx, cy - r)
                close()
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 1.45f
                ),
                radius = r * 1.45f,
                center = Offset(cx, cy)
            )
            drawPath(path = path, color = color, style = Fill)
        }

        // 큰 별 (좌중단), 중간 별 (우상단), 작은 별 (우하단) — young-newt-20 SVG 비율 1:1
        drawFourPointStar(w * 0.38f, h * 0.50f, w * 0.30f)
        drawFourPointStar(w * 0.75f, h * 0.25f, w * 0.17f)
        drawFourPointStar(w * 0.70f, h * 0.78f, w * 0.13f)
    }
}
