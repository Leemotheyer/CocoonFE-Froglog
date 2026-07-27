#!/usr/bin/env python3
"""Add Open in Froglog to game start menu (ke/d0.smali O0 only)."""
from __future__ import annotations

import sys
from pathlib import Path

MARKER = "FroglogGameMenuAction"

# Wrong patch (legacy): matched ke/d0.A1 and used p0 (wa.a) as game title + clobbered v14.
A1_WRONG_TAIL = """    move-object/from16 v0, v55

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

A1_RESTORE_TAIL = """    move-object/from16 v0, v55

    .line 1499
    .line 1500
    filled-new-array {v15, v0}, [Lve/w4;"""

# Unique to O0 (game start menu): third menu row uses v13 + v0, not v15.
O0_ANCHOR = """    .line 1593
    .line 1594
    .line 1595
    move-object/from16 v0, v55

    .line 1596
    .line 1597
    filled-new-array {v13, v0}, [Lve/w4;"""

# v11 = composer; p0 = game title. Use v0–v15 only (invoke-virtual / filled-new-array limits).
O0_FROGLOG_BLOCK = """    .line 1593
    .line 1594
    .line 1595
    move-object/from16 v0, v55

    move-object/from16 v6, p0

    sget-object v1, Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;->b:Lz0/m2;

    invoke-virtual {v11, v1}, Lz0/e0;->j(Lz0/n1;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/content/Context;

    invoke-virtual {v11, v1}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v2

    invoke-virtual {v11, v6}, Lz0/e0;->f(Ljava/lang/Object;)Z

    move-result v3

    or-int/2addr v2, v3

    invoke-virtual {v11}, Lz0/e0;->Q()Ljava/lang/Object;

    move-result-object v3

    if-nez v2, :froglog_game_remembered

    sget-object v2, Lz0/j;->a:Lz0/c;

    if-ne v3, v2, :froglog_game_invoke

    :froglog_game_remembered
    new-instance v3, Lrip/moth/cocoonshell/froglog/game/FroglogGameMenuAction;

    invoke-direct {v3, v1, v6}, Lrip/moth/cocoonshell/froglog/game/FroglogGameMenuAction;-><init>(Landroid/content/Context;Ljava/lang/String;)V

    invoke-virtual {v11, v3}, Lz0/e0;->m0(Ljava/lang/Object;)V

    :froglog_game_invoke
    check-cast v3, Lwa/a;

    move-object v15, v11

    move-object v2, v13

    new-instance v4, Lve/w4;

    const-string v5, "X"

    sget-object v6, Lme/b;->CHECK:Lme/b;

    move-object v7, v3

    const/16 v8, 0x0

    const/16 v9, 0x0

    const/16 v10, 0x0

    const/4 v11, 0x0

    const/16 v12, 0x0

    const/16 v13, 0x0

    const/16 v14, 0x3f0

    invoke-direct/range {v4 .. v14}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    move-object v11, v15

    move-object v13, v2

    .line 1596
    .line 1597
    filled-new-array {v13, v0, v4}, [Lve/w4;"""

O0_MARKER = "filled-new-array {v13, v0, v4}, [Lve/w4;"


def revert_a1_wrong_patch(text: str) -> tuple[str, bool]:
    if A1_WRONG_TAIL not in text:
        return text, False
    return text.replace(A1_WRONG_TAIL, A1_RESTORE_TAIL, 1), True


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    path = Path(sys.argv[1]) / "smali_classes3/ke/d0.smali"
    text = path.read_text(encoding="utf-8")

    text, reverted = revert_a1_wrong_patch(text)
    if reverted:
        print("Reverted erroneous Froglog patch from ke/d0.A1")

    if O0_MARKER in text:
        if reverted:
            path.write_text(text)
        else:
            print("Game menu Froglog action already patched (O0)")
        return

    bad_o0_markers = (
        "move-object/from16 v66, p0",
        "new-instance v70, Lve/w4;",
        "filled-new-array {v13, v0, v70}, [Lve/w4;",
    )
    if any(m in text for m in bad_o0_markers) and O0_ANCHOR not in text:
        raise SystemExit("ke/d0 has broken O0 Froglog patch; re-run build on clean decode")

    if MARKER in text and O0_MARKER not in text:
        raise SystemExit(
            "ke/d0 has FroglogGameMenuAction but O0 menu patch missing; manual fix needed"
        )

    if O0_ANCHOR not in text:
        raise SystemExit("ke/d0 O0 game menu anchor not found")

    text = text.replace(O0_ANCHOR, O0_FROGLOG_BLOCK, 1)
    path.write_text(text)
    print("Patched", path, "(O0 game menu)")


if __name__ == "__main__":
    main()
