package com.egolite.mobile.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.egolite.mobile.model.ThemeColorPreset

val EgoCobalt = Color(0xFF1A73E8)
val EgoEmerald = Color(0xFF10B981)
val EgoEmeraldGlow = Color(0xFF2EF2AA)
val EgoDangerRed = Color(0xFFD93025)

// Google Material 3 기본 주간/야간 서피스 톤
val M3LightBackground = Color(0xFFECEEF5)
val M3LightSurfaceLowest = Color(0xFFFFFFFF)
val M3LightSurfaceLow = Color(0xFFF3F5FA)
val M3LightSurfaceMid = Color(0xFFE8ECF4)
val M3LightSurfaceHigh = Color(0xFFE0E5F0)
val M3LightSurfaceHighest = Color(0xFFD5DCEB)

val M3DarkBackground = Color(0xFF181A1F)
val M3DarkSurfaceLowest = Color(0xFF131519)
val M3DarkSurfaceLow = Color(0xFF21232A)
val M3DarkSurfaceMid = Color(0xFF292C33)
val M3DarkSurfaceHigh = Color(0xFF353B4B)
val M3DarkSurfaceHighest = Color(0xFF40475A)

val LightTabSelected = M3LightSurfaceLowest
val LightTabUnselected = Color(0xFFE1E4EE)
val DarkTabStripBg = M3DarkBackground
val DarkTabSelected = M3DarkSurfaceHigh
val DarkTabUnselected = M3DarkSurfaceLow

@Immutable
data class EgoChromeColors(
    val backdropBg: Color,
    val groupSurface: Color,
    val selectedTabBg: Color,
    val unselectedTabBg: Color,
    val settingsCanvasBg: Color,
    val settingsCardBg: Color,
    val selectedNavBg: Color
)

val LocalEgoChromeColors = staticCompositionLocalOf {
    EgoChromeColors(
        backdropBg = Color(0xFF181A1F),
        groupSurface = Color(0xFF353B4B),
        selectedTabBg = Color(0xFF353B4B),
        unselectedTabBg = Color(0xFF21232A),
        settingsCanvasBg = Color(0xFF181A1F),
        settingsCardBg = Color(0xFF252932),
        selectedNavBg = Color(0xFF353B4B)
    )
}

@Immutable
data class PalettePreviewSwatch(
    val backdrop: Color,
    val group: Color,
    val accent: Color
)

fun previewSwatchForPreset(preset: ThemeColorPreset, darkTheme: Boolean): PalettePreviewSwatch {
    return if (darkTheme) {
        when (preset) {
            ThemeColorPreset.CHROME_SLATE -> PalettePreviewSwatch(Color(0xFF181A1F), Color(0xFF353B4B), Color(0xFFA8C7FA))
            ThemeColorPreset.SYSTEM_DYNAMIC -> PalettePreviewSwatch(Color(0xFF1E1F24), Color(0xFF333947), Color(0xFF9ECAFF))
            ThemeColorPreset.CHROME_NEUTRAL -> PalettePreviewSwatch(Color(0xFF1E1F24), Color(0xFF2E3138), Color(0xFFA8C7FA))
            ThemeColorPreset.CHROME_CLASSIC -> PalettePreviewSwatch(Color(0xFF1E1F24), Color(0xFF292C33), Color(0xFF8AB4F8))
            ThemeColorPreset.OCEAN_BLUE -> PalettePreviewSwatch(Color(0xFF141923), Color(0xFF26344B), Color(0xFF7CC4FF))
            ThemeColorPreset.FOREST_EMERALD -> PalettePreviewSwatch(Color(0xFF141C19), Color(0xFF243831), Color(0xFF6DD58C))
            ThemeColorPreset.ROYAL_LAVENDER -> PalettePreviewSwatch(Color(0xFF1A1723), Color(0xFF342C48), Color(0xFFD0BCFF))
            ThemeColorPreset.SUNSET_AMBER -> PalettePreviewSwatch(Color(0xFF1D1916), Color(0xFF3B3128), Color(0xFFFFB951))
            ThemeColorPreset.ROSE_CRIMSON -> PalettePreviewSwatch(Color(0xFF1E161A), Color(0xFF3F2A34), Color(0xFFFFB1C8))
            ThemeColorPreset.OLED_MIDNIGHT -> PalettePreviewSwatch(Color(0xFF000000), Color(0xFF1E2028), Color(0xFFA8C7FA))
        }
    } else {
        when (preset) {
            ThemeColorPreset.CHROME_SLATE -> PalettePreviewSwatch(Color(0xFFE7ECF6), Color(0xFFFFFFFF), Color(0xFF1A73E8))
            ThemeColorPreset.SYSTEM_DYNAMIC -> PalettePreviewSwatch(Color(0xFFECEEF5), Color(0xFFFFFFFF), Color(0xFF3B66C5))
            ThemeColorPreset.CHROME_NEUTRAL -> PalettePreviewSwatch(Color(0xFFECEEF5), Color(0xFFFFFFFF), Color(0xFF1A73E8))
            ThemeColorPreset.CHROME_CLASSIC -> PalettePreviewSwatch(Color(0xFFE8EAED), Color(0xFFFFFFFF), Color(0xFF1A73E8))
            ThemeColorPreset.OCEAN_BLUE -> PalettePreviewSwatch(Color(0xFFE4EEF8), Color(0xFFFFFFFF), Color(0xFF0B57D0))
            ThemeColorPreset.FOREST_EMERALD -> PalettePreviewSwatch(Color(0xFFE5F2EC), Color(0xFFFFFFFF), Color(0xFF0D7A4E))
            ThemeColorPreset.ROYAL_LAVENDER -> PalettePreviewSwatch(Color(0xFFEEEAF8), Color(0xFFFFFFFF), Color(0xFF6750A4))
            ThemeColorPreset.SUNSET_AMBER -> PalettePreviewSwatch(Color(0xFFF6EFE6), Color(0xFFFFFFFF), Color(0xFFB35D00))
            ThemeColorPreset.ROSE_CRIMSON -> PalettePreviewSwatch(Color(0xFFF7E9EE), Color(0xFFFFFFFF), Color(0xFFB3265A))
            ThemeColorPreset.OLED_MIDNIGHT -> PalettePreviewSwatch(Color(0xFFE5E7EE), Color(0xFFFFFFFF), Color(0xFF1A73E8))
        }
    }
}

