#!/usr/bin/env bash
# Project-local Android SDK (android-sdk/ is gitignored). Used by build-froglog-apk.sh on Windows/macOS/Linux.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ROOT}/android-sdk}"
export ANDROID_HOME="$SDK"

if [[ -x "${SDK}/build-tools/34.0.0/apksigner" || -x "${SDK}/build-tools/34.0.0/apksigner.bat" ]]; then
  echo "Android SDK OK: $SDK"
  exit 0
fi

mkdir -p "$SDK"
CMD="${SDK}/cmdline-tools/latest/bin"
if [[ ! -x "${CMD}/sdkmanager" && ! -f "${CMD}/sdkmanager.bat" ]]; then
  echo "==> Download Android command-line tools"
  TMP="${ROOT}/android/.sdk-bootstrap"
  rm -rf "$TMP"
  mkdir -p "$TMP"
  ZIP="${TMP}/cmdline-tools.zip"
  curl -fsSL -o "$ZIP" "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" \
    || curl -fsSL -o "$ZIP" "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  unzip -q -o "$ZIP" -d "$TMP/extract"
  mkdir -p "${SDK}/cmdline-tools/latest"
  if [[ -d "$TMP/extract/cmdline-tools" ]]; then
    cp -a "$TMP/extract/cmdline-tools/." "${SDK}/cmdline-tools/latest/"
  else
    cp -a "$TMP/extract/." "${SDK}/cmdline-tools/latest/"
  fi
  rm -rf "$TMP"
fi

SDKMAN="${CMD}/sdkmanager"
[[ -f "${CMD}/sdkmanager.bat" ]] && SDKMAN="${CMD}/sdkmanager.bat"

echo "==> Accept Android SDK licenses"
yes 2>/dev/null | "$SDKMAN" --sdk_root="$SDK" --licenses >/dev/null || true

echo "==> sdkmanager: platform 34 + build-tools 34.0.0"
yes 2>/dev/null | "$SDKMAN" --sdk_root="$SDK" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "Android SDK ready: $SDK"
