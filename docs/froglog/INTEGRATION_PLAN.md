# Froglog × Cocoon Log Pod — integration plan

This document describes how **CocoonFE-Froglog** will push Log Pod / playtime data to a user’s **Froglog** account. Froglog API details are not finalized yet; see [API_CONTRACT.md](./API_CONTRACT.md) for placeholders and open questions.

## Goals

- Push **completed game sessions** (and optionally aggregates) from Cocoon to Froglog.
- Authenticate as the **user’s Froglog account** (mechanism TBD from API docs).
- Work **offline-first**: queue locally, sync when network is available.
- Keep **upstream merge path** clear: Cocoon Shell sources live under `android/` after import; Froglog code lives in a dedicated Gradle module.

## Non-goals (initial release)

- Replacing Cocoon’s local `game_sessions` storage (local DB remains source of truth on device).
- Bidirectional sync (Froglog → Cocoon) unless the API explicitly supports it later.
- Syncing Picnic screenshots or full library metadata (playtime / Log Pod scope only).

---

## Cocoon side — how Log Pod gets its data today

Cocoon 3 **Log Pod** reads from the same playtime pipeline as widgets and the game info Activity tab:

| Layer | Responsibility |
|--------|----------------|
| **GameStateTracker** | Starts/pauses/ends sessions from emulator foreground focus, screen on/off, grace period. |
| **Session persist** | On end, writes a `GameSession` row to Room (`game_sessions`). |
| **Log Pod UI** | Daily activity, rankings, share-as-image (not a sync API). |
| **Export / backup** | JSON export with ROM-stable keys (`playtime_sessions_backup.json` in unified backup). |

**Integration hook (preferred):** immediately after a session is successfully inserted into Room, enqueue a Froglog sync event. **Secondary hook:** periodic `WorkManager` job that uploads any rows not yet acknowledged by Froglog.

Reference types (from Cocoon 3 `beta-3.0` APK, names will match imported sources):

- `rip.moth.cocoonshell.data.model.GameSession`
- `GameSessionDao` / repository wrapping `getAllSessions()`
- Play history export: `GameSessionsBackup` / per-session DTOs with `game_path`, `platform_id`, `start_time`, `end_time`, `duration_minutes`, `date`

When source is imported, locate the exact save path by searching for `GameSessionDao.insert` and the callback from `GameStateTracker` session end.

---

## Target architecture in this fork

```text
android/                          # Cocoon Shell (imported from upstream release)
  app/                            # main application module (upstream)
  froglog-sync/                 # NEW — Froglog client + queue + workers
docs/froglog/                   # plans + API contract
scripts/import-cocoon-source-from-release.sh
```

### Module: `froglog-sync`

| Component | Purpose |
|-----------|---------|
| `FroglogSettings` | DataStore or EncryptedSharedPreferences: base URL (if configurable), credentials/token, sync enabled, Wi‑Fi only. |
| `FroglogSessionMapper` | Maps `GameSession` (+ optional `Game` join) → Froglog payload (schema TBD). |
| `FroglogSyncQueue` | Room table or reuse pattern: `id`, `sessionId`, `payloadJson`, `state`, `attempts`, `lastError`. |
| `FroglogApi` | Ktor/Retrofit client generated from final OpenAPI. |
| `FroglogSyncWorker` | `WorkManager` periodic + expedited after session end. |
| `FroglogAuthManager` | Login / API key / OAuth — per API docs. |

### UI touchpoints

1. **Settings → Accounts (or Integrations)** — connect Froglog, disconnect, test connection, last sync time.
2. **Log Pod** — optional status chip (“Synced” / “Pending” / “Error”) and “Sync now” action.
3. **First connect** — deep link or in-app WebView if Froglog uses OAuth (TBD).

### Build / branding

- `applicationId` suffix or product flavor `froglog` (decision when Gradle project exists): e.g. `rip.moth.cocoonshell.froglog` vs same id with feature flag.
- `BuildConfig.FROGLOG_SYNC_ENABLED = true` in this fork only.

