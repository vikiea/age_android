/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.keys

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.age.android.core.model.KeyEntry
import com.age.android.ui.glass.GlassBackdrop
import com.age.android.ui.glass.GlassButton
import com.age.android.ui.glass.GlassDialog
import com.age.android.ui.glass.GlassEmphasis
import com.age.android.ui.glass.GlassStatusPanel
import com.age.android.ui.glass.GlassSurface
import com.age.android.ui.glass.GlassTextButton
import com.age.android.ui.glass.GlassTextField
import com.age.android.ui.glass.GlassTopBar
import com.age.android.ui.glass.GlassTonalSurface
import com.age.android.ui.glass.StatusTone
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun KeyDetailScreen(
    keyId: Long,
    onBack: () -> Unit,
    viewModel: KeysViewModel = hiltViewModel(),
    backdrop: GlassBackdrop? = null
) {
    val keys by viewModel.keys.collectAsState()
    val key = keys.find { it.id == keyId }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var privateKeyRevealed by remember { mutableStateOf(false) }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.writeExportToUri(uri)
        } else {
            viewModel.cancelExport()
        }
    }

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
        GlassTopBar(
            title = key?.name ?: "密钥详情",
            backdrop = backdrop,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        if (key == null) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                backdrop = backdrop,
                emphasis = GlassEmphasis.Normal
            ) {
                Text("密钥未找到", modifier = Modifier.padding(16.dp))
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.error?.let { message ->
                GlassStatusPanel(
                    title = "操作失败",
                    tone = StatusTone.Error,
                    onDismiss = { viewModel.clearMessages() }
                ) {
                    Text(message)
                }
            }

            uiState.success?.let { message ->
                GlassStatusPanel(
                    title = "操作完成",
                    tone = StatusTone.Success,
                    onDismiss = { viewModel.clearMessages() }
                ) {
                    Text(message)
                }
            }

            KeyTextSection(
                title = "公钥",
                value = key.publicKey,
                onCopy = { clipboard.setText(AnnotatedString(key.publicKey)) },
                backdrop = backdrop
            )

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                backdrop = backdrop,
                emphasis = GlassEmphasis.Normal
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("信息", modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.showRenameDialog(key) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "重命名", modifier = Modifier.size(18.dp))
                        }
                    }
                    InfoRow("类型", key.keyType.name)
                    InfoRow("有私钥", if (key.hasPrivateKey) "是" else "否")
                    InfoRow(
                        "创建时间",
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(key.createdAt)
                    )
                }
            }

            key.privateKey?.let { privateKey ->
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    backdrop = backdrop,
                    emphasis = GlassEmphasis.Normal
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionTitle("私钥")
                        if (privateKeyRevealed) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = privateKey,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = { clipboard.setText(AnnotatedString(privateKey)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                                }
                            }
                        } else {
                            GlassButton(
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

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                backdrop = backdrop,
                emphasis = GlassEmphasis.Subtle
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionTitle("导出")
                    GlassButton(
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

    LaunchedEffect(uiState.error, uiState.success) {
        if (uiState.error != null || uiState.success != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    if (uiState.showRenameDialog) {
        GlassDialog(
            title = "重命名密钥",
            onDismissRequest = { viewModel.hideRenameDialog() },
            backdrop = backdrop,
            actions = {
                GlassTextButton(onClick = { viewModel.hideRenameDialog() }) {
                    Text("取消")
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(onClick = { viewModel.renameKey() }) {
                    Text("确定")
                }
            }
        ) {
            GlassTextField(
                value = uiState.renameText,
                onValueChange = { viewModel.setRenameText(it) },
                label = "新名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun KeyTextSection(
    title: String,
    value: String,
    onCopy: () -> Unit,
    backdrop: GlassBackdrop?
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backdrop = backdrop,
        emphasis = GlassEmphasis.Normal
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle(title)
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                }
            }
        }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}
