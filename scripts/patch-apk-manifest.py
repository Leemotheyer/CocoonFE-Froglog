#!/usr/bin/env python3
"""Add Froglog Pod activity + init provider to apktool AndroidManifest.xml."""
from __future__ import annotations

import sys
from pathlib import Path

ACTIVITY_SNIPPET = """
        <activity
            android:name="rip.moth.cocoonshell.froglog.pod.FroglogPodActivity"
            android:exported="true"
            android:label="Froglog Pod"
            android:theme="@style/Theme.Cocoon"
            android:configChanges="screenSize|screenLayout|orientation|keyboardHidden" />
        <activity
            android:name="rip.moth.cocoonshell.froglog.game.FroglogGameLinkActivity"
            android:exported="false"
            android:label="Open in Froglog"
            android:theme="@style/Theme.Cocoon"
            android:configChanges="screenSize|screenLayout|orientation|keyboardHidden" />
        <provider
            android:name="rip.moth.cocoonshell.froglog.FroglogInitProvider"
            android:authorities="rip.moth.cocoonshell.froglog-init"
            android:exported="false"
            android:initOrder="100" />
"""


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    manifest = Path(sys.argv[1]) / "AndroidManifest.xml"
    text = manifest.read_text()
    if "froglog.pod.FroglogPodActivity" in text:
        print("Manifest already patched")
        return
    anchor = "rip.moth.cocoonshell.ui.activity.PicnicPodActivity"
    idx = text.find(anchor)
    if idx == -1:
        raise SystemExit("PicnicPodActivity anchor not found")
    close = text.find("/>", idx)
    if close == -1:
        raise SystemExit("Could not close Picnic activity tag")
    insert_at = close + 2
    manifest.write_text(text[:insert_at] + ACTIVITY_SNIPPET + text[insert_at:])
    print("Patched", manifest)


if __name__ == "__main__":
    main()
