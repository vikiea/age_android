/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.model

enum class EncryptMode {
    BATCH_PACK,   // 打包加密（合并压缩）
    SEPARATE,     // 分别加密（单文件压缩）
    DECRYPT       // 解密
}
