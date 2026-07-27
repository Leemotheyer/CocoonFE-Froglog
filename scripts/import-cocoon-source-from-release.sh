#!/usr/bin/env bash
# Import Cocoon Shell Android sources from a GitHub release asset.
set -euo pipefail

REPO="${REPO:-inssekt/CocoonFE}"
TAG="${TAG:-beta-3.0}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="${ROOT}/android"
TMP="${TMPDIR:-/tmp}/cocoon-source-import-$$"

cleanup() { rm -rf "$TMP"; }
trap cleanup EXIT

mkdir -p "$TMP" "$ANDROID_DIR"

echo "Fetching release assets for ${REPO} tag ${TAG}..."
mapfile -t ASSETS < <(gh api "repos/${REPO}/releases/tags/${TAG}" --jq '.assets[] | "\(.name)\t\(.browser_download_url)"')

if [[ ${#ASSETS[@]} -eq 0 ]]; then
  echo "No assets found for tag ${TAG}." >&2
  exit 1
fi

SOURCE_URL=""
SOURCE_NAME=""
for entry in "${ASSETS[@]}"; do
  name="${entry%%$'\t'*}"
  url="${entry#*$'\t'}"
  lower="$(echo "$name" | tr '[:upper:]' '[:lower:]')"
  if [[ "$lower" == *.apk ]]; then
    continue
  fi
  if [[ "$lower" == *.zip || "$lower" == *.tar.gz || "$lower" == *.tgz ]]; then
    if [[ "$lower" =~ source|src|cocoonshell|android ]]; then
      SOURCE_URL="$url"
      SOURCE_NAME="$name"
      break
    fi
    # Fallback: any non-APK archive
    if [[ -z "$SOURCE_URL" ]]; then
      SOURCE_URL="$url"
      SOURCE_NAME="$name"
    fi
  fi
done

if [[ -z "$SOURCE_URL" ]]; then
  echo "No source archive found on release ${TAG}." >&2
  echo "Assets on this release:" >&2
  for entry in "${ASSETS[@]}"; do
    echo "  - ${entry%%$'\t'*}" >&2
  done
  echo "" >&2
  echo "When upstream publishes a source zip, re-run this script." >&2
  exit 2
fi

echo "Downloading ${SOURCE_NAME}..."
archive="${TMP}/${SOURCE_NAME}"
curl -fsSL -o "$archive" "$SOURCE_URL"

extract_dir="${TMP}/extract"
mkdir -p "$extract_dir"

case "$SOURCE_NAME" in
  *.tar.gz|*.tgz) tar -xzf "$archive" -C "$extract_dir" ;;
  *.zip) unzip -q "$archive" -d "$extract_dir" ;;
  *) echo "Unsupported archive type: ${SOURCE_NAME}" >&2; exit 3 ;;
esac

# Find Gradle root (settings.gradle or settings.gradle.kts)
gradle_root="$(find "$extract_dir" -maxdepth 4 \( -name 'settings.gradle.kts' -o -name 'settings.gradle' \) -print -quit | xargs -r dirname)"
if [[ -z "$gradle_root" ]]; then
  echo "Could not find Gradle settings file in archive." >&2
  exit 4
fi

echo "Gradle project root: ${gradle_root}"

# Preserve froglog docs at android/README.md
readme_backup=""
if [[ -f "${ANDROID_DIR}/README.md" ]]; then
  readme_backup="${TMP}/android-README.md"
  cp "${ANDROID_DIR}/README.md" "$readme_backup"
fi

# Remove old imported tree except README and froglog-sync stub
find "$ANDROID_DIR" -mindepth 1 -maxdepth 1 ! -name 'README.md' ! -name 'froglog-core' -exec rm -rf {} +

shopt -s dotglob
for item in "$gradle_root"/*; do
  base="$(basename "$item")"
  if [[ "$base" == "froglog-core" ]]; then
    continue
  fi
  cp -a "$item" "${ANDROID_DIR}/"
done
shopt -u dotglob

if [[ -n "$readme_backup" ]]; then
  cp "$readme_backup" "${ANDROID_DIR}/README.md"
fi

echo "$TAG" > "${ANDROID_DIR}/SOURCE_VERSION"
echo "Imported ${TAG} into ${ANDROID_DIR}"
echo "Next: open android/ in Android Studio and add the froglog-sync module (see docs/froglog/INTEGRATION_PLAN.md)."
