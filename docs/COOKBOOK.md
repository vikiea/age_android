# Age Android - 构建与测试 Cookbook

## 目录

1. [环境依赖](#1-环境依赖)
2. [项目结构](#2-项目结构)
3. [Go 引擎编译 (gomobile)](#3-go-引擎编译-gomobile)
4. [Android 应用构建](#4-android-应用构建)
5. [模拟器设置与测试](#5-模拟器设置与测试)
6. [常见问题排查](#6-常见问题排查)

---

## 1. 环境依赖

### 必需工具

| 工具 | 版本 | 安装方式 |
|------|------|----------|
| Go | >= 1.25 | `brew install go` |
| Android SDK | API 35 | Android Studio 或 commandlinetools |
| Android NDK | 27.x | `sdkmanager "ndk;27.2.12479018"` |
| JDK | 11+ | `brew install openjdk@17` |
| gomobile | latest | `go install golang.org/x/mobile/cmd/gomobile@latest` |
| gobind | latest | `go install golang.org/x/mobile/cmd/gobind@latest` |

### 环境变量

```bash
# Android SDK
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=$ANDROID_HOME

# NDK (gomobile 需要)
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/27.2.12479018

# Go
export GOPATH=$HOME/go
export PATH=$PATH:$GOPATH/bin

# Java
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### 验证环境

```bash
go version                    # go version go1.25.x ...
java -version                 # openjdk version "17.x.x"
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --version
gomobile version              # gomobile version +hash
ndk-build --version           # NDK 27.x
```

---

## 2. 项目结构

```
age_android/
├── app/                          # Android 应用模块
│   ├── build.gradle.kts          # 应用构建配置
│   └── src/main/java/com/age/android/
│       ├── core/
│       │   ├── age/AgeEngine.kt  # 引擎接口定义
│       │   └── di/               # Hilt DI 模块
│       ├── ui/                   # Compose UI
│       └── data/                 # Room 数据库
├── age-engine/                   # Go 引擎封装模块
│   ├── build.gradle.kts          # 模块构建配置
│   ├── libs/
│   │   └── age-engine.aar        # gomobile 编译产物 (8.5MB)
│   └── src/main/
│       ├── go/
│       │   ├── age_engine.go     # Go 源码 (filippo.io/age 封装)
│       │   ├── go.mod
│       │   └── go.sum
│       └── java/com/age/engine/
│           └── AgeEngineImpl.kt  # Kotlin 桥接实现
├── build.gradle.kts              # 根构建配置
├── settings.gradle.kts           # 模块声明
├── gradle/wrapper/
│   └── gradle-wrapper.properties # Gradle 8.14
└── docs/
    └── COOKBOOK.md                # 本文档
```

### 关键依赖关系

```
app (Compose + Hilt + Room)
 └── age-engine (Android Library)
      └── age-engine.aar (gomobile 编译的 Go 原生库)
           └── filippo.io/age (Go age 加密库)
```

### 文件格式

加密输出格式：`.tar.gz.age`（先压缩后加密，与 bash 脚本参考实现一致）

---

## 3. Go 引擎编译 (gomobile)

### 3.1 安装 gomobile 工具链

```bash
# 安装 gomobile 和 gobind
go install golang.org/x/mobile/cmd/gomobile@latest
go install golang.org/x/mobile/cmd/gobind@latest

# 初始化 gomobile (只需执行一次)
gomobile init
```

### 3.2 获取 Go 依赖

```bash
cd age-engine/src/main/go

# 获取 age 库
go get filippo.io/age

# 获取 gomobile 绑定依赖 (编译必需)
go get golang.org/x/mobile/bind
```

### 3.3 编译 AAR

```bash
cd age-engine/src/main/go

gomobile bind \
  -target=android \
  -androidapi=26 \
  -o ../../libs/age-engine.aar \
  -javapkg=com.age.engine \
  .
```

参数说明：
- `-target=android`: 编译目标为 Android
- `-androidapi=26`: 最低 Android API 26 (Android 8.0)
- `-o ../../libs/age-engine.aar`: 输出到 `age-engine/libs/` 目录
- `-javapkg=com.age.engine`: 生成的 Java 包名为 `com.age.engine`
- `.`: 编译当前目录的 Go 包

### 3.4 验证编译产物

```bash
# 检查 AAR 文件
ls -lh age-engine/libs/age-engine.aar
# 应该约 8.5MB

# 查看 AAR 内容
unzip -l age-engine/libs/age-engine.aar

# 应包含:
# - classes.jar (含 com.age.engine.ageengine.Ageengine 等类)
# - jni/arm64-v8a/libgojni.so
# - jni/armeabi-v7a/libgojni.so
# - jni/x86/libgojni.so
# - jni/x86_64/libgojni.so
```

### 3.5 gomobile 生成的类

| 类名 | 说明 |
|------|------|
| `com.age.engine.ageengine.Ageengine` | 静态方法桥接类 |
| `com.age.engine.ageengine.KeyPairResult` | 密钥对结果（含 `getPublicKey()`, `getPrivateKey()`） |
| `go.Seq` | gomobile 运行时序列化 |
| `go.Universe` | gomobile 运行时全局注册 |

---

## 4. Android 应用构建

### 4.1 构建 Debug APK

```bash
# 在项目根目录执行
./gradlew :app:assembleDebug
```

构建产物位置：`app/build/outputs/apk/debug/app-debug.apk`

### 4.2 构建 Release APK

```bash
./gradlew :app:assembleRelease
```

### 4.3 清理构建

```bash
./gradlew clean
```

### 4.4 构建配置概览

| 配置项 | 值 |
|--------|-----|
| compileSdk | 35 |
| minSdk | 26 |
| targetSdk | 35 |
| Gradle | 8.14 |
| AGP | 8.7.3 |
| Kotlin | 2.1.0 |
| Hilt | 2.53.1 |
| Compose BOM | 2024.12.01 |
| Room | 2.6.1 |

---

## 5. 模拟器设置与测试

### 5.1 安装系统镜像

```bash
# 接受 SDK 许可证
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# 安装 Android 35 系统镜像 (arm64)
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
  "system-images;android-35;google_apis;arm64-v8a"
```

### 5.2 创建 AVD (Android Virtual Device)

```bash
# 创建 AVD
$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
  -n AgeTest \
  -k "system-images;android-35;google_apis;arm64-v8a" \
  -d "pixel_6"

# 验证 AVD 列表
$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager list avd
```

### 5.3 启动模拟器

```bash
# 启动模拟器 (后台运行)
$ANDROID_HOME/emulator/emulator -avd AgeTest &

# 等待模拟器就绪
$ANDROID_HOME/platform-tools/adb wait-for-device

# 检查设备连接
$ANDROID_HOME/platform-tools/adb devices
```

### 5.4 安装并启动应用

```bash
# 安装 APK
$ANDROID_HOME/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk

# 启动应用
$ANDROID_HOME/platform-tools/adb shell am start -n com.age.android/.MainActivity
```

### 5.5 查看日志

```bash
# 查看应用日志
$ANDROID_HOME/platform-tools/adb logcat -s "AgeAndroid"

# 过滤错误
$ANDROID_HOME/platform-tools/adb logcat *:E

# 清除日志
$ANDROID_HOME/platform-tools/adb logcat -c
```

---

## 6. 常见问题排查

### 6.1 gomobile: "no usable NDK"

**原因**: gomobile 无法自动找到 NDK

**解决**:
```bash
# 确认 NDK 已安装
ls $ANDROID_HOME/ndk/27.2.12479018/

# 如果未安装
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "ndk;27.2.12479018"
```

### 6.2 gomobile: "unsupported API version 16"

**原因**: NDK 27 不支持 API 16-20

**解决**: 添加 `-androidapi=26` 参数
```bash
gomobile bind -target=android -androidapi=26 ...
```

### 6.3 gomobile: "unable to import bind: no Go package in golang.org/x/mobile/bind"

**原因**: Go 模块缺少 mobile/bind 依赖

**解决**:
```bash
cd age-engine/src/main/go
go get golang.org/x/mobile/bind
```

### 6.4 Gradle: "Could not find :age-engine.aar"

**原因**: AAR 文件路径不正确

**解决**: 确认文件存在于 `age-engine/libs/age-engine.aar`，且 `build.gradle.kts` 中引用正确：
```kotlin
implementation(files("libs/age-engine.aar"))
```

### 6.5 模拟器: "No space left on device"

**原因**: 磁盘空间不足（系统镜像约 2GB）

**解决**:
```bash
# 清理 Go 模块缓存 (可释放数 GB)
go clean -modcache

# 清理 Gradle 缓存
rm -rf ~/.gradle/caches

# 检查可用空间
df -h /
```

### 6.6 模拟器: "SDK license not accepted"

**解决**:
```bash
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

### 6.7 APK 体积优化

当前 APK 约 32MB，主要因为包含多架构原生库。优化方案：

1. **ABI 分包** - 在 `app/build.gradle.kts` 中添加：
```kotlin
splits {
    abi {
        isEnable = true
        reset()
        include("arm64-v8a", "armeabi-v7a")
        isUniversalApk = false
    }
}
```

2. **启用 minify** - Release 构建时启用代码压缩：
```kotlin
release {
    isMinifyEnabled = true
    proguardFiles(...)
}
```

---

## 快速参考

### 完整构建流程（一键）

```bash
# 1. 编译 Go 引擎
cd age-engine/src/main/go && \
gomobile bind -target=android -androidapi=26 -o ../../libs/age-engine.aar -javapkg=com.age.engine . && \
cd ../../..

# 2. 构建 APK
./gradlew :app:assembleDebug

# 3. 安装到模拟器
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. 启动应用
$ANDROID_HOME/platform-tools/adb shell am start -n com.age.android/.MainActivity
```

### 常用命令速查

```bash
# 构建
./gradlew :app:assembleDebug           # Debug APK
./gradlew :app:assembleRelease         # Release APK
./gradlew clean                        # 清理

# 设备
adb devices                            # 列出设备
adb install -r app.apk                # 安装 APK
adb shell am start -n com.age.android/.MainActivity  # 启动应用
adb logcat                             # 查看日志
adb shell pm list packages | grep age  # 确认安装

# Go 引擎
cd age-engine/src/main/go
gomobile bind -target=android -androidapi=26 -o ../../libs/age-engine.aar -javapkg=com.age.engine .
go clean -modcache                     # 清理模块缓存
```
