/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.vikiea.age.R
import io.github.vikiea.age.core.data.DuplicateStrategy
import io.github.vikiea.age.core.data.ThemeAccent
import io.github.vikiea.age.core.data.ThemeMode
import io.github.vikiea.age.core.data.AppLanguage
import io.github.vikiea.age.core.update.ReleaseInfo
import io.github.vikiea.age.ui.glass.GlassBackdrop
import io.github.vikiea.age.ui.glass.GlassButton
import io.github.vikiea.age.ui.glass.GlassEmphasis
import io.github.vikiea.age.ui.glass.GlassOutlinedButton
import io.github.vikiea.age.ui.glass.GlassSegmentOption
import io.github.vikiea.age.ui.glass.GlassSegmentedControl
import io.github.vikiea.age.ui.glass.GlassSurface
import io.github.vikiea.age.ui.glass.GlassSwitch
import io.github.vikiea.age.ui.glass.GlassTextButton
import io.github.vikiea.age.ui.glass.AppTopBar
import io.github.vikiea.age.ui.glass.TopBarActionButton
import java.util.Locale
import io.github.vikiea.age.ui.glass.GlassTonalSurface

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backdrop: GlassBackdrop? = null
) {
    val duplicateStrategy by viewModel.duplicateStrategy.collectAsState()
    val outputDirUri by viewModel.outputDirUri.collectAsState()
    val compressEnabled by viewModel.compressEnabled.collectAsState()
    val concurrency by viewModel.concurrency.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val themeAccent by viewModel.themeAccent.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    var showDonationDialog by remember { mutableStateOf(false) }

    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        viewModel.setOutputDirUri(uri)
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onInstallPermissionResult()
    }

    val resolvedPath = remember(outputDirUri) {
        viewModel.resolveUriToPath(outputDirUri)
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            SectionTitle(stringResource(R.string.appearance))
            SettingsSection(backdrop = backdrop, title = stringResource(R.string.theme)) {
                Text(
                    text = themeModeDescription(themeMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlassSegmentedControl(
                    options = ThemeMode.entries.map { mode -> GlassSegmentOption(mode, themeModeLabel(mode)) },
                    selectedValue = themeMode,
                    onSelected = { viewModel.setThemeMode(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsSection(backdrop = backdrop, title = stringResource(R.string.language)) {
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(AppLanguage.SYSTEM, stringResource(R.string.follow_system)),
                        GlassSegmentOption(AppLanguage.ZH_CN, stringResource(R.string.chinese)),
                        GlassSegmentOption(AppLanguage.ENGLISH, stringResource(R.string.english))
                    ),
                    selectedValue = appLanguage,
                    onSelected = viewModel::setAppLanguage,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsSection(backdrop = backdrop, title = stringResource(R.string.theme_color)) {
                ThemeAccentPalette(
                    selectedAccent = themeAccent,
                    onAccentSelected = { viewModel.setThemeAccent(it) }
                )
            }

            SectionTitle(stringResource(R.string.file_storage))
            SettingsSection(backdrop = backdrop, title = stringResource(R.string.save_location)) {
                Text(
                    text = resolvedPath ?: stringResource(R.string.default_location, viewModel.getDefaultDirPath()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassOutlinedButton(onClick = { dirPicker.launch(null) }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (outputDirUri != null) R.string.change_directory else R.string.choose_directory))
                    }
                    if (outputDirUri != null) {
                        GlassOutlinedButton(onClick = { viewModel.setOutputDirUri(null) }) {
                            Text(stringResource(R.string.restore_default))
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.save_location_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(backdrop = backdrop, title = stringResource(R.string.duplicate_files)) {
                Text(
                    text = stringResource(R.string.duplicate_files_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(DuplicateStrategy.RENAME, stringResource(R.string.auto_rename)),
                        GlassSegmentOption(DuplicateStrategy.OVERWRITE, stringResource(R.string.overwrite))
                    ),
                    selectedValue = duplicateStrategy,
                    onSelected = { viewModel.setDuplicateStrategy(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = when (duplicateStrategy) {
                        DuplicateStrategy.RENAME -> stringResource(R.string.auto_rename_description)
                        DuplicateStrategy.OVERWRITE -> stringResource(R.string.overwrite_description)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ToggleSection(
                backdrop = backdrop,
                title = stringResource(R.string.compress_when_packing),
                description = stringResource(if (compressEnabled) R.string.compress_on_description else R.string.compress_off_description),
                checked = compressEnabled,
                onCheckedChange = { viewModel.setCompressEnabled(it) }
            )

            SettingsSection(backdrop = backdrop, title = stringResource(R.string.concurrency)) {
                Text(
                    text = stringResource(R.string.concurrency_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassSegmentedControl(
                        options = listOf(1, 2, 4, 8).map { value ->
                            GlassSegmentOption(value, "$value")
                        },
                        selectedValue = concurrency,
                        onSelected = { viewModel.setConcurrency(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionTitle(stringResource(R.string.about))
            SettingsSection(backdrop = backdrop, title = stringResource(R.string.app_info)) {
                DetailRow(stringResource(R.string.version), viewModel.getCurrentVersion())
                DetailRow(stringResource(R.string.encryption_engine), "age (filippo.io)")
                DetailRow(stringResource(R.string.open_source_license), "MIT License")
            }

            UpdateSection(
                backdrop = backdrop,
                updateState = updateState,
                onCheck = { viewModel.checkForUpdate() },
                onDownload = { viewModel.downloadUpdate() },
                onInstall = { viewModel.installDownloadedUpdate() },
                onRequestInstallPermission = {
                    val intent = viewModel.createInstallPermissionIntent()
                    if (intent != null) {
                        installPermissionLauncher.launch(intent)
                    } else {
                        viewModel.requestInstallPermission()
                    }
                },
                onDismiss = { viewModel.dismissUpdate() }
            )

                SettingsSection(backdrop = backdrop, title = stringResource(R.string.author_project)) {
                DetailRow(stringResource(R.string.author), "vikiea")
                GlassOutlinedButton(
                    onClick = { showDonationDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.support_development))
                }
                ExternalLinkButton(
                    text = "github.com/vikiea/age_android",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/vikiea/age_android".toUri()
                            )
                        )
                    }
                )
                ExternalLinkButton(
                    text = stringResource(R.string.privacy_policy),
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://vikiea.github.io/age_android/privacy/".toUri()
                            )
                        )
                    }
                )
                }
            }
        }
    }

    if (showDonationDialog) {
        DonationDialog(
            backdrop = backdrop,
            onDismiss = { showDonationDialog = false }
        )
    }
}

@Composable
private fun DonationDialog(
    backdrop: GlassBackdrop?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            backdrop = backdrop,
            emphasis = GlassEmphasis.Strong
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.donation_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.donation_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DonationQrGrid()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassTextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationQrGrid() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 430.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DonationQrCard(
                    title = stringResource(R.string.alipay),
                    hint = stringResource(R.string.scan_alipay),
                    drawableRes = R.drawable.donation_alipay_qr,
                    modifier = Modifier.weight(1f)
                )
                DonationQrCard(
                    title = stringResource(R.string.wechat_pay),
                    hint = stringResource(R.string.scan_wechat),
                    drawableRes = R.drawable.donation_wechat_qr,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DonationQrCard(
                    title = stringResource(R.string.alipay),
                    hint = stringResource(R.string.scan_alipay),
                    drawableRes = R.drawable.donation_alipay_qr,
                    modifier = Modifier.fillMaxWidth()
                )
                DonationQrCard(
                    title = stringResource(R.string.wechat_pay),
                    hint = stringResource(R.string.scan_wechat),
                    drawableRes = R.drawable.donation_wechat_qr,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DonationQrCard(
    title: String,
    hint: String,
    drawableRes: Int,
    modifier: Modifier = Modifier
) {
    GlassTonalSurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(androidx.compose.ui.graphics.Color.White)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(drawableRes),
                    contentDescription = stringResource(R.string.payment_qr_description, title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsSection(
    backdrop: GlassBackdrop?,
    title: String,
    emphasis: GlassEmphasis = GlassEmphasis.Normal,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            content()
    }
}

@Composable
private fun ToggleSection(
    backdrop: GlassBackdrop?,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            GlassSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
    }
}

@Composable
private fun ThemeAccentPalette(
    selectedAccent: ThemeAccent,
    onAccentSelected: (ThemeAccent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = themeAccentDescription(selectedAccent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp)
        ) {
            items(ThemeAccent.entries, key = { it.name }) { accent ->
                AccentSwatch(
                    accent = accent,
                    selected = accent == selectedAccent,
                    onClick = { onAccentSelected(accent) },
                    modifier = Modifier.width(92.dp)
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: ThemeAccent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
    val labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                MaterialTheme.shapes.medium
            )
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.previewBrush)
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                    CircleShape
                )
        )
        Text(
            text = themeAccentLabel(accent),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UpdateSection(
    backdrop: GlassBackdrop?,
    updateState: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRequestInstallPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    SettingsSection(backdrop = backdrop, title = stringResource(R.string.check_updates)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.check_updates_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            GlassOutlinedButton(
                onClick = onCheck,
                enabled = !updateState.isChecking
            ) {
                if (updateState.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.checking))
                } else {
                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.check_updates))
                }
            }
        }

        updateState.releaseInfo?.let { release ->
            ReleasePanel(
                release = release,
                isDownloading = updateState.isDownloading,
                isDownloaded = updateState.downloadedApkPath != null,
                requiresInstallPermission = updateState.requiresInstallPermission,
                onDownload = onDownload,
                onInstall = onInstall,
                onRequestInstallPermission = onRequestInstallPermission,
                onDismiss = onDismiss
            )
        }

        updateState.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        updateState.message?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReleasePanel(
    release: ReleaseInfo,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    requiresInstallPermission: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRequestInstallPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassTonalSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.new_version, release.tagName),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall
            )
            if (release.body.isNotBlank()) {
                Text(
                    text = release.body.take(200) + if (release.body.length > 200) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.download_size, formatFileSize(release.apkSize)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    onClick = when {
                        requiresInstallPermission -> onRequestInstallPermission
                        isDownloaded -> onInstall
                        else -> onDownload
                    },
                    enabled = !isDownloading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.downloading))
                    } else if (isDownloaded) {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (requiresInstallPermission) R.string.request_install_permission else R.string.install_now))
                    } else {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.download_update))
                    }
                }
                GlassOutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ignore))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExternalLinkButton(
    text: String,
    onClick: () -> Unit
) {
    GlassOutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.LIGHT -> R.string.theme_light
    }
)

@Composable
private fun themeModeDescription(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system_description
        ThemeMode.DARK -> R.string.theme_dark_description
        ThemeMode.LIGHT -> R.string.theme_light_description
    }
)

