# froglog-core

Android library module: Froglog API, auth, sync queue, `FroglogBridge`, and **Froglog Pod** UI.

## Build (library only)

```bash
cd android
cp build.gradle.froglog build.gradle
cp settings.gradle.froglog settings.gradle
cp gradle.properties.froglog gradle.properties
# After APK import, :app is present but may not compile; froglog-core should:
./gradlew :froglog-core:assembleRelease
```

Requires Android SDK (API 34) and JDK 17.

## Cocoon integration

| Piece | Role |
|--------|------|
| `FroglogPodActivity` | Pod UI (login, sync, Wi‑Fi only) |
| `FroglogCocoonHooks` | Call from Cocoon when a `GameSession` ends |
| `FroglogInitProvider` | Loads bridge at process start |
| `scripts/patch-cocoon-manifest-froglog.sh` | Adds Pod to Cocoon manifest with `Theme.Cocoon` |

### Session hook (Java, from decompiled Cocoon)

```java
FroglogCocoonHooks.onGameSessionEnded(
    context,
    session.getId(),
    session.getGameId(),
    session.getGameName(),
    session.getPlatformId(),
    session.getDate(),
    session.getDurationMinutes(),
    session.getEmulatorPackage()
);
```

Search decompiled tree for `GameSessionDao` insert / session end callback.

### Launch Pod (debug)

```bash
adb shell am start -n rip.moth.cocoonshell/rip.moth.cocoonshell.froglog.pod.FroglogPodActivity
```

Home screen Pod shortcut: patch pod registry (see `kd/s.java` near `LogPodActivity`).

## Package layout

```text
froglog/api/          Froglog REST client
froglog/auth/         Encrypted JWT storage
froglog/bridge/       FroglogBridge + repository
froglog/data/         Offline queue + game links
froglog/sync/         WorkManager worker
froglog/pod/          FroglogPodActivity
```
