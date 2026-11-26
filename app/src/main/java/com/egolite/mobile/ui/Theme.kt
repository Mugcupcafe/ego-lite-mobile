package com.egolite.mobile.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val EgoCobalt = Color(0xFF1A73E8)
val EgoEmerald = Color(0xFF10B981)
val EgoEmeraldGlow = Color(0xFF2EF2AA)
val EgoDangerRed = Color(0xFFD93025)

// Google Material 3 표준 주간/야간 배경 및 서피스 톤
val M3LightBackground = Color(0xFFF8F9FA)
val M3LightSurfaceLowest = Color(0xFFFFFFFF)
val M3LightSurfaceLow = Color(0xFFF1F3F4)
val M3LightSurfaceMid = Color(0xFFE8EAED)
val M3LightSurfaceHigh = Color(0xFFE2E5EA)
val M3LightSurfaceHighest = Color(0xFFDADCE0)

val M3DarkBackground = Color(0xFF131314)
val M3DarkSurfaceLowest = Color(0xFF0E0E0F)
val M3DarkSurfaceLow = Color(0xFF1B1B1D)
val M3DarkSurfaceMid = Color(0xFF1E1F22)
val M3DarkSurfaceHigh = Color(0xFF282A2C)
val M3DarkSurfaceHighest = Color(0xFF333537)

// Selected vs unselected tab fallback colors
val LightTabSelected = M3LightSurfaceLowest
val LightTabUnselected = M3LightSurfaceMid
val DarkTabStripBg = M3DarkBackground
val DarkTabSelected = M3DarkSurfaceHigh
val DarkTabUnselected = M3DarkSurfaceLow

private val FallbackLightColors = lightColorScheme(
    primary = EgoCobalt,
    secondary = EgoEmerald,
    background = M3LightBackground,
    surface = M3LightBackground,
    surfaceVariant = M3LightSurfaceMid,
    surfaceContainerLowest = M3LightSurfaceLowest,
    surfaceContainerLow = M3LightSurfaceLow,
    surfaceContainer = M3LightSurfaceMid,
    surfaceContainerHigh = M3LightSurfaceHigh,
    surfaceContainerHighest = M3LightSurfaceHighest,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F1F1F),
    onSurface = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF444746)
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    secondary = Color(0xFF6DD58C),
    background = M3DarkBackground,
    surface = M3DarkBackground,
    surfaceVariant = M3DarkSurfaceHigh,
    surfaceContainerLowest = M3DarkSurfaceLowest,
    surfaceContainerLow = M3DarkSurfaceLow,
    surfaceContainer = M3DarkSurfaceMid,
    surfaceContainerHigh = M3DarkSurfaceHigh,
    surfaceContainerHighest = M3DarkSurfaceHighest,
    onPrimary = Color(0xFF062E6F),
    onSecondary = Color(0xFF0A3818),
    onBackground = Color(0xFFE3E3E3),
    onSurface = Color(0xFFE3E3E3),
    onSurfaceVariant = Color(0xFFC4C7C5)
)

/**
 * Material 3 Dynamic Color (Material You) + Google Material 3 표준 뉴트럴 주간/야간 배경
 */
@Composable
fun EgoLiteTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(
                    background = M3DarkBackground,
                    surface = M3DarkBackground,
                    surfaceContainerLowest = M3DarkSurfaceLowest,
                    surfaceContainerLow = M3DarkSurfaceLow,
                    surfaceContainer = M3DarkSurfaceMid,
                    surfaceContainerHigh = M3DarkSurfaceHigh,
                    surfaceContainerHighest = M3DarkSurfaceHighest
                )
            } else {
                dynamicLightColorScheme(context).copy(
                    background = M3LightBackground,
                    surface = M3LightBackground,
                    surfaceContainerLowest = M3LightSurfaceLowest,
                    surfaceContainerLow = M3LightSurfaceLow,
                    surfaceContainer = M3LightSurfaceMid,
                    surfaceContainerHigh = M3LightSurfaceHigh,
                    surfaceContainerHighest = M3LightSurfaceHighest
                )
            }
        }
        darkTheme -> FallbackDarkColors
        else -> FallbackLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