@Composable
private fun themeAccentLabel(accent: ThemeAccent): String = stringResource(
    when (accent) {
        ThemeAccent.LIQUID_DEFAULT -> R.string.accent_default
        ThemeAccent.LIQUID_AURORA -> R.string.accent_aurora
        ThemeAccent.LIQUID_SUNRISE -> R.string.accent_sunrise
        ThemeAccent.LIQUID_OCEAN -> R.string.accent_ocean
        ThemeAccent.LIQUID_GRAPE -> R.string.accent_grape
        ThemeAccent.SOLID_GREEN -> R.string.accent_green
        ThemeAccent.SOLID_BLUE -> R.string.accent_blue
        ThemeAccent.SOLID_RED -> R.string.accent_red
        ThemeAccent.SOLID_PURPLE -> R.string.accent_purple
        ThemeAccent.SOLID_ORANGE -> R.string.accent_orange
    }
)

@Composable
private fun themeAccentDescription(accent: ThemeAccent): String = stringResource(
    when (accent) {
        ThemeAccent.LIQUID_DEFAULT -> R.string.accent_default_description
        ThemeAccent.LIQUID_AURORA -> R.string.accent_aurora_description
        ThemeAccent.LIQUID_SUNRISE -> R.string.accent_sunrise_description
        ThemeAccent.LIQUID_OCEAN -> R.string.accent_ocean_description
        ThemeAccent.LIQUID_GRAPE -> R.string.accent_grape_description
        ThemeAccent.SOLID_GREEN -> R.string.accent_green_description
        ThemeAccent.SOLID_BLUE -> R.string.accent_blue_description
        ThemeAccent.SOLID_RED -> R.string.accent_red_description
        ThemeAccent.SOLID_PURPLE -> R.string.accent_purple_description
        ThemeAccent.SOLID_ORANGE -> R.string.accent_orange_description
    }
)

