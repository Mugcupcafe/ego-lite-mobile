package com.egolite.mobile.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
import com.egolite.mobile.model.CardVanishStyle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class VanishSnapshot(
    val rect: Rect? = null,
    val touchRoot: Offset? = null,
    val bitmap: Bitmap? = null
)

data class DustVanishRequest(
    val id: Long,
    val rect: Rect,
    val accentColor: Color,
    val thumbnail: Bitmap?,
    val style: CardVanishStyle = CardVanishStyle.HARMONY_DUST,
    val particleCount: Int = 1800,
    val durationMs: Int = 1300,
    val originX: Float = -1f,
    val originY: Float = -1f,
    val maxFps: Int = 60
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
    val sparkle: BooleanArray,
    val zDepth: FloatArray
)

// Android 13+ AGSL GPU 셰이더: 터치 기준 사방 FBM 노이즈 침식 + 미세 충격파 왜곡 + 경계 발광
private const val HARMONY_DISSOLVE_AGSL = """
uniform shader content;
uniform float2 resolution;
uniform float2 origin;
uniform float progress;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    return vnoise(p) * 0.58 + vnoise(p * 2.15 + float2(7.3, 3.1)) * 0.28 + vnoise(p * 4.3 + float2(11.7, 5.9)) * 0.14;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / max(resolution, float2(1.0));
    float2 delta = uv - origin;
    float aspect = resolution.x / max(resolution.y, 1.0);
    float dist = length(float2(delta.x * aspect, delta.y)) / max(1.0, aspect * 0.95);
    float n = fbm(uv * float2(6.5, 6.5));
    float threshold = clamp(dist * 0.72 + n * 0.30, 0.01, 0.98);
    float front = progress * 1.48;

    if (front > threshold) {
        return half4(0.0);
    }

    float edgeDist = threshold - front;
    float rimWidth = 0.085;
    float2 sampleCoord = fragCoord;

    // 경계선 직전 미세 충격파(Shockwave) 픽셀 밀림
    if (edgeDist < rimWidth) {
        float push = (1.0 - edgeDist / rimWidth) * 5.0;
        float2 dir = normalize(delta + float2(0.0001));
        sampleCoord = clamp(fragCoord - dir * push, float2(0.0), resolution - float2(1.0));
    }

    half4 col = content.eval(sampleCoord);
    if (col.a < 0.02) {
        return half4(0.0);
    }

    // 침식 직전 경계 발광 띠
    if (edgeDist < rimWidth) {
        float g = 1.0 - (edgeDist / rimWidth);
        float alphaFade = clamp(edgeDist / (rimWidth * 0.32), 0.0, 1.0);
        half3 glowTint = half3(0.72, 0.86, 1.0);
        col.rgb = mix(col.rgb, glowTint, half(g * 0.65));
        col *= half(alphaFade);
    }
    return col;
}
"""

private fun cpuHash2(x: Float, y: Float): Float {
    val s = sin(x * 127.1f + y * 311.7f) * 43758.5453f
    return s - floor(s)
}

private fun cpuNoise2(x: Float, y: Float): Float {
    val ix = floor(x)
    val iy = floor(y)
    val fx = x - ix
    val fy = y - iy
    val ux = fx * fx * (3f - 2f * fx)
    val uy = fy * fy * (3f - 2f * fy)
    val a = cpuHash2(ix, iy)
    val b = cpuHash2(ix + 1f, iy)
    val c = cpuHash2(ix, iy + 1f)
    val d = cpuHash2(ix + 1f, iy + 1f)
    return a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy
}

