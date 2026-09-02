/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.ui.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val contentColor = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            navigationIcon?.invoke()
            Box(modifier = Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
fun TopBarActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current
) {
    val glassEffectEnabled = LocalGlassEffectEnabled.current
    val resolvedTint = if (enabled) {
        tint
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = enabled
    ) {
        if (glassEffectEnabled) {
            GlassSurface(
                modifier = Modifier.size(40.dp),
                backdrop = backdrop,
                shape = CircleShape,
                emphasis = GlassEmphasis.Subtle
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(24.dp),
                        tint = resolvedTint
                    )
                }
            }
        } else {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = resolvedTint
            )
        }
    }
}

@Composable
fun GlassActionFooter(
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        backdrop = backdrop,
        shape = GlassDefaults.NavShape,
        emphasis = GlassEmphasis.Strong
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun GlassBottomTabs(
    items: List<GlassTabItem>,
    selectedRoute: String?,
    onItemClick: (GlassTabItem) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        backdrop = backdrop,
        shape = GlassDefaults.NavShape,
        emphasis = GlassEmphasis.Strong
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = selectedRoute == item.route
                GlassTabButton(
                    item = item,
                    selected = selected,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.GlassTabButton(
    item: GlassTabItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.20f
    val shape = MaterialTheme.shapes.large
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.78f else 0.82f)
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 62.dp)
                .padding(horizontal = 2.dp)
                .clip(shape)
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = onClick
                )
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = contentColor
                )
            }
            Text(item.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

data class GlassTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
