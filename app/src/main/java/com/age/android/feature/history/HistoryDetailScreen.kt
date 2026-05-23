/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.history

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.age.android.core.model.OperationRecord
import com.age.android.core.model.OperationStatus
import com.age.android.core.model.OperationType
import com.age.android.ui.glass.GlassBackdrop
import com.age.android.ui.glass.GlassEmphasis
import com.age.android.ui.glass.GlassSurface
import com.age.android.ui.glass.GlassTextButton
import com.age.android.ui.glass.GlassTopBar
import com.age.android.ui.glass.GlassTonalSurface
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
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "操作详情",
            backdrop = backdrop,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        )

        if (operation == null) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                backdrop = backdrop,
                emphasis = GlassEmphasis.Normal
            ) {
                Text("记录未找到", modifier = Modifier.padding(16.dp))
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
            DetailSection(title = "加密信息", backdrop = backdrop) {
                InfoRow("加密方式", op.recipientInfo)
            }
            if (op.status == OperationStatus.FAILED && !op.errorMessage.isNullOrBlank()) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    backdrop = backdrop,
                    emphasis = GlassEmphasis.Strong
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTitle("错误信息", color = MaterialTheme.colorScheme.error)
                        Text(op.errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            DetailSection(title = "时间信息", backdrop = backdrop) {
                InfoRow(
                    "操作时间",
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(op.timestamp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("确定要删除此操作记录吗？此操作不可撤销。") },
            confirmButton = {
                GlassTextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteOperation { onBack() }
                }) { Text("删除") }
            },
            dismissButton = {
                GlassTextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun OperationInfoSection(
    op: OperationRecord,
    backdrop: GlassBackdrop?
) {
    DetailSection(title = "操作信息", backdrop = backdrop) {
        InfoRow("类型", if (op.type == OperationType.ENCRYPT) "加密" else "解密")
        InfoRow("模式", op.mode.name)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "状态",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val (color, label) = when (op.status) {
                OperationStatus.SUCCESS -> MaterialTheme.colorScheme.primary to "成功"
                OperationStatus.FAILED -> MaterialTheme.colorScheme.error to "失败"
                OperationStatus.RUNNING -> MaterialTheme.colorScheme.outline to "进行中"
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
private fun FileInfoSection(
    op: OperationRecord,
    backdrop: GlassBackdrop?
) {
    DetailSection(title = "文件信息", backdrop = backdrop) {
        Text(
            "输入文件:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                op.inputFiles.forEach { file ->
                    Text(file, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        HorizontalDivider()
        Text(
            "输出路径",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(op.outputPath, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailSection(
    title: String,
    backdrop: GlassBackdrop?,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backdrop = backdrop,
        emphasis = GlassEmphasis.Normal
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                SectionTitle(title)
                content()
            }
        )
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
