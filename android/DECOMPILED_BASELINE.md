# Decompiled Cocoon baseline (from release APK)

Cocoon Shell is imported with `scripts/import-cocoon-from-apk.sh`, which:

1. Downloads the release **APK** (e.g. `cocoon-3.apk` for `beta-3.0`).
2. Saves **`android/archives/cocoon-<tag>.zip`** — the same bytes as the APK (APK format is ZIP).
3. Runs **jadx** with `--export-gradle` into `android/`.

## Expectations

| Topic | Reality |
|--------|---------|
| **Readable code** | Yes — Java under `app/src/main/java/`, including `rip.moth.cocoonshell.*` |
| **Clean Kotlin** | No — obfuscated names (`ke`, `pf`, …) and decompilation gaps |
| **Gradle build** | jadx emits a **stub** `app/build.gradle` with almost no dependencies; **will not compile** as-is |
| **Froglog work** | Implement in **`froglog-core`**; hook via manifest/intents or gradually replace stubs |
| **Legal** | Treat as upstream binary; prefer official sources when published on the tag archive |

## Finding Log Pod / sessions

After import, search decompiled sources:

```bash
rg -l 'LogPodActivity' android/app/src/main/java
rg -l 'GameSession' android/app/src/main/java
```

## Optional archives

```bash
EXPORT_UNPACKED_ZIP=1 ./scripts/import-cocoon-from-apk.sh   # classes.dex/resources as zip
EXPORT_DECOMPILED_ZIP=1 ./scripts/import-cocoon-from-apk.sh # zip entire android/ tree
```

## jadx install

- Linux CI/image: `/opt/jadx/bin/jadx` (see workflow)
- Local: [jadx releases](https://github.com/skylot/jadx/releases), then `export JADX_BIN=/path/to/jadx`