private fun cpuFbm(x: Float, y: Float): Float =
    cpuNoise2(x, y) * 0.58f + cpuNoise2(x * 2.15f + 7.3f, y * 2.15f + 3.1f) * 0.28f + cpuNoise2(x * 4.3f + 11.7f, y * 4.3f + 5.9f) * 0.14f

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
    var animProgress by remember(burst.id) { mutableFloatStateOf(0f) }
    val reusablePaint = remember(burst.id) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val reusableAdditivePaint = remember(burst.id) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
    }
    val reusableBitmapPaint = remember(burst.id) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
    val reusableShaderPaint = remember(burst.id) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
    val reusableRectF = remember(burst.id) { RectF() }

    val effectiveDuration = when (burst.style) {
        CardVanishStyle.HARMONY_DUST -> burst.durationMs.coerceIn(500, 2200)
        CardVanishStyle.HARMONY_GPU_SHADER -> burst.durationMs.coerceIn(550, 2200)
        CardVanishStyle.LIGHT_PARTICLES -> 560
        CardVanishStyle.SCALE_FADE -> 240
        CardVanishStyle.OFF -> 1
    }

    val normOriginX = remember(burst.id) {
        if (burst.originX >= 0f && burst.rect.width > 1f) {
            ((burst.originX - burst.rect.left) / burst.rect.width).coerceIn(0f, 1f)
        } else 0.88f
    }
    val normOriginY = remember(burst.id) {
        if (burst.originY >= 0f && burst.rect.height > 1f) {
            ((burst.originY - burst.rect.top) / burst.rect.height).coerceIn(0f, 1f)
        } else 0.18f
    }

    val gpuRuntimeShader = remember(burst.id) {
        if (burst.style == CardVanishStyle.HARMONY_GPU_SHADER &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            burst.thumbnail != null &&
            !burst.thumbnail.isRecycled
        ) {
            runCatching {
                val bmp = burst.thumbnail
                val shader = RuntimeShader(HARMONY_DISSOLVE_AGSL)
                val bmpShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val mat = Matrix().apply {
                    setScale(
                        burst.rect.width / bmp.width.coerceAtLeast(1).toFloat(),
                        burst.rect.height / bmp.height.coerceAtLeast(1).toFloat()
                    )
                }
                bmpShader.setLocalMatrix(mat)
                shader.setInputShader("content", bmpShader)
                shader.setFloatUniform("resolution", burst.rect.width.coerceAtLeast(1f), burst.rect.height.coerceAtLeast(1f))
                shader.setFloatUniform("origin", normOriginX, normOriginY)
                shader
            }.getOrNull()
        } else null
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

            when (burst.style) {
                CardVanishStyle.LIGHT_PARTICLES -> {
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
                    val zd = FloatArray(n) { 1f }
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
                            } else accentRgb
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
                    LosslessParticleBuffer(n, sx, sy, vx, vy, ph, sw, dl, rd, rgb, sp, zd)
                }

                CardVanishStyle.HARMONY_GPU_SHADER -> {
                    val n = (burst.particleCount * 1.35f).toInt().coerceIn(1400, 4200)
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
                    val zd = FloatArray(n)
                    val aspect = (rect.width / rect.height.coerceAtLeast(1f)).coerceAtLeast(0.5f)
                    val oxPx = rect.left + normOriginX * rect.width
                    val oyPx = rect.top + normOriginY * rect.height

                    for (i in 0 until n) {
                        val u = rng.nextFloat()
                        val v = rng.nextFloat()
                        val px = rect.left + u * rect.width
                        val py = rect.top + v * rect.height
                        sx[i] = px
                        sy[i] = py

                        var sampledRgb = if (v > 0.72f) accentRgb else (if (rng.nextFloat() < 0.38f) accentRgb else 0x00CBD5E1)
                        if (hasBmp) {
                            val bx = (u * (bmp!!.width - 1)).toInt().coerceIn(0, bmp.width - 1)
                            val by = (v * (bmp.height - 1)).toInt().coerceIn(0, bmp.height - 1)
                            val pixel = runCatching { bmp.getPixel(bx, by) }.getOrDefault(0)
                            if ((pixel ushr 24) > 20) {
                                sampledRgb = pixel and 0x00FFFFFF
                            }
                        }
                        rgb[i] = sampledRgb

                        val du = (u - normOriginX) * aspect
                        val dv = v - normOriginY
                        val dist = hypot(du, dv) / (aspect * 0.95f).coerceAtLeast(1f)
                        val noiseVal = cpuFbm(u * 6.5f, v * 6.5f)
                        val thresh = (dist * 0.72f + noiseVal * 0.30f).coerceIn(0.01f, 0.96f)
                        dl[i] = (thresh * 0.56f).coerceIn(0f, 0.62f)

                        val dirAngle = atan2(py - oyPx, px - oxPx) + (rng.nextFloat() - 0.5f) * 0.75f
                        val z = 0.55f + rng.nextFloat() * 1.25f
                        zd[i] = z
                        val impulse = (75f + rng.nextFloat() * 210f) * z
                        vx[i] = cos(dirAngle) * impulse
                        vy[i] = sin(dirAngle) * impulse - 24f * z
                        ph[i] = rng.nextFloat() * 6.283f
                        sw[i] = (rng.nextFloat() - 0.5f) * 28f
                        val isBokeh = rng.nextFloat() < 0.14f
                        sp[i] = isBokeh
                        rd[i] = if (isBokeh) (1.9f + rng.nextFloat() * 2.1f) * z else (0.95f + rng.nextFloat() * 1.45f) * z
                    }
                    LosslessParticleBuffer(n, sx, sy, vx, vy, ph, sw, dl, rd, rgb, sp, zd)
                }

                else -> {
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
                    val zd = FloatArray(n) { 1f }
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
                    LosslessParticleBuffer(n, sx, sy, vx, vy, ph, sw, dl, rd, rgb, sp, zd)
                }
            }
        }
    }

    LaunchedEffect(burst.id) {
        if (burst.style == CardVanishStyle.OFF) {
            onFinished()
            return@LaunchedEffect
        }
        val durationNs = effectiveDuration.toLong() * 1_000_000L
        val minStepNs = if (burst.maxFps > 0) 1_000_000_000L / burst.maxFps.coerceIn(30, 144) else 0L
        var startNs = -1L
        var lastRenderNs = 0L
        while (true) {
            val done = withFrameNanos { frameNs ->
                if (startNs < 0L) {
                    startNs = frameNs
                    lastRenderNs = frameNs
                    animProgress = 0f
                    false
                } else {
                    val elapsed = frameNs - startNs
                    val nextP = (elapsed.toFloat() / durationNs.toFloat()).coerceIn(0f, 1f)
                    if (nextP >= 1f) {
                        animProgress = 1f
                        true
                    } else {
                        if (minStepNs == 0L || (frameNs - lastRenderNs) >= minStepNs) {
                            lastRenderNs = frameNs
                            animProgress = nextP
                        }
                        false
                    }
                }
            }
            if (done) break
        }
        onFinished()
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .zIndex(45f)
    ) {
        val t = animProgress
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

        if (burst.style == CardVanishStyle.HARMONY_GPU_SHADER && t < 0.72f) {
            if (gpuRuntimeShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gpuRuntimeShader.setFloatUniform("progress", t)
                reusableShaderPaint.shader = gpuRuntimeShader
                native.save()
                native.translate(rect.left, rect.top)
                native.drawRect(0f, 0f, rect.width, rect.height, reusableShaderPaint)
                native.restore()
            } else if (bmp != null && !bmp.isRecycled) {
                val remAlpha = ((1f - t / 0.55f).coerceIn(0f, 1f) * 255f).toInt()
                if (remAlpha > 2) {
                    reusableRectF.set(rect.left, rect.top, rect.right, rect.bottom)
                    reusableBitmapPaint.alpha = remAlpha
                    native.drawBitmap(bmp, null, reusableRectF, reusableBitmapPaint)
                }
            }
        }

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
        val isGpuHarmony = burst.style == CardVanishStyle.HARMONY_GPU_SHADER

        for (i in 0 until n) {
            val dl = buf.delay[i]
            val lt = (t - dl) / (1f - dl)
            if (lt <= 0f || lt >= 1f) continue

            val ph = buf.phase[i]
            if (isGpuHarmony) {
                val dragEase = 1f - exp(-4.2f * lt)
                val curlX = sin(ph + lt * 5.2f) * buf.swirl[i] * lt
                val curlY = cos(ph + lt * 4.4f) * 12f * lt
                val px = buf.startX[i] + buf.vx[i] * dragEase + curlX
                val py = buf.startY[i] + buf.vy[i] * dragEase + curlY
                val fade = (1f - lt.pow(1.45f)).coerceIn(0f, 1f)
                val alphaInt = (fade * (if (buf.sparkle[i]) 205f else 240f)).toInt()
                if (alphaInt <= 1) continue

                val z = buf.zDepth[i]
                if (buf.sparkle[i]) {
                    val rad = (buf.radius[i] * (0.9f + lt * 0.45f * z)).coerceAtLeast(0.8f)
                    reusableAdditivePaint.color = (alphaInt shl 24) or buf.baseRgb[i]
                    native.drawCircle(px, py, rad, reusableAdditivePaint)
                } else {
                    val rad = (buf.radius[i] * (1.05f - lt * 0.58f)).coerceAtLeast(0.55f)
                    reusablePaint.color = (alphaInt shl 24) or buf.baseRgb[i]
                    native.drawCircle(px, py, rad, reusablePaint)
                }
            } else {
                val inv = 1f - lt
                val ease = 1f - inv * inv * (if (burst.style == CardVanishStyle.HARMONY_DUST) inv.pow(0.2f) else 1f)
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
}
