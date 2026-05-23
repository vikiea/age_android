/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val surfaceColor = GlassDefaults.glassContainerColor(emphasis)
    val sheenBrush = GlassDefaults.glassSheenBrush(emphasis)
    val contentColor = GlassDefaults.glassContentColor()
    val border = GlassDefaults.glassBorder()
    val shouldUseBackdrop = useRealBackdrop && backdrop != null && GlassDefaults.realBackdropEnabled

    if (shouldUseBackdrop) {
        val drawSurface: DrawScope.() -> Unit = remember(surfaceColor, sheenBrush) {
            {
                drawRect(surfaceColor)
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
                    shadow = { Shadow.Default.copy(radius = 18.dp, alpha = if (emphasis == GlassEmphasis.Subtle) 0.28f else 0.56f) },
                    onDrawSurface = drawSurface
                )
                .clip(shape)
                .border(border, shape),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    } else {
        Box(
            modifier = modifier
                .background(surfaceColor, shape)
                .clip(shape)
                .glassSheen(sheenBrush)
                .border(border, shape),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

@Composable
fun GlassTonalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = GlassDefaults.glassContainerColor(GlassEmphasis.Subtle),
    border: BorderStroke? = GlassDefaults.glassBorder(),
    content: @Composable BoxScope.() -> Unit
) {
    val contentColor = GlassDefaults.glassContentColor()
    val sheenBrush = GlassDefaults.glassSheenBrush(GlassEmphasis.Subtle)
    val surfaceModifier = if (border != null) {
        modifier
            .background(containerColor, shape)
            .clip(shape)
            .glassSheen(sheenBrush)
            .border(border, shape)
    } else {
        modifier
            .background(containerColor, shape)
            .clip(shape)
            .glassSheen(sheenBrush)
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
