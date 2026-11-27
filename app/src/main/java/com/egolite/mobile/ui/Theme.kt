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
    val s = presetSpecOf(preset)
    return if (darkTheme) {
        PalettePreviewSwatch(s.darkBackdrop, s.darkGroup, s.primaryDark)
    } else {
        PalettePreviewSwatch(s.lightBackdrop, s.lightGroup, s.primaryLight)
    }
}


private fun tonalSpec(
    darkAccent: Long,
    onDarkAccent: Long,
    darkBg: Long,
    darkGroup: Long,
    darkUnselected: Long,
    darkCard: Long,
    lightAccent: Long,
    lightContainer: Long,
    onLightContainer: Long,
    lightBg: Long,
    lightUnselected: Long
): PresetSpec = PresetSpec(
    primaryDark = Color(darkAccent),
    onPrimaryDark = Color(onDarkAccent),
    primaryContainerDark = Color(darkGroup),
    onPrimaryContainerDark = Color(0xFFF1F4FA),
    darkBackdrop = Color(darkBg),
    darkGroup = Color(darkGroup),
    darkUnselected = Color(darkUnselected),
    darkCard = Color(darkCard),
    primaryLight = Color(lightAccent),
    primaryContainerLight = Color(lightContainer),
    onPrimaryContainerLight = Color(onLightContainer),
    lightBackdrop = Color(lightBg),
    lightGroup = Color(0xFFFFFFFF),
    lightUnselected = Color(lightUnselected)
)

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
    ThemeColorPreset.SAPPHIRE_DEEP -> tonalSpec(0xFF6BB4FF, 0xFF002E5C, 0xFF101726, 0xFF203252, 0xFF172238, 0xFF1A2740, 0xFF0057B8, 0xFFD4E6FF, 0xFF001C3B, 0xFFE5EEFA, 0xFFD8E4F5)
    ThemeColorPreset.NORDIC_ICE -> tonalSpec(0xFF88D4F5, 0xFF003547, 0xFF141B21, 0xFF253540, 0xFF1B262F, 0xFF1F2B35, 0xFF006783, 0xFFC5EBFA, 0xFF001F2A, 0xFFE7F2F7, 0xFFD9E9F0)
    ThemeColorPreset.CYBER_CYAN -> tonalSpec(0xFF4EEAF5, 0xFF00363A, 0xFF0E1A1E, 0xFF1B363E, 0xFF14272D, 0xFF172C33, 0xFF006972, 0xFFB8F3F8, 0xFF002023, 0xFFE3F5F7, 0xFFD4ECEF)
    ThemeColorPreset.INDIGO_NIGHT -> tonalSpec(0xFFA5B4FC, 0xFF1E1B5E, 0xFF141524, 0xFF282B4A, 0xFF1C1E34, 0xFF20233C, 0xFF4338CA, 0xFFE0E7FF, 0xFF1E1B4B, 0xFFEAECFA, 0xFFDFE2F5)
    ThemeColorPreset.COBALT_MIST -> tonalSpec(0xFF93C5FD, 0xFF0C2D57, 0xFF151A24, 0xFF283449, 0xFF1D2434, 0xFF222B3C, 0xFF2563EB, 0xFFDBEAFE, 0xFF1E3A8A, 0xFFE8F0FB, 0xFFDBE6F7)
    ThemeColorPreset.MINT_BREEZE -> tonalSpec(0xFF6EE7B7, 0xFF003826, 0xFF111C18, 0xFF203A32, 0xFF172923, 0xFF1B3029, 0xFF059669, 0xFFD1FAE5, 0xFF064E3B, 0xFFE6F6F0, 0xFFD7EEE5)
    ThemeColorPreset.JADE_SPRING -> tonalSpec(0xFF5EEAD4, 0xFF003731, 0xFF101C1B, 0xFF1E3936, 0xFF162927, 0xFF1A302D, 0xFF0D9488, 0xFFCCFBF1, 0xFF134E4A, 0xFFE4F6F4, 0xFFD5EDE9)
    ThemeColorPreset.SAGE_BOTANIC -> tonalSpec(0xFFA3D9A5, 0xFF113815, 0xFF161C17, 0xFF2A382B, 0xFF1E281F, 0xFF232E24, 0xFF3A7D44, 0xFFD8F3DC, 0xFF1B4332, 0xFFEBF3EC, 0xFFDFEBDF)
    ThemeColorPreset.TEAL_LAGOON -> tonalSpec(0xFF67E8F9, 0xFF003640, 0xFF101A1D, 0xFF1E363D, 0xFF16272C, 0xFF1A2D33, 0xFF0891B2, 0xFFCFFAFE, 0xFF164E63, 0xFFE4F4F7, 0xFFD6ECF0)
    ThemeColorPreset.MATCHA_LEAF -> tonalSpec(0xFFBEF264, 0xFF263800, 0xFF171C13, 0xFF2D3822, 0xFF202819, 0xFF252E1D, 0xFF4D7C0F, 0xFFECFCCB, 0xFF1A2E05, 0xFFEFF5E7, 0xFFE3EDD7)
    ThemeColorPreset.SAKURA_BLUSH -> tonalSpec(0xFFFBCFE8, 0xFF591438, 0xFF1E171B, 0xFF3D2B35, 0xFF2A1F25, 0xFF31242B, 0xFFDB2777, 0xFFFCE7F3, 0xFF500724, 0xFFF9EDF3, 0xFFF2DFEA)
    ThemeColorPreset.CORAL_PEACH -> tonalSpec(0xFFFDA4AF, 0xFF5C1120, 0xFF1F1718, 0xFF402B2E, 0xFF2C1F21, 0xFF332426, 0xFFE11D48, 0xFFFFE4E6, 0xFF4C0519, 0xFFF9ECEE, 0xFFF2DFE2)
    ThemeColorPreset.RUBY_VELVET -> tonalSpec(0xFFFF8A9B, 0xFF630A1C, 0xFF1E1417, 0xFF42242C, 0xFF2D1A20, 0xFF341F25, 0xFFBE123C, 0xFFFFE4E8, 0xFF4C0519, 0xFFF8EAEF, 0xFFF0DCE2)
    ThemeColorPreset.BERRY_PLUM -> tonalSpec(0xFFF0ABFC, 0xFF50105C, 0xFF1D1520, 0xFF3B2842, 0xFF291D2E, 0xFF302236, 0xFFA21CAF, 0xFFFAE8FF, 0xFF4A044E, 0xFFF6EBF9, 0xFFEDDDF3)
    ThemeColorPreset.MAGENTA_NEON -> tonalSpec(0xFFF472B6, 0xFF5C0B36, 0xFF1F141B, 0xFF422538, 0xFF2E1A27, 0xFF351F2E, 0xFFBE185D, 0xFFFCE7F3, 0xFF500724, 0xFFF9EBF3, 0xFFF1DCE8)
    ThemeColorPreset.AMETHYST_HAZE -> tonalSpec(0xFFC4B5FD, 0xFF2E186B, 0xFF181622, 0xFF312B47, 0xFF221E32, 0xFF272339, 0xFF7C3AED, 0xFFEDE9FE, 0xFF2E1065, 0xFFF0EDFA, 0xFFE5E0F5)
    ThemeColorPreset.ORCHID_DUSK -> tonalSpec(0xFFD8B4FE, 0xFF3F1669, 0xFF1B1622, 0xFF372A47, 0xFF261E31, 0xFF2C2339, 0xFF9333EA, 0xFFF3E8FF, 0xFF3B0764, 0xFFF3EDFA, 0xFFE9E0F5)
    ThemeColorPreset.LILAC_CLOUD -> tonalSpec(0xFFE9D5FF, 0xFF3E1C61, 0xFF1A1721, 0xFF352E44, 0xFF252030, 0xFF2B2537, 0xFF7E22CE, 0xFFF3E8FF, 0xFF3B0764, 0xFFF2EEF9, 0xFFE8E2F4)
    ThemeColorPreset.WARM_TERRACOTTA -> tonalSpec(0xFFFDBA74, 0xFF572600, 0xFF1F1814, 0xFF403026, 0xFF2C221B, 0xFF332720, 0xFFEA580C, 0xFFFFEDD5, 0xFF431407, 0xFFF9EFE8, 0xFFF2E3D8)
    ThemeColorPreset.HONEY_GOLD -> tonalSpec(0xFFFCD34D, 0xFF4D3500, 0xFF1D1A13, 0xFF3C3422, 0xFF292418, 0xFF302A1D, 0xFFD97706, 0xFFFEF3C7, 0xFF451A03, 0xFFF8F3E6, 0xFFF0E8D5)
    ThemeColorPreset.MOCHA_ESPRESSO -> tonalSpec(0xFFD6B499, 0xFF3E2614, 0xFF1B1715, 0xFF382F2A, 0xFF26201D, 0xFF2C2522, 0xFF8C5327, 0xFFF3E4D7, 0xFF361D0B, 0xFFF4EFEA, 0xFFEBE2DA)
    ThemeColorPreset.GRAPHITE_MONO -> tonalSpec(0xFFD4D7DF, 0xFF1C1E24, 0xFF16171A, 0xFF2C2E34, 0xFF1F2025, 0xFF24262B, 0xFF475569, 0xFFE2E8F0, 0xFF0F172A, 0xFFECEEF2, 0xFFE0E3EA)
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
