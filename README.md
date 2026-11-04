# HuaweiChatGPTWeb

Android web-app shell for using the official ChatGPT website on Huawei devices where the official app is unavailable or unsuitable.

## Current version

- versionName: `0.4.0`
- versionCode: `8`
- package: `app.chatgpt.web`
- minSdk: 26
- targetSdk: 35

## Product goal

The app should feel like an Android application, not like a browser tab.

The main UI is a full-screen Android `WebView` that loads the official
`https://chatgpt.com/` site directly inside the app:

- no browser address bar
- no browser toolbar
- no visible Custom Tab chrome
- hardware acceleration enabled
- normal WebView cache and DOM storage
- cookies persisted by Android WebView
- no overscroll glow
- hidden scrollbars
- activity state restored across configuration changes
- file chooser support for web uploads

## Navigation and safety

The ChatGPT/OpenAI web surface remains inside the application.
Unrelated external hosts are opened by Android through the normal external-app/browser flow.

The application:

- does not contain an OpenAI API key
- does not intercept or export auth tokens
- does not inject authentication cookies
- does not bypass TLS/SSL errors
- does not spoof the browser User-Agent
- does not disable Safe Browsing
- does not modify ChatGPT page content with injected JavaScript/CSS

OAuth providers may reject embedded WebView authentication. That is a platform/provider limitation, not something the app should bypass. Huawei nova 10 SE remains the primary device for real authentication testing.

## Build

Requires JDK 17+, Gradle 8.9 and Android SDK 35.

```bash
./gradlew clean :app:assembleDebug
./gradlew :app:lintDebug
```

Expected APK:

`app/build/outputs/apk/debug/app-debug.apk`

A successful build is not considered device verification. Real acceptance requires install + launch + interaction on Huawei nova 10 SE.
