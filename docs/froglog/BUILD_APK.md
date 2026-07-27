# Building Cocoon + Froglog APK

The decompiled Gradle `:app` module does not compile. The supported path is **merge `froglog-core` into the release APK** with apktool.

## Requirements

- JDK 17
- Android SDK (API 34, build-tools 34)
- `apktool` 2.9+
- `baksmali` 2.5+ (downloaded automatically by the script)
- Cocoon `beta-3.0` APK (downloaded automatically)

## One command

```bash
export ANDROID_HOME=/path/to/android-sdk   # or /workspace/android-sdk in CI
chmod +x scripts/build-froglog-apk.sh
./scripts/build-froglog-apk.sh
```

**Output:** `android/dist/cocoon-froglog.apk` (signed with a debug keystore generated on first run). Application id: `rip.moth.cocoonshell.froglog` (installs beside stock Cocoon).

## GitHub Releases (alpha)

Published APKs use tags and filenames **`cocoon-froglog-<version>-alpha`** (pre-release). Upload `android/dist/cocoon-froglog.apk` renamed to match, e.g. `cocoon-froglog-1.0.1-alpha.apk`.

## What the build does

1. Assembles `froglog-injector` (thin app that packages `froglog-core`).
2. Baksmalis injector DEX and copies `rip/moth/cocoonshell/froglog/**` into `smali_classes6`.
3. Decodes Cocoon with apktool.
4. Patches `pf/c0.smali` to call `FroglogCocoonHooks.onGameSessionEndedWithD0` after each saved session.
5. Registers the Froglog Pod on the home Pods overlay and adds **Submit to Froglog** on Picnic screenshot detail (`ke/ff.smali`).
6. Merges Froglog Pod activity + `FroglogInitProvider` into `AndroidManifest.xml`.
7. Sets application id **`rip.moth.cocoonshell.froglog`** and launcher label **Cocoon (Froglog)**.
8. Rebuilds and signs the APK.

## On device

```bash
adb install -r android/dist/cocoon-froglog.apk
adb shell am start -n rip.moth.cocoonshell.froglog/rip.moth.cocoonshell.froglog.pod.FroglogPodActivity
```

Sign in with a Froglog account, play a game in Cocoon, then open Froglog Pod and tap **Sync now** (sessions also enqueue automatically on save).

To send a Picnic screenshot: open **Picnic** → open a screenshot → tap **Submit to Froglog** (sign in first if prompted). Then sync from Froglog Pod when ready.

## Library-only build

```bash
cd android
cp build.gradle.froglog build.gradle settings.gradle.froglog settings.gradle gradle.properties.froglog gradle.properties
./gradlew :froglog-core:assembleRelease
```
