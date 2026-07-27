#!/usr/bin/env python3
"""Add manual Submit to Froglog on Picnic screenshot detail (ke/ff.smali method Y)."""
from __future__ import annotations

import sys
from pathlib import Path

MARKER = "FroglogPicnicSubmitAction"
SELECT_BLOCK = """    .line 726
    const-string v3, "SELECT"

    .line 727
    .line 728
    move-object/from16 v4, p1

    .line 729
    .line 730
    const/4 v5, 0x0

    .line 731
    invoke-static {v3, v4, v5, v10, v1}, Lke/ff;->W(Ljava/lang/String;Lwa/a;Lp1/o;Lz0/e0;I)V"""

FROGLOG_SUBMIT_BLOCK = """    move-object/from16 v8, p0

    const v3, 0x7f0d0693

    invoke-static {v3, v10}, Lo1/d;->y(ILz0/e0;)Ljava/lang/String;

    move-result-object v3

    sget-object v4, Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;->b:Lz0/m2;

    invoke-virtual {v10, v4}, Lz0/e0;->j(Lz0/n1;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Landroid/content/Context;

    invoke-virtual {v10, v4}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v6

    invoke-virtual {v10, v8}, Lz0/e0;->f(Ljava/lang/Object;)Z

    move-result v7

    or-int/2addr v6, v7

    invoke-virtual {v10}, Lz0/e0;->Q()Ljava/lang/Object;

    move-result-object v7

    if-nez v6, :froglog_submit_remembered

    sget-object v6, Lz0/j;->a:Lz0/c;

    if-ne v7, v6, :froglog_submit_invoke

    :froglog_submit_remembered
    new-instance v7, Lrip/moth/cocoonshell/froglog/picnic/FroglogPicnicSubmitAction;

    invoke-direct {v7, v4, v8}, Lrip/moth/cocoonshell/froglog/picnic/FroglogPicnicSubmitAction;-><init>(Landroid/content/Context;Ljava/lang/Object;)V

    invoke-virtual {v10, v7}, Lz0/e0;->m0(Ljava/lang/Object;)V

    :froglog_submit_invoke
    move-object v4, v7

    check-cast v4, Lwa/a;

    const/4 v5, 0x0

    invoke-static {v3, v4, v5, v10, v1}, Lke/ff;->W(Ljava/lang/String;Lwa/a;Lp1/o;Lz0/e0;I)V

    .line 726
    const-string v3, "SELECT"

    .line 727
    .line 728
    move-object/from16 v4, p1

    .line 729
    .line 730
    const/4 v5, 0x0

    .line 731
    invoke-static {v3, v4, v5, v10, v1}, Lke/ff;->W(Ljava/lang/String;Lwa/a;Lp1/o;Lz0/e0;I)V"""


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    path = Path(sys.argv[1]) / "smali_classes3/ke/ff.smali"
    text = path.read_text(encoding="utf-8")
    if MARKER in text:
        print("Picnic Froglog submit button already patched")
        return
    if SELECT_BLOCK not in text:
        raise SystemExit("Picnic SELECT button anchor not found in ke/ff.smali")
    path.write_text(text.replace(SELECT_BLOCK, FROGLOG_SUBMIT_BLOCK, 1))
    print("Patched", path)


if __name__ == "__main__":
    main()
