package com.age.android.feature.keys

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var privateKeyRevealed by remember { mutableStateOf(false) }

    // CreateDocument launcher for export
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.writeExportToUri(uri)
        } else {
            viewModel.cancelExport()
        }
    }

    // Launch export file picker when pendingExportFileName is set
    LaunchedEffect(uiState.pendingExportFileName) {
        uiState.pendingExportFileName?.let { fileName ->
            createDocLauncher.launch(fileName)
        }
    }

    fun authenticate(onSuccess: () -> Unit) {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("验证身份")
            .setSubtitle("查看私钥前需要验证身份")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(key?.name ?: "密钥详情") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } }
        )

        if (key == null) {
            Text("密钥未找到", modifier = Modifier.padding(16.dp))
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("公钥", style = MaterialTheme.typography.titleSmall)
                    Row {
                        Text(key.publicKey, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(key.publicKey)) }) { Icon(Icons.Default.ContentCopy, contentDescription = "复制") }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("信息", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.showRenameDialog(key) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "重命名", modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("类型: ${key.keyType.name}")
                    Text("有私钥: ${if (key.hasPrivateKey) "是" else "否"}")
                    Text("创建时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(key.createdAt)}")
                }
            }

            if (key.privateKey != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("私钥", style = MaterialTheme.typography.titleSmall)
                        if (privateKeyRevealed) {
                            Row {
                                Text(key.privateKey, modifier = Modifier.weight(1f))
                                IconButton(onClick = { clipboard.setText(AnnotatedString(key.privateKey)) }) { Icon(Icons.Default.ContentCopy, contentDescription = "复制") }
                            }
                        } else {
                            Button(
                                onClick = { authenticate { privateKeyRevealed = true } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("验证身份以查看私钥")
                            }
                        }
                    }
                }
            }

            // Export key button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("导出", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = {
                            if (key.privateKey != null && !privateKeyRevealed) {
                                authenticate { viewModel.prepareExport(key, true) }
                            } else {
                                viewModel.prepareExport(key, key.privateKey != null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("导出密钥文件")
                    }
                    Text(
                        text = "保存为 age 格式密钥文件，可导入到其他 age 工具中使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Auto-dismiss messages
    LaunchedEffect(uiState.error, uiState.success) {
        if (uiState.error != null || uiState.success != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    // Snackbar for export status
    uiState.error?.let { Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) } }
    uiState.success?.let { Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) } }

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
}
