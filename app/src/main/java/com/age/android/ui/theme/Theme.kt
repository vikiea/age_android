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
    background = Color(0xFFF5FBF7),
    onBackground = AgeInk,
    surface = Color(0xFFF9FEFB),
    onSurface = AgeInk,
    surfaceVariant = Color(0xFFDDEAE4),
    onSurfaceVariant = AgeSlate,
    outline = Color(0xFF6F837A)
)

@Composable
fun AgeAndroidTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
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
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
