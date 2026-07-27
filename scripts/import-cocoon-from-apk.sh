#!/usr/bin/env bash
# Download the Cocoon release APK, optionally export it as zip(s), and decompile
# to a Gradle tree under android/ using jadx --export-gradle.
#
# The APK is already a ZIP archive; this script can also copy/repack it for tooling
# that expects .zip. The working tree for Froglog is the jadx Gradle export.
set -euo pipefail

REPO="${REPO:-inssekt/CocoonFE}"
TAG="${TAG:-beta-3.0}"
APK_NAME="${APK_NAME:-}" # auto-detect from release if empty
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="${ROOT}/android"
ARCHIVES_DIR="${ANDROID_DIR}/archives"
TMP="${TMPDIR:-/tmp}/cocoon-apk-import-$$"
JADX_BIN="${JADX_BIN:-}"

if [[ -z "$JADX_BIN" ]]; then
  if [[ -x /opt/jadx/bin/jadx ]]; then
    JADX_BIN=/opt/jadx/bin/jadx
  elif command -v jadx >/dev/null 2>&1; then
    JADX_BIN="$(command -v jadx)"
  else
    echo "jadx not found. Install from https://github.com/skylot/jadx or set JADX_BIN." >&2
    exit 1
  fi
fi

cleanup() { rm -rf "$TMP"; }
trap cleanup EXIT

mkdir -p "$TMP" "$ANDROID_DIR" "$ARCHIVES_DIR"

resolve_apk_url() {
  if [[ -n "${APK_URL:-}" ]]; then
    echo "$APK_URL"
    return
  fi
  if ! command -v gh >/dev/null 2>&1; then
    echo "https://github.com/${REPO}/releases/download/${TAG}/cocoon-3.apk"
    return
  fi
  local url name
  url="$(gh api "repos/${REPO}/releases/tags/${TAG}" --jq '.assets[] | select(.name | test("\\.apk$"; "i")) | .browser_download_url' | head -1)"
  if [[ -z "$url" ]]; then
    echo "No APK asset on release ${TAG}." >&2
    exit 1
  fi
  echo "$url"
}

APK_URL_RESOLVED="$(resolve_apk_url)"
APK_FILE="${TMP}/cocoon.apk"

echo "Downloading APK from ${APK_URL_RESOLVED}..."
curl -fsSL -o "$APK_FILE" "$APK_URL_RESOLVED"

# APK is ZIP — store a .zip copy for tools that want that extension
ARCHIVE_ZIP="${ARCHIVES_DIR}/cocoon-${TAG}.zip"
cp "$APK_FILE" "$ARCHIVE_ZIP"
echo "Wrote APK-as-zip: ${ARCHIVE_ZIP}"

if [[ "${EXPORT_UNPACKED_ZIP:-0}" == "1" ]]; then
  UNPACK="${TMP}/apk-unpacked"
  mkdir -p "$UNPACK"
  unzip -q "$APK_FILE" -d "$UNPACK"
  UNPACKED_ZIP="${ARCHIVES_DIR}/cocoon-${TAG}-unpacked.zip"
  (cd "$UNPACK" && zip -qr "$UNPACKED_ZIP" .)
  echo "Wrote unpacked APK zip: ${UNPACKED_ZIP}"
fi

echo "Decompiling with jadx (this may take a few minutes)..."
JADX_OUT="${TMP}/jadx-gradle"
rm -rf "$JADX_OUT"
"$JADX_BIN" --export-gradle -d "$JADX_OUT" "$APK_FILE" || true

if [[ ! -f "${JADX_OUT}/settings.gradle" ]]; then
  echo "jadx did not produce settings.gradle." >&2
  exit 2
fi

readme_backup=""
if [[ -f "${ANDROID_DIR}/README.md" ]]; then
  readme_backup="${TMP}/android-README.md"
  cp "${ANDROID_DIR}/README.md" "$readme_backup"
fi

