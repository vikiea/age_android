# Age Android v5.0.1

This patch preserves all v5.0.0 features and fixes update-source failover.

## Fixed

- Continue to the configured mirror sources when GitHub's unauthenticated API returns HTTP 403, 429, or another non-success response.
- Close every update-check response before trying the next source.

## Downloads

Use the Universal APK unless you specifically need a smaller ABI build.

| Asset | Size | SHA-256 |
| --- | ---: | --- |
| `app-universal-release.apk` | 16.5 MiB | `8f5f8994578a2c440a4d2e97844bb234ee60737aec2e3510332b5c0dd1ee36f0` |
| `app-arm64-v8a-release.apk` | 6.3 MiB | `8c24b5ef969bb1bfd1a3af8097d159ea32f5552268f414e41246cc320311b65b` |
| `app-armeabi-v7a-release.apk` | 6.4 MiB | `5719553a081ba8f8ba7b4d1ce28dd2a38f43f976d0137bc4ee09f4c79adcb6f8` |
| `app-x86_64-release.apk` | 6.6 MiB | `df2cc88208079dc1a7407696c884b9a7e3408fd42ed789e81dda193e1829c7e3` |
