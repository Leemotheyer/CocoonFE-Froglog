# Froglog sync module (scaffold)

Gradle module to be created **after** Cocoon Shell sources are imported into `android/`.

Planned package root: `rip.moth.cocoonshell.froglog` (or `com.froglog.cocoon.sync` — finalize when sources are visible).

## Contents (Phase 1+)

- API client (Ktor/Retrofit)
- Room queue for outbound sessions
- `WorkManager` workers
- Settings repository

See [docs/froglog/INTEGRATION_PLAN.md](../../docs/froglog/INTEGRATION_PLAN.md).
