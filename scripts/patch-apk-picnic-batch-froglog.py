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

# Method b: non-range ops only v0–v15; filled-new-array same. Save v11 (composer) and v12 before v4..v14 /range.
BATCH_BLOCK = """    invoke-direct/range {v12 .. v22}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    sget-object v0, Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;->b:Lz0/m2;

    invoke-virtual {v11, v0}, Lz0/e0;->j(Lz0/n1;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/content/Context;

    invoke-virtual {v11, v0}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v1

    invoke-virtual {v11, v3}, Lz0/e0;->h(Ljava/lang/Object;)Z

    move-result v2

    or-int/2addr v1, v2

    invoke-virtual {v11}, Lz0/e0;->Q()Ljava/lang/Object;

    move-result-object v2

    if-nez v1, :froglog_batch_remembered

    sget-object v1, Lz0/j;->a:Lz0/c;

    if-ne v2, v1, :froglog_batch_invoke

    :froglog_batch_remembered
    new-instance v2, Lrip/moth/cocoonshell/froglog/picnic/FroglogPicnicBatchSubmitAction;

    invoke-direct {v2, v0, v3}, Lrip/moth/cocoonshell/froglog/picnic/FroglogPicnicBatchSubmitAction;-><init>(Landroid/content/Context;Ljava/util/List;)V

    invoke-virtual {v11, v2}, Lz0/e0;->m0(Ljava/lang/Object;)V

    :froglog_batch_invoke
    check-cast v2, Lwa/a;

    move-object v15, v11

    move-object v0, v12

    new-instance v4, Lve/w4;

    const-string v5, "X"

    sget-object v6, Lme/b;->SHARE:Lme/b;

    move-object v7, v2

    const/16 v8, 0x0

    const/16 v9, 0x0

    const/16 v10, 0x0

    const/4 v11, 0x0

    const/16 v12, 0x0

    const/16 v13, 0x0

    const/16 v14, 0x3f0

    invoke-direct/range {v4 .. v14}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    move-object v11, v15

    move-object v12, v0

    filled-new-array {v4, v12}, [Lve/w4;

    move-result-object v0

    invoke-static {v0}, Lia/k;->z0([Ljava/lang/Object;)Ljava/util/List;

    move-result-object v10"""

BATCH_WRONG = """    new-instance v20, Lve/w4;

    const-string v21, "X"

    sget-object v22, Lme/b;->SHARE:Lme/b;

    move-object/from16 v23, v2

    const/16 v24, 0x0

    const/16 v25, 0x0

    const/16 v26, 0x0

    const/16 v27, 0x0

    const/16 v28, 0x0

    const/16 v29, 0x0

    const/16 v30, 0x3f0

    invoke-direct/range {v20 .. v30}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    filled-new-array {v20, v12}, [Lve/w4;"""

BATCH_WRONG2 = """    new-instance v4, Lve/w4;

    const-string v5, "X"

    sget-object v6, Lme/b;->SHARE:Lme/b;

    move-object v7, v2

    const/16 v8, 0x0

    const/16 v9, 0x0

    const/16 v10, 0x0

    const/16 v13, 0x0

    const/16 v14, 0x3f0

    invoke-direct/range {v4 .. v14}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    filled-new-array {v4, v12}, [Lve/w4;"""

BATCH_FIXED = """    move-object v15, v11

    move-object v0, v12

    new-instance v4, Lve/w4;

    const-string v5, "X"

    sget-object v6, Lme/b;->SHARE:Lme/b;

    move-object v7, v2

    const/16 v8, 0x0

    const/16 v9, 0x0

    const/16 v10, 0x0

    const/4 v11, 0x0

    const/16 v12, 0x0

    const/16 v13, 0x0

    const/16 v14, 0x3f0

    invoke-direct/range {v4 .. v14}, Lve/w4;-><init>(Ljava/lang/String;Lme/b;Lwa/a;ZLjava/lang/String;FZILve/x4;I)V

    move-object v11, v15

    move-object v12, v0

    filled-new-array {v4, v12}, [Lve/w4;"""


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    path = Path(sys.argv[1]) / "smali_classes3/ke/ff.smali"
    text = path.read_text(encoding="utf-8")

    for wrong in (BATCH_WRONG, BATCH_WRONG2):
        if wrong in text:
            text = text.replace(wrong, BATCH_FIXED, 1)
            path.write_text(text)
            print("Fixed Picnic batch Froglog w4 registers (ke/ff.smali)")
            return

    if MARKER in text:
        print("Picnic batch Froglog already patched")
        return
    if ANCHOR not in text:
        raise SystemExit("Picnic batch toolbar anchor not found in ke/ff.smali")
    path.write_text(text.replace(ANCHOR, BATCH_BLOCK, 1))
    print("Patched", path)


if __name__ == "__main__":
    main()
