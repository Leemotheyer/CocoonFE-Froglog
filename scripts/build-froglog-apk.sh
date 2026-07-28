#!/usr/bin/env bash
# Build Cocoon APK with Froglog merged (apktool + smali patches).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${ROOT}/android/.tools"
mkdir -p "$TOOLS"
if command -v python >/dev/null 2>&1; then
  PY="$(command -v python)"
elif command -v python3 >/dev/null 2>&1; then
  PY="$(command -v python3)"
else
  echo "python not found (install Python 3)" >&2
  exit 1
fi
cat > "${TOOLS}/python3" <<EOF
#!/usr/bin/env sh
exec "$PY" "\$@"
EOF
chmod +x "${TOOLS}/python3"
export PATH="${TOOLS}:${PATH}"
ANDROID_HOME="${ANDROID_HOME:-${ROOT}/android-sdk}"
export ANDROID_HOME
APK_IN="${APK_IN:-${ROOT}/android/dist/cocoon-3.apk}"
APKTOOL_DIR="${ROOT}/android/apktool-cocoon"
OUT_APK="${ROOT}/android/dist/cocoon-froglog-unsigned.apk"
SIGNED_APK="${ROOT}/android/dist/cocoon-froglog.apk"
BAKSMALI_JAR="${BAKSMALI_JAR:-${ROOT}/android/.tools/baksmali.jar}"

mkdir -p "${ROOT}/android/dist"
if ! command -v apktool >/dev/null 2>&1; then
  APKTOOL_JAR="${TOOLS}/apktool_2.9.3.jar"
  if [[ ! -f "$APKTOOL_JAR" ]]; then
    curl -fsSL -o "$APKTOOL_JAR" "https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar"
  fi
  cat > "${TOOLS}/apktool" <<EOF
#!/usr/bin/env sh
exec java -jar "$APKTOOL_JAR" "\$@"
EOF
  chmod +x "${TOOLS}/apktool"
  export PATH="${TOOLS}:${PATH}"
fi
if [[ ! -x "${ANDROID_HOME}/build-tools/34.0.0/apksigner" && ! -f "${ANDROID_HOME}/build-tools/34.0.0/apksigner.bat" ]]; then
  bash "${ROOT}/scripts/bootstrap-android-sdk.sh"
fi

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

if [[ -z "${ANDROID_HOME:-}" && -d "${ROOT}/android-sdk" ]]; then
  export ANDROID_HOME="${ROOT}/android-sdk"
fi
if [[ -n "${ANDROID_HOME:-}" ]]; then
  printf 'sdk.dir=%s\n' "$(cd "$ANDROID_HOME" && pwd)" > local.properties
fi

if [[ ! -x ./gradlew ]]; then
  echo "==> Bootstrap Gradle wrapper (gradlew missing)"
  GRADLE_BOOT="${ROOT}/android/.gradle-bootstrap"
  GRADLE_ZIP="${GRADLE_BOOT}/gradle-8.2-bin.zip"
  mkdir -p "$GRADLE_BOOT"
  if [[ ! -d "${GRADLE_BOOT}/gradle-8.2" ]]; then
    curl -fsSL -o "$GRADLE_ZIP" "https://services.gradle.org/distributions/gradle-8.2-bin.zip"
    unzip -q -o "$GRADLE_ZIP" -d "$GRADLE_BOOT"
  fi
  "${GRADLE_BOOT}/gradle-8.2/bin/gradle" wrapper --gradle-version 8.2 --no-daemon
fi

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

FROGLOG_APK_VERSION="${FROGLOG_APK_VERSION:-1.0.10-alpha}"
export FROGLOG_APK_VERSION

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
python3 "${ROOT}/scripts/patch-apk-runtime-shim.py" "$APKTOOL_DIR"
python3 "${ROOT}/scripts/patch-apk-blendmode-shim.py" "$APKTOOL_DIR"
if [[ "${FROGLOG_GAME_MENU_PATCH:-1}" == "1" ]]; then
  echo "==> Game menu smali patch (Open in Froglog; set FROGLOG_GAME_MENU_PATCH=0 to skip)"
  python3 "${ROOT}/scripts/patch-apk-game-froglog.py" "$APKTOOL_DIR"
else
  echo "==> Skipping game menu smali patch (FROGLOG_GAME_MENU_PATCH=0)"
fi

python3 "${ROOT}/scripts/verify-froglog-apk.py" "$APKTOOL_DIR"

echo "==> Merge manifest entries"
python3 "${ROOT}/scripts/patch-apk-manifest.py" "$APKTOOL_DIR"

echo "==> Distinct application id (side-by-side with stock Cocoon)"
python3 "${ROOT}/scripts/patch-apk-application-id.py" "$APKTOOL_DIR"

echo "==> apktool build"
apktool b "$APKTOOL_DIR" -o "$OUT_APK"

ALIGNED_APK="${ROOT}/android/dist/cocoon-froglog-aligned.apk"
ZIPALIGN="${ANDROID_HOME}/build-tools/34.0.0/zipalign"
APKSIGNER="${ANDROID_HOME}/build-tools/34.0.0/apksigner"

if [[ -x "$ZIPALIGN" || -f "${ANDROID_HOME}/build-tools/34.0.0/zipalign.exe" ]]; then
  [[ -f "${ANDROID_HOME}/build-tools/34.0.0/zipalign.exe" ]] && ZIPALIGN="${ANDROID_HOME}/build-tools/34.0.0/zipalign.exe"
  [[ -f "${ANDROID_HOME}/build-tools/34.0.0/apksigner.bat" ]] && APKSIGNER="${ANDROID_HOME}/build-tools/34.0.0/apksigner.bat"
fi

if [[ -x "$ZIPALIGN" || -f "$ZIPALIGN" ]] && { [[ -x "$APKSIGNER" ]] || [[ -f "$APKSIGNER" ]]; }; then
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
  OUT_VERSION="${ROOT}/android/dist/cocoon-froglog-${FROGLOG_APK_VERSION}.apk"
  cp -f "$SIGNED_APK" "$OUT_VERSION"
  echo "Versioned copy: $OUT_VERSION"
  python3 "${ROOT}/scripts/verify-froglog-apk.py" "$APKTOOL_DIR"
  python3 "${ROOT}/scripts/verify-froglog-built-apk.py" "$SIGNED_APK" "$APKTOOL_DIR"
  chmod +x "${ROOT}/scripts/smoke-test-froglog-apk.sh"
  "${ROOT}/scripts/smoke-test-froglog-apk.sh" "$SIGNED_APK" || true
else
  echo "Unsigned APK: $OUT_APK (install zipalign + apksigner to sign)"
fi
