# Cocoon Shell (Android) sources

This directory will contain the **Cocoon Shell** Gradle project imported from [inssekt/CocoonFE releases](https://github.com/inssekt/CocoonFE/releases).

The public `main` branch of upstream currently ships **platform JSON** only; the Android app is distributed as an APK. When upstream attaches a **source archive** to a release, import it here before implementing Froglog sync.

## Import sources

From the repository root:

```bash
# Latest beta tag (override with TAG=beta-3.0)
./scripts/import-cocoon-source-from-release.sh

# Specific tag
TAG=beta-3.0 ./scripts/import-cocoon-source-from-release.sh
```

The script will:

1. List assets on the GitHub release.
2. Download the first asset that looks like source (`source`, `src`, `.zip` excluding `.apk`).
3. Extract into `android/` (see script for layout normalization).
4. Write `android/SOURCE_VERSION` with the tag name.

### If no source asset exists yet

As of **beta-3.0**, the release only publishes `cocoon-3.apk`. The APK does **not** include Kotlin sources. Track [CocoonFE releases](https://github.com/inssekt/CocoonFE/releases) for a source bundle, or obtain sources directly from the maintainer.

Do **not** commit decompiled code into this fork; wait for the official archive.

## After import

1. Open `android/` in Android Studio (Giraffe or newer recommended).
2. Sync Gradle and confirm `:app` assembles.
3. Add the **`froglog-core`** module per [docs/froglog/INTEGRATION_PLAN.md](../docs/froglog/INTEGRATION_PLAN.md):
   - Create `android/froglog-core/`
   - `include(":froglog-core")` in `settings.gradle.kts`
   - `implementation(project(":froglog-core"))` in `app`
4. Register **Froglog Pod** and hook `GameSession` / `FroglogBridge` per [docs/froglog/FROGLOG_POD.md](../docs/froglog/FROGLOG_POD.md).

## Version pin

| Field | Value |
|--------|--------|
| Target Cocoon release | `beta-3.0` (Cocoon 3 — Metamorphosis) |
| Imported | _not yet — run import script when source asset is available_ |

See `SOURCE_VERSION` after a successful import.
