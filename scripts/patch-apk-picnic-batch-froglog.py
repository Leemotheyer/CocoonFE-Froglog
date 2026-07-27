#!/usr/bin/env python3
"""Add batch Submit to Froglog on Picnic gallery selection toolbar (ke/ff.smali b)."""
from __future__ import annotations

import sys
from pathlib import Path

MARKER = "FroglogPicnicBatchSubmitAction"
ANCHOR = """    invoke-direct/range {v12 .. v22}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    .line 215
    .line 216
    .line 217
    invoke-static {v12}, Landroid/support/v4/media/session/b;->Z(Ljava/lang/Object;)Ljava/util/List;

    .line 218
    .line 219
    .line 220
    move-result-object v10"""

BATCH_BLOCK = """    sget-object v23, Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;->b:Lz0/m2;

    invoke-virtual {v11, v23}, Lz0/e0;->j(Lz0/n1;)Ljava/lang/Object;

    move-result-object v23

    check-cast v23, Landroid/content/Context;

    invoke-virtual {v11, v23}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v24

    invoke-virtual {v11, v3}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v25

    or-int/2addr v24, v25

    invoke-virtual {v11}, Lz0/e0;->Q()Ljava/lang/Object;

    move-result-object v25

    if-nez v24, :froglog_batch_remembered

    sget-object v24, Lz0/j;->a:Lz0/c;

    if-ne v25, v24, :froglog_batch_invoke

    :froglog_batch_remembered
    new-instance v25, Lrip/moth/cocoonshell/froglog/picnic/FroglogPicnicBatchSubmitAction;

    invoke-direct {v25, v23, v3}, Lrip/moth/cocoonshell/froglog/picnic/FroglogPicnicBatchSubmitAction;-><init>(Landroid/content/Context;Ljava/util/List;)V

    invoke-virtual {v11, v25}, Lz0/e0;->m0(Ljava/lang/Object;)V

    :froglog_batch_invoke
    move-object/from16 v58, v25

    check-cast v58, Lwa/a;

    new-instance v26, Lve/w4;

    const v27, 0x7f0d0696

    invoke-static {v27, v11}, Lo1/d;->y(ILz0/e0;)Ljava/lang/String;

    move-result-object v28

    sget-object v29, Lme/b;->SHARE:Lme/b;

    const-string v31, "X"

    const/16 v32, 0x0

    const/16 v33, 0x0

    const/16 v34, 0x0

    const/16 v35, 0x0

    const/16 v36, 0x3f0

    move-object/from16 v30, v58

    invoke-direct/range {v26 .. v36}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    invoke-direct/range {v12 .. v22}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    filled-new-array {v26, v12}, [Lve/w4;

    move-result-object v23

    invoke-static {v23}, Lia/k;->z0([Ljava/lang/Object;)Ljava/util/List;

    move-result-object v10"""


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    path = Path(sys.argv[1]) / "smali_classes3/ke/ff.smali"
    text = path.read_text(encoding="utf-8")
    if MARKER in text:
        print("Picnic batch Froglog already patched")
        return
    if ANCHOR not in text:
        raise SystemExit("Picnic batch toolbar anchor not found in ke/ff.smali")
    path.write_text(text.replace(ANCHOR, BATCH_BLOCK, 1))
    print("Patched", path)


if __name__ == "__main__":
    main()
