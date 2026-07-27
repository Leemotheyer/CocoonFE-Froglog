# Froglog API contract (Cocoon integration)

Official docs: [FrogDocs — API](https://wiki.froglog.co.uk/Api) · [Documentation](https://wiki.froglog.co.uk/Api/Documentation) · [Examples](https://wiki.froglog.co.uk/Api/Examples)

## Base URLs

| Environment | Base URL |
|-------------|----------|
| Production | `https://api.froglog.co.uk/api` |
| Development | `http://localhost:3001/api` |

Cocoon ships **production** only in release builds; optional dev override in Froglog Pod debug settings.

## Authentication

JWT on all routes except `/api/auth/*`.

```http
Authorization: Bearer <jwt>
```

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/register` | `POST` | `{ "username", "password" }` → `{ "token", "username" }` (201) |
| `/api/auth/login` | `POST` | `{ "username", "password" }` → `{ "token", "username", "avatar_url" }` (200) |

- Token lifetime: **30 days** (per wiki).
- Store refresh UX: re-login from Froglog Pod when requests return `401`.
- Rate limit: auth endpoints **20 / 15 min / IP**.

## Endpoints used by Cocoon (v1)

### Library

| Endpoint | Method | Cocoon use |
|----------|--------|------------|
| `/api/games` | `GET` | Cache user library; resolve `froglog_game_id` for linked Cocoon games |
| `/api/games` | `POST` | Create Froglog entry when no match (rate limit **60 / hour / user**) |
| `/api/games/:id` | `PUT` | Optional metadata refresh (hours, dates) |
| `/api/search?q=` | `GET` | Match Cocoon title → RAWG/IGDB-enriched results (up to 10) |
| `/api/search/fetch?title=` | `GET` | Detail for a chosen search hit when adding a game |

### Play sessions (Log Pod / session tracker)

| Endpoint | Method | Cocoon use |
|----------|--------|------------|
| `/api/games/:id/sessions` | `GET` | Show recent sessions in Froglog Pod; reconcile after sync |
| `/api/games/:id/sessions` | `POST` | **Primary upload** for completed Cocoon `GameSession` |
| `/api/games/:id/sessions/:sessionId` | `PUT` | Correct duration/notes if Cocoon session edited locally |
| `/api/games/:id/sessions/:sessionId` | `DELETE` | Only if user deletes session in Cocoon and sync-delete is enabled (future) |

**Create session body:**

```json
{
  "date": "YYYY-MM-DD",
  "hours": 1.25,
  "notes": "optional",
  "spoiler": false,
  "is_public": true,
  "sync_ref": "cocoon:session:<gameSessionId>"
}
```

**Idempotency:** use `sync_ref` (documented on API) so retries do not duplicate rows. Format:

```text
cocoon:session:{GameSession.id}
```

For live-service titles (user moved game on Froglog), use:

```text
POST /api/live-service/:id/sessions
```

(same body shape per wiki). Cocoon stores whether the linked Froglog row is **library** vs **live-service** in local `froglog_game_links`.

### Wishlist / Up Next (later)

| Endpoint | Method | Future Cocoon use |
|----------|--------|-------------------|
| `/api/wishlist` | `GET` / `POST` | “Up Next” in Froglog Pod |
| `/api/wishlist/:id/move-to-games` | `POST` | Start playing from queue |

### Public / social (Froglog Pod UI)

| Endpoint | Method | Use |
|----------|--------|-----|
| `/api/users/:username/stats` | `GET` | Stats tab in Pod |
| `/api/users/:username/games` | `GET` | Public library preview |

## Cocoon → Froglog field mapping

### Session (`GameSession` → `POST .../sessions`)

| Cocoon | Froglog | Notes |
|--------|---------|--------|
| `date` or `startTime` (local TZ) | `date` | `YYYY-MM-DD` |
| `durationMinutes` | `hours` | `durationMinutes / 60.0`; add sub-minute remainder when available |
| — | `notes` | e.g. `"Cocoon · {emulatorPackage}"` or achievement summary |
| user setting | `is_public` | Default `true` |
| `false` | `spoiler` | Default `false` |
| stable id | `sync_ref` | `cocoon:session:{id}` |

### Game (first link)

| Cocoon | Froglog `POST /api/games` |
|--------|---------------------------|
| scraped / display title | `title` (required) |
| `platformId` | `platform` |
| metadata | `description`, `genre`, `dev`, `img` / `cover_image` when known |
| Steam row | `steam_app_id` when Cocoon has Steam binding |
| — | `session_tracking: true`, `sessions_public` per user prefs |

Search-first flow: `GET /api/search?q={title}` → user picks or auto-pick best match → `POST /api/games` with enriched fields from `/api/search/fetch` when appropriate.

## Rate limits (client behavior)

| Limit | Handling |
|-------|----------|
| Game insert 60/hr | Batch backfill slowly; queue creates |
| Auth 20/15min | Backoff; no tight retry loops |
| Wishlist / Steam sync | Not used in v1 |

Read `Retry-After` / rate-limit headers when present; exponential backoff in `WorkManager`.

## Errors

| Code | Action |
|------|--------|
| `401` | Clear token validity; prompt login in Froglog Pod |
| `404` | Game link stale → re-resolve or recreate link |
| `429` | Reschedule worker |

## Not in API today (Picnic / screenshots)

The public wiki may not document screenshot uploads yet. Cocoon tries `POST /api/games/:id/screenshots` (multipart `image`, `sync_ref`, optional `caption`) and falls back to a minimal `POST .../sessions` note when that route is unavailable.

Picnic supplies `gameId`, `gameName`, and `platformId` on each `PicnicScreenshotRecord`; Froglog game resolution uses the same linking rules as play sessions (`cocoon:picnic:{id}` `sync_ref`).

## References

- [Getting Started](https://wiki.froglog.co.uk/Getting%20Started) — accounts, LilyPad (desktop auto-track; Cocoon is the Android counterpart)
- [Lilypad](https://wiki.froglog.co.uk/Lilypad) — behavioral reference for session auto-submit