---

## Sync behavior

### Triggers

| Trigger | Behavior |
|---------|----------|
| Session completed | Enqueue one event; schedule expedited worker if constraints pass. |
| App startup | Drain queue; backfill sessions missing `froglog_synced_at` (local marker). |
| Periodic | e.g. every 6h while charging + Wi‑Fi (user-configurable). |
| Manual | Log Pod or Settings “Sync now”. |

### Idempotency

Assume Froglog (or our client) must cope with retries:

- Client sends a stable **`client_session_key`** derived from Cocoon `GameSession.id` or `(gameId, startTime, endTime)`.
- Server returns acknowledged IDs; store on session row or side table.

### Conflict policy

- **Cocoon wins** for playtime unless Froglog API defines merges.
- No deletion sync until API specifies tombstones.

### Constraints (user settings)

- Sync only on Wi‑Fi (default on).
- Optional: only while charging.
- Respect Android Doze; use `WorkManager` network constraints.

---

## Payload mapping (draft — fill when API docs arrive)

Map each completed session to something Froglog can display on a user profile / activity feed:

| Cocoon field | Froglog field (TBD) | Notes |
|--------------|---------------------|--------|
| `gameId` + ROM path | `game_external_id` | Prefer export-style `game_path` + `platform_id`. |
| `gameName` | `title` | Display fallback. |
| `platformId` | `platform` | Cocoon platform slug. |
| `startTime` / `endTime` | `started_at` / `ended_at` | ISO-8601 UTC. |
| `durationMinutes` | `duration_seconds` | Convert; sub-minute from `playtime_remainder` optional v2. |
| `emulatorPackage` | `emulator` | Optional metadata. |
| `achievementUnlocksJson` | `achievements` | Only if Froglog supports it. |

Aggregates (daily totals, rankings) can be **derived on Froglog** from sessions; only push raw sessions unless the API requires summaries.

---

## Security & privacy

- Store tokens in **EncryptedSharedPreferences** or Android Keystore-backed DataStore.
- Never log credentials or full tokens.
- TLS only; certificate pinning optional later.
- Clear queue on logout; offer “remove my data from this device” locally.

---

## Implementation phases

### Phase 0 — Repo prep (this branch)

- [x] Document plan and source import workflow.
- [x] `android/` placeholder + import script.
- [ ] Import Cocoon Shell sources when release asset is available (see `android/README.md`).

### Phase 1 — Scaffold with real sources

- Import `beta-3.0` (or newer) source tree into `android/`.
- Add `froglog-sync` module; wire into `settings.gradle.kts`.
- Confirm debug build on CI or local README steps.

### Phase 2 — API contract

- Replace placeholders in `API_CONTRACT.md` with Froglog OpenAPI.
- Implement `FroglogApi` + auth flow.
- Unit tests for mapper and idempotency keys.

### Phase 3 — Session pipeline

- Hook session insert → enqueue.
- Implement worker + local sync markers.
- Settings UI for account linking.

### Phase 4 — Log Pod UX

- Sync status in Log Pod.
- Manual sync + error surfacing.

### Phase 5 — Hardening

- Rate limits, exponential backoff, analytics-free error reporting.
- Document user-facing setup in root `README.md`.

---

## Upstream relationship

- **Git remote `upstream`:** `https://github.com/inssekt/CocoonFE.git` — platform JSON and release APKs.
- **Platforms:** continue merging `platforms/` from upstream as needed (`scripts/sync-upstream-platforms.sh`).
- **Android sources:** not in the public git tree today; imported from [GitHub Releases](https://github.com/inssekt/CocoonFE/releases) via `scripts/import-cocoon-source-from-release.sh` when a source archive is published.

---

## Open decisions (for you)

1. Froglog auth model (API key, OAuth, device code).
2. Whether Froglog identifies games by ROM hash, path, IGDB id, or custom slug.
3. Same APK as Cocoon with toggle vs separate `CocoonFE-Froglog` application id.
4. Whether to sync historical sessions on first link (default: yes, batched).
