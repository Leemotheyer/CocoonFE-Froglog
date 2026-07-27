#!/usr/bin/env python3
"""Fail the build if known-bad smali patterns remain in ke/d0 (launch VerifyError)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

D0 = "smali_classes3/ke/d0.smali"

BAD_A1_PATTERNS = (
    "FroglogGameMenuAction",
    "filled-new-array {v15, v0, v4}, [Lve/w4;",
    "move-object/from16 v9, p0",
    "invoke-direct {v3, v1, v9}, Lrip/moth/cocoonshell/froglog/game/FroglogGameMenuAction;",
)


def extract_a1(text: str) -> str:
    m = re.search(r"\.method public static final A1\(.*?^\.end method", text, re.M | re.S)
    if not m:
        raise SystemExit(f"{D0}: method A1 not found")
    return m.group(0)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    path = Path(sys.argv[1]) / D0
    text = path.read_text(encoding="utf-8")
    a1 = extract_a1(text)
    for pat in BAD_A1_PATTERNS:
        if pat in a1:
            raise SystemExit(
                f"ke/d0.A1 still contains bad Froglog smali ({pat!r}). "
                "This causes VerifyError (v9 wa.a vs String). Re-decode Cocoon and rebuild."
            )
    if "filled-new-array {v13, v0, v4}" in text and ":froglog_game_remembered" in text:
        print("OK: Froglog game menu patch present in O0 only")
    print("OK: ke/d0.A1 clean")


if __name__ == "__main__":
    main()
