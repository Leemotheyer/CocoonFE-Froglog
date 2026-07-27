#!/usr/bin/env python3
"""Register Froglog Pod in Cocoon home Pods overlay (smali + resources)."""
from __future__ import annotations

import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
EXTRA_RES = ROOT / "apk-extra-res"

FROGLOG_DRAWABLE_ID = "0x7f0501e7"
FROGLOG_OVERLAY_STRING_ID = "0x7f0d0692"
MARKER_P0 = "FROGLOG:Ljd/p0"
MARKER_R0 = "cocoon://pod/froglog"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def patch_resources(base: Path) -> None:
    strings = base / "res/values/strings.xml"
    text = _read(strings)
    if "pods_overlay_froglog" not in text:
        text = text.replace(
            '    <string name="pods_overlay_silk">Silk</string>\n',
            '    <string name="pods_overlay_silk">Silk</string>\n'
            '    <string name="pods_overlay_froglog">Froglog</string>\n'
            '    <string name="picnic_submit_froglog">Submit to Froglog</string>\n'
            '    <string name="picnic_froglog_queued">Screenshot queued for Froglog</string>\n',
        )
        _write(strings, text)

    public_xml = base / "res/values/public.xml"
    pub = _read(public_xml)
    if 'name="pods_overlay_froglog"' not in pub:
        pub = pub.replace(
            f'    <public type="string" name="pods_overlay_silk" id="0x7f0d037b" />\n',
            f'    <public type="string" name="pods_overlay_silk" id="0x7f0d037b" />\n'
            f'    <public type="string" name="pods_overlay_froglog" id="{FROGLOG_OVERLAY_STRING_ID}" />\n'
            f'    <public type="string" name="picnic_submit_froglog" id="0x7f0d0693" />\n'
            f'    <public type="string" name="picnic_froglog_queued" id="0x7f0d0694" />\n',
        )
    if 'name="froglog"' not in pub:
        pub = pub.replace(
            f'    <public type="drawable" name="silk" id="0x7f0501e3" />\n',
            f'    <public type="drawable" name="silk" id="0x7f0501e3" />\n'
            f'    <public type="drawable" name="froglog" id="{FROGLOG_DRAWABLE_ID}" />\n',
        )
        _write(public_xml, pub)
    else:
        _write(public_xml, pub)

    drawable_dst = base / "res/drawable/froglog.xml"
    shutil.copy2(EXTRA_RES / "drawable/froglog.xml", drawable_dst)
    patch_extra_froglog_strings(base)


def patch_extra_froglog_strings(base: Path) -> None:
    strings = base / "res/values/strings.xml"
    text = _read(strings)
    extras = [
        ("froglog_open_in_froglog", "Open in Froglog"),
        ("picnic_submit_froglog_batch", "Submit to Froglog"),
    ]
    for name, value in extras:
        if f'name="{name}"' not in text:
            if 'name="pods_overlay_froglog"' in text:
                text = text.replace(
                    "    <string name=\"pods_overlay_froglog\">Froglog</string>\n",
                    f"    <string name=\"pods_overlay_froglog\">Froglog</string>\n"
                    f'    <string name="{name}">{value}</string>\n',
                    1,
                )
            else:
                text = text.replace(
                    "</resources>",
                    f'    <string name="{name}">{value}</string>\n</resources>',
                    1,
                )
    _write(strings, text)

    public_xml = base / "res/values/public.xml"
    pub = _read(public_xml)
    id_map = [
        ("froglog_open_in_froglog", "0x7f0d0695"),
        ("picnic_submit_froglog_batch", "0x7f0d0696"),
    ]
    for name, sid in id_map:
        if f'name="{name}"' not in pub:
            pub = pub.replace(
                '    <public type="string" name="picnic_froglog_queued" id="0x7f0d0694" />\n',
                f'    <public type="string" name="picnic_froglog_queued" id="0x7f0d0694" />\n'
                f'    <public type="string" name="{name}" id="{sid}" />\n',
                1,
            )
    _write(public_xml, pub)


