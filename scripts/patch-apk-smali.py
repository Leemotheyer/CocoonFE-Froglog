#!/usr/bin/env python3
"""Inject Froglog session hook into apktool-decoded pf/c0.smali."""
from __future__ import annotations

import sys
from pathlib import Path

HOOK = """
    iget-object v10, v0, Lpf/y;->a:Lpf/d0;

    move-object v11, v4

    invoke-virtual {v1}, Ljava/lang/Number;->longValue()J

    move-result-wide v5

    sget-object v1, Lld/a;->p:Lrip/moth/cocoonshell/CocoonApp;

    invoke-static {v1, v10, v5, v6, v11}, Lrip/moth/cocoonshell/froglog/FroglogCocoonHooks;->onGameSessionEndedWithD0(Landroid/content/Context;Ljava/lang/Object;JLjava/lang/String;)V

    move-wide v1, v5
"""

MARKER = "FroglogCocoonHooks;->onGameSessionEndedWithD0"
PICNIC_MARKER = "FroglogCocoonHooks;->onPicnicScreenshotSaved"
PICNIC_HOOK = """
    sget-object v0, Lld/a;->p:Lrip/moth/cocoonshell/CocoonApp;

    invoke-static {v0, p1}, Lrip/moth/cocoonshell/froglog/FroglogCocoonHooks;->onPicnicScreenshotSaved(Landroid/content/Context;Ljava/lang/Object;)V
"""


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    base = Path(sys.argv[1])
    path = base / "smali_classes3/pf/c0.smali"
    text = path.read_text()
    if MARKER in text:
        print("c0.smali already patched")
    else:
        needle = "    invoke-virtual {v1}, Ljava/lang/Number;->longValue()J\n\n    .line 134\n    .line 135\n    .line 136\n    move-result-wide v1\n"
        if needle not in text:
            # looser match
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

    picnic_path = base / "smali_classes3/rip/moth/cocoonshell/data/local/PicnicScreenshotDao_Impl.smali"
    picnic_text = picnic_path.read_text()
    if PICNIC_MARKER in picnic_text:
        print("PicnicScreenshotDao_Impl already patched")
        return
    picnic_needle = """    move-result-wide p0

    .line 12
    return-wide p0
.end method

.method public static synthetic j"""
    if picnic_needle not in picnic_text:
        raise SystemExit("Could not find picnic insert hook point")
    picnic_text = picnic_text.replace(
        picnic_needle,
        f"""    move-result-wide p0

{PICNIC_HOOK.strip()}

    .line 12
    return-wide p0
.end method

.method public static synthetic j""",
        1,
    )
    picnic_path.write_text(picnic_text)
    print("Patched", picnic_path)


if __name__ == "__main__":
    main()
