/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.encrypt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import android.net.Uri
import io.github.vikiea.age.core.model.EncryptMode
import io.github.vikiea.age.core.model.buildFileTree
import io.github.vikiea.age.ui.components.FilePathTreeView
import io.github.vikiea.age.ui.components.FileTreeView
import io.github.vikiea.age.ui.glass.GlassActionFooter
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassButton
import io.github.vikiea.age.ui.glass.GlassCard
import io.github.vikiea.age.ui.glass.GlassEmphasis
import io.github.vikiea.age.ui.glass.GlassOutlinedButton
import io.github.vikiea.age.ui.glass.GlassSegmentOption
import io.github.vikiea.age.ui.glass.GlassSegmentedControl
import io.github.vikiea.age.ui.glass.GlassStatusPanel
import io.github.vikiea.age.ui.glass.GlassTextButton
import io.github.vikiea.age.ui.glass.GlassTextField
import io.github.vikiea.age.ui.glass.GlassTopBar
import io.github.vikiea.age.ui.glass.GlassTonalSurface
import io.github.vikiea.age.ui.glass.StatusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptScreen(
    onNavigateToSettings: () -> Unit = {},
    sharedUris: List<Uri>? = null,
    onSharedUrisConsumed: () -> Unit = {},
    backdrop: GlassBackdrop? = null,
    viewModel: EncryptViewModel = hiltViewModel()
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
        GlassTopBar(
            title = "加密工作台",
            backdrop = backdrop,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), backdrop = backdrop, emphasis = GlassEmphasis.Normal) {
                SectionTitle("文件")
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(EncryptMode.BATCH_PACK, "打包加密"),
                        GlassSegmentOption(EncryptMode.SEPARATE, "分别加密")
                    ),
                    selectedValue = uiState.mode,
                    onSelected = { viewModel.setMode(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isProcessing
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassOutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f), enabled = !uiState.isProcessing) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加文件")
                    }
                    GlassOutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.weight(1f), enabled = !uiState.isProcessing) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("选择文件夹")
                    }
                }

                if (uiState.files.isNotEmpty()) {
                    GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                Text("已选项目 (${uiState.files.sumOf { it.files.size }} 个文件)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                GlassTextButton(onClick = { viewModel.clearFiles() }, enabled = !uiState.isProcessing) {
                                    Text("清空", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            uiState.files.forEachIndexed { index, file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    FileTreeView(
                                        nodes = buildFileTree(file.files.map { it.relativePath }),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.removeFile(index) }, enabled = !uiState.isProcessing, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "移除", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth(), backdrop = backdrop, emphasis = GlassEmphasis.Normal) {
                SectionTitle("加密方式")
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(true, "密码加密"),
                        GlassSegmentOption(false, "公钥加密")
                    ),
                    selectedValue = uiState.usePassphrase,
                    onSelected = { viewModel.setUsePassphrase(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isProcessing
                )

                if (uiState.usePassphrase) {
                    var showPass by remember { mutableStateOf(false) }
                    GlassTextField(
                        value = uiState.passphrase,
                        onValueChange = { viewModel.setPassphrase(it) },
                        label = "输入密码",
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
                            GlassTextField(value = uiState.selectedPublicKey, onValueChange = { viewModel.setSelectedPublicKey(it) }, label = "公钥", modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                keys.forEach { key ->
                                    DropdownMenuItem(text = { Text("${key.name}: ${key.publicKey.take(20)}...") }, onClick = { viewModel.setSelectedPublicKey(key.publicKey); expanded = false })
                                }
                            }
                        }
                    } else {
                        GlassTextField(value = uiState.selectedPublicKey, onValueChange = { viewModel.setSelectedPublicKey(it) }, label = "输入公钥 (age1xxx)", modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
            }

            if (uiState.mode == EncryptMode.BATCH_PACK) {
                GlassCard(modifier = Modifier.fillMaxWidth(), backdrop = backdrop, emphasis = GlassEmphasis.Normal) {
                    SectionTitle("输出")
                    GlassTextField(
                        value = uiState.outputFileBaseName,
                        onValueChange = { viewModel.setOutputFileBaseName(it) },
                        label = "输出文件名",
                        suffix = { Text(if (uiState.compressEnabled) ".tar.gz.age" else ".tar.age") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        if (uiState.shouldShowActionFooter) {
            GlassActionFooter(backdrop = backdrop) {
                if (uiState.isProcessing) {
                    LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth())
                    val statusText = if (uiState.phase.isNotEmpty()) {
                        "${uiState.phase} ${uiState.processedCount}/${uiState.totalCount}"
                    } else {
                        "处理中: ${uiState.processedCount}/${uiState.totalCount} (成功: ${uiState.successCount}, 失败: ${uiState.failCount})"
                    }
                    Text(statusText)
                }

                uiState.result?.let {
                    GlassStatusPanel(
                        title = "加密完成",
                        tone = StatusTone.Success,
                        onDismiss = { viewModel.clearResult() },
                        actions = {
                            GlassOutlinedButton(
                                onClick = { viewModel.shareOutput() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("分享文件")
                            }
                        }
                    ) {
                        Text(it)
                        uiState.outputDir?.let { dir -> Text("目录: $dir", style = MaterialTheme.typography.bodySmall) }
                        if (uiState.outputFiles.isNotEmpty()) {
                            FilePathTreeView(paths = uiState.outputFiles)
                        }
                    }
                }

                uiState.error?.let {
                    GlassStatusPanel(
                        title = "加密失败",
                        tone = StatusTone.Error,
                        onDismiss = { viewModel.clearError() }
                    ) {
                        Text(it)
                    }
                }

                GlassButton(
                    onClick = { viewModel.startEncrypt() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isProcessing && uiState.result == null
                ) {
                    Text("开始加密")
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
