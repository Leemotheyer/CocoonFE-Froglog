# Froglog Pod (Cocoon mini-app)

## Product intent

**One Cocoon app** (`rip.moth.cocoonshell` — same package as upstream, Froglog enabled in this fork). Froglog is not a side APK or flavor; it is a first-class **Pod** alongside Log, Picnic, and Silk.

The Froglog Pod is the **home for account, linking, and sync status**. Other Pods call a shared Kotlin API (`FroglogBridge`) so playtime, screenshots, and future features do not duplicate HTTP or auth logic.

```text
┌─────────────────────────────────────────────────────────┐
│                    Cocoon Shell (1 APK)                  │
├─────────────┬─────────────┬──────────────┬──────────────┤
│  Log Pod    │ Picnic Pod  │ Froglog Pod  │  …           │
│  (local UI) │ (screens)   │ login/stats  │              │
└──────┬──────┴──────┬──────┴──────┬───────┴──────────────┘
       │             │             │
       └─────────────┴─────────────┘
                     │
              FroglogBridge (interface)
                     │
              android/froglog-core/
         API · auth · queue · game links
                     │
              api.froglog.co.uk/api
```

## Froglog Pod — v1 screens

Mirror Cocoon 3 Pod patterns (`LogPodActivity`, `PicnicPodActivity` — exact names from imported sources):

| Screen | Purpose |
|--------|---------|
| **Signed out** | Welcome, link to login/register (in-app forms → `POST /api/auth/login` or register) |
| **Home** | Avatar, username, last sync, pending queue count, “Sync now” |
| **Library link** | Games missing Froglog mapping; search Froglog; confirm match |
| **Stats** | Pull `GET /api/users/:username/stats` (or local cache) |
| **Settings** | Wi‑Fi only, session visibility, auto-sync toggle, sign out |

Optional shortcuts on Cocoon home grid: **Froglog Pod** widget / Pod shortcut (same as other Pods).

## Registration in Cocoon (when sources are imported)

1. **Manifest** — `FroglogPodActivity` (name TBD) with Pod metadata consumed by the home screen Pod registry.
2. **Pod list** — Add entry next to Log / Picnic (icon, label “Froglog”, dual/single layout like other Pods).
3. **Start menu** — Optional “Add Froglog Pod shortcut”.
4. **DI** — Hilt/Koin module `FroglogModule` providing `FroglogBridge` singleton.

Search imported tree for `LogPodActivity` and Pod registration tables to copy the pattern.

## `FroglogBridge` (cross-Pod API)

Kotlin interface in `froglog-core` (implemented by `FroglogRepository`):

```kotlin
interface FroglogBridge {
    val authState: Flow<FroglogAuthState>
    val syncState: Flow<FroglogSyncState>

    suspend fun ensureLinked(cocoonGameId: Long): Result<FroglogGameLink>
    suspend fun enqueueSession(gameSessionId: Long)
    suspend fun syncNow(): Result<Unit>

    // Picnic (future)
    suspend fun enqueueScreenshot(cocoonScreenshotId: Long): Result<Unit>
}
```

| Caller | Behavior |
|--------|----------|
| **Session pipeline** (on `GameSession` insert) | `enqueueSession(id)` if auto-sync on |
| **Log Pod** | Optional badge via `syncState`; deep link to Froglog Pod on error |
| **Picnic Pod** (later) | `enqueueScreenshot` when API exists; until then stub returns `NotSupported` |
| **Froglog Pod** | Full UI + `syncNow()` |

Log Pod **does not** own login or API keys; it only reflects sync health.

## Game linking (local Room)

Table `froglog_game_links`:

| Column | Purpose |
|--------|---------|
| `cocoon_game_id` | PK |
| `froglog_game_id` | Library game id |
| `froglog_kind` | `LIBRARY` \| `LIVE_SERVICE` |
| `sync_ref_prefix` | Optional per-game prefix |
| `linked_at` | Timestamp |

Resolution order: existing link → title match in cached `GET /api/games` → `GET /api/search` → `POST /api/games`.

## LilyPad parity

[LilyPad](https://wiki.froglog.co.uk/Lilypad) auto-tracks desktop play and submits sessions. Cocoon is the **Android** equivalent: session end → queue → `POST /api/games/:id/sessions` with `sync_ref`. No separate LilyPad install required on device.

## UX principles

- User never needs a second app for Froglog basics (login + sync in Pod).
- Failures are visible in Froglog Pod; other Pods show minimal status only.
- Offline: queue grows; sync when constraints met.
