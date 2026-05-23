# Age Android

Android native app wrapping the [age](https://filippo.io/age) encryption tool with streaming I/O and batch processing.

## Features

- **Batch encryption**: Pack multiple files into tar/tar.gz, then encrypt as `.tar.gz.age` or `.tar.age`
- **Separate encryption**: Encrypt individual files as `.tar.age` (original extension hidden)
- **Passphrase & public key encryption**: Support both scrypt passphrase and X25519 key pair
- **Streaming I/O**: Entire pipeline uses streaming — handles 1GB+ files without OOM
- **Go engine**: tar/tar.gz compression and age encryption via gomobile, ~32KB memory footprint
- **Custom save directory**: SAF-based directory picker with persistent URI permission
- **Key management**: Generate, import, and manage X25519 key pairs
- **Operation history**: Track all encryption/decryption operations

## Architecture

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Compose UI  │───▶│  ViewModel   │───▶│  Repository  │
│  (Material3) │    │  (MVVM+Flow) │    │  (Room+SAF)  │
└──────────────┘    └──────┬───────┘    └──────────────┘
                           │
                    ┌──────▼───────┐
                    │  Go Engine   │
                    │  (gomobile)  │
                    └──────────────┘
```

- **UI**: Jetpack Compose + Material Design 3
- **DI**: Hilt
- **DB**: Room (key storage, operation history)
- **Settings**: DataStore Preferences
- **Engine**: Go `filippo.io/age` compiled via gomobile → AAR

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin, Go |
| UI | Jetpack Compose, Material3 |
| DI | Hilt |
| DB | Room |
| Settings | DataStore Preferences |
| Crypto | [filippo.io/age](https://filippo.io/age) |
| Native | gomobile → AAR |
| Build | Gradle 8.14, Go 1.25+ |

## Build

### Prerequisites

- Android SDK (min SDK 26, target SDK 35)
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

All files are tar-packed before encryption to hide original file extensions.

## Privacy

Age Android does not collect, transmit, or store any user data. All operations are performed locally on your device.

[Privacy Policy](https://vikiea.github.io/age_android/privacy-policy.html)

## Support

Age Android is free and open source. If it helps you, you can voluntarily support ongoing development. Support is optional and does not unlock, change, or limit any app feature.

自愿支持，不影响任何功能。

<p>
  <img src="docs/assets/donation/alipay.jpg" alt="Alipay donation QR code" width="260">
  <img src="docs/assets/donation/wechat_pay.jpg" alt="WeChat Pay donation QR code" width="260">
</p>

## License

[MIT](LICENSE)
