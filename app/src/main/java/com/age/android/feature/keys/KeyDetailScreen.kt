package com.age.android.feature.keys

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyDetailScreen(
    keyId: Long,
    onBack: () -> Unit,
    viewModel: KeysViewModel = hiltViewModel()
) {
    val keys by viewModel.keys.collectAsState()
    val key = keys.find { it.id == keyId }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(key?.name ?: "密钥详情") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } }
            )
        }
    ) { padding ->
        if (key == null) {
            Text("密钥未找到", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("公钥", style = MaterialTheme.typography.titleSmall)
                    Row {
                        Text(key.publicKey, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(key.publicKey)) }) { Icon(Icons.Default.ContentCopy, contentDescription = "复制") }
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("信息", style = MaterialTheme.typography.titleSmall)
                    Text("类型: ${key.keyType.name}")
                    Text("有私钥: ${if (key.hasPrivateKey) "是" else "否"}")
                    Text("创建时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(key.createdAt)}")
                }
            }
        }
    }
}
