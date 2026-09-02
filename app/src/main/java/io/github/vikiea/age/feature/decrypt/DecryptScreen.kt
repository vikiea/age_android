/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.decrypt

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.vikiea.age.R
import io.github.vikiea.age.core.model.buildFileTree
import io.github.vikiea.age.ui.components.FilePathTreeView
import io.github.vikiea.age.ui.components.FileTreeView
import io.github.vikiea.age.ui.glass.GlassActionFooter
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassButton
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
fun DecryptScreen(
    onNavigateToSettings: () -> Unit = {},
    sharedUris: List<android.net.Uri>? = null,
    onSharedUrisConsumed: () -> Unit = {},
    backdrop: GlassBackdrop? = null,
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
                SectionTitle(stringResource(R.string.decryption_method))
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(true, stringResource(R.string.passphrase_decrypt)),
                        GlassSegmentOption(false, stringResource(R.string.identity_decrypt))
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
                    val privateKeys = keys.filter { it.hasPrivateKey }
                    if (privateKeys.isNotEmpty()) {
                        val selectedKeyName = privateKeys.find { it.id == uiState.selectedPrivateKeyId }?.name ?: ""
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            GlassTextField(
                                value = selectedKeyName,
                                onValueChange = {},
                                label = stringResource(R.string.select_private_key),
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                singleLine = true,
                                readOnly = true
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                privateKeys.forEach { key ->
                                    DropdownMenuItem(text = { Text(key.name) }, onClick = { viewModel.setSelectedPrivateKeyId(key.id); expanded = false })
                                }
                            }
                        }
                    } else {
                        Text(stringResource(R.string.no_private_keys), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (uiState.shouldShowActionFooter) {
            GlassActionFooter(backdrop = backdrop) {
                if (uiState.isProcessing) {
                    LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.common_processing_count, uiState.processedCount, uiState.totalCount))
                    GlassOutlinedButton(onClick = viewModel::cancelOperation, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_cancel_task))
                    }
                }

                uiState.result?.let {
                    GlassStatusPanel(
                        title = stringResource(R.string.decrypt_complete),
                        tone = StatusTone.Success,
                        onDismiss = { viewModel.clearResult() },
                        actions = {
                            if (uiState.successCount > 0) {
                                GlassOutlinedButton(
                                    onClick = { viewModel.shareOutput() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.common_share_files))
                                }
                            }
                        }
                    ) {
                        Text(it)
                        uiState.outputDir?.let { dir -> Text(stringResource(R.string.saved_to, dir), style = MaterialTheme.typography.bodySmall) }
                        if (uiState.outputFiles.isNotEmpty()) {
                            FilePathTreeView(paths = uiState.outputFiles)
                        }
                    }
                }

                uiState.error?.let {
                    GlassStatusPanel(
                        title = stringResource(R.string.decrypt_failed),
                        tone = StatusTone.Error,
                        onDismiss = { viewModel.clearError() }
                    ) {
                        Text(it)
                    }
                }

                if (!uiState.isProcessing) {
                    GlassButton(
                        onClick = { viewModel.startDecrypt() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.result == null
                    ) {
                        Text(stringResource(R.string.start_decrypt))
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