private data class PresetSpec(
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val darkBackdrop: Color,
    val darkGroup: Color,
    val darkUnselected: Color,
    val darkCard: Color,
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val lightBackdrop: Color,
    val lightGroup: Color,
    val lightUnselected: Color
)

private fun presetSpecOf(preset: ThemeColorPreset): PresetSpec = when (preset) {
    ThemeColorPreset.CHROME_SLATE, ThemeColorPreset.SYSTEM_DYNAMIC -> PresetSpec(
        primaryDark = Color(0xFFA8C7FA),
        onPrimaryDark = Color(0xFF062E6F),
        primaryContainerDark = Color(0xFF353B4B),
        onPrimaryContainerDark = Color(0xFFE8EAED),
        darkBackdrop = Color(0xFF181A1F),
        darkGroup = Color(0xFF353B4B),
        darkUnselected = Color(0xFF21232A),
        darkCard = Color(0xFF252932),
        primaryLight = Color(0xFF1A73E8),
        primaryContainerLight = Color(0xFFDCE5F9),
        onPrimaryContainerLight = Color(0xFF0B2B5C),
        lightBackdrop = Color(0xFFECEEF5),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFE1E4EE)
    )
    ThemeColorPreset.CHROME_NEUTRAL -> PresetSpec(
        primaryDark = Color(0xFFA8C7FA),
        onPrimaryDark = Color(0xFF062E6F),
        primaryContainerDark = Color(0xFF2E3138),
        onPrimaryContainerDark = Color(0xFFE8EAED),
        darkBackdrop = Color(0xFF1E1F24),
        darkGroup = Color(0xFF2E3138),
        darkUnselected = Color(0xFF25272D),
        darkCard = Color(0xFF292C33),
        primaryLight = Color(0xFF1A73E8),
        primaryContainerLight = Color(0xFFDCE5F9),
        onPrimaryContainerLight = Color(0xFF0B2B5C),
        lightBackdrop = Color(0xFFECEEF5),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFE2E4EC)
    )
    ThemeColorPreset.CHROME_CLASSIC -> PresetSpec(
        primaryDark = Color(0xFF8AB4F8),
        onPrimaryDark = Color(0xFF062E6F),
        primaryContainerDark = Color(0xFF292C33),
        onPrimaryContainerDark = Color(0xFFE8EAED),
        darkBackdrop = Color(0xFF1E1F24),
        darkGroup = Color(0xFF292C33),
        darkUnselected = Color(0xFF23252B),
        darkCard = Color(0xFF292C33),
        primaryLight = Color(0xFF1A73E8),
        primaryContainerLight = Color(0xFFDCE5F9),
        onPrimaryContainerLight = Color(0xFF0B2B5C),
        lightBackdrop = Color(0xFFE8EAED),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFDCE0E5)
    )
    ThemeColorPreset.OCEAN_BLUE -> PresetSpec(
        primaryDark = Color(0xFF7CC4FF),
        onPrimaryDark = Color(0xFF003258),
        primaryContainerDark = Color(0xFF26344B),
        onPrimaryContainerDark = Color(0xFFD6E9FF),
        darkBackdrop = Color(0xFF141923),
        darkGroup = Color(0xFF26344B),
        darkUnselected = Color(0xFF1C2331),
        darkCard = Color(0xFF1E2736),
        primaryLight = Color(0xFF0B57D0),
        primaryContainerLight = Color(0xFFD3E3FD),
        onPrimaryContainerLight = Color(0xFF041E49),
        lightBackdrop = Color(0xFFE4EEF8),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFD6E4F3)
    )
    ThemeColorPreset.FOREST_EMERALD -> PresetSpec(
        primaryDark = Color(0xFF6DD58C),
        onPrimaryDark = Color(0xFF003919),
        primaryContainerDark = Color(0xFF243831),
        onPrimaryContainerDark = Color(0xFFD2F5DE),
        darkBackdrop = Color(0xFF141C19),
        darkGroup = Color(0xFF243831),
        darkUnselected = Color(0xFF1B2723),
        darkCard = Color(0xFF1E2B26),
        primaryLight = Color(0xFF0D7A4E),
        primaryContainerLight = Color(0xFFC8F0DA),
        onPrimaryContainerLight = Color(0xFF002112),
        lightBackdrop = Color(0xFFE5F2EC),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFD6EAE0)
    )
    ThemeColorPreset.ROYAL_LAVENDER -> PresetSpec(
        primaryDark = Color(0xFFD0BCFF),
        onPrimaryDark = Color(0xFF381E72),
        primaryContainerDark = Color(0xFF342C48),
        onPrimaryContainerDark = Color(0xFFEADDFF),
        darkBackdrop = Color(0xFF1A1723),
        darkGroup = Color(0xFF342C48),
        darkUnselected = Color(0xFF242031),
        darkCard = Color(0xFF282336),
        primaryLight = Color(0xFF6750A4),
        primaryContainerLight = Color(0xFFEADDFF),
        onPrimaryContainerLight = Color(0xFF21005D),
        lightBackdrop = Color(0xFFEEEAF8),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFE2DCF2)
    )
    ThemeColorPreset.SUNSET_AMBER -> PresetSpec(
        primaryDark = Color(0xFFFFB951),
        onPrimaryDark = Color(0xFF452B00),
        primaryContainerDark = Color(0xFF3B3128),
        onPrimaryContainerDark = Color(0xFFFFDDB3),
        darkBackdrop = Color(0xFF1D1916),
        darkGroup = Color(0xFF3B3128),
        darkUnselected = Color(0xFF28221E),
        darkCard = Color(0xFF2D2621),
        primaryLight = Color(0xFFB35D00),
        primaryContainerLight = Color(0xFFFFDDB3),
        onPrimaryContainerLight = Color(0xFF2B1700),
        lightBackdrop = Color(0xFFF6EFE6),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFEDE2D4)
    )
    ThemeColorPreset.ROSE_CRIMSON -> PresetSpec(
        primaryDark = Color(0xFFFFB1C8),
        onPrimaryDark = Color(0xFF5E1133),
        primaryContainerDark = Color(0xFF3F2A34),
        onPrimaryContainerDark = Color(0xFFFFD9E2),
        darkBackdrop = Color(0xFF1E161A),
        darkGroup = Color(0xFF3F2A34),
        darkUnselected = Color(0xFF2A2025),
        darkCard = Color(0xFF30232A),
        primaryLight = Color(0xFFB3265A),
        primaryContainerLight = Color(0xFFFFD9E2),
        onPrimaryContainerLight = Color(0xFF3E001D),
        lightBackdrop = Color(0xFFF7E9EE),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFEFDCE3)
    )
    ThemeColorPreset.OLED_MIDNIGHT -> PresetSpec(
        primaryDark = Color(0xFFA8C7FA),
        onPrimaryDark = Color(0xFF062E6F),
        primaryContainerDark = Color(0xFF1E2028),
        onPrimaryContainerDark = Color(0xFFE8EAED),
        darkBackdrop = Color(0xFF000000),
        darkGroup = Color(0xFF1E2028),
        darkUnselected = Color(0xFF121318),
        darkCard = Color(0xFF14161C),
        primaryLight = Color(0xFF1A73E8),
        primaryContainerLight = Color(0xFFDCE5F9),
        onPrimaryContainerLight = Color(0xFF0B2B5C),
        lightBackdrop = Color(0xFFE5E7EE),
        lightGroup = Color(0xFFFFFFFF),
        lightUnselected = Color(0xFFD9DCE6)
    )
}

