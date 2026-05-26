/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.age.android.R
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.ThemeAccent
import com.age.android.core.data.ThemeMode
import com.age.android.core.update.ReleaseInfo
import com.age.android.ui.glass.GlassBackdrop
import com.age.android.ui.glass.GlassButton
import com.age.android.ui.glass.GlassEmphasis
import com.age.android.ui.glass.GlassOutlinedButton
import com.age.android.ui.glass.GlassSegmentOption
import com.age.android.ui.glass.GlassSegmentedControl
import com.age.android.ui.glass.GlassSurface
import com.age.android.ui.glass.GlassSwitch
import com.age.android.ui.glass.GlassTextButton
import com.age.android.ui.glass.GlassTopBar
import java.util.Locale
import com.age.android.ui.glass.GlassTonalSurface

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
    val glassEffectEnabled by viewModel.glassEffectEnabled.collectAsState()
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

    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "设置",
            backdrop = backdrop,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
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
            SectionTitle("外观")
            SettingsSection(backdrop = backdrop, title = "主题") {
                Text(
                    text = themeMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlassSegmentedControl(
                    options = ThemeMode.entries.map { mode -> GlassSegmentOption(mode, mode.label) },
                    selectedValue = themeMode,
                    onSelected = { viewModel.setThemeMode(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsSection(backdrop = backdrop, title = "主题颜色") {
                ThemeAccentPalette(
                    selectedAccent = themeAccent,
                    onAccentSelected = { viewModel.setThemeAccent(it) }
                )
            }

            ToggleSection(
                backdrop = backdrop,
                title = "半透明玻璃效果",
                description = if (glassEffectEnabled) {
                    "开启背景透光、模糊与高光层次"
                } else {
                    "关闭透光效果，浅色为纯白，深色为纯黑"
                },
                checked = glassEffectEnabled,
                onCheckedChange = { viewModel.setGlassEffectEnabled(it) }
            )

            SectionTitle("文件存储")
            SettingsSection(backdrop = backdrop, title = "保存位置") {
                Text(
                    text = resolvedPath ?: "默认: ${viewModel.getDefaultDirPath()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassOutlinedButton(onClick = { dirPicker.launch(null) }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (outputDirUri != null) "更换目录" else "选择目录")
                    }
                    if (outputDirUri != null) {
                        GlassOutlinedButton(onClick = { viewModel.setOutputDirUri(null) }) {
                            Text("恢复默认")
                        }
                    }
                }
                Text(
                    text = "加密和解密文件保存在同一目录的 encrypted/ 和 decrypted/ 子目录中",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(backdrop = backdrop, title = "文件名重复时") {
                Text(
                    text = "当输出目录已存在同名文件时的处理方式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlassSegmentedControl(
                    options = listOf(
                        GlassSegmentOption(DuplicateStrategy.RENAME, "自动重命名"),
                        GlassSegmentOption(DuplicateStrategy.OVERWRITE, "覆盖")
                    ),
                    selectedValue = duplicateStrategy,
                    onSelected = { viewModel.setDuplicateStrategy(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = when (duplicateStrategy) {
                        DuplicateStrategy.RENAME -> "自动添加 _1, _2 等后缀，保留原文件"
                        DuplicateStrategy.OVERWRITE -> "直接覆盖已有文件"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ToggleSection(
                backdrop = backdrop,
                title = "打包时压缩",
                description = if (compressEnabled) {
                    "tar.gz 格式，体积更小但速度较慢"
                } else {
                    "tar 格式，速度更快但体积更大"
                },
                checked = compressEnabled,
                onCheckedChange = { viewModel.setCompressEnabled(it) }
            )

            SettingsSection(backdrop = backdrop, title = "并发数") {
                Text(
                    text = "同时处理的文件数量，数值越大速度越快但内存占用更高",
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

            SectionTitle("关于")
            SettingsSection(backdrop = backdrop, title = "应用信息") {
                DetailRow("版本", viewModel.getCurrentVersion())
                HorizontalDivider()
                DetailRow("加密引擎", "age (filippo.io)")
                HorizontalDivider()
                DetailRow("开源协议", "MIT License")
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

            SettingsSection(backdrop = backdrop, title = "作者与项目") {
                DetailRow("作者", "vikiea")
                HorizontalDivider()
                GlassOutlinedButton(
                    onClick = { showDonationDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("支持开发")
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
                    text = "隐私政策",
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
                        text = "感谢你的支持",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "如果 Age 对你有帮助，可以通过下面的二维码支持开发与维护。自愿支持，不影响任何功能。",
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
                        Text("关闭")
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
                    title = "支付宝",
                    hint = "请使用支付宝扫一扫",
                    drawableRes = R.drawable.donation_alipay_qr,
                    modifier = Modifier.weight(1f)
                )
                DonationQrCard(
                    title = "微信支付",
                    hint = "请使用微信扫一扫",
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
                    title = "支付宝",
                    hint = "请使用支付宝扫一扫",
                    drawableRes = R.drawable.donation_alipay_qr,
                    modifier = Modifier.fillMaxWidth()
                )
                DonationQrCard(
                    title = "微信支付",
                    hint = "请使用微信扫一扫",
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
                    contentDescription = "$title 收款码",
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
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backdrop = backdrop,
        emphasis = emphasis
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
}

@Composable
private fun ToggleSection(
    backdrop: GlassBackdrop?,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backdrop = backdrop,
        emphasis = GlassEmphasis.Normal
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
}

@Composable
private fun ThemeAccentPalette(
    selectedAccent: ThemeAccent,
    onAccentSelected: (ThemeAccent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = selectedAccent.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccentGroup(
            title = "Liquid Glass",
            accents = ThemeAccent.entries.filter { it.liquidGlass },
            selectedAccent = selectedAccent,
            onAccentSelected = onAccentSelected
        )
        AccentGroup(
            title = "纯色强调",
            accents = ThemeAccent.entries.filterNot { it.liquidGlass },
            selectedAccent = selectedAccent,
            onAccentSelected = onAccentSelected
        )
    }
}

@Composable
private fun AccentGroup(
    title: String,
    accents: List<ThemeAccent>,
    selectedAccent: ThemeAccent,
    onAccentSelected: (ThemeAccent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 380.dp
            val rows = if (compact) accents.chunked(3) else accents.chunked(5)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowAccents ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowAccents.forEach { accent ->
                            AccentSwatch(
                                accent = accent,
                                selected = accent == selectedAccent,
                                onClick = { onAccentSelected(accent) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat((if (compact) 3 else 5) - rowAccents.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
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
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.previewBrush)
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                    CircleShape
                )
        )
        Text(
            text = accent.label,
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
    SettingsSection(backdrop = backdrop, title = "检查更新") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "从 GitHub 获取最新版本",
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
                    Text("检查中...")
                } else {
                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("检查更新")
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
                text = "新版本: ${release.tagName}",
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
                text = "大小: ${formatFileSize(release.apkSize)}",
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
                        Text("下载中...")
                    } else if (isDownloaded) {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (requiresInstallPermission) "申请安装权限" else "立即安装")
                    } else {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("下载更新")
                    }
                }
                GlassOutlinedButton(onClick = onDismiss) {
                    Text("忽略")
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

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.DARK -> "深色"
        ThemeMode.LIGHT -> "浅色"
    }

private val ThemeMode.description: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "根据系统外观自动切换"
        ThemeMode.DARK -> "始终使用深色主题"
        ThemeMode.LIGHT -> "始终使用浅色主题"
    }

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
