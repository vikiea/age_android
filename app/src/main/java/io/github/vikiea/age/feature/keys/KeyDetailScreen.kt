/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.keys

import android.content.ClipData
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.vikiea.age.R
import io.github.vikiea.age.core.model.KeyEntry
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassButton
import io.github.vikiea.age.ui.glass.GlassDialog
import io.github.vikiea.age.ui.glass.GlassStatusPanel
import io.github.vikiea.age.ui.glass.GlassTextButton
import io.github.vikiea.age.ui.glass.GlassTextField
import io.github.vikiea.age.ui.glass.AppTopBar
import io.github.vikiea.age.ui.glass.TopBarActionButton
import io.github.vikiea.age.ui.glass.GlassTonalSurface
import io.github.vikiea.age.ui.glass.StatusTone
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun KeyDetailScreen(
    keyId: Long,
    onBack: () -> Unit,
    viewModel: KeysViewModel = hiltViewModel(),
    backdrop: GlassBackdrop? = null
) {
    val keys by viewModel.keys.collectAsState()
    val key = keys.find { it.id == keyId }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val publicKeyLabel = stringResource(R.string.public_key)
    val privateKeyLabel = stringResource(R.string.private_key)
    var privateKeyMaterial by remember(keyId) { mutableStateOf<String?>(null) }
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val canAuthenticate = remember(context) {
        BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val authenticateTitle = stringResource(R.string.authenticate_title)
    val authenticateSubtitle = stringResource(R.string.authenticate_subtitle)

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
            .setTitle(authenticateTitle)
            .setSubtitle(authenticateSubtitle)
            .setAllowedAuthenticators(authenticators)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            navigationIcon = {
                TopBarActionButton(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onBack,
                    backdrop = backdrop
                )
            }
        )

        if (key == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.key_not_found), modifier = Modifier.padding(16.dp))
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

            KeyTextSection(
                title = publicKeyLabel,
                value = key.publicKey,
                onCopy = { coroutineScope.launch { clipboard.setPlainText(publicKeyLabel, key.publicKey) } },
                backdrop = backdrop
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(stringResource(R.string.key_info), modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.showRenameDialog(key) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_rename), modifier = Modifier.size(18.dp))
                        }
                    }
                    InfoRow(stringResource(R.string.key_type), if (key.ageKeyType == io.github.vikiea.age.core.model.AgeKeyType.POST_QUANTUM) "ML-KEM-768 + X25519" else "X25519")
                    InfoRow(stringResource(R.string.has_private_key), stringResource(if (key.hasPrivateKey) R.string.common_yes else R.string.common_no))
                    InfoRow(
                        stringResource(R.string.created_at),
                        SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(key.createdAt)
                    )
            }

            if (key.hasPrivateKey) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        SectionTitle(stringResource(R.string.private_key))
                        if (privateKeyMaterial != null) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = privateKeyMaterial.orEmpty(),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = {
                                    authenticate {
                                        viewModel.withPrivateKey(key.id) { privateKey ->
                                            coroutineScope.launch { clipboard.setPlainText(privateKeyLabel, privateKey) }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.common_copy))
                                }
                            }
                        } else {
                            GlassButton(
                                onClick = {
                                    if (canAuthenticate) authenticate {
                                        viewModel.withPrivateKey(key.id) { privateKeyMaterial = it }
                                    } else context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(if (canAuthenticate) R.string.authenticate_to_view_private else R.string.configure_lock_to_view))
                            }
                        }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                    SectionTitle(stringResource(R.string.common_export))
                    GlassButton(
                        onClick = {
                            if (key.hasPrivateKey && canAuthenticate) authenticate { viewModel.prepareExport(key, true) }
                            else if (key.hasPrivateKey) context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                            else viewModel.prepareExport(key, false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.export_key_file))
                    }
                    Text(
                        text = stringResource(R.string.export_key_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

private suspend fun androidx.compose.ui.platform.Clipboard.setPlainText(
    label: String,
    text: String
) {
    setClipEntry(ClipEntry(ClipData.newPlainText(label, text)))
}

@Composable
private fun KeyTextSection(
    title: String,
    value: String,
    onCopy: () -> Unit,
    backdrop: GlassBackdrop?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.common_copy))
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
