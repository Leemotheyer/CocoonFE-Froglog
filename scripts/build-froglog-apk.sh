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
  java -jar "$BAKSMALI_JAR" d "$dex" -o "$TMP_SMALI" 2>/dev/null || true
done
# If baksmali merged, ensure rip tree exists
if [[ ! -d "$TMP_SMALI/rip/moth/cocoonshell/froglog" ]]; then
  rm -rf "$TMP_SMALI"
  java -jar "$BAKSMALI_JAR" d "$INJECTOR_APK" -o "$TMP_SMALI"
fi

echo "==> apktool decode Cocoon"
rm -rf "$APKTOOL_DIR"
apktool d -f "$APK_IN" -o "$APKTOOL_DIR"

TARGET_SMALI="${APKTOOL_DIR}/smali_classes6"
mkdir -p "${TARGET_SMALI}/rip/moth/cocoonshell"
echo "==> Copy froglog smali"
cp -a "$TMP_SMALI/rip/moth/cocoonshell/froglog" "${TARGET_SMALI}/rip/moth/cocoonshell/"

python3 "${ROOT}/scripts/patch-apk-smali.py" "$APKTOOL_DIR"

echo "==> Merge manifest entries"
python3 "${ROOT}/scripts/patch-apk-manifest.py" "$APKTOOL_DIR"

echo "==> apktool build"
apktool b "$APKTOOL_DIR" -o "$OUT_APK"

if [[ -x "${ANDROID_HOME}/build-tools/34.0.0/apksigner" ]]; then
  KEY="${ROOT}/android/dist/froglog-debug.keystore"
  if [[ ! -f "$KEY" ]]; then
    keytool -genkey -v -keystore "$KEY" -alias froglog -keyalg RSA -keysize 2048 -validity 10000 \
      -storepass android -keypass android -dname "CN=Froglog Cocoon"
  fi
  "${ANDROID_HOME}/build-tools/34.0.0/apksigner" sign \
    --ks "$KEY" --ks-pass pass:android --key-pass pass:android \
    --out "$SIGNED_APK" "$OUT_APK"
  echo "Signed APK: $SIGNED_APK"
else
  echo "Unsigned APK: $OUT_APK (install apksigner to sign)"
fi
