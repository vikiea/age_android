# Age Android v5.0.2

This release refines the v5 interface, fixes language switching, and improves the default encryption workflow.

## Changed

- Redesign the phone interface with opaque content transitions, frosted Backdrop surfaces, compact icon-only top bars, and a theme-aware bottom navigation bar.
- Replace system dynamic color and the glass toggle with a consistent fixed-accent frosted-glass presentation.
- Default encryption and decryption to age keys instead of passphrases.
- Add a local timestamp to the default packed archive name, such as `archive-20260902-153012`.
- Use Android Gradle Plugin's built-in Kotlin support.

## Fixed

- Switch between English, Simplified Chinese, and the system language without recreating the activity or flashing a black frame.
- Clear the legacy framework locale override once during upgrade, then leave locale state untouched on later launches.
- Show key import failures and other notices as styled in-app surfaces without repeating a dialog error after the dialog closes.
- Make key import and generation actions more compact and align their dialogs with the current visual system.

## Downloads

Use the Universal APK unless you specifically need a smaller ABI build.

| Asset | Size | SHA-256 |
| --- | ---: | --- |
| `app-universal-release.apk` | 16.4 MiB | `0b0bc6c34c6812e1ba54246e3b7620f8b74eb48b7cb200a604868193fde42e23` |
| `app-arm64-v8a-release.apk` | 6.2 MiB | `f30b51f5fa9ce2c3643dc5e79c4a604f116aef2685e3025c00b855a617df1502` |
| `app-armeabi-v7a-release.apk` | 6.3 MiB | `6ceae25945976fad552ec5f770e3af7470616784c0fb46870f98eb96e13990bd` |
| `app-x86_64-release.apk` | 6.5 MiB | `b8adee460df53c6205ff845f8c03bb72354ab9aeea99de4ae00842f94849746f` |
