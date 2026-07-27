#!/usr/bin/env python3
"""Set distinct application id so Froglog APK installs beside stock Cocoon."""
from __future__ import annotations

import re
import sys
from pathlib import Path

BASE_ID = "rip.moth.cocoonshell"
FROGLOG_ID = "rip.moth.cocoonshell.froglog"
MARKER = f'package="{FROGLOG_ID}"'


def patch_manifest(manifest: Path) -> None:
    text = manifest.read_text(encoding="utf-8")
    if MARKER in text:
        print("Application id already patched")
        return
    if f'package="{BASE_ID}"' not in text:
        raise SystemExit(f"Expected package={BASE_ID} in manifest")

    text = text.replace(f'package="{BASE_ID}"', f'package="{FROGLOG_ID}"', 1)
    text = text.replace(
        f'android:name="{BASE_ID}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"',
        f'android:name="{FROGLOG_ID}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"',
    )
    text = text.replace(
        f'android:authorities="{BASE_ID}.',
        f'android:authorities="{FROGLOG_ID}.',
    )
    manifest.write_text(text, encoding="utf-8")
    print("Patched application id in", manifest)


def patch_app_label(base: Path) -> None:
    strings = base / "res/values/strings.xml"
    if not strings.is_file():
        return
    text = strings.read_text(encoding="utf-8")
    if "Cocoon (Froglog)" in text:
        return
    text = re.sub(
        r'(<string name="app_name">)([^<]*)(</string>)',
        r"\1Cocoon (Froglog)\3",
        text,
        count=1,
    )
    strings.write_text(text, encoding="utf-8")
    print("Patched launcher label in", strings)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    apktool = Path(sys.argv[1])
    patch_manifest(apktool / "AndroidManifest.xml")
    patch_app_label(apktool)


if __name__ == "__main__":
    main()
