# Age Android

Android native app wrapping the [age](https://filippo.io/age) encryption tool with post-quantum keys, streaming I/O, cancellable batch processing, and a local-first adaptive Compose interface.

## Features

- **Batch encryption**: Pack multiple files into tar/tar.gz, then encrypt as `.tar.gz.age` or `.tar.age`
- **Separate encryption**: Encrypt individual files or selected subfolders as standalone `.tar.age` units (original extensions hidden)
- **Folder tree handling**: Selected subfolders stay as removable units, with nested files shown and restored in a file-manager style tree
- **Post-quantum by default**: Generate hybrid ML-KEM-768 + X25519 keys, with classic X25519 available explicitly
- **Streaming I/O**: Entire pipeline uses streaming — handles 1GB+ files without OOM
- **Go engine**: tar/tar.gz compression and age encryption via gomobile, ~32KB memory footprint
- **Android-native intake and sharing**: Open files from the system picker, folders, or Android share intents, then share encrypted/decrypted outputs back out
- **Custom save directory**: SAF-based directory picker with persistent URI permission and `encrypted/` / `decrypted/` subfolders
- **Secure key storage**: Private keys are encrypted with an Android Keystore AES-256-GCM key and stored outside Room in `noBackupFilesDir`
- **Protected sensitive actions**: Revealing, copying, or exporting a private key requires strong biometrics or device credentials
- **Cancellable work**: Tar, gzip, encryption, decryption, SAF copies, and output writes can be cancelled with partial-output cleanup
- **Bilingual UI**: Follow the system language or choose English / Simplified Chinese in Settings
- **Adaptive Material 3 UI**: Bottom navigation on phones, navigation rail on wider windows, dynamic color, and restrained Backdrop glass for navigation and overlays
- **Detailed history**: Track authentication type, safe key hints, compression, duplicate policy, concurrency, success/failure, and cancellation
- **Verified updates**: Select an ABI APK with Universal fallback, verify its GitHub SHA-256 digest and signing certificate, then hand off to Android's installer

## Architecture

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Compose UI   │───▶│  ViewModel   │───▶│  Repository  │
│ Material 3 + │    │  MVVM + Flow │    │ Room + SAF + │
│ Backdrop     │    │              │    │ Keystore     │
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
| App version | 5.0.0 (`versionCode` 15) |
| Language | Kotlin 2.4.10, Go 1.25 |
| Android | compileSdk 37, minSdk 26, targetSdk 36 |
| UI | Jetpack Compose BOM 2026.08.00, Material 3 1.4.0, Backdrop 2.0.1 |
| Navigation | Navigation Compose 2.10.0 + Material 3 Adaptive Navigation Suite |
| DI | Hilt 2.60.1, Hilt Navigation Compose 1.4.0 |
| DB | Room 2.8.4 |
| Settings | DataStore Preferences 1.1.1 |
| File / archive | AndroidX DocumentFile, Apache Commons Compress, FileProvider |
| Networking | OkHttp 4.12.0 for update checks |
| Security UX | AndroidX Biometric 1.1.0 for private-key reveal |
| Crypto | [filippo.io/age](https://filippo.io/age) 1.3.1 |
| Native | gomobile → AAR / extracted JNI libs |
| Build | Gradle 9.4.1, Android Gradle Plugin 9.2.0 |

## Build

### Prerequisites

- Android SDK (compile SDK 37, min SDK 26, target SDK 36)
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

Age Android does not collect or transmit user data. Operations and history remain local. Private keys are wrapped by Android Keystore and stored in `noBackupFilesDir`; they are intentionally not restored to another device, so restored database entries fall back to public-key-only records.

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
