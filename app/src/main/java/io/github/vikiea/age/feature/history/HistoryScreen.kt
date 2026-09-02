/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.vikiea.age.R
import io.github.vikiea.age.core.model.OperationRecord
import io.github.vikiea.age.core.model.OperationStatus
import io.github.vikiea.age.core.model.OperationType
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassSegmentOption
import io.github.vikiea.age.ui.glass.GlassSegmentedControl
import io.github.vikiea.age.ui.glass.GlassTextButton
import io.github.vikiea.age.ui.glass.AppTopBar
import io.github.vikiea.age.ui.glass.TopBarActionButton
import io.github.vikiea.age.ui.glass.GlassTonalSurface
import io.github.vikiea.age.ui.components.FilePathTreeView
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    onOperationClick: (Long) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
    backdrop: GlassBackdrop? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val operations by viewModel.operations.collectAsState()
    var selectedOperationId by rememberSaveable { mutableStateOf<Long?>(null) }

    val filteredOps = if (uiState.filterType != null) {
        operations.filter { it.type == uiState.filterType }
    } else {
        operations
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            actions = {
                TopBarActionButton(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = stringResource(R.string.clear_history),
                    onClick = { viewModel.showClearDialog() },
                    backdrop = backdrop,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassSegmentedControl(
                options = listOf(
                    GlassSegmentOption<OperationType?>(null, stringResource(R.string.all)),
                    GlassSegmentOption(OperationType.ENCRYPT, stringResource(R.string.nav_encrypt)),
                    GlassSegmentOption(OperationType.DECRYPT, stringResource(R.string.nav_decrypt))
                ),
                selectedValue = uiState.filterType,
                onSelected = { viewModel.setFilter(it) },
                modifier = Modifier.fillMaxWidth()
            )

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val isWide = maxWidth >= 840.dp
                val selected = filteredOps.firstOrNull { it.id == selectedOperationId }
                    ?: filteredOps.firstOrNull()
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (isWide) 16.dp else 0.dp)) {
                    HistoryList(
                        operations = filteredOps,
                        selectedId = if (isWide) selected?.id else null,
                        onClick = { operation ->
                            if (isWide) selectedOperationId = operation.id else onOperationClick(operation.id)
                        },
                        modifier = if (isWide) Modifier.weight(0.46f) else Modifier.fillMaxWidth(),
                        onSwipeLeft = viewModel::showNextFilter,
                        onSwipeRight = viewModel::showPreviousFilter
                    )
                    if (isWide) {
                        VerticalDivider()
                        HistoryPreview(selected, Modifier.weight(0.54f))
                    }
                }
            }
        }
    }

    if (uiState.showClearDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideClearDialog() },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.clear_history_confirm)) },
            confirmButton = { GlassTextButton(onClick = { viewModel.clearHistory() }) { Text(stringResource(R.string.common_clear)) } },
            dismissButton = { GlassTextButton(onClick = { viewModel.hideClearDialog() }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@Composable
private fun HistoryList(
    operations: List<OperationRecord>,
    selectedId: Long?,
    onClick: (OperationRecord) -> Unit,
    modifier: Modifier,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    Column(
        modifier = modifier.historyTabSwipe(onSwipeLeft, onSwipeRight),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(stringResource(R.string.records))
        if (operations.isEmpty()) {
            Text(
                stringResource(R.string.no_history),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(operations, key = { it.id }) { operation ->
                    OperationRow(operation = operation, onClick = { onClick(operation) })
                }
            }
        }
    }
}

@Composable
private fun HistoryPreview(operation: OperationRecord?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.operation_detail))
        if (operation == null) {
            Text(stringResource(R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Text(
            stringResource(if (operation.type == OperationType.ENCRYPT) R.string.nav_encrypt else R.string.nav_decrypt),
            style = MaterialTheme.typography.titleSmall
        )
        Text("${operation.authMethod.name} · ${operation.status.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (operation.keyHint.isNotBlank()) Text(operation.keyHint, style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.input_files), style = MaterialTheme.typography.labelLarge)
        FilePathTreeView(paths = operation.inputFiles)
        if (operation.outputFiles.isNotEmpty()) {
            Text(stringResource(R.string.output_files), style = MaterialTheme.typography.labelLarge)
            FilePathTreeView(paths = operation.outputFiles)
        }
        Text(stringResource(R.string.saved_to, operation.outputPath), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.historyTabSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = pointerInput(onSwipeLeft, onSwipeRight) {
    val swipeThreshold = 72.dp.toPx()
    var dragTotal = 0f
    detectHorizontalDragGestures(
        onDragStart = {
            dragTotal = 0f
        },
        onHorizontalDrag = { change, dragAmount ->
            dragTotal += dragAmount
            change.consume()
        },
        onDragEnd = {
            when {
                dragTotal <= -swipeThreshold -> onSwipeLeft()
                dragTotal >= swipeThreshold -> onSwipeRight()
            }
            dragTotal = 0f
        },
        onDragCancel = {
            dragTotal = 0f
        }
    )
}

@Composable
private fun OperationRow(
    operation: OperationRecord,
    onClick: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    GlassTonalSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${stringResource(if (operation.type == OperationType.ENCRYPT) R.string.nav_encrypt else R.string.nav_decrypt)} - ${operation.mode.name}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.history_row_files, operation.inputFiles.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.history_row_time, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(operation.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val (color, label) = when (operation.status) {
                OperationStatus.SUCCESS -> MaterialTheme.colorScheme.primary to stringResource(R.string.status_success)
                OperationStatus.FAILED -> MaterialTheme.colorScheme.error to stringResource(R.string.status_failed)
                OperationStatus.RUNNING -> MaterialTheme.colorScheme.outline to stringResource(R.string.status_running)
                OperationStatus.CANCELLED -> MaterialTheme.colorScheme.tertiary to stringResource(R.string.status_cancelled)
            }
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}
