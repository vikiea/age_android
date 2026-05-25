/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.ui.glass

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

val LocalGlassEffectEnabled = compositionLocalOf { true }

object GlassDefaults {
    val ScreenPadding = 16.dp
    val SectionSpacing = 14.dp
    val PanelShape @Composable get() = MaterialTheme.shapes.large
    val CompactShape @Composable get() = MaterialTheme.shapes.medium
    val NavShape @Composable get() = MaterialTheme.shapes.large

    val supportsBackdrop: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val supportsLens: Boolean
        get() = realBackdropEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val realBackdropEnabled: Boolean
        get() = supportsBackdrop && !isLikelySoftwareRenderer

    val isLikelySoftwareRenderer: Boolean
        get() {
            val hardware = Build.HARDWARE.lowercase(Locale.US)
            val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
            val model = Build.MODEL.lowercase(Locale.US)
            val product = Build.PRODUCT.lowercase(Locale.US)
            return hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                fingerprint.contains("generic") ||
                model.contains("emulator") ||
                product.contains("sdk")
        }

    fun backdropBlurRadius(emphasis: GlassEmphasis): Float = when (emphasis) {
        GlassEmphasis.Subtle -> 18f
        GlassEmphasis.Normal -> 28f
        GlassEmphasis.Strong -> 38f
    }

    fun useLens(emphasis: GlassEmphasis): Boolean =
        supportsLens && emphasis == GlassEmphasis.Strong

    @Composable
    fun glassContainerColor(emphasis: GlassEmphasis = GlassEmphasis.Normal): Color {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.20f
        if (!LocalGlassEffectEnabled.current) {
            return if (dark) Color.Black else Color.White
        }
        val alpha = if (dark) {
            when (emphasis) {
                GlassEmphasis.Subtle -> 0.22f
                GlassEmphasis.Normal -> 0.32f
                GlassEmphasis.Strong -> 0.44f
            }
        } else {
            when (emphasis) {
                GlassEmphasis.Subtle -> 0.34f
                GlassEmphasis.Normal -> 0.54f
                GlassEmphasis.Strong -> 0.70f
            }
        }
        val source = if (dark) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }
        return source.copy(alpha = alpha)
    }

    @Composable
    fun glassSheenBrush(emphasis: GlassEmphasis = GlassEmphasis.Normal): Brush {
        if (!LocalGlassEffectEnabled.current) {
            return Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent
                )
            )
        }
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.20f
        val topAlpha = if (dark) {
            when (emphasis) {
                GlassEmphasis.Subtle -> 0.050f
                GlassEmphasis.Normal -> 0.085f
                GlassEmphasis.Strong -> 0.120f
            }
        } else {
            when (emphasis) {
                GlassEmphasis.Subtle -> 0.30f
                GlassEmphasis.Normal -> 0.42f
                GlassEmphasis.Strong -> 0.50f
            }
        }
        val accentAlpha = if (dark) 0.055f else 0.070f
        return Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = topAlpha * 0.26f),
                Color.Transparent,
                MaterialTheme.colorScheme.primary.copy(alpha = accentAlpha)
            )
        )
    }

    @Composable
    fun glassContentColor(): Color = MaterialTheme.colorScheme.onSurface

    @Composable
    fun glassBorder(emphasis: GlassEmphasis = GlassEmphasis.Normal): BorderStroke? {
        if (!LocalGlassEffectEnabled.current) {
            val color = if (MaterialTheme.colorScheme.background.luminance() < 0.20f) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.Black.copy(alpha = 0.05f)
            }
            return BorderStroke(1.dp, color)
        }
        if (emphasis == GlassEmphasis.Subtle) return null
        val color = if (MaterialTheme.colorScheme.background.luminance() < 0.20f) {
            Color.White.copy(alpha = if (emphasis == GlassEmphasis.Strong) 0.10f else 0.07f)
        } else {
            Color.White.copy(alpha = if (emphasis == GlassEmphasis.Strong) 0.38f else 0.26f)
        }
        return BorderStroke(1.dp, color)
    }

    @Composable
    fun tonalCardColors() = CardDefaults.cardColors(
        containerColor = if (LocalGlassEffectEnabled.current) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Immutable
enum class GlassEmphasis {
    Subtle,
    Normal,
    Strong
}

@Composable
fun GlassFallbackSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassDefaults.PanelShape,
    emphasis: GlassEmphasis = GlassEmphasis.Normal,
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = if (emphasis == GlassEmphasis.Subtle) 0.dp else 8.dp,
            shape = shape,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ),
        shape = shape,
        color = GlassDefaults.glassContainerColor(emphasis),
        contentColor = GlassDefaults.glassContentColor(),
        border = GlassDefaults.glassBorder(emphasis),
        tonalElevation = tonalElevation,
        content = content
    )
}
