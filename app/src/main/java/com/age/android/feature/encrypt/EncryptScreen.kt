package com.age.android.feature.encrypt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.age.android.core.model.EncryptMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptScreen(viewModel: EncryptViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val keys by viewModel.keys.collectAsState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                itemsIndexed(uiState.files) { index, file ->
                    ListItem(headlineContent = { Text(file.name) }, trailingContent = {
                        IconButton(onClick = { viewModel.removeFile(index) }) { Icon(Icons.Default.Close, contentDescription = "移除") }
                    })
                }
            }
        }

        Text("加密方式", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(true) }, label = { Text("密码加密") })
            FilterChip(selected = !uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(false) }, label = { Text("公钥加密") })
        }

        if (uiState.usePassphrase) {
            OutlinedTextField(value = uiState.passphrase, onValueChange = { viewModel.setPassphrase(it) }, label = { Text("输入密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        } else {
            if (keys.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = uiState.selectedPublicKey, onValueChange = { viewModel.setSelectedPublicKey(it) }, label = { Text("公钥") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
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

        Spacer(Modifier.weight(1f))

        if (uiState.isProcessing) {
            LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth())
            Text("处理中: ${uiState.processedCount}/${uiState.totalCount} (成功: ${uiState.successCount}, 失败: ${uiState.failCount})")
        }
        uiState.result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = { viewModel.startEncrypt() }, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isProcessing) { Text("开始加密") }
    }
}
