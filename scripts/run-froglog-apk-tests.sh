#!/usr/bin/env bash
# Run all automated Froglog APK checks (no device required except optional smoke test).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="${1:-${ROOT}/android/dist/cocoon-froglog.apk}"
APKTOOL_DIR="${2:-${ROOT}/android/apktool-cocoon}"

echo "==> Smali checks (pre/post build tree)"
if [[ -d "$APKTOOL_DIR" ]]; then
  python3 "${ROOT}/scripts/verify-froglog-apk.py" "$APKTOOL_DIR"
  python3 "${ROOT}/scripts/verify-froglog-built-apk.py" "$APK" "$APKTOOL_DIR"
else
  python3 "${ROOT}/scripts/verify-froglog-built-apk.py" "$APK"
fi

echo "==> Optional device smoke test"
chmod +x "${ROOT}/scripts/smoke-test-froglog-apk.sh"
"${ROOT}/scripts/smoke-test-froglog-apk.sh" "$APK"

echo "==> All Froglog APK tests finished"
