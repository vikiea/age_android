/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.decrypt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecryptScreen(
    onNavigateToSettings: () -> Unit = {},
    sharedUris: List<android.net.Uri>? = null,
    onSharedUrisConsumed: () -> Unit = {},
    viewModel: DecryptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keys by viewModel.keys.collectAsState()

    // Handle shared URIs from other apps
    LaunchedEffect(sharedUris) {
        if (!sharedUris.isNullOrEmpty()) {
            viewModel.addFiles(sharedUris)
            onSharedUrisConsumed()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.addFilesFromFolder(uri)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("解密") },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("选择加密文件", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加文件")
                }
                OutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("选择文件夹")
                }
            }

            if (uiState.files.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            Text("已选文件 (${uiState.files.size})", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.clearFiles() }, enabled = !uiState.isProcessing, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("清空", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        uiState.files.forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(file.name, maxLines = 1, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.removeFile(index) }, enabled = !uiState.isProcessing, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "移除", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Text("解密方式", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(true) }, label = { Text("密码解密") })
                FilterChip(selected = !uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(false) }, label = { Text("私钥解密") })
            }

            if (uiState.usePassphrase) {
                var showPass by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = uiState.passphrase,
                    onValueChange = { viewModel.setPassphrase(it) },
                    label = { Text("输入密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (showPass) "隐藏" else "显示")
                        }
                    }
                )
            } else {
                val privateKeys = keys.filter { it.privateKey != null }
                if (privateKeys.isNotEmpty()) {
                    // Show selected key name (masked), not the actual key
                    val selectedKeyName = privateKeys.find { it.privateKey == uiState.selectedPrivateKey }?.name ?: ""
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedKeyName,
                            onValueChange = {},
                            label = { Text("选择私钥") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            singleLine = true,
                            readOnly = true
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            privateKeys.forEach { key ->
                                DropdownMenuItem(text = { Text(key.name) }, onClick = { viewModel.setSelectedPrivateKey(key.privateKey!!); expanded = false })
                            }
                        }
                    }
                } else {
                    Text("暂无私钥，请先在密钥管理中生成或导入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Bottom section: fixed at bottom
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider()

            if (uiState.isProcessing) {
                LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth())
                Text("处理中: ${uiState.processedCount}/${uiState.totalCount}")
            }

            uiState.result?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(it, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                uiState.outputDir?.let { dir -> Text("目录: $dir", style = MaterialTheme.typography.bodySmall) }
                                uiState.outputFiles.forEach { name -> Text("  $name", style = MaterialTheme.typography.bodySmall) }
                            }
                            IconButton(onClick = { viewModel.clearResult() }) {
                                Icon(Icons.Default.Close, contentDescription = "关闭")
                            }
                        }
                        if (uiState.successCount > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.shareOutput() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("分享文件")
                                }
                            }
                        }
                    }
                }
            }

            uiState.error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }
            }

            Button(onClick = { viewModel.startDecrypt() }, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isProcessing && uiState.result == null) { Text("开始解密") }
        }
    }
}
