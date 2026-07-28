#!/usr/bin/env python3
"""Guard o0/f RuntimeShader shims for API < 31 (BlueStacks Android 9, etc.)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

MARKER = "froglog_rs_api_guard"
PATH = "smali/o0/f.smali"

METHOD_B = """.method public static bridge synthetic b(Ljava/lang/Object;)Landroid/graphics/RuntimeShader;
    .locals 2

    if-eqz p0, :froglog_rs_ret_null

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1f

    if-ge v0, v1, :froglog_rs_ret_null

    check-cast p0, Landroid/graphics/RuntimeShader;

    return-object p0

    :froglog_rs_ret_null
    const/4 p0, 0x0

    return-object p0
.end method"""

METHOD_B_OLD = """.method public static bridge synthetic b(Ljava/lang/Object;)Landroid/graphics/RuntimeShader;
    .locals 2

    if-nez p0, :froglog_rs_ret_null

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1f

    if-lt v0, v1, :froglog_rs_cast

    :froglog_rs_ret_null
    const/4 p0, 0x0

    return-object p0

    :froglog_rs_cast
    check-cast p0, Landroid/graphics/RuntimeShader;

    return-object p0
.end method"""

METHOD_B_STOCK = """.method public static bridge synthetic b(Ljava/lang/Object;)Landroid/graphics/RuntimeShader;
    .locals 0

    .line 1
    check-cast p0, Landroid/graphics/RuntimeShader;

    return-object p0
.end method"""

METHOD_C = """.method public static synthetic c(Ljava/lang/String;)Landroid/graphics/RuntimeShader;
    .locals 2

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1f

    if-ge v0, v1, :froglog_api_ok

    const/4 v0, 0x0

    return-object v0

    :froglog_api_ok
    new-instance v0, Landroid/graphics/RuntimeShader;

    invoke-direct {v0, p0}, Landroid/graphics/RuntimeShader;-><init>(Ljava/lang/String;)V

    return-object v0
.end method"""

METHOD_C_OLD = """.method public static synthetic c(Ljava/lang/String;)Landroid/graphics/RuntimeShader;
    .locals 1

    .line 1
    new-instance v0, Landroid/graphics/RuntimeShader;

    invoke-direct {v0, p0}, Landroid/graphics/RuntimeShader;-><init>(Ljava/lang/String;)V

    return-object v0
.end method"""

METHOD_A = """.method public static bridge synthetic a(Landroid/graphics/RuntimeShader;)Landroid/graphics/RenderEffect;
    .locals 2

    if-nez p0, :froglog_re_null

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1f

    if-ge v0, v1, :froglog_re_null

    const-string v0, "content"

    invoke-static {p0, v0}, Landroid/graphics/RenderEffect;->createRuntimeShaderEffect(Landroid/graphics/RuntimeShader;Ljava/lang/String;)Landroid/graphics/RenderEffect;

    move-result-object p0

    return-object p0

    :froglog_re_null
    const/4 p0, 0x0

    return-object p0
.end method"""

METHOD_A_OLD = """.method public static bridge synthetic a(Landroid/graphics/RuntimeShader;)Landroid/graphics/RenderEffect;
    .locals 1

    .line 1
    const-string v0, "content"

    invoke-static {p0, v0}, Landroid/graphics/RenderEffect;->createRuntimeShaderEffect(Landroid/graphics/RuntimeShader;Ljava/lang/String;)Landroid/graphics/RenderEffect;

    move-result-object p0

    return-object p0
.end method"""

VOID_GUARD = """
    # {MARKER}
    if-nez p0, :froglog_rs_ok_{tag}
    return-void
    :froglog_rs_ok_{tag}
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, 0x1f
    if-ge v0, v1, :froglog_rs_do_{tag}
    return-void
    :froglog_rs_do_{tag}
""".replace("{MARKER}", MARKER)


VOID_METHOD = re.compile(
    r"(\.method public static (?:\S+ )+\w+\("
    r"Landroid/graphics/RuntimeShader;[^\n]*\)V\n"
    r"    \.locals (\d+)\n)",
    re.MULTILINE,
)


def inject_void_guards(text: str) -> str:
    if MARKER in text:
        return text

    def repl(m: re.Match[str]) -> str:
        header = m.group(1)
        loc = int(m.group(2))
        name_m = re.search(r"\.method public static (?:\S+ )+(\w+)\(", header)
        tag = name_m.group(1) if name_m else "x"
        need_locals = max(2, loc)
        if need_locals != loc:
            header = header.replace(f".locals {loc}", f".locals {need_locals}", 1)
        return header + VOID_GUARD.replace("{tag}", tag)

    return VOID_METHOD.sub(repl, text)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    file = Path(sys.argv[1]) / PATH
    text = file.read_text(encoding="utf-8")

    if MARKER in text and "if-eqz p0, :froglog_rs_ret_null" in text:
        print("RuntimeShader API shim already applied in", file)
        return

    for old in (METHOD_B_OLD, METHOD_B_STOCK):
        if old in text:
            text = text.replace(old, METHOD_B, 1)
            break
    else:
        if METHOD_B.split(".end method")[0] not in text:
            raise SystemExit("o0/f.smali: method b anchor not found")

    if METHOD_C_OLD in text:
        text = text.replace(METHOD_C_OLD, METHOD_C, 1)
    elif METHOD_C.split(":froglog_api_ok")[0] not in text:
        raise SystemExit("o0/f.smali: method c anchor not found")

    if METHOD_A_OLD in text:
        text = text.replace(METHOD_A_OLD, METHOD_A, 1)

    text = inject_void_guards(text)
    file.write_text(text, encoding="utf-8")
    print("Patched", file, "(RuntimeShader API 31+ guard)")


if __name__ == "__main__":
    main()
