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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun GlassBackdropHost(
    modifier: Modifier = Modifier,
    glassEffectEnabled: Boolean = true,
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
                .then(
                    if (glassEffectEnabled) {
                        Modifier.glassBackdropLayer(backdrop)
                    } else {
                        Modifier
                    }
                )
        )
        CompositionLocalProvider(LocalGlassEffectEnabled provides glassEffectEnabled) {
            content(backdrop)
        }
    }
}

@Composable
private fun glassBackgroundColor(): Color {
    return MaterialTheme.colorScheme.background
}
