/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.keys

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(
    viewModel: KeysViewModel = hiltViewModel(),
    onKeyClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val keys by viewModel.keys.collectAsState()

    val keyFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.parseKeyFile(uri)
    }

    LaunchedEffect(uiState.error, uiState.success) {
        if (uiState.error != null || uiState.success != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("密钥管理") })
        },
        floatingActionButton = {
            Column {
                SmallFloatingActionButton(onClick = { viewModel.showImportDialog() }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "导入")
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(onClick = { viewModel.showGenerateDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "生成")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (keys.isEmpty()) {
                Text("暂无密钥，点击右下角按钮生成或导入", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(keys) { key ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onKeyClick(key.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(key.name) },
                                supportingContent = { Text("${key.publicKey.take(25)}... | ${key.keyType.name}") },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { viewModel.showRenameDialog(key) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "重命名")
                                        }
                                        IconButton(onClick = { viewModel.deleteKey(key.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "删除")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Generate dialog
    if (uiState.showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideGenerateDialog() },
            title = { Text("生成新密钥") },
            text = { OutlinedTextField(value = uiState.newName, onValueChange = { viewModel.setNewName(it) }, label = { Text("密钥名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.generateKey() }, enabled = !uiState.isGenerating) { Text("生成") } },
            dismissButton = { TextButton(onClick = { viewModel.hideGenerateDialog() }) { Text("取消") } }
        )
    }

    // Import dialog
    if (uiState.showImportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideImportDialog() },
            title = { Text("导入密钥") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.importName,
                        onValueChange = { viewModel.setImportName(it) },
                        label = { Text("密钥名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.importPublicKey,
                        onValueChange = { viewModel.setImportPublicKey(it) },
                        label = { Text("公钥 (age1xxx)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.importPrivateKey,
                        onValueChange = { viewModel.setImportPrivateKey(it) },
                        label = { Text("私钥 (AGE-SECRET-KEY-xxx)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider()

                    OutlinedButton(
                        onClick = { keyFilePicker.launch(arrayOf("text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.importFileUri != null) "重新选择文件" else "从文件导入")
                    }
                    if (uiState.importFileUri != null) {
                        Text(
                            text = "已选择文件，名称和密钥已自动填入，可手动修改后点击导入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "支持 age 原生格式密钥文件，自动解析并填入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.importKey() }) { Text("导入") } },
            dismissButton = { TextButton(onClick = { viewModel.hideImportDialog() }) { Text("取消") } }
        )
    }

    // Rename dialog
    if (uiState.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRenameDialog() },
            title = { Text("重命名密钥") },
            text = {
                OutlinedTextField(
                    value = uiState.renameText,
                    onValueChange = { viewModel.setRenameText(it) },
                    label = { Text("新名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.renameKey() }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { viewModel.hideRenameDialog() }) { Text("取消") } }
        )
    }

    // Snackbar
    uiState.error?.let { Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) } }
    uiState.success?.let { Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) } }
}
