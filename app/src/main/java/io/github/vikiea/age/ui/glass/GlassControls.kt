/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.ui.glass

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
import androidx.compose.ui.draw.shadow
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
    val glassEnabled = LocalGlassEffectEnabled.current
    val shape = MaterialTheme.shapes.extraLarge
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (glassEnabled) 0.78f else 1f),
            contentColor = if (dark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = glassControlContainerColor(stronger = true).copy(alpha = if (dark) 0.44f else 0.54f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
        ),
        border = if (glassEnabled) {
            BorderStroke(1.dp, Color.White.copy(alpha = if (dark) 0.32f else 0.58f))
        } else {
            null
        },
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (glassEnabled) 8.dp else 1.dp,
            pressedElevation = 1.dp,
            focusedElevation = 5.dp,
            hoveredElevation = 5.dp,
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
    val shape = MaterialTheme.shapes.extraLarge
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .then(
                if (LocalGlassEffectEnabled.current) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.14f)
                    )
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = glassControlContainerColor(stronger = true),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = glassControlContainerColor(stronger = true).copy(alpha = 0.34f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
        ),
        border = glassControlBorder(),
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
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.64f else 0.86f),
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
            .background(glassControlContainerColor(stronger = true), shape)
            .then(glassControlBorder()?.let { Modifier.border(it, shape) } ?: Modifier)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val selected = option.value == selectedValue
            val itemShape = RoundedCornerShape(999.dp)
            val itemContainer = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.36f else 0.24f)
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
                        .then(
                            if (selected && LocalGlassEffectEnabled.current) {
                                Modifier.shadow(
                                    elevation = 2.dp,
                                    shape = itemShape,
                                    ambientColor = Color.Black.copy(alpha = 0.10f),
                                    spotColor = Color.Black.copy(alpha = 0.14f)
                                )
                            } else {
                                Modifier
                            }
                        )
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
        return if (stronger) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
    }
    return if (dark) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (stronger) 0.72f else 0.60f)
    } else {
        Color.White.copy(alpha = if (stronger) 0.62f else 0.50f)
    }
}

@Composable
private fun glassControlBorderColor(): Color {
    if (!LocalGlassEffectEnabled.current) {
        return MaterialTheme.colorScheme.outlineVariant
    }
    return if (isGlassDarkTheme()) {
        Color.White.copy(alpha = 0.26f)
    } else {
        Color.White.copy(alpha = 0.64f)
    }
}

@Composable
private fun glassControlBorder(): BorderStroke? {
    if (!LocalGlassEffectEnabled.current) {
        return BorderStroke(1.dp, glassControlBorderColor())
    }
    return BorderStroke(1.dp, glassControlBorderColor())
}

@Composable
private fun isGlassDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.20f
