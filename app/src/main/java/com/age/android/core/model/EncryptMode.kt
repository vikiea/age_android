package com.age.android.core.model

enum class EncryptMode {
    BATCH_PACK,   // 打包加密（合并压缩）
    SEPARATE,     // 分别加密（单文件压缩）
    DECRYPT       // 解密
}
