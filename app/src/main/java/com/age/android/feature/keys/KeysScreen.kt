/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.keys

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.age.android.core.model.KeyEntry
import com.age.android.ui.glass.GlassBackdrop
import com.age.android.ui.glass.GlassButton
import com.age.android.ui.glass.GlassDialog
import com.age.android.ui.glass.GlassEmphasis
import com.age.android.ui.glass.GlassFloatingActionButton
import com.age.android.ui.glass.GlassOutlinedButton
import com.age.android.ui.glass.GlassStatusPanel
import com.age.android.ui.glass.GlassSurface
import com.age.android.ui.glass.GlassTextButton
import com.age.android.ui.glass.GlassTextField
import com.age.android.ui.glass.GlassTopBar
import com.age.android.ui.glass.GlassTonalSurface
import com.age.android.ui.glass.StatusTone

@Composable
fun KeysScreen(
    viewModel: KeysViewModel = hiltViewModel(),
    onKeyClick: (Long) -> Unit = {},
    backdrop: GlassBackdrop? = null
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = "密钥管理", backdrop = backdrop)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    backdrop = backdrop,
                    emphasis = GlassEmphasis.Normal
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionTitle("密钥库")

                        if (keys.isEmpty()) {
                            GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "暂无密钥，点击右下角按钮生成或导入",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(keys, key = { it.id }) { key ->
                                    KeyRow(
                                        key = key,
                                        onClick = { onKeyClick(key.id) },
                                        onRename = { viewModel.showRenameDialog(key) },
                                        onDelete = { viewModel.deleteKey(key.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassFloatingActionButton(
                onClick = { viewModel.showImportDialog() },
                backdrop = backdrop,
                size = 60.dp,
                contentDescription = "导入密钥"
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = "导入")
                Spacer(Modifier.width(8.dp))
                Text("导入")
            }
            GlassFloatingActionButton(
                onClick = { viewModel.showGenerateDialog() },
                backdrop = backdrop,
                size = 64.dp,
                contentDescription = "生成新密钥"
            ) {
                Icon(Icons.Default.Add, contentDescription = "生成")
                Spacer(Modifier.width(8.dp))
                Text("生成密钥")
            }
        }
    }

    if (uiState.showGenerateDialog) {
        GlassDialog(
            title = "生成新密钥",
            subtitle = "创建一组新的 age 密钥，并保存到本地密钥库。",
            onDismissRequest = { viewModel.hideGenerateDialog() },
            backdrop = backdrop,
            actions = {
                GlassTextButton(onClick = { viewModel.hideGenerateDialog() }) {
                    Text("取消")
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(
                    onClick = { viewModel.generateKey() },
                    enabled = !uiState.isGenerating
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("生成中...")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("生成")
                    }
                }
            }
        ) {
            GlassTextField(
                value = uiState.newName,
                onValueChange = { viewModel.setNewName(it) },
                label = "密钥名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }

    if (uiState.showImportDialog) {
        GlassDialog(
            title = "导入密钥",
            subtitle = "粘贴 age 公钥/私钥，或从密钥文件自动填入。",
            onDismissRequest = { viewModel.hideImportDialog() },
            backdrop = backdrop,
            actions = {
                GlassTextButton(onClick = { viewModel.hideImportDialog() }) {
                    Text("取消")
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(onClick = { viewModel.importKey() }) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("导入")
                }
            }
        ) {
            GlassTextField(
                value = uiState.importName,
                onValueChange = { viewModel.setImportName(it) },
                label = "密钥名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            GlassTextField(
                value = uiState.importPublicKey,
                onValueChange = { viewModel.setImportPublicKey(it) },
                label = "公钥 (age1xxx)",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            GlassTextField(
                value = uiState.importPrivateKey,
                onValueChange = { viewModel.setImportPrivateKey(it) },
                label = "私钥 (AGE-SECRET-KEY-xxx)",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            GlassOutlinedButton(
                onClick = { keyFilePicker.launch(arrayOf("text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.importFileUri != null) "重新选择文件" else "从文件导入")
            }
            Text(
                text = if (uiState.importFileUri != null) {
                    "已选择文件，名称和密钥已自动填入，可手动修改后点击导入"
                } else {
                    "支持 age 原生格式密钥文件，自动解析并填入"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.importFileUri != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
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
private fun KeyRow(
    key: KeyEntry,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    GlassTonalSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = key.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${key.publicKey.take(25)}... | ${key.keyType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row {
                IconButton(onClick = onRename, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "重命名")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}
