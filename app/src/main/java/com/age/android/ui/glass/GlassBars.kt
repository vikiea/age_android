/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val contentColor = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            navigationIcon?.invoke()
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1
            )
            actions()
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        backdrop = backdrop,
        shape = GlassDefaults.NavShape,
        emphasis = GlassEmphasis.Strong
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
    val indicatorColor = if (selected) {
        if (isDark) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
        }
    } else {
        Color.Transparent
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp)
                .padding(horizontal = 2.dp)
                .clip(shape)
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = onClick
                )
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(item.icon, contentDescription = null)
            Text(item.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(18.dp)
                    .height(3.dp)
                    .background(indicatorColor, RoundedCornerShape(999.dp))
            )
        }
    }
}

data class GlassTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
