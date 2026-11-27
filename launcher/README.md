# UNO Pulse Launcher — isolated GitHub Actions build

This is a separate project on the branch `feat/uno-pulse-launcher`.
The existing HuaweiChatGPTWeb `main` application remains unchanged.

The complete launcher source is stored as `uno-pulse-sources.tar.xz`,
sha256: `71dcb1a8585eac32919623f113b509b1d5889fa37e0c60bb2f6d94a5d6ec5f67`.
Extract with `tar -xJf launcher/uno-pulse-sources.tar.xz`.

The branch-only workflow `.github/workflows/uno-pulse-launcher.yml`
verifies the archive, compiles Java logic tests, assembles a debug APK,
checks the package and Android signature, calculates SHA-256, and
uploads the APK as a GitHub Actions artifact.

Package: `app.uno.pulse`. Minimum API 26, target API 34.
This is an unsigned-for-release, debug-signed experimental build.
No root, Android permissions or network are needed for the launcher.

**Note:** APK build and device behavior are only verified after the corresponding
GitHub Actions run completes, and after installation on a device, respectively.