/**
 * Material 3 Dynamic Color (Material You) + 사용자 선택 색상 팔레트 엔진
 */
@Composable
fun EgoLiteTheme(
    darkTheme: Boolean,
    colorPreset: ThemeColorPreset = ThemeColorPreset.CHROME_SLATE,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val spec = presetSpecOf(colorPreset)

    val useSystemDynamic = colorPreset == ThemeColorPreset.SYSTEM_DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = if (useSystemDynamic) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        darkColorScheme(
            primary = spec.primaryDark,
            onPrimary = spec.onPrimaryDark,
            primaryContainer = spec.primaryContainerDark,
            onPrimaryContainer = spec.onPrimaryContainerDark,
            secondary = EgoEmerald,
            onSecondary = Color(0xFF0A3818),
            background = spec.darkBackdrop,
            surface = spec.darkBackdrop,
            surfaceVariant = spec.darkGroup,
            surfaceContainerLowest = spec.darkBackdrop,
            surfaceContainerLow = spec.darkUnselected,
            surfaceContainer = spec.darkCard,
            surfaceContainerHigh = spec.darkGroup,
            surfaceContainerHighest = spec.darkGroup,
            onBackground = Color(0xFFE8EAED),
            onSurface = Color(0xFFE8EAED),
            onSurfaceVariant = Color(0xFFB4B9C4)
        )
    } else {
        lightColorScheme(
            primary = spec.primaryLight,
            onPrimary = Color.White,
            primaryContainer = spec.primaryContainerLight,
            onPrimaryContainer = spec.onPrimaryContainerLight,
            secondary = EgoEmerald,
            onSecondary = Color.White,
            background = spec.lightBackdrop,
            surface = spec.lightBackdrop,
            surfaceVariant = spec.lightUnselected,
            surfaceContainerLowest = spec.lightGroup,
            surfaceContainerLow = spec.lightGroup,
            surfaceContainer = spec.lightUnselected,
            surfaceContainerHigh = spec.lightUnselected,
            surfaceContainerHighest = spec.primaryContainerLight,
            onBackground = Color(0xFF1F1F1F),
            onSurface = Color(0xFF1F1F1F),
            onSurfaceVariant = Color(0xFF444746)
        )
    }

    val chromeColors = if (useSystemDynamic) {
        if (darkTheme) {
            EgoChromeColors(
                backdropBg = colorScheme.surfaceContainerLowest,
                groupSurface = colorScheme.surfaceContainerHigh,
                selectedTabBg = colorScheme.surfaceContainerHigh,
                unselectedTabBg = colorScheme.surfaceContainerLow,
                settingsCanvasBg = colorScheme.surfaceContainerLowest,
                settingsCardBg = colorScheme.surfaceContainer,
                selectedNavBg = colorScheme.secondaryContainer
            )
        } else {
            EgoChromeColors(
                backdropBg = colorScheme.surfaceContainer,
                groupSurface = colorScheme.surfaceContainerLowest,
                selectedTabBg = colorScheme.surfaceContainerLowest,
                unselectedTabBg = colorScheme.surfaceContainerHigh,
                settingsCanvasBg = colorScheme.surfaceContainer,
                settingsCardBg = colorScheme.surfaceContainerLowest,
                selectedNavBg = colorScheme.secondaryContainer
            )
        }
    } else if (darkTheme) {
        EgoChromeColors(
            backdropBg = spec.darkBackdrop,
            groupSurface = spec.darkGroup,
            selectedTabBg = spec.darkGroup,
            unselectedTabBg = spec.darkUnselected,
            settingsCanvasBg = spec.darkBackdrop,
            settingsCardBg = spec.darkCard,
            selectedNavBg = spec.darkGroup
        )
    } else {
        EgoChromeColors(
            backdropBg = spec.lightBackdrop,
            groupSurface = spec.lightGroup,
            selectedTabBg = spec.lightGroup,
            unselectedTabBg = spec.lightUnselected,
            settingsCanvasBg = spec.lightBackdrop,
            settingsCardBg = spec.lightGroup,
            selectedNavBg = spec.primaryContainerLight
        )
    }

    CompositionLocalProvider(LocalEgoChromeColors provides chromeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
