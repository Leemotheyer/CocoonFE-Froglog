#!/usr/bin/env python3
"""Add Open in Froglog to game start menu (ke/d0.smali O0)."""
from __future__ import annotations

import sys
from pathlib import Path

MARKER = "FroglogGameMenuAction"
ANCHOR = """    const/16 v65, 0x3f0

    .line 1494
    .line 1495
    invoke-direct/range {v55 .. v65}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    .line 1496
    .line 1497
    .line 1498
    move-object/from16 v0, v55

    .line 1499
    .line 1500
    filled-new-array {v15, v0}, [Lve/w4;"""

FROGLOG_BLOCK = """    const/16 v65, 0x3f0

    .line 1494
    .line 1495
    invoke-direct/range {v55 .. v65}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    .line 1496
    .line 1497
    .line 1498
    move-object/from16 v0, v55

    move-object/from16 v9, p0

    sget-object v1, Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;->b:Lz0/m2;

    invoke-virtual {v14, v1}, Lz0/e0;->j(Lz0/n1;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/content/Context;

    invoke-virtual {v14, v1}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v2

    invoke-virtual {v14, v9}, Lz0/e0;->f(Ljava/lang/Object;)Z

    move-result v3

    or-int/2addr v2, v3

    invoke-virtual {v14}, Lz0/e0;->Q()Ljava/lang/Object;

    move-result-object v3

    if-nez v2, :froglog_game_remembered

    sget-object v2, Lz0/j;->a:Lz0/c;

    if-ne v3, v2, :froglog_game_invoke

    :froglog_game_remembered
    new-instance v3, Lrip/moth/cocoonshell/froglog/game/FroglogGameMenuAction;

    invoke-direct {v3, v1, v9}, Lrip/moth/cocoonshell/froglog/game/FroglogGameMenuAction;-><init>(Landroid/content/Context;Ljava/lang/String;)V

    invoke-virtual {v14, v3}, Lz0/e0;->m0(Ljava/lang/Object;)V

    :froglog_game_invoke
    check-cast v3, Lwa/a;

    new-instance v4, Lve/w4;

    const-string v5, "X"

    sget-object v6, Lme/b;->CHECK:Lme/b;

    move-object v7, v3

    const/16 v8, 0x0

    const/16 v10, 0x0

    const/16 v11, 0x0

    const/16 v12, 0x0

    const/16 v13, 0x0

    const/16 v14, 0x3f0

    invoke-direct/range {v4 .. v14}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    filled-new-array {v15, v0, v4}, [Lve/w4;"""


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    path = Path(sys.argv[1]) / "smali_classes3/ke/d0.smali"
    text = path.read_text(encoding="utf-8")
    if MARKER in text:
        print("Game menu Froglog action already patched")
        return
    if ANCHOR not in text:
        raise SystemExit("ke/d0 O0 game menu anchor not found")
    path.write_text(text.replace(ANCHOR, FROGLOG_BLOCK, 1))
    print("Patched", path)


if __name__ == "__main__":
    main()
