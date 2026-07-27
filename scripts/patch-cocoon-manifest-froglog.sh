#!/usr/bin/env bash
# Add Froglog Pod activity to decompiled Cocoon AndroidManifest (Cocoon theme).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MANIFEST="${ROOT}/android/app/src/main/AndroidManifest.xml"

if [[ ! -f "$MANIFEST" ]]; then
  echo "Missing $MANIFEST — run import-cocoon-from-apk.sh first." >&2
  exit 1
fi

if grep -q 'froglog.pod.FroglogPodActivity' "$MANIFEST"; then
  echo "Manifest already patched."
  exit 0
fi

ROOT="$ROOT" python3 <<'PY'
import os
import pathlib
root = pathlib.Path(os.environ["ROOT"])
manifest = root / "android/app/src/main/AndroidManifest.xml"
snippet = (root / "docs/froglog/manifest-froglog-pod.xml").read_text()
text = manifest.read_text()
needle = "rip.moth.cocoonshell.ui.activity.PicnicPodActivity"
idx = text.find(needle)
if idx == -1:
    raise SystemExit("PicnicPodActivity anchor not found in manifest")
close = text.find("/>", idx)
if close == -1:
    raise SystemExit("Could not find end of Picnic activity tag")
insert_at = close + 2
manifest.write_text(text[:insert_at] + "\n" + snippet + text[insert_at:])
print("Patched", manifest)
PY
