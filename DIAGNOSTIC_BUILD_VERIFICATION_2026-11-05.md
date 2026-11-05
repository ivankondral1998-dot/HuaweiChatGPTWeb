# 0.4.4 diagnostic build verification

Input archive SHA-256: `321e6930099eff4e633e631a63aa40271eddf1393ae478f94db9248fdfb8e0d6`.
Baseline: verified local 0.4.4 archive, restored in commit da35066 above cf65b97. Input ZIP matches all baseline files except MainActivity/DiagnosticsActivity and adds tracker, tests and handoffs.

Only correction to the supplied ZIP: declare JUnit 4.13.2 via testImplementation. Without it, compileDebugUnitTestJavaWithJavac fails (org.junit missing). Runtime code matches the diagnostic ZIP exactly. No WebView settings or permissions changed relative to 0.4.4.

Environment: full JDK17, Gradle8.9, SDK35, system proxy/CA configuration from existing /workspace/toolchains/setup.sh. Existing debug signing key reused, not included in Git.

Commands run from project root:
```
bash ./gradlew --no-daemon --max-workers=2 clean assembleDebug lintDebug testDebugUnitTest
javac -d /tmp/mic-policy-test app/src/main/java/app/chatgpt/web/MicrophoneOriginPolicy.java app/src/test/java/app/chatgpt/web/MicrophoneOriginPolicyTest.java
java -cp /tmp/mic-policy-test app.chatgpt.web.MicrophoneOriginPolicyTest
apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
apksigner verify --verbose --print-certs /workspace/artifacts/ChatGPTWeb-0.4.4-debug.apk
aapt dump badging app/build/outputs/apk/debug/app-debug.apk
adb devices -l
```
Results: Gradle BUILD SUCCESSFUL, 48 tasks executed. UiStallTrackerTest: 2 tests, 0 failures/errors/skipped. MicrophoneOriginPolicyTest standalone: 17 checks passed. Lint: 0 errors, 2 warnings (DataExtractionRules, MissingApplicationIcon).

APK: ChatGPTWeb-0.4.4-performance-diagnostics-debug.apk, 20472 bytes.
SHA-256: `f483a1b56b7164ab190c7fc5cbb7b6721d836b0dbccbb79392285818d9474587`.
Package app.chatgpt.web; versionName 0.4.4; versionCode 12; minSdk26; targetSdk35. Both APK signatures valid; new and previous 0.4.4 certificate SHA-256 match: `483fd179dbe5aa5786156cae966c438657b88858c543ad50900583c5c5f5f8de`.
Same versionCode is retained intentionally; Android permits replacing the same version with the same signer. The certificate comparison is against the prior APK in this environment, not a certificate independently extracted from the phone.

No device connected. Installation, auth persistence, microphone, UI heartbeat integration and overhead on Huawei are unverified. No performance fix is claimed.

## Device protocol

Install over the existing 0.4.4 without uninstalling. To establish a baseline, use the chat briefly, press Home, then long-press the launcher icon and choose Diagnostics; copy the values. Return to the app, reproduce the problem, press Home again, then reopen Diagnostics and copy the new values. If necessary close and reopen Diagnostics to refresh its once-per-creation snapshot. Do not force-stop/recreate MainActivity between baseline and reproduction: counters are per Activity instance. Background time is excluded from the heartbeat.

UI scheduling delay means lateness relative to the planned 250ms callback. A stall is one sampled lateness >=350ms, not every 350ms stretch of main-thread blocking. Max delay and sample count are also reported. No samples means no useful measurement. Zero stalls does not exclude shorter main-thread jank. The heartbeat is not renderer FPS, and silence does not prove Chromium/network health. Last main-frame load is page navigation only; SPA chat switches may not trigger it.

Compare UI scheduling stalls, max delay, and samples before/after visible stalls. Rising counters demonstrate observed main-thread scheduling delays; quiet counters while visual stalls persist motivate renderer/JS/layout/network investigation, without deciding the cause in advance.
