# Cocoon Shell (Android) sources

This directory holds the **Cocoon Shell** Gradle project for Froglog integration.

## Canonical source archive (per tag)

GitHub serves a zip of the repository at each tag:

**[https://github.com/inssekt/CocoonFE/archive/refs/tags/beta-3.0.zip](https://github.com/inssekt/CocoonFE/archive/refs/tags/beta-3.0.zip)**

Pattern: `https://github.com/inssekt/CocoonFE/archive/refs/tags/<TAG>.zip`

The import script downloads that URL by default (`SOURCE_MODE=auto` or `tag`).

If upstream also attaches a separate source zip to a [GitHub Release](https://github.com/inssekt/CocoonFE/releases), the script falls back to that when the tag tree has no Gradle project.

## Import

From the repository root:

```bash
chmod +x scripts/import-cocoon-source-from-release.sh

# Default: TAG=beta-3.0, tries tag zip then release assets
./scripts/import-cocoon-source-from-release.sh

# Explicit tag
TAG=beta-3.0 ./scripts/import-cocoon-source-from-release.sh

# Tag zip only
SOURCE_MODE=tag TAG=beta-3.0 ./scripts/import-cocoon-source-from-release.sh

# Release asset only (if present)
SOURCE_MODE=release TAG=beta-3.0 ./scripts/import-cocoon-source-from-release.sh
```

On success, `android/SOURCE_VERSION` records the tag and import source.

If import fails with exit code `4`, the archive did not contain `settings.gradle(.kts)` — the tag snapshot may still be platforms-only until Android sources are added to the repo at that tag.

## After import

1. Open `android/` in Android Studio.
2. Sync Gradle and confirm `:app` builds.
3. Add **`froglog-core`** per [docs/froglog/INTEGRATION_PLAN.md](../docs/froglog/INTEGRATION_PLAN.md).
4. Register **Froglog Pod** per [docs/froglog/FROGLOG_POD.md](../docs/froglog/FROGLOG_POD.md).

## Version pin

| Field | Value |
|--------|--------|
| Target tag | `beta-3.0` (Cocoon 3 — Metamorphosis) |
| Tag archive | [beta-3.0.zip](https://github.com/inssekt/CocoonFE/archive/refs/tags/beta-3.0.zip) |
| Imported | Run script; see `SOURCE_VERSION` |
