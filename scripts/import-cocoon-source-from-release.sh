#!/usr/bin/env bash
# Import Cocoon Shell Android sources for a given upstream tag.
#
# Canonical tag archive (GitHub-generated):
#   https://github.com/inssekt/CocoonFE/archive/refs/tags/<TAG>.zip
#
# Optional: a release asset zip (if upstream attaches one) is tried when the tag
# archive does not contain a Gradle project.
set -euo pipefail

REPO="${REPO:-inssekt/CocoonFE}"
TAG="${TAG:-beta-3.0}"
# tag | release | apk | auto
SOURCE_MODE="${SOURCE_MODE:-auto}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="${ROOT}/android"
TMP="${TMPDIR:-/tmp}/cocoon-source-import-$$"

cleanup() { rm -rf "$TMP"; }
trap cleanup EXIT

mkdir -p "$TMP" "$ANDROID_DIR"

tag_archive_url() {
  echo "https://github.com/${REPO}/archive/refs/tags/${TAG}.zip"
}

download_archive() {
  local url="$1"
  local dest="$2"
  echo "Downloading ${url}..."
  curl -fsSL -o "$dest" "$url"
}

extract_archive() {
  local archive="$1"
  local dest="$2"
  mkdir -p "$dest"
  case "$archive" in
    *.tar.gz|*.tgz) tar -xzf "$archive" -C "$dest" ;;
    *.zip) unzip -q "$archive" -d "$dest" ;;
    *) echo "Unsupported archive: ${archive}" >&2; return 1 ;;
  esac
}

find_gradle_root() {
  local search_root="$1"
  find "$search_root" \( -name 'settings.gradle.kts' -o -name 'settings.gradle' \) -print -quit \
    | xargs -r dirname
}

try_import_from_archive() {
  local archive="$1"
  local label="$2"
  local extract_dir="${TMP}/extract-${label}"
  rm -rf "$extract_dir"
  extract_archive "$archive" "$extract_dir"

  local gradle_root
  gradle_root="$(find_gradle_root "$extract_dir")"
  if [[ -z "$gradle_root" ]]; then
    echo "No Gradle project in ${label}." >&2
    return 1
  fi

  echo "Gradle project root (${label}): ${gradle_root}"

  local readme_backup=""
  if [[ -f "${ANDROID_DIR}/README.md" ]]; then
    readme_backup="${TMP}/android-README.md"
    cp "${ANDROID_DIR}/README.md" "$readme_backup"
  fi

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
  echo "${label}" >> "${ANDROID_DIR}/SOURCE_VERSION"
  echo "Imported ${TAG} into ${ANDROID_DIR} from ${label}"
  echo "Next: open android/ in Android Studio and add froglog-core (docs/froglog/INTEGRATION_PLAN.md)."
  return 0
}

try_release_asset_archive() {
  if ! command -v gh >/dev/null 2>&1; then
    return 1
  fi
  mapfile -t ASSETS < <(gh api "repos/${REPO}/releases/tags/${TAG}" --jq '.assets[] | "\(.name)\t\(.browser_download_url)"' 2>/dev/null || true)
  [[ ${#ASSETS[@]} -gt 0 ]] || return 1

  local entry name url lower archive
  for entry in "${ASSETS[@]}"; do
    name="${entry%%$'\t'*}"
    url="${entry#*$'\t'}"
    lower="$(echo "$name" | tr '[:upper:]' '[:lower:]')"
    [[ "$lower" == *.apk ]] && continue
    [[ "$lower" == *.zip || "$lower" == *.tar.gz || "$lower" == *.tgz ]] || continue

    archive="${TMP}/${name}"
    download_archive "$url" "$archive"
    if try_import_from_archive "$archive" "release asset ${name}"; then
      return 0
    fi
  done
  return 1
}

try_tag_archive() {
  local url archive
  url="$(tag_archive_url)"
  archive="${TMP}/tag-${TAG}.zip"
  download_archive "$url" "$archive"
  try_import_from_archive "$archive" "tag archive ${url}"
}

imported=1
if [[ "$SOURCE_MODE" == "apk" ]]; then
  "${ROOT}/scripts/import-cocoon-from-apk.sh" && imported=0
elif [[ "$SOURCE_MODE" == "tag" ]]; then
  try_tag_archive && imported=0
elif [[ "$SOURCE_MODE" == "release" ]]; then
  try_release_asset_archive && imported=0
else
  # auto: tag archive → release asset → APK decompile
  if try_tag_archive 2>/dev/null; then
    imported=0
  elif try_release_asset_archive 2>/dev/null; then
    imported=0
  elif "${ROOT}/scripts/import-cocoon-from-apk.sh"; then
    imported=0
  fi
fi

if [[ "$imported" -ne 0 ]]; then
  echo "" >&2
  echo "Could not import an Android Gradle tree for tag ${TAG}." >&2
  echo "" >&2
  echo "Tag archive (canonical):" >&2
  echo "  $(tag_archive_url)" >&2
  echo "" >&2
  echo "That zip is the git snapshot for the tag. If it only contains platforms/" >&2
  echo "and README, the Cocoon Shell app sources are not in that tree yet — check for a" >&2
  echo "newer tag or a separate release asset (non-APK zip)." >&2
  echo "Tried: tag archive, release assets, and APK decompile (scripts/import-cocoon-from-apk.sh)." >&2
  echo "Force APK import: SOURCE_MODE=apk ./scripts/import-cocoon-source-from-release.sh" >&2
  exit 4
fi
