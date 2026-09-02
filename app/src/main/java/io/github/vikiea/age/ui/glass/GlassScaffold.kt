/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
                .background(glassBackgroundColor())
                .glassBackdropLayer(backdrop)
        )
        CompositionLocalProvider(LocalGlassEffectEnabled provides true) {
            content(backdrop)
        }
    }
}

@Composable
private fun glassBackgroundColor(): Color {
    return MaterialTheme.colorScheme.background
}
