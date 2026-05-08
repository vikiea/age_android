# Age Android — 设计规格文档

## 概述

为开源加密工具 [age](https://github.com/FiloSottile/age) 开发 Android 原生 App，提供 UI 封装、批量处理和密钥管理能力。参考 bash 脚本的核心功能（打包加密、分别加密、批量解密），并扩展密钥管理、文件浏览器、操作历史和分享集成。

## 技术栈

| 层级 | 技术选型 |
|------|---------|
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + Clean Architecture |
| DI | Hilt |
| 数据库 | Room（操作历史、密钥元数据） |
| 加密引擎 | age Go 库通过 Gomobile 编译为 .aar |
| 文件访问 | Storage Access Framework (SAF) |
| 并发 | Kotlin Coroutines + Flow + Semaphore |
| minSdk | 26 (Android 8.0) |
| targetSdk | 35 |

## 模块划分

```
app/
├── feature/
│   ├── encrypt/       # 加密功能（打包/分别加密）
│   ├── decrypt/       # 解密功能（批量解密）
│   ├── keys/          # 密钥管理（生成/导入/导出/列表）
│   └── history/       # 操作历史
├── core/
│   ├── age-engine/    # Gomobile 封装层
│   ├── data/          # Room 数据库、Repository
│   ├── model/         # 数据模型
│   ├── file-picker/   # SAF 文件选择器封装
│   └── share/         # 分享集成
└── app/               # 主 Activity、导航、主题
```

## 导航结构

底部导航栏（Bottom Navigation），4 个 Tab：

1. **加密** — 默认页，打包加密/分别加密
2. **解密** — 批量解密
3. **密钥** — 密钥管理
4. **历史** — 操作记录

## 页面设计

### Tab 1 — 加密

- 顶部：加密模式切换（打包加密 / 分别加密）
- 文件选择区：点击添加文件/目录，支持多选，显示文件列表
- 加密方式：密码加密（输入框）/ 公钥加密（从密钥库选择或手动输入 age1xxx）
- 参数：输出文件名（打包模式）/ 输出目录（分别模式）、是否 ASCII 装甲
- 底部：大按钮「开始加密」+ 进度条（分别模式下显示并发进度）

### Tab 2 — 解密

- 文件选择：选择 .tar.gz.age 文件（支持多选）
- 解密方式：密码输入 / 选择私钥
- 输出目录选择
- 底部：「开始解密」+ 批量进度

### Tab 3 — 密钥管理

- 密钥列表：显示已保存的密钥对（名称、公钥摘要、创建时间）
- 操作：生成新密钥对、从文件导入、导出公钥/私钥、删除
- 密钥详情：公钥（可复制）、关联的私钥状态

### Tab 4 — 历史

- 按时间倒序显示操作记录
- 每条记录：操作类型（加密/解密）、文件名、时间、状态（成功/失败）、模式
- 支持筛选和清除历史

## 数据模型

### KeyEntry（密钥）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 用户自定义名称 |
| publicKey | String | age1xxx 公钥 |
| keyType | Enum | PASSPHRASE / AGE_KEY / SSH_KEY |
| hasPrivateKey | Boolean | 是否有关联私钥 |
| keystoreAlias | String? | Android Keystore 别名 |
| createdAt | Long | 创建时间戳 |

### OperationRecord（操作历史）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| type | Enum | ENCRYPT / DECRYPT |
| mode | Enum | BATCH_PACK / SEPARATE / DECRYPT |
| inputFiles | List\<String\> | 输入文件路径 |
| outputPath | String | 输出路径 |
| recipientInfo | String | 公钥摘要或"密码加密" |
| status | Enum | RUNNING / SUCCESS / FAILED |
| errorMessage | String? | 错误详情 |
| timestamp | Long | 操作时间戳 |

## 核心流程

### 加密流程（分别加密）

1. 用户选择文件 → SAF 返回 Uri 列表
2. 用户选择公钥或输入密码
3. ViewModel 调用 Repository
4. Repository 对每个文件启动协程（最多 4 个并发，Semaphore 控制）
5. 每个协程：读取文件 → 调用 age-engine Go 库压缩+加密 → 写入输出
6. 通过 Flow 上报每个文件的进度
7. 完成后写入 OperationRecord

### 解密流程

1. 用户选择 .tar.gz.age 文件
2. 用户输入密码或选择私钥
3. 并发解密（默认 4 并发）
4. 每个协程：解密 → 解压 → 写入输出目录
5. 汇总报告

## 密钥存储

- **私钥**：通过 Android Keystore 加密后存储在 Room 的 KeyEntry 表中
- **公钥**：明文存储（公开信息）
- **导入/导出**：支持从文件导入 `.txt` 格式的密钥，导出公钥到文件供分享
- **Keystore 优先**：默认使用 Android Keystore 生成和存储密钥，同时支持从文件导入

## 错误处理

- Go 层异常通过 Gomobile 转为 Java Exception，统一在 Repository 层捕获
- UI 层通过 Snackbar 展示错误信息，关键操作失败弹 Dialog 详情
- 密码错误、密钥不匹配等场景给出明确中文提示
- 文件权限不足时引导用户重新授权 SAF

## 并发控制

- 分别加密/批量解密默认 4 并发，最大 8 并发（用户可在设置中调整）
- 使用 `Semaphore` 控制并发数，避免 OOM
- 单个文件失败不影响其他文件，最终汇总报告成功/失败数量

## 分享集成

- 加密完成后通过 `Intent.ACTION_SEND` 分享 .age 文件
- 注册 `IntentFilter` 接收其他 App 分享来的文件，直接跳转到加密/解密页

## 文件格式

与 bash 脚本保持一致：
- 加密后缀固定为 `.tar.gz.age`
- 打包加密：所有输入文件合并为一个 tar.gz 后加密
- 分别加密：每个文件单独压缩后加密

## 测试策略

- **Unit Test**: ViewModel、Repository、age-engine 封装层
- **Integration Test**: 完整加密→解密往返测试
- **UI Test**: 关键流程的 Compose UI 测试
