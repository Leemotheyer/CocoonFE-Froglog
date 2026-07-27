#!/usr/bin/env python3
"""Inject Froglog session hook into apktool-decoded pf/c0.smali."""
from __future__ import annotations

import sys
from pathlib import Path

# At :goto_1, v0 is Lpf/d0 (not Lpf/y); v4 is the session date string; v1 is insert row id.
HOOK = """
    move-object v10, v0

    move-object v11, v4

    invoke-virtual {v1}, Ljava/lang/Number;->longValue()J

    move-result-wide v5

    sget-object v1, Lld/a;->p:Lrip/moth/cocoonshell/CocoonApp;

    invoke-static {v1, v10, v5, v6, v11}, Lrip/moth/cocoonshell/froglog/FroglogCocoonHooks;->onGameSessionEndedWithD0(Landroid/content/Context;Ljava/lang/Object;JLjava/lang/String;)V

    move-wide v1, v5
"""

MARKER = "FroglogCocoonHooks;->onGameSessionEndedWithD0"
WRONG_D0_LOAD = "    iget-object v10, v0, Lpf/y;->a:Lpf/d0;"
CORRECT_D0_LOAD = "    move-object v10, v0"


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    base = Path(sys.argv[1])
    path = base / "smali_classes3/pf/c0.smali"
    text = path.read_text()
    if MARKER in text:
        if WRONG_D0_LOAD in text:
            text = text.replace(WRONG_D0_LOAD, CORRECT_D0_LOAD, 1)
            path.write_text(text)
            print("Fixed pf/c0.smali Froglog hook (v0 is pf/d0 at :goto_1)")
        else:
            print("c0.smali already patched")
        return
    needle = "    invoke-virtual {v1}, Ljava/lang/Number;->longValue()J\n\n    .line 134\n    .line 135\n    .line 136\n    move-result-wide v1\n"
    if needle not in text:
        idx = text.find("invoke-virtual {v1}, Ljava/lang/Number;->longValue()J")
        if idx == -1:
            raise SystemExit("Could not find longValue hook point in c0.smali")
        end = text.find("move-result-wide v1", idx)
        if end == -1:
            raise SystemExit("Could not find move-result-wide after longValue")
        end = text.find("\n", end) + 1
        text = text[:idx] + HOOK.strip() + "\n\n" + text[end:]
    else:
        text = text.replace(needle, HOOK.strip() + "\n\n", 1)
    path.write_text(text)
    print("Patched", path)


if __name__ == "__main__":
    main()
