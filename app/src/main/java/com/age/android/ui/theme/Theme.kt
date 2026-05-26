/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.age.android.core.data.ThemeAccent
import com.age.android.core.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AgeGreenLight,
    onPrimary = AgeBlack,
    primaryContainer = Color(0xFF0B3024),
    onPrimaryContainer = Color(0xFFC7F5DE),
    secondary = AgeCyanLight,
    onSecondary = AgeBlack,
    secondaryContainer = Color(0xFF082C33),
    onSecondaryContainer = Color(0xFFC7F4FB),
    tertiary = Color(0xFFE7C172),
    onTertiary = AgeBlack,
    tertiaryContainer = Color(0xFF352507),
    onTertiaryContainer = Color(0xFFFFE5AF),
    background = AgeBlack,
    onBackground = AgeDarkText,
    surface = AgeBlackSurface,
    onSurface = AgeDarkText,
    surfaceVariant = AgeBlackSurfaceVariant,
    onSurfaceVariant = AgeDarkTextMuted,
    surfaceDim = AgeBlack,
    surfaceBright = Color(0xFF161D1A),
    surfaceContainerLowest = AgeBlack,
    surfaceContainerLow = AgeBlackSurface,
    surfaceContainer = Color(0xFF090D0B),
    surfaceContainerHigh = AgeBlackSurfaceVariant,
    surfaceContainerHighest = Color(0xFF17201C),
    outline = Color.White.copy(alpha = 0.07f),
    outlineVariant = Color.White.copy(alpha = 0.04f),
    inverseSurface = AgeDarkText,
    inverseOnSurface = AgeBlack,
    inversePrimary = AgeGreen,
    surfaceTint = Color.Transparent,
    scrim = AgeBlack,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF3C0808),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = AgeGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF5E4),
    onPrimaryContainer = Color(0xFF003827),
    secondary = AgeCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F2FA),
    onSecondaryContainer = Color(0xFF003640),
    tertiary = Color(0xFF8B5A00),
    background = Color(0xFFF3F6FA),
    onBackground = AgeInk,
    surface = Color(0xFFFAFAFA),
    onSurface = AgeInk,
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = AgeSlate,
    outline = Color(0xFF6F837A)
)

@Composable
fun AgeAndroidTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeAccent: ThemeAccent = ThemeAccent.LIQUID_DEFAULT,
    glassEffectEnabled: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorSchemeFor(themeAccent, glassEffectEnabled)
        else -> lightColorSchemeFor(themeAccent, glassEffectEnabled)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun lightColorSchemeFor(accent: ThemeAccent, glassEffectEnabled: Boolean) = LightColorScheme.copy(
    primary = accent.lightPrimary,
    primaryContainer = accent.lightPrimaryContainer,
    onPrimaryContainer = accent.lightOnPrimaryContainer,
    secondary = accent.lightSecondary,
    secondaryContainer = accent.lightSecondaryContainer,
    onSecondaryContainer = accent.lightOnSecondaryContainer,
    tertiary = accent.lightTertiary,
    background = Color(0xFFF3F6FA),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFE8E8E8),
    surfaceDim = Color(0xFFF7F7F7),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFCFC),
    surfaceContainer = Color(0xFFF8F8F8),
    surfaceContainerHigh = Color(0xFFF3F3F3),
    surfaceContainerHighest = Color(0xFFEDEDED),
    outline = accent.lightOutline
)

private fun darkColorSchemeFor(accent: ThemeAccent, glassEffectEnabled: Boolean) = DarkColorScheme.copy(
    primary = accent.darkPrimary,
    primaryContainer = accent.darkPrimaryContainer,
    onPrimaryContainer = accent.darkOnPrimaryContainer,
    secondary = accent.darkSecondary,
    secondaryContainer = accent.darkSecondaryContainer,
    onSecondaryContainer = accent.darkOnSecondaryContainer,
    tertiary = accent.darkTertiary,
    background = Color.Black,
    surface = Color(0xFF050505),
    surfaceVariant = Color(0xFF151515),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF181818),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF0B0B0B),
    surfaceContainerHigh = Color(0xFF111111),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    inversePrimary = accent.lightPrimary
)

