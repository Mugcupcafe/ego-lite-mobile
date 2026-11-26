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

val EgoCobalt = Color(0xFF2563EB)
val EgoEmerald = Color(0xFF10B981)
val EgoEmeraldGlow = Color(0xFF2EF2AA)
val EgoDangerRed = Color(0xFFDC2626)

// Selected vs unselected tab fallback colors
val LightTabSelected = Color(0xFFFFFFFF)
val LightTabUnselected = Color(0xFFDCE3EC)
val DarkTabStripBg = Color(0xFF0B1120)
val DarkTabSelected = Color(0xFF334155)
val DarkTabUnselected = Color(0xFF162033)

private val FallbackLightColors = lightColorScheme(
    primary = EgoCobalt,
    secondary = EgoEmerald,
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF34D399),
    background = DarkTabStripBg,
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

/**
 * Material 3 Dynamic Color (Material You) 자동 적용 테마
 * - Android 12(API 31) 이상에서는 기기에서 선택한 시스템 색상을 브라우저 뼈대 전체에 자동 반영
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
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkColors
        else -> FallbackLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
