#!/usr/bin/env python3
"""Attach Froglog sync affordance overlay on Log Pod."""
from __future__ import annotations

import sys
from pathlib import Path

MARKER = "FroglogLogPodAttach;->attach"
ANCHOR = """    invoke-static {p0, v0}, Ld/f;->a(Lc/j;Lj1/h;)V

    .line 105
    .line 106
    .line 107
    return-void"""

INSERT = """    invoke-static {p0, v0}, Ld/f;->a(Lc/j;Lj1/h;)V

    invoke-static {p0}, Lrip/moth/cocoonshell/froglog/pod/FroglogLogPodAttach;->attach(Landroid/app/Activity;)V

    .line 105
    .line 106
    .line 107
    return-void"""


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    path = Path(sys.argv[1]) / "smali_classes3/rip/moth/cocoonshell/ui/activity/LogPodActivity.smali"
    text = path.read_text(encoding="utf-8")
    if MARKER in text:
        print("Log Pod Froglog affordance already patched")
        return
    if ANCHOR not in text:
        raise SystemExit("LogPodActivity onCreate anchor not found")
    path.write_text(text.replace(ANCHOR, INSERT, 1))
    print("Patched", path)


if __name__ == "__main__":
    main()