def patch_p0(path: Path) -> None:
    text = _read(path)
    if MARKER_P0 in text:
        return
    text = text.replace(
        ".field public static final enum SETTINGS:Ljd/p0;\n",
        ".field public static final enum FROGLOG:Ljd/p0;\n\n"
        ".field public static final enum SETTINGS:Ljd/p0;\n",
    )
    old_settings = """    new-instance v5, Ljd/p0;

    .line 52
    .line 53
    const-string v6, "SETTINGS"

    .line 54
    .line 55
    const/4 v7, 0x5

    .line 56
    invoke-direct {v5, v6, v7}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    .line 57
    .line 58
    .line 59
    sput-object v5, Ljd/p0;->SETTINGS:Ljd/p0;

    .line 60
    .line 61
    filled-new-array/range {v0 .. v5}, [Ljd/p0;"""
    new_block = """    new-instance v5, Ljd/p0;

    .line 52
    .line 53
    const-string v6, "FROGLOG"

    .line 54
    .line 55
    const/4 v7, 0x5

    .line 56
    invoke-direct {v5, v6, v7}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    .line 57
    .line 58
    .line 59
    sput-object v5, Ljd/p0;->FROGLOG:Ljd/p0;

    .line 60
    .line 61
    new-instance v6, Ljd/p0;

    .line 62
    .line 63
    const-string v7, "SETTINGS"

    .line 64
    .line 65
    const/4 v8, 0x6

    .line 66
    invoke-direct {v6, v7, v8}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    .line 67
    .line 68
    .line 69
    sput-object v6, Ljd/p0;->SETTINGS:Ljd/p0;

    .line 70
    .line 71
    filled-new-array/range {v0 .. v6}, [Ljd/p0;"""
    if old_settings not in text:
        raise SystemExit("p0.smali: SETTINGS block not found")
    text = text.replace(old_settings, new_block)
    text = text.replace(".locals 8\n", ".locals 9\n", 1)
    _write(path, text)


def patch_b4(path: Path) -> None:
    text = _read(path)
    if "Ljd/p0;->FROGLOG" in text:
        return
    insert = """    :catch_4
    :try_start_5
    sget-object v1, Ljd/p0;->FROGLOG:Ljd/p0;

    .line 54
    .line 55
    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    .line 56
    .line 57
    .line 58
    move-result v1

    .line 59
    const/4 v2, 0x6

    .line 60
    aput v2, v0, v1
    :try_end_5
    .catch Ljava/lang/NoSuchFieldError; {:try_start_5 .. :try_end_5} :catch_5

    .line 61
    .line 62
    :catch_5
    :try_start_6
    sget-object v1, Ljd/p0;->SETTINGS:Ljd/p0;

    .line 54
    .line 55
    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    .line 56
    .line 57
    .line 58
    move-result v1

    .line 59
    const/4 v2, 0x7

    .line 60
    aput v2, v0, v1
    :try_end_6
    .catch Ljava/lang/NoSuchFieldError; {:try_start_6 .. :try_end_6} :catch_6

    .line 61
    .line 62
    :catch_6
    sput-object v0, Lpf/b4;->a:[I"""
    old = """    :catch_4
    :try_start_5
    sget-object v1, Ljd/p0;->SETTINGS:Ljd/p0;

    .line 54
    .line 55
    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    .line 56
    .line 57
    .line 58
    move-result v1

    .line 59
    const/4 v2, 0x6

    .line 60
    aput v2, v0, v1
    :try_end_5
    .catch Ljava/lang/NoSuchFieldError; {:try_start_5 .. :try_end_5} :catch_5

    .line 61
    .line 62
    :catch_5
    sput-object v0, Lpf/b4;->a:[I"""
    if old not in text:
        raise SystemExit("b4.smali: SETTINGS mapping not found")
    _write(path, text.replace(old, insert))