private val ThemeAccent.lightPrimary: Color
    get() = when (this) {
        ThemeAccent.LIQUID_DEFAULT -> AgeGreen
        ThemeAccent.LIQUID_AURORA -> Color(0xFF3D6FE8)
        ThemeAccent.LIQUID_SUNRISE -> Color(0xFFC55240)
        ThemeAccent.LIQUID_OCEAN -> Color(0xFF007C8E)
        ThemeAccent.LIQUID_GRAPE -> Color(0xFF8A3FB0)
        ThemeAccent.SOLID_GREEN -> Color(0xFF0E8F68)
        ThemeAccent.SOLID_BLUE -> Color(0xFF1565C0)
        ThemeAccent.SOLID_RED -> Color(0xFFC62828)
        ThemeAccent.SOLID_PURPLE -> Color(0xFF6A1B9A)
        ThemeAccent.SOLID_ORANGE -> Color(0xFFEF6C00)
    }

private val ThemeAccent.lightSecondary: Color
    get() = if (liquidGlass) when (this) {
        ThemeAccent.LIQUID_DEFAULT -> AgeCyan
        ThemeAccent.LIQUID_AURORA -> Color(0xFF9B5DE5)
        ThemeAccent.LIQUID_SUNRISE -> Color(0xFFD64C7F)
        ThemeAccent.LIQUID_OCEAN -> Color(0xFF16A084)
        ThemeAccent.LIQUID_GRAPE -> Color(0xFFCC4778)
        else -> lightPrimary
    } else {
        lightPrimary
    }

private val ThemeAccent.lightTertiary: Color
    get() = if (liquidGlass) when (this) {
        ThemeAccent.LIQUID_DEFAULT -> Color(0xFF8B5A00)
        ThemeAccent.LIQUID_AURORA -> Color(0xFF0077B6)
        ThemeAccent.LIQUID_SUNRISE -> Color(0xFFA85F00)
        ThemeAccent.LIQUID_OCEAN -> Color(0xFF326FBA)
        ThemeAccent.LIQUID_GRAPE -> Color(0xFF7E57C2)
        else -> lightPrimary
    } else {
        lightPrimary
    }

private val ThemeAccent.lightPrimaryContainer: Color get() = lightPrimary.blendWith(Color.White, 0.78f)
private val ThemeAccent.lightSecondaryContainer: Color get() = lightSecondary.blendWith(Color.White, 0.80f)
private val ThemeAccent.lightOnPrimaryContainer: Color get() = lightPrimary.blendWith(Color.Black, 0.55f)
private val ThemeAccent.lightOnSecondaryContainer: Color get() = lightSecondary.blendWith(Color.Black, 0.55f)
private val ThemeAccent.lightBackground: Color get() = if (liquidGlass) lightPrimary.blendWith(Color.White, 0.94f) else Color(0xFFF8FAFB)
private val ThemeAccent.lightSurface: Color get() = if (liquidGlass) lightSecondary.blendWith(Color.White, 0.96f) else Color(0xFFFCFCFD)
private val ThemeAccent.lightSurfaceVariant: Color get() = lightPrimary.blendWith(Color.White, 0.86f)
private val ThemeAccent.lightOutline: Color get() = lightPrimary.blendWith(Color(0xFF6B7280), 0.50f)

private val ThemeAccent.darkPrimary: Color get() = lightPrimary.blendWith(Color.White, 0.48f)
private val ThemeAccent.darkSecondary: Color get() = lightSecondary.blendWith(Color.White, 0.48f)
private val ThemeAccent.darkTertiary: Color get() = lightTertiary.blendWith(Color.White, 0.44f)
private val ThemeAccent.darkPrimaryContainer: Color get() = lightPrimary.blendWith(Color.Black, 0.68f)
private val ThemeAccent.darkSecondaryContainer: Color get() = lightSecondary.blendWith(Color.Black, 0.70f)
private val ThemeAccent.darkOnPrimaryContainer: Color get() = darkPrimary.blendWith(Color.White, 0.58f)
private val ThemeAccent.darkOnSecondaryContainer: Color get() = darkSecondary.blendWith(Color.White, 0.58f)
private val ThemeAccent.darkSurfaceBright: Color get() = lightPrimary.blendWith(Color(0xFF101418), 0.18f)
private val ThemeAccent.darkSurfaceContainer: Color get() = lightPrimary.blendWith(Color(0xFF070A0D), 0.08f)
private val ThemeAccent.darkSurfaceContainerHigh: Color get() = lightPrimary.blendWith(Color(0xFF0B1014), 0.13f)
private val ThemeAccent.darkSurfaceContainerHighest: Color get() = lightSecondary.blendWith(Color(0xFF111820), 0.18f)

private fun Color.blendWith(other: Color, otherFraction: Float): Color {
    val selfFraction = 1f - otherFraction
    return Color(
        red = red * selfFraction + other.red * otherFraction,
        green = green * selfFraction + other.green * otherFraction,
        blue = blue * selfFraction + other.blue * otherFraction,
        alpha = alpha * selfFraction + other.alpha * otherFraction
    )
}
