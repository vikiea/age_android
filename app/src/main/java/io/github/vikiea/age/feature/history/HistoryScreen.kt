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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.vikiea.age.core.model.OperationRecord
import io.github.vikiea.age.core.model.OperationStatus
import io.github.vikiea.age.core.model.OperationType
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassEmphasis
import io.github.vikiea.age.ui.glass.GlassSegmentOption
import io.github.vikiea.age.ui.glass.GlassSegmentedControl
import io.github.vikiea.age.ui.glass.GlassSurface
import io.github.vikiea.age.ui.glass.GlassTextButton
import io.github.vikiea.age.ui.glass.GlassTopBar
import io.github.vikiea.age.ui.glass.GlassTonalSurface
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

    val filteredOps = if (uiState.filterType != null) {
        operations.filter { it.type == uiState.filterType }
    } else {
        operations
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "操作历史",
            backdrop = backdrop,
            actions = {
                IconButton(onClick = { viewModel.showClearDialog() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "清除")
                }
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
                    GlassSegmentOption<OperationType?>(null, "全部"),
                    GlassSegmentOption(OperationType.ENCRYPT, "加密"),
                    GlassSegmentOption(OperationType.DECRYPT, "解密")
                ),
                selectedValue = uiState.filterType,
                onSelected = { viewModel.setFilter(it) },
                modifier = Modifier.fillMaxWidth()
            )

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .historyTabSwipe(
                        onSwipeLeft = { viewModel.showNextFilter() },
                        onSwipeRight = { viewModel.showPreviousFilter() }
                    ),
                backdrop = backdrop,
                emphasis = GlassEmphasis.Normal
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitle("记录")

                    if (filteredOps.isEmpty()) {
                        GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "暂无操作记录",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(filteredOps, key = { it.id }) { op ->
                                OperationRow(
                                    operation = op,
                                    onClick = { onOperationClick(op.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showClearDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideClearDialog() },
            title = { Text("清除历史") },
            text = { Text("确定要清除所有操作记录吗？此操作不可撤销。") },
            confirmButton = { GlassTextButton(onClick = { viewModel.clearHistory() }) { Text("清除") } },
            dismissButton = { GlassTextButton(onClick = { viewModel.hideClearDialog() }) { Text("取消") } }
        )
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
                    text = "${if (operation.type == OperationType.ENCRYPT) "加密" else "解密"} - ${operation.mode.name}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "文件: ${operation.inputFiles.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(operation.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val (color, label) = when (operation.status) {
                OperationStatus.SUCCESS -> MaterialTheme.colorScheme.primary to "成功"
                OperationStatus.FAILED -> MaterialTheme.colorScheme.error to "失败"
                OperationStatus.RUNNING -> MaterialTheme.colorScheme.outline to "进行中"
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
