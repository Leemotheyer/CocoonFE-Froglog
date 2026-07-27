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

**Output:** `android/dist/cocoon-froglog.apk` (signed with a debug keystore generated on first run).

## What the build does

1. Assembles `froglog-injector` (thin app that packages `froglog-core`).
2. Baksmalis injector DEX and copies `rip/moth/cocoonshell/froglog/**` into `smali_classes6`.
3. Decodes Cocoon with apktool.
4. Patches `pf/c0.smali` to call `FroglogCocoonHooks.onGameSessionEndedWithD0` after each saved session.
5. Merges Froglog Pod activity + `FroglogInitProvider` into `AndroidManifest.xml`.
6. Rebuilds and signs the APK.

## On device

```bash
adb install -r android/dist/cocoon-froglog.apk
adb shell am start -n rip.moth.cocoonshell/.rip.moth.cocoonshell.froglog.pod.FroglogPodActivity
```

Sign in with a Froglog account, play a game in Cocoon, then open Froglog Pod and tap **Sync now** (sessions also enqueue automatically on save).

## Library-only build

```bash
cd android
cp build.gradle.froglog build.gradle settings.gradle.froglog settings.gradle gradle.properties.froglog gradle.properties
./gradlew :froglog-core:assembleRelease
```