froglog_core_backup=""
if [[ -d "${ANDROID_DIR}/froglog-core" ]]; then
  froglog_core_backup="${TMP}/froglog-core"
  cp -a "${ANDROID_DIR}/froglog-core" "$froglog_core_backup"
fi

# Replace android/ tree with decompiled project (keep archives/)
find "$ANDROID_DIR" -mindepth 1 -maxdepth 1 \
  ! -name 'README.md' \
  ! -name 'froglog-core' \
  ! -name 'archives' \
  ! -name 'DECOMPILED_BASELINE.md' \
  -exec rm -rf {} +

shopt -s dotglob
for item in "$JADX_OUT"/*; do
  base="$(basename "$item")"
  if [[ "$base" == "froglog-core" ]]; then
    continue
  fi
  cp -a "$item" "${ANDROID_DIR}/"
done
shopt -u dotglob

if [[ -n "$froglog_core_backup" ]]; then
  rm -rf "${ANDROID_DIR}/froglog-core"
  cp -a "$froglog_core_backup" "${ANDROID_DIR}/froglog-core"
fi

if [[ -n "$readme_backup" ]]; then
  cp "$readme_backup" "${ANDROID_DIR}/README.md"
fi

# Wire froglog-core module
if [[ -f "${ANDROID_DIR}/settings.gradle" ]] && ! grep -q "froglog-core" "${ANDROID_DIR}/settings.gradle"; then
  printf "\ninclude ':froglog-core'\n" >> "${ANDROID_DIR}/settings.gradle"
fi

if [[ -f "${ANDROID_DIR}/app/build.gradle" ]] && ! grep -q "froglog-core" "${ANDROID_DIR}/app/build.gradle"; then
  # jadx stub leaves a placeholder dependencies block
  if grep -q "// some dependencies" "${ANDROID_DIR}/app/build.gradle"; then
    sed -i "s|// some dependencies|implementation project(':froglog-core')|" "${ANDROID_DIR}/app/build.gradle"
  else
    printf "\ndependencies {\n    implementation project(':froglog-core')\n}\n" >> "${ANDROID_DIR}/app/build.gradle"
  fi
fi

{
  echo "$TAG"
  echo "import_method=apk-jadx"
  echo "apk_url=$APK_URL_RESOLVED"
  echo "jadx=$("$JADX_BIN" --version 2>/dev/null || echo unknown)"
  echo "imported_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "${ANDROID_DIR}/SOURCE_VERSION"

if [[ "${EXPORT_DECOMPILED_ZIP:-0}" == "1" ]]; then
  DIST_ZIP="${ARCHIVES_DIR}/cocoon-${TAG}-decompiled-gradle.zip"
  (cd "$ANDROID_DIR" && zip -qr "$DIST_ZIP" . -x 'archives/*')
  echo "Wrote decompiled tree zip: ${DIST_ZIP}"
fi

echo ""
echo "Import complete."
echo "  Gradle tree: ${ANDROID_DIR}"
echo "  APK as zip:  ${ARCHIVE_ZIP}"
echo ""
echo "This is decompiled output — expect jadx errors and a non-buildable app module"
echo "until dependencies are restored. New Froglog code belongs in froglog-core."
echo "See android/DECOMPILED_BASELINE.md"

# Apply Froglog Gradle templates and manifest hook for Cocoon theme
if [[ -f "${ROOT}/android/build.gradle.froglog" ]]; then
  cp "${ROOT}/android/build.gradle.froglog" "${ANDROID_DIR}/build.gradle"
fi
if [[ -f "${ROOT}/android/gradle.properties.froglog" ]]; then
  cp "${ROOT}/android/gradle.properties.froglog" "${ANDROID_DIR}/gradle.properties"
fi
if [[ -x "${ROOT}/scripts/patch-cocoon-manifest-froglog.sh" ]]; then
  "${ROOT}/scripts/patch-cocoon-manifest-froglog.sh" || true
fi
