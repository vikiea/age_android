/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vikiea.age.R

@Immutable
data class AppNotice(
    val title: String,
    val message: String,
    val tone: AppNoticeTone
)

enum class AppNoticeTone { Info, Success, Error }

@Composable
fun AppNoticeHost(
    notice: AppNotice?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notice != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        notice?.let {
            AppNoticeCard(
                notice = it,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AppNoticeCard(
    notice: AppNotice,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = notice.tone.colors()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.22f)),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = colors.icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = notice.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = colors.content
                )
            }
        }
    }
}

@Immutable
private data class NoticeColors(
    val container: Color,
    val content: Color,
    val accent: Color,
    val icon: ImageVector
)

@Composable
private fun AppNoticeTone.colors(): NoticeColors = when (this) {
    AppNoticeTone.Info -> NoticeColors(
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        accent = MaterialTheme.colorScheme.secondary,
        icon = Icons.Default.Info
    )
    AppNoticeTone.Success -> NoticeColors(
        container = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
        accent = MaterialTheme.colorScheme.primary,
        icon = Icons.Default.CheckCircle
    )
    AppNoticeTone.Error -> NoticeColors(
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
        accent = MaterialTheme.colorScheme.error,
        icon = Icons.Default.Error
    )
}
