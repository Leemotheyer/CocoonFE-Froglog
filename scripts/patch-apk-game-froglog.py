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

    sget-object v66, Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;->b:Lz0/m2;

    invoke-virtual {v14, v66}, Lz0/e0;->j(Lz0/n1;)Ljava/lang/Object;

    move-result-object v66

    check-cast v66, Landroid/content/Context;

    invoke-virtual {v14, v66}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v67

    move-object/from16 v68, p0

    invoke-virtual {v14, v68}, Lz0/e0;->f(Ljava/lang/Object;)Z

    move-result v69

    or-int/2addr v67, v69

    invoke-virtual {v14}, Lz0/e0;->Q()Ljava/lang/Object;

    move-result-object v69

    if-nez v67, :froglog_game_remembered

    sget-object v67, Lz0/j;->a:Lz0/c;

    if-ne v69, v67, :froglog_game_invoke

    :froglog_game_remembered
    new-instance v69, Lrip/moth/cocoonshell/froglog/game/FroglogGameMenuAction;

    invoke-direct {v69, v66, v68}, Lrip/moth/cocoonshell/froglog/game/FroglogGameMenuAction;-><init>(Landroid/content/Context;Ljava/lang/String;)V

    invoke-virtual {v14, v69}, Lz0/e0;->m0(Ljava/lang/Object;)V

    :froglog_game_invoke
    move-object/from16 v58, v69

    check-cast v58, Lwa/a;

    new-instance v53, Lve/w4;

    const v66, 0x7f0d0695

    invoke-static {v66, v14}, Lo1/d;->y(ILz0/e0;)Ljava/lang/String;

    move-result-object v54

    sget-object v56, Lme/b;->CHECK:Lme/b;

    const-string v55, "X"

    move-object/from16 v57, v58

    const/16 v59, 0x0

    const/16 v60, 0x0

    const/16 v61, 0x0

    const/16 v62, 0x0

    const/16 v63, 0x0

    const/16 v64, 0x3f0

    invoke-direct/range {v53 .. v64}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    filled-new-array {v15, v0, v53}, [Lve/w4;"""


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
