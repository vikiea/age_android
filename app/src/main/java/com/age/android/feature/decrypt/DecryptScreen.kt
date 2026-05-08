package com.age.android.feature.decrypt

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

@Composable
fun DecryptScreen(viewModel: DecryptViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val keys by viewModel.keys.collectAsState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("选择加密文件", style = MaterialTheme.typography.titleMedium)

        OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加 .tar.gz.age 文件")
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

        Text("解密方式", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(true) }, label = { Text("密码解密") })
            FilterChip(selected = !uiState.usePassphrase, onClick = { viewModel.setUsePassphrase(false) }, label = { Text("私钥解密") })
        }

        if (uiState.usePassphrase) {
            OutlinedTextField(value = uiState.passphrase, onValueChange = { viewModel.setPassphrase(it) }, label = { Text("输入密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        } else {
            if (keys.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = uiState.selectedPrivateKey, onValueChange = { viewModel.setSelectedPrivateKey(it) }, label = { Text("私钥") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        keys.filter { it.hasPrivateKey }.forEach { key ->
                            DropdownMenuItem(text = { Text(key.name) }, onClick = { viewModel.setSelectedPrivateKey(key.publicKey); expanded = false })
                        }
                    }
                }
            } else {
                OutlinedTextField(value = uiState.selectedPrivateKey, onValueChange = { viewModel.setSelectedPrivateKey(it) }, label = { Text("输入私钥 (AGE-SECRET-KEY-1...)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        }

        Spacer(Modifier.weight(1f))

        if (uiState.isProcessing) {
            LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth())
            Text("处理中: ${uiState.processedCount}/${uiState.totalCount}")
        }
        uiState.result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = { viewModel.startDecrypt() }, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isProcessing) { Text("开始解密") }
    }
}
