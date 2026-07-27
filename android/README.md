# Cocoon Shell (Android) workspace

## How sources get here

| Method | When |
|--------|------|
| **APK decompile (default for `beta-3.0`)** | `SOURCE_MODE=apk` or `auto` when tag zip has no Gradle tree |
| **Tag archive** | [beta-3.0.zip](https://github.com/inssekt/CocoonFE/archive/refs/tags/beta-3.0.zip) when it contains a real Gradle project |
| **Release asset** | Non-APK zip on GitHub Releases |

### Recommended: import from release APK

```bash
chmod +x scripts/*.sh

# Decompile cocoon-3.apk → android/ (+ android/archives/cocoon-beta-3.0.zip)
SOURCE_MODE=apk TAG=beta-3.0 ./scripts/import-cocoon-from-apk.sh

# Or use the umbrella script (auto falls through to APK)
./scripts/import-cocoon-source-from-release.sh
```

**APK as zip:** `android/archives/cocoon-<tag>.zip` is a copy of the APK (same format as ZIP).

Read [DECOMPILED_BASELINE.md](./DECOMPILED_BASELINE.md) for limits (jadx stubs, obfuscation, build not guaranteed).

### Requirements

- `curl`, `unzip`, `zip`
- [jadx](https://github.com/skylot/jadx/releases) (`JADX_BIN` or `/opt/jadx/bin/jadx`)
- `gh` CLI optional (for resolving APK URL from release metadata)

## Froglog module

After import, `settings.gradle` includes `:froglog-core`. Implement Froglog Pod and sync there — see [docs/froglog/INTEGRATION_PLAN.md](../docs/froglog/INTEGRATION_PLAN.md).

## Version

See `SOURCE_VERSION` after import.
