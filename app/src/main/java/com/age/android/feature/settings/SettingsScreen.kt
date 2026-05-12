package com.age.android.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        viewModel.setOutputDirUri(uri)
    }

    val resolvedPath = remember(outputDirUri) {
        viewModel.resolveUriToPath(outputDirUri)
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

            Text("关于", style = MaterialTheme.typography.titleMedium)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("版本", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("加密引擎", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("age (filippo.io)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
