/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.ui.glass

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
        GlassEmphasis.Subtle -> 22f
        GlassEmphasis.Normal -> 34f
        GlassEmphasis.Strong -> 46f
    }

    fun shadowElevation(emphasis: GlassEmphasis): Dp = when (emphasis) {
        GlassEmphasis.Subtle -> 8.dp
        GlassEmphasis.Normal -> 18.dp
        GlassEmphasis.Strong -> 30.dp
    }

    fun useLens(emphasis: GlassEmphasis): Boolean =
        supportsLens && emphasis == GlassEmphasis.Strong

    @Composable
    fun glassContainerColor(emphasis: GlassEmphasis = GlassEmphasis.Normal): Color {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.20f
        return glassContainerBaseColor(dark, emphasis).copy(
            alpha = glassContainerAlpha(
                glassEffectEnabled = LocalGlassEffectEnabled.current,
                dark = dark,
                emphasis = emphasis
            )
        )
    }

    @Composable
    fun glassBackdropTint(emphasis: GlassEmphasis = GlassEmphasis.Normal): Color {
        return glassContainerColor(emphasis)
    }

    internal fun glassContainerAlpha(
        glassEffectEnabled: Boolean,
        dark: Boolean,
        emphasis: GlassEmphasis
    ): Float {
        if (!glassEffectEnabled) {
            return 1f
        }
        return if (dark) {
            when (emphasis) {
                GlassEmphasis.Subtle -> 0.70f
                GlassEmphasis.Normal -> 0.78f
                GlassEmphasis.Strong -> 0.86f
            }
        } else {
            when (emphasis) {
                GlassEmphasis.Subtle -> 0.56f
                GlassEmphasis.Normal -> 0.66f
                GlassEmphasis.Strong -> 0.76f
            }
        }
    }

    private fun glassContainerBaseColor(dark: Boolean, emphasis: GlassEmphasis): Color {
        return if (dark) {
            when (emphasis) {
                GlassEmphasis.Subtle -> Color(0xFF0B0B0B)
                GlassEmphasis.Normal -> Color(0xFF121212)
                GlassEmphasis.Strong -> Color(0xFF1A1A1A)
            }
        } else {
            Color.White
        }
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
                GlassEmphasis.Subtle -> 0.070f
                GlassEmphasis.Normal -> 0.120f
                GlassEmphasis.Strong -> 0.170f
            }
        } else {
            when (emphasis) {
                GlassEmphasis.Subtle -> 0.38f
                GlassEmphasis.Normal -> 0.52f
                GlassEmphasis.Strong -> 0.62f
            }
        }
        return Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = topAlpha * 0.26f),
                Color.Transparent,
                Color.Black.copy(alpha = if (dark) 0.18f else 0.06f)
            )
        )
    }

    @Composable
    fun glassShadowAmbientColor(emphasis: GlassEmphasis): Color {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.20f
        val alpha = when (emphasis) {
            GlassEmphasis.Subtle -> if (dark) 0.06f else 0.16f
            GlassEmphasis.Normal -> if (dark) 0.09f else 0.20f
            GlassEmphasis.Strong -> if (dark) 0.12f else 0.26f
        }
        return if (dark) {
            Color.White.copy(alpha = alpha)
        } else {
            Color.Black.copy(alpha = alpha)
        }
    }

    @Composable
    fun glassShadowSpotColor(emphasis: GlassEmphasis): Color {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.20f
        val alpha = when (emphasis) {
            GlassEmphasis.Subtle -> if (dark) 0.30f else 0.20f
            GlassEmphasis.Normal -> if (dark) 0.42f else 0.28f
            GlassEmphasis.Strong -> if (dark) 0.54f else 0.36f
        }
        return Color.Black.copy(alpha = alpha)
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
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.20f
        val color = if (dark) {
            Color.White.copy(alpha = if (emphasis == GlassEmphasis.Strong) 0.13f else 0.09f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = if (emphasis == GlassEmphasis.Strong) 0.14f else 0.10f)
        }
        return BorderStroke(0.7.dp, color)
    }

    @Composable
    fun tonalCardColors() = CardDefaults.cardColors(
        containerColor = glassContainerColor(GlassEmphasis.Subtle),
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
            elevation = GlassDefaults.shadowElevation(emphasis),
            shape = shape,
            ambientColor = GlassDefaults.glassShadowAmbientColor(emphasis),
            spotColor = GlassDefaults.glassShadowSpotColor(emphasis)
        ),
        shape = shape,
        color = GlassDefaults.glassContainerColor(emphasis),
        contentColor = GlassDefaults.glassContentColor(),
        border = GlassDefaults.glassBorder(emphasis),
        tonalElevation = tonalElevation,
        content = content
    )
}