def patch_r0(path: Path) -> None:
    text = _read(path)
    if MARKER_R0 in text:
        return
    old = f"""    invoke-direct/range {{v4 .. v12}}, Ljd/q0;-><init>(Ljd/p0;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)V

    .line 114
    .line 115
    .line 116
    new-instance v5, Ljd/q0;

    .line 117
    .line 118
    sget-object v6, Ljd/p0;->SETTINGS:Ljd/p0;

    .line 119
    .line 120
    const-string v12, "settings"

    .line 121
    .line 122
    const v13, 0x7f0d037a

    .line 123
    .line 124
    .line 125
    const-string v7, "cocoon://pod/settings"

    .line 126
    .line 127
    const-string v8, "Settings"

    .line 128
    .line 129
    const-string v9, "Settings"

    .line 130
    .line 131
    const-string v10, "Open Cocoon settings."

    .line 132
    .line 133
    const v11, 0x7f0501e0

    .line 134
    .line 135
    .line 136
    invoke-direct/range {{v5 .. v13}}, Ljd/q0;-><init>(Ljd/p0;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)V

    .line 137
    .line 138
    .line 139
    filled-new-array/range {{v0 .. v5}}, [Ljd/q0;"""
    new = f"""    invoke-direct/range {{v4 .. v12}}, Ljd/q0;-><init>(Ljd/p0;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)V

    .line 114
    .line 115
    .line 116
    new-instance v5, Ljd/q0;

    .line 117
    .line 118
    sget-object v6, Ljd/p0;->FROGLOG:Ljd/p0;

    .line 119
    .line 120
    const-string v12, "froglog"

    .line 121
    .line 122
    const v13, {FROGLOG_OVERLAY_STRING_ID}

    .line 123
    .line 124
    .line 125
    const-string v7, "cocoon://pod/froglog"

    .line 126
    .line 127
    const-string v8, "Froglog"

    .line 128
    .line 129
    const-string v9, "Froglog"

    .line 130
    .line 131
    const-string v10, "Sync playtime and Picnic screenshots with Froglog."

    .line 132
    .line 133
    const v11, {FROGLOG_DRAWABLE_ID}

    .line 134
    .line 135
    .line 136
    invoke-direct/range {{v5 .. v13}}, Ljd/q0;-><init>(Ljd/p0;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)V

    .line 137
    .line 138
    .line 139
    new-instance v6, Ljd/q0;

    .line 140
    .line 141
    sget-object v7, Ljd/p0;->SETTINGS:Ljd/p0;

    .line 142
    .line 143
    const-string v13, "settings"

    .line 144
    .line 145
    const v14, 0x7f0d037a

    .line 146
    .line 147
    .line 148
    const-string v8, "cocoon://pod/settings"

    .line 149
    .line 150
    const-string v9, "Settings"

    .line 151
    .line 152
    const-string v10, "Settings"

    .line 153
    .line 154
    const-string v11, "Open Cocoon settings."

    .line 155
    .line 156
    const v12, 0x7f0501e0

    .line 157
    .line 158
    .line 159
    invoke-direct/range {{v6 .. v14}}, Ljd/q0;-><init>(Ljd/p0;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)V

    .line 160
    .line 161
    .line 162
    filled-new-array/range {{v0 .. v6}}, [Ljd/q0;"""
    if old not in text:
        raise SystemExit("r0.smali: picnic/settings block not found")
    text = text.replace(old, new)
    text = text.replace(".locals 14\n", ".locals 15\n", 1)
    _write(path, text)


def patch_kd_s(path: Path) -> None:
    text = _read(path)
    if "FroglogPodLauncher;->open" in text:
        return
    handler = """
    .line 191
    :pswitch_froglog
    invoke-static {v3}, Lrip/moth/cocoonshell/froglog/pod/FroglogPodLauncher;->open(Landroid/content/Context;)V

    goto/16 :goto_2

"""
    text = text.replace(
        "    .line 192\n    :pswitch_3\n    const/16 p1, 0x3c\n",
        handler + "    .line 192\n    :pswitch_3\n    const/16 p1, 0x3c\n",
    )
    text = text.replace(
        """    :pswitch_data_1
    .packed-switch 0x1
        :pswitch_8
        :pswitch_7
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
    .end packed-switch""",
        """    :pswitch_data_1
    .packed-switch 0x1
        :pswitch_8
        :pswitch_7
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_froglog
        :pswitch_3
    .end packed-switch""",
    )
    _write(path, text)


def patch_kd_a(path: Path) -> None:
    text = _read(path)
    if "FROGLOG:Lkd/a" in text:
        return
    text = text.replace(
        ".field public static final enum SILK:Lkd/a;\n",
        ".field public static final enum FROGLOG:Lkd/a;\n\n.field public static final enum SILK:Lkd/a;\n",
    )
    old = """    sput-object v2, Lkd/a;->PICNIC:Lkd/a;

    .line 30
    .line 31
    filled-new-array {v0, v1, v2}, [Lkd/a;"""
    new = """    sput-object v2, Lkd/a;->PICNIC:Lkd/a;

    .line 30
    .line 31
    new-instance v3, Lkd/a;

    const-string v4, "FROGLOG"

    const/4 v5, 0x3

    invoke-direct {v3, v4, v5}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v3, Lkd/a;->FROGLOG:Lkd/a;

    filled-new-array {v0, v1, v2, v3}, [Lkd/a;"""
    if old not in text:
        raise SystemExit("kd/a.smali: PICNIC block not found")
    text = text.replace(old, new)
    text = text.replace(".locals 5\n", ".locals 6\n", 1)
    _write(path, text)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <apktool-cocoon-dir>")
    base = Path(sys.argv[1])
    smali3 = base / "smali_classes3"
    patch_resources(base)
    patch_p0(smali3 / "jd/p0.smali")
    patch_b4(smali3 / "pf/b4.smali")
    patch_r0(smali3 / "jd/r0.smali")
    patch_kd_s(smali3 / "kd/s.smali")
    patch_kd_a(smali3 / "kd/a.smali")
    print("Froglog pod registry patched")


if __name__ == "__main__":
    main()
