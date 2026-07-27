# froglog-core (Gradle module)

Shared Froglog integration for **one Cocoon APK**:

- JWT auth, Ktor/Retrofit client (`https://api.froglog.co.uk/api`)
- Session queue + `WorkManager`
- `FroglogBridge` for Log Pod, Picnic Pod, and others
- ViewModels / Compose for **Froglog Pod**

Create this module **after** importing Cocoon Shell into `android/`.

```text
android/froglog-core/
  src/main/java/.../froglog/
    api/
    auth/
    bridge/
    data/
    sync/
    pod/
```

Wire-up:

1. `settings.gradle.kts` → `include(":froglog-core")`
2. `app` → `implementation(project(":froglog-core"))`
3. Register `FroglogPodActivity` per [docs/froglog/FROGLOG_POD.md](../../docs/froglog/FROGLOG_POD.md)
4. Hook `GameSession` insert → `FroglogBridge.enqueueSession`

See [docs/froglog/INTEGRATION_PLAN.md](../../docs/froglog/INTEGRATION_PLAN.md).
