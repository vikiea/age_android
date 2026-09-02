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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import android.net.Uri
import io.github.vikiea.age.R
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
import io.github.vikiea.age.ui.glass.AppTopBar
import io.github.vikiea.age.ui.glass.TopBarActionButton
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
        AppTopBar(
            actions = {
                TopBarActionButton(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    onClick = onNavigateToSettings,
                    backdrop = backdrop
                )
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
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.input_files))
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(EncryptMode.BATCH_PACK, stringResource(R.string.batch_encrypt)),
                        GlassSegmentOption(EncryptMode.SEPARATE, stringResource(R.string.separate_encrypt))
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
                        Text(stringResource(R.string.add_files))
                    }
                    GlassOutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.weight(1f), enabled = !uiState.isProcessing) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.select_folder))
                    }
                }

                if (uiState.files.isNotEmpty()) {
                    GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(stringResource(R.string.selected_file_count, uiState.files.sumOf { it.files.size }), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                GlassTextButton(onClick = { viewModel.clearFiles() }, enabled = !uiState.isProcessing) {
                                    Text(stringResource(R.string.common_clear), style = MaterialTheme.typography.labelSmall)
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
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_remove), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.encryption_method))
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(true, stringResource(R.string.passphrase_encrypt)),
                        GlassSegmentOption(false, stringResource(R.string.recipient_encrypt))
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
                        label = stringResource(R.string.enter_passphrase),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = stringResource(if (showPass) R.string.common_hide else R.string.common_show))
                            }
                        }
                    )
                } else {
                    if (keys.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            GlassTextField(value = uiState.selectedPublicKey, onValueChange = { viewModel.setSelectedPublicKey(it) }, label = stringResource(R.string.public_key), modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                keys.forEach { key ->
                                    DropdownMenuItem(text = { Text("${key.name}: ${key.publicKey.take(20)}...") }, onClick = { viewModel.setSelectedPublicKey(key.publicKey); expanded = false })
                                }
                            }
                        }
                    } else {
                        GlassTextField(value = uiState.selectedPublicKey, onValueChange = { viewModel.setSelectedPublicKey(it) }, label = stringResource(R.string.enter_public_key), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
            }

            if (uiState.mode == EncryptMode.BATCH_PACK) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(R.string.output))
                    GlassTextField(
                        value = uiState.outputFileBaseName,
                        onValueChange = { viewModel.setOutputFileBaseName(it) },
                        label = stringResource(R.string.output_file_name),
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
                        stringResource(R.string.processing_summary, uiState.processedCount, uiState.totalCount, uiState.successCount, uiState.failCount)
                    }
                    Text(statusText)
                    GlassOutlinedButton(onClick = viewModel::cancelOperation, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_cancel_task))
                    }
                }

                uiState.result?.let {
                    GlassStatusPanel(
                        title = stringResource(R.string.encrypt_complete),
                        tone = StatusTone.Success,
                        onDismiss = { viewModel.clearResult() },
                        actions = {
                            GlassOutlinedButton(
                                onClick = { viewModel.shareOutput() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.common_share_files))
                            }
                        }
                    ) {
                        Text(it)
                        uiState.outputDir?.let { dir -> Text(stringResource(R.string.common_directory, dir), style = MaterialTheme.typography.bodySmall) }
                        if (uiState.outputFiles.isNotEmpty()) {
                            FilePathTreeView(paths = uiState.outputFiles)
                        }
                    }
                }

                uiState.error?.let {
                    GlassStatusPanel(
                        title = stringResource(R.string.encrypt_failed),
                        tone = StatusTone.Error,
                        onDismiss = { viewModel.clearError() }
                    ) {
                        Text(it)
                    }
                }

                if (!uiState.isProcessing) {
                    GlassButton(
                        onClick = { viewModel.startEncrypt() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.result == null
                    ) {
                        Text(stringResource(R.string.start_encrypt))
                    }
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
