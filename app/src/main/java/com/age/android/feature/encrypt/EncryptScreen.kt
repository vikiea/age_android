package com.age.android.feature.encrypt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.age.android.core.model.EncryptMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: EncryptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keys by viewModel.keys.collectAsState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("加密") },
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
            Text("加密模式", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.mode == EncryptMode.BATCH_PACK, onClick = { viewModel.setMode(EncryptMode.BATCH_PACK) }, label = { Text("打包加密") })
                FilterChip(selected = uiState.mode == EncryptMode.SEPARATE, onClick = { viewModel.setMode(EncryptMode.SEPARATE) }, label = { Text("分别加密") })
            }

            OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加文件")
            }

            if (uiState.files.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("已选文件 (${uiState.files.size})", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                        uiState.files.forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(file.name, maxLines = 1, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.removeFile(index) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "移除", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Text("加密方式", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(true) }, label = { Text("密码加密") })
                FilterChip(selected = !uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(false) }, label = { Text("公钥加密") })
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
                if (keys.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = uiState.selectedPublicKey, onValueChange = { viewModel.setSelectedPublicKey(it) }, label = { Text("公钥") }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            keys.forEach { key ->
                                DropdownMenuItem(text = { Text("${key.name}: ${key.publicKey.take(20)}...") }, onClick = { viewModel.setSelectedPublicKey(key.publicKey); expanded = false })
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = uiState.selectedPublicKey, onValueChange = { viewModel.setSelectedPublicKey(it) }, label = { Text("输入公钥 (age1xxx)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }

            if (uiState.mode == EncryptMode.BATCH_PACK) {
                OutlinedTextField(value = uiState.outputFileName, onValueChange = { viewModel.setOutputFileName(it) }, label = { Text("输出文件名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }

            Spacer(Modifier.height(8.dp))
        }

        // Bottom section: always visible
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider()

            if (uiState.isProcessing) {
                LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth())
                Text("处理中: ${uiState.processedCount}/${uiState.totalCount} (成功: ${uiState.successCount}, 失败: ${uiState.failCount})")
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

            Button(onClick = { viewModel.startEncrypt() }, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isProcessing) { Text("开始加密") }
        }
    }
}
