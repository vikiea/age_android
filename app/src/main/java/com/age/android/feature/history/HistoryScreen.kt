package com.age.android.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.age.android.core.model.OperationStatus
import com.age.android.core.model.OperationType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val operations by viewModel.operations.collectAsState()

    val filteredOps = if (uiState.filterType != null) {
        operations.filter { it.type == uiState.filterType }
    } else {
        operations
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("操作历史") },
                actions = {
                    IconButton(onClick = { viewModel.showClearDialog() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清除")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.filterType == null, onClick = { viewModel.setFilter(null) }, label = { Text("全部") })
                FilterChip(selected = uiState.filterType == OperationType.ENCRYPT, onClick = { viewModel.setFilter(OperationType.ENCRYPT) }, label = { Text("加密") })
                FilterChip(selected = uiState.filterType == OperationType.DECRYPT, onClick = { viewModel.setFilter(OperationType.DECRYPT) }, label = { Text("解密") })
            }

            Spacer(Modifier.height(12.dp))

            if (filteredOps.isEmpty()) {
                Text("暂无操作记录", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredOps) { op ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text("${if (op.type == OperationType.ENCRYPT) "加密" else "解密"} - ${op.mode.name}") },
                                supportingContent = {
                                    Column {
                                        Text("文件: ${op.inputFiles.joinToString(", ")}")
                                        Text("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(op.timestamp)}")
                                    }
                                },
                                trailingContent = {
                                    val (color, label) = when (op.status) {
                                        OperationStatus.SUCCESS -> MaterialTheme.colorScheme.primary to "成功"
                                        OperationStatus.FAILED -> MaterialTheme.colorScheme.error to "失败"
                                        OperationStatus.RUNNING -> MaterialTheme.colorScheme.outline to "进行中"
                                    }
                                    Text(label, color = color)
                                }
                            )
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
            confirmButton = { TextButton(onClick = { viewModel.clearHistory() }) { Text("清除") } },
            dismissButton = { TextButton(onClick = { viewModel.hideClearDialog() }) { Text("取消") } }
        )
    }
}
