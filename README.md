# Age Android

Android native app wrapping the [age](https://filippo.io/age) encryption tool with streaming I/O, batch processing, and a local-first Compose interface.

## Features

- **Batch encryption**: Pack multiple files into tar/tar.gz, then encrypt as `.tar.gz.age` or `.tar.age`
- **Separate encryption**: Encrypt individual files or selected subfolders as standalone `.tar.age` units (original extensions hidden)
- **Folder tree handling**: Selected subfolders stay as removable units, with nested files shown and restored in a file-manager style tree
- **Passphrase & public key encryption**: Support both scrypt passphrase and X25519 key pair
- **Streaming I/O**: Entire pipeline uses streaming — handles 1GB+ files without OOM
- **Go engine**: tar/tar.gz compression and age encryption via gomobile, ~32KB memory footprint
- **Android-native intake and sharing**: Open files from the system picker, folders, or Android share intents, then share encrypted/decrypted outputs back out
- **Custom save directory**: SAF-based directory picker with persistent URI permission and `encrypted/` / `decrypted/` subfolders
- **User controls**: Theme mode, theme accent color, duplicate handling, compression, and processing concurrency are configurable from Settings
- **Key management**: Generate, import, and manage X25519 key pairs
- **Protected key detail**: Private keys can be revealed behind device biometric authentication when available
- **Operation history**: Track all encryption/decryption operations and inspect operation details
- **In-app updates and support**: Check GitHub Releases for APK updates, with optional donation QR support that does not change any app capability
- **Liquid Glass-inspired UI**: Compose Material 3 surfaces with backdrop-aware glass components and graceful fallback on older renderers

## Architecture

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Compose UI   │───▶│  ViewModel   │───▶│  Repository  │
│ Material3 +  │    │  MVVM + Flow │    │  Room + SAF  │
│ Glass system │    │              │    │              │
└──────────────┘    └──────┬───────┘    └──────────────┘
                           │
              ┌────────────▼────────────┐
              │ Go age engine (gomobile)│
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │ FileProvider / Android  │
              │ SAF / DownloadManager   │
              └─────────────────────────┘
```

- **UI**: Jetpack Compose + Material Design 3, with local `ui/glass` components backed by `io.github.kyant0:backdrop`
- **Navigation**: Navigation Compose routes for encryption, decryption, keys, history, detail, and settings screens
- **DI**: Hilt
- **DB**: Room (key storage, operation history, migrations)
- **Settings**: DataStore Preferences
- **File access**: Android Storage Access Framework plus `FileProvider` for output sharing and APK install intents
- **Engine**: Go `filippo.io/age` compiled via gomobile → AAR / JNI libs
- **Updates**: OkHttp + GitHub Release API + DownloadManager

## Tech Stack

| Layer | Technology |
|-------|-----------|
| App version | 3.0.1 (`versionCode` 9) |
| Language | Kotlin 2.3.10, Go 1.25 |
| Android | compileSdk 36, minSdk 26, targetSdk 36 |
| UI | Jetpack Compose BOM 2026.02.00, Material3, `io.github.kyant0:backdrop` |
| Navigation | Navigation Compose 2.8.5 |
| DI | Hilt 2.58, Hilt Navigation Compose 1.3.0 |
| DB | Room 2.8.4 |
| Settings | DataStore Preferences 1.1.1 |
| File / archive | AndroidX DocumentFile, Apache Commons Compress, FileProvider |
| Networking | OkHttp 4.12.0 for update checks |
| Security UX | AndroidX Biometric 1.1.0 for private-key reveal |
| Crypto | [filippo.io/age](https://filippo.io/age) 1.3.1 |
| Native | gomobile → AAR / extracted JNI libs |
| Build | Gradle 8.14, Android Gradle Plugin 8.13.2 |

## Build

### Prerequisites

- Android SDK (compile SDK 36, min SDK 26, target SDK 36)
- Go 1.25+ with gomobile (`go install golang.org/x/mobile/cmd/gomobile@latest`)
- NDK 27+ (for gomobile)

### Build Go Engine

```bash
make engine
```

### Build APK

```bash
make build        # incremental
make rebuild      # clean + build
make full         # engine + clean build
```

### Deploy to Device

```bash
make deploy       # build + install
make connect ADB_DEVICE=<ip>:<port>  # connect via TCP
```

Run `make help` for all available commands.

## File Format

| Mode | Compressed | Extension |
|------|-----------|-----------|
| Batch pack | Yes | `.tar.gz.age` |
| Batch pack | No | `.tar.age` |
| Separate | — | `.tar.age` |

All files are tar-packed before encryption to hide original file extensions. In separate mode, selected folders are first packed as their own tar units before encryption. Folder selections keep their relative paths in the archive, and decryption restores tar/tar.gz archives into the same tree structure when detected. Non-archive `.age` payloads fall back to single-file output.

## Privacy

Age Android does not collect, transmit, or store any user data. All operations are performed locally on your device.

[Privacy Policy](https://vikiea.github.io/age_android/privacy/)

[Official Website](https://vikiea.github.io/age_android/)

## Support

Age Android is free and open source. If it helps you, you can voluntarily support ongoing development. Support is optional and does not unlock, change, or limit any app feature.

自愿支持，不影响任何功能。

<p>
  <img src="docs/assets/donation/alipay.jpg" alt="Alipay donation QR code" width="260">
  <img src="docs/assets/donation/wechat_pay.jpg" alt="WeChat Pay donation QR code" width="260">
</p>

## License

[MIT](LICENSE)
