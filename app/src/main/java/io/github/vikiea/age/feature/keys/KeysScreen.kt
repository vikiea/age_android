/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.keys

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.vikiea.age.R
import io.github.vikiea.age.core.model.KeyEntry
import io.github.vikiea.age.core.model.AgeKeyType
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassButton
import io.github.vikiea.age.ui.glass.GlassDialog
import io.github.vikiea.age.ui.glass.GlassFloatingActionButton
import io.github.vikiea.age.ui.glass.GlassOutlinedButton
import io.github.vikiea.age.ui.glass.GlassTextButton
import io.github.vikiea.age.ui.glass.GlassTextField
import io.github.vikiea.age.ui.glass.GlassTopBar
import io.github.vikiea.age.ui.glass.GlassTonalSurface
import io.github.vikiea.age.ui.glass.GlassStatusPanel
import io.github.vikiea.age.ui.glass.GlassSegmentOption
import io.github.vikiea.age.ui.glass.GlassSegmentedControl
import io.github.vikiea.age.ui.glass.StatusTone

@Composable
fun KeysScreen(
    viewModel: KeysViewModel = hiltViewModel(),
    onKeyClick: (Long) -> Unit = {},
    backdrop: GlassBackdrop? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val keys by viewModel.keys.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedKeyId by rememberSaveable { mutableStateOf<Long?>(null) }

    val keyFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.parseKeyFile(uri)
    }

    LaunchedEffect(uiState.error, uiState.success) {
        if (uiState.error != null || uiState.success != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.tip) {
        uiState.tip?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearTip()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = stringResource(R.string.keys_title), backdrop = backdrop)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                uiState.error?.let { message ->
                    GlassStatusPanel(
                        title = stringResource(R.string.common_operation_failed),
                        tone = StatusTone.Error,
                        onDismiss = { viewModel.clearMessages() }
                    ) {
                        Text(message)
                    }
                }

                uiState.success?.let { message ->
                    GlassStatusPanel(
                        title = stringResource(R.string.common_operation_complete),
                        tone = StatusTone.Success,
                        onDismiss = { viewModel.clearMessages() }
                    ) {
                        Text(message)
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val isWide = maxWidth >= 840.dp
                    val selected = keys.firstOrNull { it.id == selectedKeyId } ?: keys.firstOrNull()
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (isWide) 16.dp else 0.dp)) {
                        KeyList(
                            keys = keys,
                            onClick = { key -> if (isWide) selectedKeyId = key.id else onKeyClick(key.id) },
                            onRename = viewModel::showRenameDialog,
                            onDelete = { viewModel.deleteKey(it.id) },
                            modifier = if (isWide) Modifier.weight(0.46f) else Modifier.fillMaxWidth()
                        )
                        if (isWide) {
                            VerticalDivider()
                            KeyPreview(selected, onOpen = { selected?.let { onKeyClick(it.id) } }, modifier = Modifier.weight(0.54f))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )

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
                contentDescription = stringResource(R.string.import_key)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.common_import))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.common_import))
            }
            GlassFloatingActionButton(
                onClick = { viewModel.showGenerateDialog() },
                backdrop = backdrop,
                size = 64.dp,
                contentDescription = stringResource(R.string.generate_new_key)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.common_generate))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.generate_key))
            }
        }
    }

    if (uiState.showGenerateDialog) {
        GlassDialog(
            title = stringResource(R.string.generate_new_key),
            subtitle = stringResource(R.string.generate_key_subtitle),
            onDismissRequest = { viewModel.hideGenerateDialog() },
            backdrop = backdrop,
            actions = {
                GlassTextButton(onClick = { viewModel.hideGenerateDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(
                    onClick = { viewModel.generateKey() },
                    enabled = !uiState.isGenerating
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.generating))
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.common_generate))
                    }
                }
            }
        ) {
            GlassTextField(
                value = uiState.newName,
                onValueChange = { viewModel.setNewName(it) },
                label = stringResource(R.string.key_name),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            GlassSegmentedControl(
                options = listOf(
                    GlassSegmentOption(AgeKeyType.POST_QUANTUM, stringResource(R.string.post_quantum_key)),
                    GlassSegmentOption(AgeKeyType.X25519, stringResource(R.string.classic_x25519_key))
                ),
                selectedValue = uiState.newKeyType,
                onSelected = viewModel::setNewKeyType,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(if (uiState.newKeyType == AgeKeyType.POST_QUANTUM) R.string.pq_key_description else R.string.x25519_key_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (uiState.showImportDialog) {
        GlassDialog(
            title = stringResource(R.string.import_key),
            subtitle = stringResource(R.string.import_key_subtitle),
            onDismissRequest = { viewModel.hideImportDialog() },
            backdrop = backdrop,
            actions = {
                GlassTextButton(onClick = { viewModel.hideImportDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(onClick = { viewModel.importKey() }) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.common_import))
                }
            }
        ) {
            GlassTextField(
                value = uiState.importName,
                onValueChange = { viewModel.setImportName(it) },
                label = stringResource(R.string.key_name),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            GlassTextField(
                value = uiState.importPublicKey,
                onValueChange = { viewModel.setImportPublicKey(it) },
                label = stringResource(R.string.public_key_hint),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            GlassTextField(
                value = uiState.importPrivateKey,
                onValueChange = { viewModel.setImportPrivateKey(it) },
                label = stringResource(R.string.private_key_hint),
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
                Text(stringResource(if (uiState.importFileUri != null) R.string.choose_another_file else R.string.import_from_file))
            }
            Text(
                text = if (uiState.importFileUri != null) {
                    stringResource(R.string.key_file_selected)
                } else {
                    stringResource(R.string.key_file_supported)
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
            title = stringResource(R.string.rename_key),
            onDismissRequest = { viewModel.hideRenameDialog() },
            backdrop = backdrop,
            actions = {
                GlassTextButton(onClick = { viewModel.hideRenameDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(onClick = { viewModel.renameKey() }) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        ) {
            GlassTextField(
                value = uiState.renameText,
                onValueChange = { viewModel.setRenameText(it) },
                label = stringResource(R.string.new_name),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun KeyList(
    keys: List<KeyEntry>,
    onClick: (KeyEntry) -> Unit,
    onRename: (KeyEntry) -> Unit,
    onDelete: (KeyEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.keyring))
        HorizontalDivider()
        if (keys.isEmpty()) {
            Text(
                text = stringResource(R.string.no_keys),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(keys, key = { it.id }) { key ->
                    KeyRow(
                        key = key,
                        onClick = { onClick(key) },
                        onRename = { onRename(key) },
                        onDelete = { onDelete(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyPreview(key: KeyEntry?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.key_detail))
        HorizontalDivider()
        if (key == null) {
            Text(stringResource(R.string.no_keys), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Text(key.name, style = MaterialTheme.typography.titleLarge)
        Text(if (key.ageKeyType == AgeKeyType.POST_QUANTUM) "ML-KEM-768 + X25519" else "X25519")
        Text(key.publicKey, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
        Text(
            "${stringResource(R.string.has_private_key)}: ${stringResource(if (key.hasPrivateKey) R.string.common_yes else R.string.common_no)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        GlassOutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.key_detail))
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
                    text = "${key.publicKey.take(18)}… · ${if (key.ageKeyType == AgeKeyType.POST_QUANTUM) "PQ" else "X25519"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row {
                IconButton(onClick = onRename, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_rename))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
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
