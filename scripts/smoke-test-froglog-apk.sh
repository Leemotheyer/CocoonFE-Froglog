#!/usr/bin/env bash
# Install APK and fail if logcat shows VerifyError / FATAL for rip.moth.cocoonshell.froglog.
# Requires: adb, running emulator or device. Skip (exit 0) if no device unless FROGLOG_SMOKE_REQUIRED=1.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="${1:-${ROOT}/android/dist/cocoon-froglog.apk}"
PKG="rip.moth.cocoonshell.froglog"
ACTIVITY="${PKG}/rip.moth.cocoonshell.MainActivity"
ADB="${ADB:-adb}"
<<<<<<< HEAD
=======
if [[ "$ADB" == "adb" && -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/adb" ]]; then
  ADB="${ANDROID_HOME}/platform-tools/adb"
fi
>>>>>>> origin/cursor/froglog-kotlin-deps-aff8
WAIT_SEC="${FROGLOG_SMOKE_WAIT_SEC:-25}"

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

if ! command -v "$ADB" >/dev/null 2>&1; then
  echo "adb not found; skip smoke test" >&2
  [[ "${FROGLOG_SMOKE_REQUIRED:-0}" == "1" ]] && exit 1 || exit 0
fi

devices="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "$devices" ]]; then
  echo "No adb device/emulator; skip smoke test (set FROGLOG_SMOKE_REQUIRED=1 to fail)" >&2
  [[ "${FROGLOG_SMOKE_REQUIRED:-0}" == "1" ]] && exit 1 || exit 0
fi

echo "==> Smoke test: uninstall/install $PKG"
"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true
"$ADB" install -r "$APK"

echo "==> Clear logcat, cold start MainActivity"
"$ADB" logcat -c
"$ADB" shell am force-stop "$PKG" >/dev/null 2>&1 || true
"$ADB" shell am start -W -n "$ACTIVITY" >/dev/null

echo "==> Wait ${WAIT_SEC}s for startup / composition"
sleep "$WAIT_SEC"

LOG="/tmp/froglog-smoke-logcat.txt"
"$ADB" logcat -d > "$LOG"

if rg -q "VerifyError.*ke\.d0|VerifyError.*ke/d0" "$LOG"; then
  echo "FAIL: VerifyError in ke.d0 (see $LOG)" >&2
  rg "VerifyError|FATAL EXCEPTION" "$LOG" | tail -30 >&2
  exit 1
fi

if rg -q "Process: ${PKG}" "$LOG" && rg -q "FATAL EXCEPTION: main" "$LOG"; then
  echo "FAIL: FATAL EXCEPTION for $PKG (see $LOG)" >&2
  rg "FATAL EXCEPTION|VerifyError|Caused by:" "$LOG" | tail -40 >&2
  exit 1
fi

echo "OK: smoke test passed (no VerifyError/FATAL for $PKG in ${WAIT_SEC}s logcat)"
