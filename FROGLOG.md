# CocoonFE-Froglog

Fork of [inssekt/CocoonFE](https://github.com/inssekt/CocoonFE): **one Cocoon app** with a **Froglog Pod** and shared **`froglog-core`** library for API access (Log Pod playtime today, Picnic and others later).

## Status

| Area | State |
|------|--------|
| Platform catalog (`platforms/`) | Tracked with upstream |
| Android sources (`android/`) | Import via `scripts/import-cocoon-source-from-release.sh` ([tag zip](https://github.com/inssekt/CocoonFE/archive/refs/tags/beta-3.0.zip)) |
| Froglog API | Documented from [wiki.froglog.co.uk/Api](https://wiki.froglog.co.uk/Api) |
| Froglog Pod + sync code | After source import → `android/froglog-core` |

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
TAG=beta-3.0 ./scripts/import-cocoon-source-from-release.sh
# Uses https://github.com/inssekt/CocoonFE/archive/refs/tags/beta-3.0.zip by default
# Open android/ in Android Studio, add :froglog-core, register Froglog Pod
```

## Upstream Cocoon

Features and install: [README.md](README.md).
