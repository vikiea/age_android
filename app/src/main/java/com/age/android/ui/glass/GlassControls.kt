/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val dark = isGlassDarkTheme()
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (dark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
            },
            contentColor = if (dark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = glassControlContainerColor().copy(alpha = if (dark) 0.40f else 0.48f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
        ),
        border = BorderStroke(
            1.dp,
            if (dark) {
                Color.White.copy(alpha = 0.16f)
            } else {
                Color.White.copy(alpha = 0.52f)
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        content = content
    )
}

@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = glassControlContainerColor(),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = glassControlContainerColor().copy(alpha = 0.34f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
        ),
        border = BorderStroke(1.dp, glassControlBorderColor()),
        content = content
    )
}

@Composable
fun GlassTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f)
        ),
        content = content
    )
}

@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    size: Dp = 56.dp,
    contentDescription: String,
    content: @Composable RowScope.() -> Unit
) {
    val shape = if (size <= 56.dp) CircleShape else MaterialTheme.shapes.extraLarge
    val interactionSource = remember { MutableInteractionSource() }
    GlassSurface(
        modifier = modifier
            .heightIn(min = size)
            .widthIn(min = size)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        backdrop = backdrop,
        shape = shape,
        emphasis = GlassEmphasis.Strong
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = size)
                .widthIn(min = size)
                .padding(horizontal = if (size <= 56.dp) 0.dp else 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dark = isGlassDarkTheme()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = if (dark) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.48f else 0.78f),
            checkedBorderColor = glassControlBorderColor(),
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dark) 0.74f else 0.66f),
            uncheckedTrackColor = glassControlContainerColor(stronger = true),
            uncheckedBorderColor = glassControlBorderColor(),
            disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            disabledCheckedTrackColor = glassControlContainerColor(),
            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
            disabledUncheckedTrackColor = glassControlContainerColor().copy(alpha = 0.30f)
        )
    )
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null
) {
    val containerColor = glassControlContainerColor(stronger = true)
    val borderColor = glassControlBorderColor()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        label = { Text(label) },
        trailingIcon = trailingIcon,
        suffix = suffix,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.34f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isGlassDarkTheme()) 0.78f else 0.68f),
            unfocusedBorderColor = borderColor,
            disabledBorderColor = borderColor.copy(alpha = 0.34f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun <T> GlassSegmentedControl(
    options: List<GlassSegmentOption<T>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dark = isGlassDarkTheme()
    val shape = MaterialTheme.shapes.extraLarge
    Row(
        modifier = modifier
            .clip(shape)
            .background(glassControlContainerColor(), shape)
            .border(BorderStroke(1.dp, glassControlBorderColor()), shape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val selected = option.value == selectedValue
            val itemShape = RoundedCornerShape(999.dp)
            val itemContainer = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.20f else 0.13f)
            } else {
                Color.Transparent
            }
            val itemContent = when {
                selected -> MaterialTheme.colorScheme.primary
                enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f)
            }
            val interactionSource = remember(option.value) { MutableInteractionSource() }
            CompositionLocalProvider(LocalContentColor provides itemContent) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(itemShape)
                        .background(itemContainer, itemShape)
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.Tab,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelected(option.value) }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

data class GlassSegmentOption<T>(
    val value: T,
    val label: String
)

@Composable
private fun glassControlContainerColor(stronger: Boolean = false): Color {
    val dark = isGlassDarkTheme()
    if (!LocalGlassEffectEnabled.current) {
        return if (dark) {
            if (stronger) Color(0xFF111111) else Color.Black
        } else {
            if (stronger) Color(0xFFF7F7F7) else Color.White
        }
    }
    val alpha = when {
        dark && stronger -> 0.28f
        dark -> 0.18f
        stronger -> 0.40f
        else -> 0.26f
    }
    val source = if (dark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    return source.copy(alpha = alpha)
}

@Composable
private fun glassControlBorderColor(): Color {
    if (!LocalGlassEffectEnabled.current) {
        return if (isGlassDarkTheme()) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color.Black.copy(alpha = 0.08f)
        }
    }
    return if (isGlassDarkTheme()) {
        Color.White.copy(alpha = 0.09f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }
}

@Composable
private fun isGlassDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.20f
