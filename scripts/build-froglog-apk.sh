#!/usr/bin/env bash
# Build Cocoon APK with Froglog merged (apktool + smali patches).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-/workspace/android-sdk}"
export ANDROID_HOME
APK_IN="${APK_IN:-/tmp/apk-import/cocoon-3.apk}"
APKTOOL_DIR="${ROOT}/android/apktool-cocoon"
OUT_APK="${ROOT}/android/dist/cocoon-froglog-unsigned.apk"
SIGNED_APK="${ROOT}/android/dist/cocoon-froglog.apk"
BAKSMALI_JAR="${BAKSMALI_JAR:-/tmp/baksmali.jar}"

mkdir -p "${ROOT}/android/dist"
curl -fsSL -o "$APK_IN" "https://github.com/inssekt/CocoonFE/releases/download/beta-3.0/cocoon-3.apk" 2>/dev/null || true

if [[ ! -f "$BAKSMALI_JAR" ]]; then
  curl -fsSL -o "$BAKSMALI_JAR" "https://bitbucket.org/JesusFreke/smali/downloads/baksmali-2.5.2.jar"
fi

echo "==> Gradle: froglog-injector + froglog-core"
cd "${ROOT}/android"
cp -f build.gradle.froglog build.gradle
cp -f gradle.properties.froglog gradle.properties
cat > settings.gradle <<'EOF'
include ':froglog-core'
include ':froglog-injector'
rootProject.name = 'CocoonFroglog'
EOF
./gradlew :froglog-injector:assembleDebug --no-daemon 2>&1 | tail -15

INJECTOR_APK="${ROOT}/android/froglog-injector/build/outputs/apk/debug/froglog-injector-debug.apk"
TMP_DEX="${ROOT}/android/dist/injector-dex"
TMP_SMALI="${ROOT}/android/dist/injector-smali"
rm -rf "$TMP_DEX" "$TMP_SMALI"
mkdir -p "$TMP_DEX"
unzip -q -j "$INJECTOR_APK" "classes*.dex" -d "$TMP_DEX"
for dex in "$TMP_DEX"/*.dex; do
  java -jar "$BAKSMALI_JAR" d "$dex" -o "$TMP_SMALI"
done
if [[ ! -d "$TMP_SMALI/rip/moth/cocoonshell/froglog" ]]; then
  echo "froglog smali missing after baksmali" >&2
  exit 1
fi
if [[ ! -f "$TMP_SMALI/kotlin/jvm/internal/Intrinsics.smali" ]]; then
  echo "kotlin stdlib smali missing from injector (need Intrinsics)" >&2
  exit 1
fi

echo "==> apktool decode Cocoon"
rm -rf "$APKTOOL_DIR"
apktool d -f "$APK_IN" -o "$APKTOOL_DIR"

TARGET_SMALI="${APKTOOL_DIR}/smali_classes6"
mkdir -p "${TARGET_SMALI}/rip/moth/cocoonshell"
# Cocoon ships R8-renamed kotlin (no kotlin.jvm.internal.Intrinsics); froglog bytecode needs stdlib + deps.
FROGLOG_SMALI_DEPS=(kotlin kotlinx okhttp3 okio _COROUTINE)
echo "==> Copy froglog smali + runtime deps (${FROGLOG_SMALI_DEPS[*]})"
for pkg in "${FROGLOG_SMALI_DEPS[@]}"; do
  if [[ -d "$TMP_SMALI/$pkg" ]]; then
    cp -a "$TMP_SMALI/$pkg" "${TARGET_SMALI}/"
  else
    echo "warning: injector smali missing package $pkg" >&2
  fi
done
cp -a "$TMP_SMALI/rip/moth/cocoonshell/froglog" "${TARGET_SMALI}/rip/moth/cocoonshell/"

python3 "${ROOT}/scripts/patch-apk-smali.py" "$APKTOOL_DIR"
python3 "${ROOT}/scripts/patch-apk-pods.py" "$APKTOOL_DIR"
python3 "${ROOT}/scripts/patch-apk-picnic-froglog.py" "$APKTOOL_DIR"
python3 "${ROOT}/scripts/patch-apk-picnic-batch-froglog.py" "$APKTOOL_DIR"
python3 "${ROOT}/scripts/patch-apk-logpod-froglog.py" "$APKTOOL_DIR"
python3 "${ROOT}/scripts/patch-apk-game-froglog.py" "$APKTOOL_DIR"

echo "==> Merge manifest entries"
python3 "${ROOT}/scripts/patch-apk-manifest.py" "$APKTOOL_DIR"

echo "==> Distinct application id (side-by-side with stock Cocoon)"
python3 "${ROOT}/scripts/patch-apk-application-id.py" "$APKTOOL_DIR"

echo "==> apktool build"
apktool b "$APKTOOL_DIR" -o "$OUT_APK"

ALIGNED_APK="${ROOT}/android/dist/cocoon-froglog-aligned.apk"
ZIPALIGN="${ANDROID_HOME}/build-tools/34.0.0/zipalign"
APKSIGNER="${ANDROID_HOME}/build-tools/34.0.0/apksigner"

if [[ -x "$ZIPALIGN" && -x "$APKSIGNER" ]]; then
  "$ZIPALIGN" -f -p 4 "$OUT_APK" "$ALIGNED_APK"
  if ! "$ZIPALIGN" -c -p 4 "$ALIGNED_APK"; then
    echo "zipalign verification failed" >&2
    exit 1
  fi
  KEY="${ROOT}/android/dist/froglog-debug.keystore"
  if [[ ! -f "$KEY" ]]; then
    keytool -genkey -v -keystore "$KEY" -alias froglog -keyalg RSA -keysize 2048 -validity 10000 \
      -storepass android -keypass android -dname "CN=Froglog Cocoon"
  fi
  "$APKSIGNER" sign \
    --ks "$KEY" --ks-pass pass:android --key-pass pass:android \
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
    --out "$SIGNED_APK" "$ALIGNED_APK"
  rm -f "$ALIGNED_APK"
  echo "Signed APK: $SIGNED_APK"
else
  echo "Unsigned APK: $OUT_APK (install zipalign + apksigner to sign)"
fi
