# HuaweiChatGPTWeb

Android web-app shell for using the official ChatGPT website on Huawei devices where the official app is unavailable or unsuitable.

## Current version

- versionName: `0.4.4`
- versionCode: `12`
- package: `app.chatgpt.web`
- minSdk: 26
- targetSdk: 35

## Product goal

Version 0.4.4 adds MODIFY_AUDIO_SETTINGS, required alongside RECORD_AUDIO by
Chromium 114's audio device enumeration and selection. Reference:
https://github.com/chromium/chromium/blob/114.0.5735.198/media/base/android/java/src/org/chromium/media/AudioManagerAndroid.java
Huawei's WebView is vendor-maintained, so microphone capture still needs device
verification. Diagnostics also reports audio-settings permission, visible input
count and microphone mute state. No
audio is recorded by diagnostics. This release does not claim to fix rendering
performance.

Version 0.4.3 handles microphone requests from the HTTPS chatgpt.com origin
through Android's RECORD_AUDIO runtime permission. Requests from other origins
are denied; only audio capture is granted. Pending requests are cancelled on
navigation, stop, destruction, or WebView cancellation. The permission is requested
when the page asks for the microphone, not at launch. Picking a document through
Android's file chooser does not require broad storage permission.

Long-press the launcher icon and select «Диагностика» to copy the WebView provider,
OS/device version, display refresh rate and microphone permission status. This
contains no chat text, URLs or account data. Frame smoothness has not been measured
on a physical Huawei device; 0.4.3 does not claim to resolve rendering jank.

Run origin-policy checks with JDK 17+:
```sh
javac -d /tmp/mic-policy-test app/src/main/java/app/chatgpt/web/MicrophoneOriginPolicy.java app/src/test/java/app/chatgpt/web/MicrophoneOriginPolicyTest.java
java -cp /tmp/mic-policy-test app.chatgpt.web.MicrophoneOriginPolicyTest
```
Device checks: microphone allow/deny, deny permanently then enable via Settings,
cancel permission request by navigating away, background/foreground, dictation,
and launcher diagnostics. JVM checks do not replace these Android checks.

Version 0.4.2 removes synchronous cookie disk writes from page load/progress
callbacks. A coalesced background write runs when the Activity stops. The launch
window, system bars and WebView container use a consistent dark background;
the container fits system insets while retaining keyboard resize. Failed state
restoration falls back to the home page. This does not override an explicit
theme selected in ChatGPT settings or change the page with injected CSS/JS.

Performance and keyboard behavior still require verification on Huawei nova
10 SE: compare typing, scrolling a long chat, opening/closing the keyboard,
returning from file selection, and cold/warm starts. Build/lint success is not
a measured improvement in frame times.

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
