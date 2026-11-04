# HuaweiChatGPTWeb

Small Android launcher for the official ChatGPT website.

## Target

- Huawei nova 10 SE
- Android 12 / API 31
- EMUI 13
- No Google Play Services required by this app

## Authentication model

ChatGPT authentication is intentionally kept out of an embedded `WebView`.
Version 0.3.0 keeps a stable native launcher screen and opens the official
`https://chatgpt.com/` in a real browser context.

The primary button uses an installed Custom Tabs provider when one is available.
A separate **Choose browser** action always remains available as a Huawei-safe
fallback. The launcher no longer immediately finishes after opening a browser,
which avoids the visible Activity/browser bounce of the previous 0.2.1 build.

The app does not inspect credentials, cookies, tokens, page contents, or TLS
traffic. It does not spoof a browser user-agent and does not bypass TLS errors.

## UI / lifecycle changes in 0.3.0

- persistent native launcher instead of instant open + `finish()`
- explicit Custom Tabs provider detection
- explicit browser chooser fallback
- no automatic reopen when returning from the browser
- simple browser/auth status text
- copy-link action for manual recovery
- dark launcher theme to reduce white flashes during Activity transitions

## Build

Requires JDK 17+ and Android SDK with platform 35/build tools installed.
The snapshot includes wrapper scripts/properties but not `gradle-wrapper.jar`.
Generate it once in a normal Android build environment:

```bash
gradle wrapper --gradle-version 8.9
./gradlew clean assembleDebug
./gradlew lintDebug
```

Expected APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Verification policy

A build is not device-tested until the APK is installed and launched on a real
device/emulator and the login flow is exercised. Huawei nova 10 SE remains the
primary target for authentication verification.
