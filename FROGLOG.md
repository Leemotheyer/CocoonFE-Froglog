# CocoonFE-Froglog

Fork of [inssekt/CocoonFE](https://github.com/inssekt/CocoonFE): **one Cocoon app** with a **Froglog Pod** and shared **`froglog-core`** library for API access (Log Pod playtime today, Picnic and others later).

## Status

| Area | State |
|------|--------|
| Platform catalog (`platforms/`) | Tracked with upstream |
| Android sources (`android/`) | Run `scripts/import-cocoon-from-apk.sh` (decompile release APK) or `import-cocoon-source-from-release.sh` (`auto`) |
| Froglog API | Documented from [wiki.froglog.co.uk/Api](https://wiki.froglog.co.uk/Api) |
| Froglog Pod + sync code | **`froglog-core`** + `./scripts/build-froglog-apk.sh` → `android/dist/cocoon-froglog.apk` |

## Documentation

| Doc | Contents |
|-----|----------|
| [INTEGRATION_PLAN.md](docs/froglog/INTEGRATION_PLAN.md) | Architecture, sync flow, phases |
| [FROGLOG_POD.md](docs/froglog/FROGLOG_POD.md) | Pod UX, `FroglogBridge`, cross-Pod integration |
| [API_CONTRACT.md](docs/froglog/API_CONTRACT.md) | Endpoints, mapping, `sync_ref` |
| [android/README.md](android/README.md) | Import Cocoon Shell sources |

## Design summary

- **Single APK** — no separate Froglog app; same package as Cocoon in this fork.
- **Froglog Pod** — login, sync, library linking, stats (like Log / Picnic Pods).
- **Log Pod** — stays local-first; sessions auto-upload via `FroglogBridge`.
- **Picnic** — later via the same bridge when Froglog has a media API.

## Developer quick start

```bash
git remote add upstream https://github.com/inssekt/CocoonFE.git  # if needed
chmod +x scripts/*.sh
SOURCE_MODE=apk TAG=beta-3.0 ./scripts/import-cocoon-from-apk.sh
# Or: ./scripts/import-cocoon-source-from-release.sh  # auto → APK decompile
# Build installable Cocoon+Froglog APK:
# ./scripts/build-froglog-apk.sh
```

## Upstream Cocoon

Features and install: [README.md](README.md).
