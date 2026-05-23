/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun GlassBackdropHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(GlassBackdrop) -> Unit
) {
    val backdrop = rememberGlassBackdrop()
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glassBackgroundBrush())
                .glassBackdropLayer(backdrop)
        )
        content(backdrop)
    }
}

@Composable
private fun glassBackgroundBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    if (scheme.background.luminance() < 0.20f) {
        return Brush.verticalGradient(
            colors = listOf(
                Color.Black,
                Color.Black
            )
        )
    }
    return Brush.verticalGradient(
        colors = listOf(
            scheme.background,
            scheme.primaryContainer.copy(alpha = 0.60f),
            scheme.secondaryContainer.copy(alpha = 0.46f),
            Color.Black.copy(alpha = 0.08f)
        )
    )
}
