# Froglog API contract (placeholder)

**Status:** waiting on official Froglog API documentation.

When docs are available, update this file (or add `froglog-openapi.yaml` alongside it) and implement `android/froglog-sync` against the real schema.

## Expected endpoints (guess — confirm with docs)

| Method | Path (example) | Purpose |
|--------|----------------|---------|
| `POST` | `/auth/token` or `/oauth/token` | Obtain access token |
| `GET` | `/me` | Validate credentials |
| `POST` | `/play-sessions` or `/activity` | Upload one or more sessions |
| `POST` | `/play-sessions/batch` | Bulk historical import |
| `DELETE` | `/devices/{id}` | Revoke device (optional) |

## Request headers (typical)

```http
Authorization: Bearer <access_token>
Content-Type: application/json
User-Agent: CocoonFE-Froglog/<version> (Android)
X-Client-Device-Id: <stable uuid per install>
```

## Example session body (draft)

```json
{
  "client_session_key": "cocoon:game_session:12345",
  "game": {
    "platform_id": "Nintendo64",
    "rom_path": "/storage/.../Mario64.z64",
    "display_name": "Super Mario 64"
  },
  "started_at": "2026-07-27T14:02:00Z",
  "ended_at": "2026-07-27T15:31:00Z",
  "duration_seconds": 5340,
  "emulator_package": "org.dolphinemu.dolphinemu",
  "source": "cocoon_log_pod"
}
```

## Response expectations

Document from Froglog:

- Success status codes and response body (e.g. server-assigned `session_id`).
- Error codes for auth failure, validation, rate limit (`429`), conflict (`409`).
- Whether batch responses are partial-success arrays.

## Local implementation checklist (post-docs)

- [ ] Add OpenAPI spec to `docs/froglog/`
- [ ] Configure Ktor/Retrofit in `froglog-sync`
- [ ] Map Cocoon `GameSession` fields to documented schema
- [ ] Implement auth + token refresh if applicable
- [ ] Add contract tests against Froglog staging environment

## Questions for Froglog API owners

1. Authentication and token lifetime / refresh.
2. Stable game identity across devices (ROM hash vs path vs external IDs).
3. Idempotency key header name (e.g. `Idempotency-Key`).
4. Rate limits and recommended batch size for backfill.
5. Privacy: minimum fields required on public profile vs private log.
