#!/usr/bin/env python3
"""Guard w1/a BlendMode / Canvas API shims for API < 29 (Android 9)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

MARKER = "froglog_bm_api_guard"
PATH = "smali/w1/a.smali"
API = "0x1d"  # Android 10 / API 29


def bump_locals(header: str, min_loc: int = 2) -> str:
    m = re.search(r"    \.locals (\d+)\n", header)
    if not m:
        return header
    loc = int(m.group(1))
    if loc >= min_loc:
        return header
    return header.replace(f".locals {loc}", f".locals {min_loc}", 1)


def guard_return_null(tag: str) -> str:
    return f"""
    # {MARKER}
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, {API}
    if-ge v0, v1, :froglog_bm_{tag}
    const/4 v0, 0x0
    return-object v0
    :froglog_bm_{tag}
"""


def guard_return_void(tag: str) -> str:
    return f"""
    # {MARKER}
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, {API}
    if-ge v0, v1, :froglog_bm_{tag}
    return-void
    :froglog_bm_{tag}
"""


def guard_return_zero(tag: str) -> str:
    return f"""
    # {MARKER}
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, {API}
    if-ge v0, v1, :froglog_bm_{tag}
    const-wide/16 v0, 0x0
    return-wide v0
    :froglog_bm_{tag}
"""


def inject_after_locals(text: str, pattern: str, tag: str, guard: str) -> str:
    if f":froglog_bm_{tag}" in text:
        return text

    def repl(m: re.Match[str]) -> str:
        header = bump_locals(m.group(0))
        return header + guard

    return re.sub(pattern, repl, text, count=1, flags=re.MULTILINE)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    file = Path(sys.argv[1]) / PATH
    if not file.is_file():
        raise SystemExit(f"missing {file}")
    text = file.read_text(encoding="utf-8")
    if MARKER in text:
        print("BlendMode API shim already applied in", file)
        return

    getter_pat = (
        r"\.method public static (?:\S+ )+\w+\(\)Landroid/graphics/BlendMode;\n"
        r"    \.locals \d+\n"
    )
    for m in list(re.finditer(getter_pat, text)):
        name_m = re.search(
            r"\.method public static (?:\S+ )+(\w+)\(\)Landroid/graphics/BlendMode;",
            m.group(0),
        )
        tag = name_m.group(1) if name_m else "x"
        if f":froglog_bm_{tag}" in text:
            continue
        header = bump_locals(m.group(0))
        text = text.replace(m.group(0), header + guard_return_null(tag), 1)

    text = inject_after_locals(
        text,
        r"\.method public static synthetic c\(ILandroid/graphics/BlendMode;\)"
        r"Landroid/graphics/BlendModeColorFilter;\n    \.locals \d+\n",
        "c",
        guard_return_null("c"),
    )
    text = inject_after_locals(
        text,
        r"\.method public static bridge synthetic f\(Landroid/graphics/Paint;"
        r"Landroid/graphics/BlendMode;\)V\n    \.locals \d+\n",
        "f",
        guard_return_void("f"),
    )
    text = inject_after_locals(
        text,
        r"\.method public static bridge synthetic e\(Landroid/graphics/Canvas;\)V\n"
        r"    \.locals \d+\n",
        "e",
        guard_return_void("e"),
    )
    text = inject_after_locals(
        text,
        r"\.method public static bridge synthetic h\(Landroid/graphics/Canvas;\)V\n"
        r"    \.locals \d+\n",
        "h",
        guard_return_void("h"),
    )
    text = inject_after_locals(
        text,
        r"\.method public static bridge synthetic a\(Lq2/v;\)J\n    \.locals \d+\n",
        "a",
        guard_return_zero("a"),
    )

    if MARKER not in text:
        raise SystemExit(f"{PATH}: failed to inject BlendMode guards")

    file.write_text(text, encoding="utf-8")
    print("Patched", file, "(BlendMode / Canvas API 29+ guard)")


if __name__ == "__main__":
    main()
