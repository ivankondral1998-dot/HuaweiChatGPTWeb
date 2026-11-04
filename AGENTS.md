# Agent instructions

This repository is a deliberately small Android project. Keep the main branch boring and reliable.

## Product goal

Provide a minimal standalone wrapper around the official ChatGPT website for Huawei nova 10 SE (Android 12 / API 31).

## Rules

- Main implementation stays Java + standard Android Gradle tooling.
- Do not hand-build APK, DEX, binary manifests or resource tables.
- Do not use smali in `main`.
- If a Java/smali experiment becomes necessary, isolate it in a separate experimental branch.
- Do not use GitHub Actions.
- Do not add GMS, Firebase, microG, GBox, OpenAI API, VPN, VpnService or proxy code unless a later task explicitly requires it.
- Never bypass TLS/SSL errors.
- Do not claim device validation without a real install, launch and logcat result.
- Prefer one Activity and no third-party dependencies.

## Required verification before claiming success

1. Ensure a standard Gradle wrapper exists; if `gradle-wrapper.jar` is absent, generate it with `gradle wrapper --gradle-version 8.9`.
2. `./gradlew clean assembleDebug`
3. `./gradlew lintDebug` when available
4. Confirm package `app.chatgpt.web`
5. Confirm `minSdk <= 31`
6. Verify APK signature with `apksigner` when available
7. Compute SHA-256
8. Device/emulator install + launch + logcat if a target exists

## Authentication architecture

- Do not send Google/Microsoft/Apple OAuth through an embedded `WebView`.
- Do not spoof a browser user-agent to bypass identity-provider WebView restrictions.
- Authentication-sensitive web flows use the system browser context (Custom Tabs, with normal browser fallback).
- Never read, copy, log, export, or inject authentication cookies or tokens.
