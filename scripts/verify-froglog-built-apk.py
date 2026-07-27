#!/usr/bin/env python3
"""Post-build checks on cocoon-froglog.apk (catch VerifyError smali before release)."""
from __future__ import annotations

import hashlib
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASELINE_A1 = ROOT / "scripts/baselines/ke-d0-A1.sha256"
D0 = "smali_classes3/ke/d0.smali"


def sha256_method(text: str, name: str, sig: str) -> str:
    m = re.search(
        rf"\.method public static final {name}\({re.escape(sig)}.*?^\.end method",
        text,
        re.M | re.S,
    )
    if not m:
        raise SystemExit(f"{name} method not found in ke/d0.smali")
    return hashlib.sha256(m.group(0).encode()).hexdigest()


def check_smali(apktool_dir: Path) -> None:
    d0 = apktool_dir / D0
    if not d0.is_file():
        raise SystemExit(f"missing {D0}")
    text = d0.read_text(encoding="utf-8")

    expected = BASELINE_A1.read_text(encoding="utf-8").strip()
    a1_hash = sha256_method(text, "A1", "Lwa/a;Lz0/e0;I)V")
    if a1_hash != expected:
        raise SystemExit(
            f"ke/d0.A1 smali changed from stock baseline (VerifyError risk).\n"
            f"  expected sha256: {expected}\n"
            f"  actual:          {a1_hash}\n"
            "  If intentional, update scripts/baselines/ke-d0-A1.sha256"
        )

    if "FroglogGameMenuAction" in text:
        j = re.search(
            r"\.method public static final j\(Lwa/a;Ljava/lang/String;Lz0/e0;I\)V.*?^\.end method",
            text,
            re.M | re.S,
        )
        if not j or "FroglogGameMenuAction" not in j.group(0):
            raise SystemExit("FroglogGameMenuAction must live inside ke/d0.j only")
        jb = j.group(0)
        if "move-object/from16 v6, p0" in jb:
            raise SystemExit("ke/d0.j: title must be p1 (String), not p0 (wa.a)")
        if "move-object/from16 v6, p1" not in jb:
            raise SystemExit("ke/d0.j: missing move-object/from16 v6, p1")

    intr = apktool_dir / "smali_classes6/kotlin/jvm/internal/Intrinsics.smali"
    if not intr.is_file():
        raise SystemExit("smali_classes6 missing kotlin.jvm.internal.Intrinsics")

    if "FroglogCocoonHooks" in (apktool_dir / "smali_classes3/pf/c0.smali").read_text():
        if "iget-object v10, v0, Lpf/y;->a:Lpf/d0;" in (
            apktool_dir / "smali_classes3/pf/c0.smali"
        ).read_text():
            raise SystemExit("pf/c0 session hook still uses wrong pf/y load on v0")

    print("OK: built APK smali checks (A1 baseline, Kotlin, game menu, c0 hook)")


def decode_apk(apk: Path, out: Path) -> None:
    subprocess.run(
        ["apktool", "d", "-f", str(apk), "-o", str(out)],
        check=True,
        capture_output=True,
        text=True,
    )


def dex_has_class(apk: Path, dex_name: str, class_path: str) -> bool:
    with zipfile.ZipFile(apk) as z:
        if dex_name not in z.namelist():
            return False
        data = z.read(dex_name)
    # cheap string scan (class descriptor in dex)
    desc = class_path.replace("/", "/")
    if desc.startswith("L"):
        needle = desc.encode("utf-8", errors="ignore")
    else:
        needle = f"L{desc};".replace(".", "/").encode()
    return needle in data


def main() -> None:
    if len(sys.argv) not in (2, 3):
        raise SystemExit(
            f"usage: {sys.argv[0]} <cocoon-froglog.apk> [apktool-cocoon-dir]\n"
            "  If apktool dir is given, skips re-decode (faster after build)."
        )
    apk = Path(sys.argv[1])
    if not apk.is_file():
        raise SystemExit(f"APK not found: {apk}")

    if not dex_has_class(apk, "classes6.dex", "kotlin/jvm/internal/Intrinsics"):
        raise SystemExit("classes6.dex missing kotlin.jvm.internal.Intrinsics")

    if len(sys.argv) == 3:
        check_smali(Path(sys.argv[2]))
    else:
        if not shutil.which("apktool"):
            raise SystemExit("apktool required to decode APK (or pass apktool dir)")
        with tempfile.TemporaryDirectory(prefix="froglog-verify-") as tmp:
            decode_apk(apk, Path(tmp))
            check_smali(Path(tmp))

    print("OK:", apk)


if __name__ == "__main__":
    main()
