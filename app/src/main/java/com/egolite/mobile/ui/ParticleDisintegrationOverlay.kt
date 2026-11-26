package com.egolite.mobile.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
import com.egolite.mobile.model.CardVanishStyle
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class DustVanishRequest(
    val id: Long,
    val rect: Rect,
    val accentColor: Color,
    val thumbnail: Bitmap?,
    val style: CardVanishStyle = CardVanishStyle.HARMONY_DUST,
    val particleCount: Int = 1800,
    val durationMs: Int = 1300
)

private class LosslessParticleBuffer(
    val count: Int,
    val startX: FloatArray,
    val startY: FloatArray,
    val vx: FloatArray,
    val vy: FloatArray,
    val phase: FloatArray,
    val swirl: FloatArray,
    val delay: FloatArray,
    val radius: FloatArray,
    val baseRgb: IntArray,
    val sparkle: BooleanArray
)

@Composable
fun ParticleDisintegrationOverlay(
    bursts: List<DustVanishRequest>,
    onBurstFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    bursts.forEach { burst ->
        androidx.compose.runtime.key(burst.id) {
            SingleParticleDisintegration(
                burst = burst,
                onFinished = { onBurstFinished(burst.id) },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SingleParticleDisintegration(
    burst: DustVanishRequest,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(burst.id) { Animatable(0f) }
    val reusablePaint = remember(burst.id) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val reusableBitmapPaint = remember(burst.id) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
    val reusableRectF = remember(burst.id) { RectF() }

    val effectiveDuration = when (burst.style) {
        CardVanishStyle.HARMONY_DUST -> burst.durationMs.coerceIn(500, 2200)
        CardVanishStyle.LIGHT_PARTICLES -> 560
        CardVanishStyle.SCALE_FADE -> 240
        CardVanishStyle.OFF -> 1
    }

    val buffer = remember(burst.id) {
        if (burst.style == CardVanishStyle.SCALE_FADE || burst.style == CardVanishStyle.OFF) {
            null
        } else {
            val rng = Random(burst.id)
            val rect = burst.rect
            val bmp = burst.thumbnail
            val hasBmp = bmp != null && !bmp.isRecycled && bmp.width > 4 && bmp.height > 4
            val accentRgb = burst.accentColor.toArgb() and 0x00FFFFFF

            if (burst.style == CardVanishStyle.LIGHT_PARTICLES) {
                val cols = 18
                val rows = 14
                val n = cols * rows
                val sx = FloatArray(n)
                val sy = FloatArray(n)
                val vx = FloatArray(n)
                val vy = FloatArray(n)
                val ph = FloatArray(n)
                val sw = FloatArray(n)
                val dl = FloatArray(n)
                val rd = FloatArray(n)
                val rgb = IntArray(n)
                val sp = BooleanArray(n)
                val cellW = rect.width / cols
                val cellH = rect.height / rows
                var idx = 0
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        sx[idx] = rect.left + (c + 0.5f) * cellW + (rng.nextFloat() - 0.5f) * cellW * 0.7f
                        sy[idx] = rect.top + (r + 0.5f) * cellH + (rng.nextFloat() - 0.5f) * cellH * 0.7f
                        rgb[idx] = if (hasBmp) {
                            val bx = ((c.toFloat() / cols) * (bmp!!.width - 1)).toInt().coerceIn(0, bmp.width - 1)
                            val by = ((r.toFloat() / rows) * (bmp.height - 1)).toInt().coerceIn(0, bmp.height - 1)
                            runCatching { bmp.getPixel(bx, by) and 0x00FFFFFF }.getOrDefault(accentRgb)
                        } else {
                            accentRgb
                        }
                        val wave = (c.toFloat() / cols) * 0.55f + (1f - r.toFloat() / rows) * 0.45f
                        dl[idx] = (wave * 0.34f + rng.nextFloat() * 0.12f).coerceIn(0f, 0.48f)
                        vx[idx] = (rng.nextFloat() - 0.32f) * 145f
                        vy[idx] = -55f - rng.nextFloat() * 140f
                        ph[idx] = rng.nextFloat() * 6.283f
                        sw[idx] = 14f
                        rd[idx] = 2.4f + rng.nextFloat() * 3.6f
                        sp[idx] = false
                        idx++
                    }
                }
                LosslessParticleBuffer(n, sx, sy, vx, vy, ph, sw, dl, rd, rgb, sp)
            } else {
                // HARMONY_DUST: Lossless high-density ultra-fine dust (1,000 ~ 3,000 particles)
                val n = burst.particleCount.coerceIn(800, 3200)
                val sx = FloatArray(n)
                val sy = FloatArray(n)
                val vx = FloatArray(n)
                val vy = FloatArray(n)
                val ph = FloatArray(n)
                val sw = FloatArray(n)
                val dl = FloatArray(n)
                val rd = FloatArray(n)
                val rgb = IntArray(n)
                val sp = BooleanArray(n)
                for (i in 0 until n) {
                    val u = rng.nextFloat()
                    val v = rng.nextFloat()
                    sx[i] = rect.left + u * rect.width
                    sy[i] = rect.top + v * rect.height
                    rgb[i] = if (hasBmp) {
                        val bx = (u * (bmp!!.width - 1)).toInt().coerceIn(0, bmp.width - 1)
                        val by = (v * (bmp.height - 1)).toInt().coerceIn(0, bmp.height - 1)
                        runCatching { bmp.getPixel(bx, by) and 0x00FFFFFF }.getOrDefault(accentRgb)
                    } else {
                        if (v > 0.72f) accentRgb else (if (rng.nextFloat() < 0.35f) accentRgb else 0x00CBD5E1)
                    }
                    // 우측 상단에서 좌측 하단으로 유기적 노이즈 파동으로 침식되며 퍼지는 粒子消散 패턴
                    val wave = (1f - u) * 0.62f + v * 0.38f
                    dl[i] = (wave * 0.46f + (rng.nextFloat() - 0.5f) * 0.14f).coerceIn(0f, 0.58f)
                    val angle = -1.10f + (rng.nextFloat() - 0.5f) * 1.15f
                    val speed = 85f + rng.nextFloat() * 225f
                    vx[i] = cos(angle) * speed
                    vy[i] = sin(angle) * speed - 30f
                    ph[i] = rng.nextFloat() * 6.283f
                    sw[i] = (rng.nextFloat() - 0.5f) * 30f
                    rd[i] = 1.25f + rng.nextFloat() * 2.45f
                    sp[i] = rng.nextFloat() < 0.14f
                }
                LosslessParticleBuffer(n, sx, sy, vx, vy, ph, sw, dl, rd, rgb, sp)
            }
        }
    }

    LaunchedEffect(burst.id) {
        if (burst.style == CardVanishStyle.OFF) {
            onFinished()
            return@LaunchedEffect
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = effectiveDuration)
        )
        onFinished()
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .zIndex(45f)
    ) {
        val t = progress.value
        val native = drawContext.canvas.nativeCanvas
        val rect = burst.rect
        val bmp = burst.thumbnail

        if (burst.style == CardVanishStyle.SCALE_FADE) {
            val scale = 1f - 0.28f * t
            val alphaInt = ((1f - t).coerceIn(0f, 1f) * 255f).toInt()
            val cx = rect.left + rect.width / 2f
            val cy = rect.top + rect.height / 2f
            val halfW = (rect.width * scale) / 2f
            val halfH = (rect.height * scale) / 2f
            reusableRectF.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
            if (bmp != null && !bmp.isRecycled) {
                reusableBitmapPaint.alpha = alphaInt
                native.drawBitmap(bmp, null, reusableRectF, reusableBitmapPaint)
            } else {
                reusablePaint.color = (alphaInt shl 24) or (burst.accentColor.toArgb() and 0x00FFFFFF)
                native.drawRoundRect(reusableRectF, 28f, 28f, reusablePaint)
            }
            return@Canvas
        }

        // HARMONY_DUST: 잔여 카드 영역이 침식 파동 뒤로 부드럽게 사라지는 경계 침식 렌더링
        if (burst.style == CardVanishStyle.HARMONY_DUST && t < 0.56f) {
            val waveFront = t / 0.56f
            val remAlpha = ((1f - waveFront * 1.35f).coerceIn(0f, 1f) * 255f).toInt()
            if (remAlpha > 2) {
                val keepW = (rect.width * (1f - waveFront * 1.05f + 0.16f)).coerceIn(0f, rect.width)
                native.save()
                native.clipRect(rect.left, rect.top, rect.left + keepW, rect.bottom)
                reusableRectF.set(rect.left, rect.top, rect.right, rect.bottom)
                if (bmp != null && !bmp.isRecycled) {
                    reusableBitmapPaint.alpha = remAlpha
                    native.drawBitmap(bmp, null, reusableRectF, reusableBitmapPaint)
                } else {
                    reusablePaint.color = (remAlpha shl 24) or (burst.accentColor.toArgb() and 0x00FFFFFF)
                    native.drawRoundRect(reusableRectF, 28f, 28f, reusablePaint)
                }
                native.restore()
            }
        }

        val buf = buffer ?: return@Canvas
        val n = buf.count
        for (i in 0 until n) {
            val dl = buf.delay[i]
            val lt = (t - dl) / (1f - dl)
            if (lt <= 0f || lt >= 1f) continue
            val inv = 1f - lt
            val ease = 1f - inv * inv * (if (burst.style == CardVanishStyle.HARMONY_DUST) inv.pow(0.2f) else 1f)
            val ph = buf.phase[i]
            val px = buf.startX[i] + buf.vx[i] * ease + sin(ph + lt * 6.0f) * buf.swirl[i] * lt
            val py = buf.startY[i] + buf.vy[i] * ease + cos(ph + lt * 4.5f) * 9f * lt
            var alpha = (1f - lt.pow(1.7f)).coerceIn(0f, 1f)
            if (buf.sparkle[i]) alpha = (alpha * 1.28f).coerceAtMost(1f)
            val alphaInt = (alpha * 255f).toInt()
            if (alphaInt <= 1) continue
            val rad = (buf.radius[i] * (1f - lt * 0.65f)).coerceAtLeast(0.65f)
            reusablePaint.color = (alphaInt shl 24) or buf.baseRgb[i]
            native.drawCircle(px, py, rad, reusablePaint)
        }
    }
}
