/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.settings

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.age.android.core.data.DuplicateStrategy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val duplicateStrategy by viewModel.duplicateStrategy.collectAsState()
    val outputDirUri by viewModel.outputDirUri.collectAsState()
    val compressEnabled by viewModel.compressEnabled.collectAsState()
    val concurrency by viewModel.concurrency.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        viewModel.setOutputDirUri(uri)
    }

    val resolvedPath = remember(outputDirUri) {
        viewModel.resolveUriToPath(outputDirUri)
    }

    // Listen for download completion
    LaunchedEffect(updateState.downloadId) {
        val downloadId = updateState.downloadId ?: return@LaunchedEffect
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    viewModel.onDownloadComplete(id)
                    ctx.unregisterReceiver(this)
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── File Storage ──
            Text("文件存储", style = MaterialTheme.typography.titleMedium)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("保存位置", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = resolvedPath ?: "默认: ${viewModel.getDefaultDirPath()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { dirPicker.launch(null) }) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (outputDirUri != null) "更换目录" else "选择目录")
                        }
                        if (outputDirUri != null) {
                            OutlinedButton(onClick = { viewModel.setOutputDirUri(null) }) { Text("恢复默认") }
                        }
                    }
                    Text(
                        text = "加密和解密文件保存在同一目录的 encrypted/ 和 decrypted/ 子目录中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("文件名重复时", style = MaterialTheme.typography.bodyLarge)
                    Text("当输出目录已存在同名文件时的处理方式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = duplicateStrategy == DuplicateStrategy.RENAME,
                            onClick = { viewModel.setDuplicateStrategy(DuplicateStrategy.RENAME) },
                            label = { Text("自动重命名") }
                        )
                        FilterChip(
                            selected = duplicateStrategy == DuplicateStrategy.OVERWRITE,
                            onClick = { viewModel.setDuplicateStrategy(DuplicateStrategy.OVERWRITE) },
                            label = { Text("覆盖") }
                        )
                    }

                    Text(
                        text = when (duplicateStrategy) {
                            DuplicateStrategy.RENAME -> "自动添加 _1, _2 等后缀，保留原文件"
                            DuplicateStrategy.OVERWRITE -> "直接覆盖已有文件"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("打包时压缩", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (compressEnabled) "tar.gz 格式，体积更小但速度较慢" else "tar 格式，速度更快但体积更大",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = compressEnabled,
                        onCheckedChange = { viewModel.setCompressEnabled(it) }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("并发数", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "同时处理的文件数量，数值越大速度越快但内存占用更高",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val concurrencyOptions = listOf(1, 2, 4, 8)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        concurrencyOptions.forEach { value ->
                            FilterChip(
                                selected = concurrency == value,
                                onClick = { viewModel.setConcurrency(value) },
                                label = { Text("$value") }
                            )
                        }
                    }
                }
            }

            // ── About ──
            Text("关于", style = MaterialTheme.typography.titleMedium)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("版本", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(viewModel.getCurrentVersion(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("加密引擎", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("age (filippo.io)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("开源协议", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("MIT License", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Update check
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("检查更新", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "从 GitHub 获取最新版本",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.checkForUpdate() },
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("新版本: ${release.tagName}", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleSmall)
                                if (release.body.isNotBlank()) {
                                    Text(
                                        text = release.body.take(200) + if (release.body.length > 200) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "大小: ${formatFileSize(release.apkSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.downloadUpdate() },
                                        enabled = !updateState.isDownloading,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (updateState.isDownloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                            Text("下载中...")
                                        } else {
                                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("下载更新")
                                        }
                                    }
                                    OutlinedButton(onClick = { viewModel.dismissUpdate() }) {
                                        Text("忽略")
                                    }
                                }
                            }
                        }
                    }

                    updateState.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    updateState.message?.let { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Author & GitHub
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("作者", style = MaterialTheme.typography.bodyLarge)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("vikiea", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    Text("项目地址", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/vikiea/age_android")))
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("github.com/vikiea/age_android")
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://vikiea.github.io/age_android/privacy-policy.html")))
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("隐私政策")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
