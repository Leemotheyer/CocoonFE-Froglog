#!/usr/bin/env python3
"""Side-by-side install id without breaking resource package (apktool renameManifestPackage)."""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

BASE_ID = "rip.moth.cocoonshell"
FROGLOG_ID = "rip.moth.cocoonshell.froglog"
YML_MARKER = f"renameManifestPackage: {FROGLOG_ID}"

FROGLOG_VERSION = os.environ.get("FROGLOG_APK_VERSION", "1.0.7-alpha")


def patch_apktool_yml(yml: Path) -> None:
    text = yml.read_text(encoding="utf-8")
    if YML_MARKER in text:
        return
    if "renameManifestPackage: null" not in text:
        raise SystemExit("apktool.yml: expected renameManifestPackage: null")
    text = text.replace(
        "renameManifestPackage: null",
        f"renameManifestPackage: {FROGLOG_ID}",
        1,
    )
    yml.write_text(text, encoding="utf-8")
    print("Set renameManifestPackage in", yml)


def patch_manifest(manifest: Path) -> None:
    text = manifest.read_text(encoding="utf-8")
    # Resources must stay on BASE_ID; install id comes from renameManifestPackage at build time.
    if f'package="{FROGLOG_ID}"' in text:
        text = text.replace(f'package="{FROGLOG_ID}"', f'package="{BASE_ID}"', 1)
        print("Restored manifest resource package to", BASE_ID)
    elif f'package="{BASE_ID}"' not in text:
        raise SystemExit("Unexpected manifest package attribute")

    text = text.replace(
        f'android:name="{BASE_ID}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"',
        f'android:name="{FROGLOG_ID}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"',
    )
    text = text.replace(
        f'android:authorities="{BASE_ID}.',
        f'android:authorities="{FROGLOG_ID}.',
    )
    manifest.write_text(text, encoding="utf-8")
    print("Patched manifest authorities for", manifest)


def patch_app_label(base: Path) -> None:
    strings = base / "res/values/strings.xml"
    if not strings.is_file():
        return
    text = strings.read_text(encoding="utf-8")
    label = f"Cocoon (Froglog {FROGLOG_VERSION})"
    text = re.sub(
        r'(<string name="app_name">)([^<]*)(</string>)',
        rf"\1{label}\3",
        text,
        count=1,
    )
    strings.write_text(text, encoding="utf-8")
    print("Patched launcher label in", strings, "->", label)


def patch_version_name(manifest: Path) -> None:
    text = manifest.read_text(encoding="utf-8")
    short = FROGLOG_VERSION.removesuffix("-alpha")
    new_attr = f'android:versionName="{short}-froglog"'
    if new_attr not in text:
        text = re.sub(
            r'android:versionName="[^"]*"',
            new_attr,
            text,
            count=1,
        )
    # Ensure install replaces stale builds (e.g. broken 1.0.5 smali).
    if 'android:versionCode="999' not in text:
        text = re.sub(
            r'android:versionCode="\d+"',
            'android:versionCode="999003"',
            text,
            count=1,
        )
    manifest.write_text(text, encoding="utf-8")
    print("Set manifest versionName/versionCode for Froglog build")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    apktool = Path(sys.argv[1])
    patch_apktool_yml(apktool / "apktool.yml")
    patch_manifest(apktool / "AndroidManifest.xml")
    patch_version_name(apktool / "AndroidManifest.xml")
    patch_app_label(apktool)


if __name__ == "__main__":
    main()
