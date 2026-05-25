/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.history

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.age.android.core.model.OperationRecord
import com.age.android.core.model.OperationStatus
import com.age.android.core.model.OperationType
import com.age.android.ui.glass.GlassBackdrop
import com.age.android.ui.glass.GlassEmphasis
import com.age.android.ui.glass.GlassSegmentOption
import com.age.android.ui.glass.GlassSegmentedControl
import com.age.android.ui.glass.GlassSurface
import com.age.android.ui.glass.GlassTextButton
import com.age.android.ui.glass.GlassTopBar
import com.age.android.ui.glass.GlassTonalSurface
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
                    .weight(1f),
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
