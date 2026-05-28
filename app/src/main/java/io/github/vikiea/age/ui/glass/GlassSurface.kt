/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

@Composable
fun rememberGlassBackdrop(): GlassBackdrop = GlassBackdrop(rememberLayerBackdrop())

fun Modifier.glassBackdropLayer(backdrop: GlassBackdrop): Modifier =
    layerBackdrop(backdrop.layer)

@Immutable
@JvmInline
value class GlassBackdrop internal constructor(internal val layer: LayerBackdrop)

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    shape: Shape = GlassDefaults.PanelShape,
    emphasis: GlassEmphasis = GlassEmphasis.Normal,
    useRealBackdrop: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val backdropTint = GlassDefaults.glassBackdropTint(emphasis)
    val sheenBrush = GlassDefaults.glassSheenBrush(emphasis)
    val contentColor = GlassDefaults.glassContentColor()
    val border = GlassDefaults.glassBorder(emphasis)
    val glassEffectEnabled = LocalGlassEffectEnabled.current
    val shouldUseBackdrop = glassEffectEnabled && useRealBackdrop && backdrop != null && GlassDefaults.realBackdropEnabled
    val shadowElevation = GlassDefaults.shadowElevation(emphasis)
    val shadowAmbientColor = GlassDefaults.glassShadowAmbientColor(emphasis)
    val shadowSpotColor = GlassDefaults.glassShadowSpotColor(emphasis)

    if (shouldUseBackdrop) {
        val drawSurface: DrawScope.() -> Unit = remember(backdropTint, sheenBrush) {
            {
                drawRect(backdropTint)
                drawRect(sheenBrush)
            }
        }
        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop.layer,
                    shape = { shape },
                    effects = {
                        blur(radius = GlassDefaults.backdropBlurRadius(emphasis))
                        if (GlassDefaults.useLens(emphasis)) {
                            lens(refractionHeight = 10f, refractionAmount = 36f)
                        }
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.78f) },
                    shadow = {
                        Shadow.Default.copy(
                            radius = when (emphasis) {
                                GlassEmphasis.Subtle -> 22.dp
                                GlassEmphasis.Normal -> 36.dp
                                GlassEmphasis.Strong -> 52.dp
                            },
                            alpha = when (emphasis) {
                                GlassEmphasis.Subtle -> 0.44f
                                GlassEmphasis.Normal -> 0.68f
                                GlassEmphasis.Strong -> 0.82f
                            }
                        )
                    },
                    onDrawSurface = drawSurface
                )
                .clip(shape)
                .then(if (border != null) Modifier.border(border, shape) else Modifier),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    } else {
        Box(
            modifier = modifier
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    ambientColor = shadowAmbientColor,
                    spotColor = shadowSpotColor
                )
                .background(GlassDefaults.glassContainerColor(emphasis), shape)
                .clip(shape)
                .then(if (glassEffectEnabled) Modifier.glassSheen(sheenBrush) else Modifier)
                .then(if (border != null) Modifier.border(border, shape) else Modifier),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    emphasis: GlassEmphasis = GlassEmphasis.Normal,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        backdrop = backdrop,
        emphasis = emphasis
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun GlassTonalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = GlassDefaults.glassContainerColor(GlassEmphasis.Subtle),
    border: BorderStroke? = GlassDefaults.glassBorder(GlassEmphasis.Subtle),
    content: @Composable BoxScope.() -> Unit
) {
    val contentColor = GlassDefaults.glassContentColor()
    val sheenBrush = GlassDefaults.glassSheenBrush(GlassEmphasis.Subtle)
    val glassEffectEnabled = LocalGlassEffectEnabled.current
    val shadowModifier = modifier.shadow(
        elevation = if (glassEffectEnabled) 10.dp else 0.dp,
        shape = shape,
        ambientColor = GlassDefaults.glassShadowAmbientColor(GlassEmphasis.Subtle),
        spotColor = GlassDefaults.glassShadowSpotColor(GlassEmphasis.Subtle)
    )
    val surfaceModifier = if (border != null) {
        shadowModifier
            .background(containerColor, shape)
            .clip(shape)
            .then(if (glassEffectEnabled) Modifier.glassSheen(sheenBrush) else Modifier)
            .border(border, shape)
    } else {
        shadowModifier
            .background(containerColor, shape)
            .clip(shape)
            .then(if (glassEffectEnabled) Modifier.glassSheen(sheenBrush) else Modifier)
    }
    Box(
        modifier = surfaceModifier,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

private fun Modifier.glassSheen(brush: Brush): Modifier =
    drawWithCache {
        onDrawWithContent {
            drawRect(brush)
            drawContent()
        }
    }