private val ThemeAccent.previewBrush: Brush
    get() = when (this) {
        ThemeAccent.LIQUID_DEFAULT -> Brush.linearGradient(listOf(Color(0xFF0E8F68), Color(0xFF1A9DB5)))
        ThemeAccent.LIQUID_AURORA -> Brush.linearGradient(listOf(Color(0xFF3D6FE8), Color(0xFF9B5DE5)))
        ThemeAccent.LIQUID_SUNRISE -> Brush.linearGradient(listOf(Color(0xFFC55240), Color(0xFFD64C7F)))
        ThemeAccent.LIQUID_OCEAN -> Brush.linearGradient(listOf(Color(0xFF007C8E), Color(0xFF16A084)))
        ThemeAccent.LIQUID_GRAPE -> Brush.linearGradient(listOf(Color(0xFF8A3FB0), Color(0xFFCC4778)))
        ThemeAccent.SOLID_GREEN -> Brush.linearGradient(listOf(Color(0xFF0E8F68), Color(0xFF0E8F68)))
        ThemeAccent.SOLID_BLUE -> Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF1565C0)))
        ThemeAccent.SOLID_RED -> Brush.linearGradient(listOf(Color(0xFFC62828), Color(0xFFC62828)))
        ThemeAccent.SOLID_PURPLE -> Brush.linearGradient(listOf(Color(0xFF6A1B9A), Color(0xFF6A1B9A)))
        ThemeAccent.SOLID_ORANGE -> Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFEF6C00)))
    }
