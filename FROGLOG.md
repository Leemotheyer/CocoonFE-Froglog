# CocoonFE-Froglog

Fork of [inssekt/CocoonFE](https://github.com/inssekt/CocoonFE) with **Froglog** integration: the Cocoon 3 **Log Pod** will push playtime sessions to a user’s Froglog account.

## Status

| Area | State |
|------|--------|
| Platform catalog (`platforms/`) | Same as upstream; use `scripts/sync-upstream-platforms.sh` |
| Android sources (`android/`) | **Not imported yet** — run `scripts/import-cocoon-source-from-release.sh` when upstream publishes a release source archive |
| Froglog API client | **Blocked** on API docs — see `docs/froglog/API_CONTRACT.md` |

## Documentation

- [Integration plan](docs/froglog/INTEGRATION_PLAN.md) — architecture, hooks, phases
- [API contract (placeholder)](docs/froglog/API_CONTRACT.md) — fill in when Froglog docs are ready
- [Android import](android/README.md) — how to vendor Cocoon Shell from releases

## Remotes

```bash
git remote add upstream https://github.com/inssekt/CocoonFE.git   # if missing
git fetch upstream
```

- **origin** — this fork (`Leemotheyer/CocoonFE-Froglog`)
- **upstream** — platform JSON + official APK releases

## Quick start (developers)

1. Clone this repo and add `upstream` as above.
2. When a source zip appears on a [CocoonFE release](https://github.com/inssekt/CocoonFE/releases):

   ```bash
   chmod +x scripts/*.sh
   TAG=beta-3.0 ./scripts/import-cocoon-source-from-release.sh
   ```

3. Open `android/` in Android Studio, verify the app builds.
4. Implement `android/froglog-sync` per the integration plan once API docs exist.

## Upstream Cocoon

Installation, features, and community links remain in [README.md](README.md).
