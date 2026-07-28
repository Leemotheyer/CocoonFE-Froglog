#!/usr/bin/env python3
"""Fail the build if known-bad smali patterns remain in ke/d0 (launch VerifyError)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

D0 = "smali_classes3/ke/d0.smali"
CORRECT_TITLE_LOADS = (
    "move-object/from16 v6, p1",
)


def extract_method(text: str, name: str, sig: str) -> str:
    m = re.search(
        rf"\.method public static final {name}\({re.escape(sig)}.*?^\.end method",
        text,
        re.M | re.S,
    )
    if not m:
        raise SystemExit(f"{D0}: method {name}{sig} not found")
    return m.group(0)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    text = (Path(sys.argv[1]) / D0).read_text(encoding="utf-8")

    a1 = extract_method(text, "A1", "Lwa/a;Lz0/e0;I)V")
    for pat in (
        "FroglogGameMenuAction",
        "filled-new-array {v15, v0, v4}, [Lve/w4;",
        "move-object/from16 v9, p0",
    ):
        if pat in a1:
            raise SystemExit(f"ke/d0.A1 contains bad Froglog smali: {pat!r}")

    if "FroglogGameMenuAction" in text:
        j = extract_method(text, "j", "Lwa/a;Ljava/lang/String;Lz0/e0;I)V")
        if "FroglogGameMenuAction" not in j:
            raise SystemExit("FroglogGameMenuAction outside ke/d0.j")
        if "move-object/from16 v6, p0" in j:
            raise SystemExit("ke/d0.j game menu must use p1 (String) for title, not p0 (wa.a)")
        if not any(pat in j for pat in CORRECT_TITLE_LOADS):
            raise SystemExit("ke/d0.j game menu missing title load from p1 (v6 or v10)")
        if "const/16 v14, 0x3f0" in j and "FroglogGameMenuAction" in j:
            raise SystemExit("ke/d0.j game menu must not clobber v14 (Composer); use v55..v65 for w4")
        if "invoke-direct/range {v4 .. v14}, Lve/w4;" in j and "FroglogGameMenuAction" in j:
            raise SystemExit("ke/d0.j game menu must not use v4..v14 w4 range (VerifyError on v14)")
        if "move-object/from16 v58, v3" not in j or "const-string v56, \"X\"" not in j:
            raise SystemExit("ke/d0.j game menu missing Froglog w4 row (v55..v65 pattern)")

    print("OK: ke/d0 smali checks passed")


if __name__ == "__main__":
    main()
