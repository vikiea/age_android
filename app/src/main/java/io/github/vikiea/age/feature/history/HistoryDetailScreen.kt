/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.vikiea.age.R
import io.github.vikiea.age.core.model.CompressionState
import io.github.vikiea.age.core.model.EncryptMode
import io.github.vikiea.age.core.model.OperationAuthMethod
import io.github.vikiea.age.core.model.OperationRecord
import io.github.vikiea.age.core.model.OperationStatus
import io.github.vikiea.age.core.model.OperationType
import io.github.vikiea.age.core.model.RecordedDuplicateStrategy
import io.github.vikiea.age.ui.components.FilePathTreeView
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassTextButton
import io.github.vikiea.age.ui.glass.AppTopBar
import io.github.vikiea.age.ui.glass.TopBarActionButton
import io.github.vikiea.age.ui.glass.GlassTonalSurface
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
    backdrop: GlassBackdrop? = null
) {
    val operation by viewModel.operation.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            navigationIcon = {
                TopBarActionButton(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onBack,
                    backdrop = backdrop
                )
            },
            actions = {
                TopBarActionButton(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    onClick = { showDeleteDialog = true },
                    backdrop = backdrop,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )

        if (operation == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.record_not_found), modifier = Modifier.padding(16.dp))
            }
            return@Column
        }

        val op = operation!!

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OperationInfoSection(op, backdrop)
            FileInfoSection(op, backdrop)
            DetailSection(title = stringResource(R.string.encryption_info), backdrop = backdrop) {
                InfoRow(stringResource(R.string.encryption_method_detail), authMethodLabel(op.authMethod))
            }
            if (op.status == OperationStatus.FAILED && !op.errorMessage.isNullOrBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        SectionTitle(stringResource(R.string.error_info), color = MaterialTheme.colorScheme.error)
                        Text(op.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            DetailSection(title = stringResource(R.string.time_info), backdrop = backdrop) {
                InfoRow(
                    stringResource(R.string.operation_time),
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(op.timestamp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_record)) },
            text = { Text(stringResource(R.string.delete_record_confirm)) },
            confirmButton = {
                GlassTextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteOperation { onBack() }
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                GlassTextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun OperationInfoSection(
    op: OperationRecord,
    backdrop: GlassBackdrop?
) {
    DetailSection(title = stringResource(R.string.operation_info), backdrop = backdrop) {
        InfoRow(stringResource(R.string.type), stringResource(if (op.type == OperationType.ENCRYPT) R.string.nav_encrypt else R.string.nav_decrypt))
        InfoRow(stringResource(R.string.mode), modeLabel(op.mode))
        InfoRow(stringResource(R.string.auth_method), authMethodLabel(op.authMethod))
        InfoRow(
            stringResource(R.string.key_hint),
            when {
                op.keyHint.isNotBlank() -> op.keyHint
                op.authMethod == OperationAuthMethod.PASSPHRASE -> stringResource(R.string.common_not_applicable)
                else -> stringResource(R.string.common_unknown)
            }
        )
        InfoRow(stringResource(R.string.compression), compressionLabel(op.compression))
        InfoRow(stringResource(R.string.duplicate_strategy), duplicateStrategyLabel(op.duplicateStrategy))
        InfoRow(stringResource(R.string.concurrency), op.concurrency.toString())
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.status),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val (color, label) = when (op.status) {
                OperationStatus.SUCCESS -> MaterialTheme.colorScheme.primary to stringResource(R.string.status_success)
                OperationStatus.FAILED -> MaterialTheme.colorScheme.error to stringResource(R.string.status_failed)
                OperationStatus.RUNNING -> MaterialTheme.colorScheme.outline to stringResource(R.string.status_running)
                OperationStatus.CANCELLED -> MaterialTheme.colorScheme.tertiary to stringResource(R.string.status_cancelled)
            }
            GlassTonalSurface {
                Text(
                    label,
                    color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun modeLabel(mode: EncryptMode): String = stringResource(
    when (mode) {
        EncryptMode.BATCH_PACK -> R.string.mode_pack
        EncryptMode.SEPARATE -> R.string.mode_separate
        EncryptMode.DECRYPT -> R.string.mode_decrypt
    }
)

@Composable
private fun authMethodLabel(method: OperationAuthMethod): String = stringResource(
    when (method) {
        OperationAuthMethod.UNKNOWN -> R.string.common_unknown
        OperationAuthMethod.PASSPHRASE -> R.string.auth_passphrase
        OperationAuthMethod.RECIPIENT -> R.string.auth_recipient
        OperationAuthMethod.IDENTITY -> R.string.auth_identity
    }
)

@Composable
private fun compressionLabel(state: CompressionState): String = stringResource(
    when (state) {
        CompressionState.UNKNOWN -> R.string.common_unknown
        CompressionState.ENABLED -> R.string.common_enabled
        CompressionState.DISABLED -> R.string.common_disabled
    }
)

@Composable
private fun duplicateStrategyLabel(strategy: RecordedDuplicateStrategy): String = stringResource(
    when (strategy) {
        RecordedDuplicateStrategy.UNKNOWN -> R.string.common_unknown
        RecordedDuplicateStrategy.RENAME -> R.string.duplicate_rename
        RecordedDuplicateStrategy.OVERWRITE -> R.string.duplicate_overwrite
    }
)

@Composable
private fun FileInfoSection(
    op: OperationRecord,
    backdrop: GlassBackdrop?
) {
    DetailSection(title = stringResource(R.string.file_info), backdrop = backdrop) {
        Text(
            stringResource(R.string.input_files),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilePathTreeView(paths = op.inputFiles)
            }
        }
        Text(
            stringResource(R.string.output_files),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (op.outputFiles.isNotEmpty()) {
            GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilePathTreeView(paths = op.outputFiles)
                }
            }
            Text(
                stringResource(R.string.saved_to, op.outputPath),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                op.outputPath,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    backdrop: GlassBackdrop?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionTitle(title)
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}
