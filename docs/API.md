# API and Domain Notes

The app has two entry points:

- server-rendered MVC pages for people using the browser
- JSON REST endpoints under `/api/v1` for programmatic clients

Swagger UI is available locally at `/swagger-ui.html` and documents the REST controllers.
MVC controllers are intentionally hidden from Swagger so the API docs stay focused.

## Local URLs

| Page | URL |
| --- | --- |
| Players | `http://localhost:8080/players` |
| Match history | `http://localhost:8080/matches` |
| Tournaments | `http://localhost:8080/tournaments` |
| REST players | `http://localhost:8080/api/v1/players` |
| REST matches | `http://localhost:8080/api/v1/matches` |
| REST tournaments | `http://localhost:8080/api/v1/tournaments` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:9090/actuator/health` |
| Build info | `http://localhost:9090/actuator/info` |

## Headers

| Header | Required | Description |
| --- | --- | --- |
| `X-Actor` | No | Legacy audit actor header. Logged-in users take priority when authentication is available. |
| `X-Correlation-Id` | No | Request tracing id. If it is missing, the app generates one and returns it in the response. |

The audit header name can be changed with `AUDIT_ACTOR_HEADER`.

## Authentication

Read-only pages and `GET /api/v1/**` endpoints are public.
Write actions require an admin user.

The browser UI uses form login at `/login`.
The REST API accepts HTTP Basic authentication for write endpoints.

For local API calls, set credentials in your shell instead of writing them into commands:

```bash
export APP_ADMIN_USERNAME=local-admin
export APP_ADMIN_PASSWORD='<choose-a-local-password>'
```

The same variables, plus `APP_USER_USERNAME` and `APP_USER_PASSWORD`, can be used to override
the local defaults. The `prod` profile requires these values from the environment.

## Request Blocking

The app can reject requests when a configured header-value pair is present.

Example:

```yaml
request-filter:
  header-restrictions:
    enabled: true
    rules:
      - header-name: X-Blocked-Client
        header-value: legacy-importer
```

Behavior:

- matching requests return `403 Forbidden`
- header values are matched exactly
- if a request has repeated header values, any matching value is enough to block it
- local config has an empty rules list, so nothing is blocked by default

## JSON REST API

REST endpoints return JSON and use the existing service layer, so UI behavior and API behavior stay consistent.
List endpoints accept pagination parameters: `page` and `size`.
Responses use this shape:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### Players API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/players` | List players by Elo rating. |
| `GET` | `/api/v1/players/{playerId}` | Get one player. |
| `POST` | `/api/v1/players` | Create a player. |

Create example:

```bash
curl -i -u "$APP_ADMIN_USERNAME:$APP_ADMIN_PASSWORD" -X POST \
  -H 'Content-Type: application/json' \
  -d '{"nickname":"Alice"}' \
  http://localhost:8080/api/v1/players
```

### Matches API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/matches` | List match history. Supports `playerId` and `opponentId`. |
| `POST` | `/api/v1/matches` | Report a match and update Elo ratings. |
| `DELETE` | `/api/v1/matches/{matchId}` | Cancel a match and repair rating history. |

Report example:

```bash
curl -i -u "$APP_ADMIN_USERNAME:$APP_ADMIN_PASSWORD" -X POST \
  -H 'Content-Type: application/json' \
  -d '{"winnerId":1,"loserId":2}' \
  http://localhost:8080/api/v1/matches
```

### Tournaments API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/tournaments` | List tournaments. |
| `GET` | `/api/v1/tournaments/{tournamentId}` | Get one tournament. |
| `POST` | `/api/v1/tournaments` | Create a draft tournament. |
| `POST` | `/api/v1/tournaments/{tournamentId}/start` | Start a tournament and generate matches. |
| `POST` | `/api/v1/tournaments/matches/{tournamentMatchId}/result` | Report a tournament match result. |

Tournament result example:

```bash
curl -i -u "$APP_ADMIN_USERNAME:$APP_ADMIN_PASSWORD" -X POST \
  -H 'Content-Type: application/json' \
  -d '{"winnerId":1}' \
  http://localhost:8080/api/v1/tournaments/matches/10/result
```

### REST Error Responses

API errors return JSON instead of redirects.
Validation errors include a field-level `validationErrors` map.

```json
{
  "timestamp": "2026-01-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/players",
  "validationErrors": {
    "nickname": "must not be blank"
  }
}
```

## MVC Pages

## Players

### `GET /players`

Shows the leaderboard page.

The model contains registered players sorted by Elo rating, the player registration form, and the match reporting form.

### `POST /players/register`

Creates a player.

Requires an authenticated admin session.

Form fields:

| Name | Required | Notes |
| --- | --- | --- |
| `nickname` | Yes | Must be unique. Validation rules are defined in `CreatePlayerRequest`. |

Success:

- redirects to `/players`
- shows `Player added successfully!`

Browser forms include CSRF tokens automatically.

## Matches

### `GET /matches`

Shows match history.

Query params:

| Name | Required | Notes |
| --- | --- | --- |
| `playerId` | No | Shows matches where this player is winner or loser. |
| `opponentId` | No | With `playerId`, shows head-to-head matches. Alone, it behaves like a single-player filter. |

Filter behavior:

- no params: all matches
- only one id: all matches for that player
- two different ids: matches between those two players in either direction
- same id twice: full history for that player

Examples:

```bash
curl -i http://localhost:8080/matches
curl -i 'http://localhost:8080/matches?playerId=1'
curl -i 'http://localhost:8080/matches?playerId=1&opponentId=2'
```

### `POST /matches/report`

Reports a match and updates Elo ratings in one transaction.

Requires an authenticated admin session.

Form fields:

| Name | Required | Notes |
| --- | --- | --- |
| `winnerId` | Yes | Match winner. |
| `loserId` | Yes | Match loser. Must be different from winner. |

Success:

- redirects to `/players`
- shows `Match reported successfully!`

Browser forms include CSRF tokens automatically.

### `POST /matches/cancel`

Cancels a match and repairs affected ratings.

Requires an authenticated admin session.

Form fields:

| Name | Required | Notes |
| --- | --- | --- |
| `matchId` | Yes | Existing match id. |

Success:

- redirects to `/matches`
- shows `Match cancelled successfully!`

## Tournaments

### `GET /tournaments`

Shows the tournament setup page.

The page contains:

- tournament creation form
- player list for seeding
- saved tournaments with seed order
- generated tournament matches and result buttons

### `POST /tournaments`

Creates a tournament setup.

Requires an authenticated admin session.

Form fields:

| Name | Required | Notes |
| --- | --- | --- |
| `name` | Yes | Tournament name. |
| `playerCount` | Yes | Supported values: `2`, `4`, `8`, `16`. |
| `seedingMode` | Yes | `MANUAL` or `RANDOM`. |
| `gameFormat` | Yes | `BO1`, `BO3`, or `BO5`. |
| `winningPoints` | Yes | Positive value, for example `11` or `21`. |
| `bracketType` | Yes | `SINGLE_ELIMINATION` or `ROUND_ROBIN`. |
| `playerIds` | Yes | Exactly `playerCount` unique ids. Repeated params preserve order for manual seeding. |

Success:

- redirects to `/tournaments`
- shows `Tournament created successfully!`

Browser forms include CSRF tokens automatically.

Manual seeding keeps the submitted player order.
Random seeding is intentionally non-deterministic; once saved, seed numbers are the source of truth.

### `POST /tournaments/{tournamentId}/start`

Starts a draft tournament and generates its first playable matches.

Requires an authenticated admin session.

Single elimination pairs top seeds against bottom seeds in the first round.
Round-robin uses a simple rotating schedule so every player meets every other player once.

Success:

- redirects to `/tournaments`
- shows `Tournament started successfully!`

### `POST /tournaments/matches/{tournamentMatchId}/report`

Records a tournament match winner.

Requires an authenticated admin session.

Form fields:

| Name | Required | Notes |
| --- | --- | --- |
| `winnerId` | Yes | Must be one of the two players in the tournament match. |

The service also creates a normal match record, so Elo ratings and match history stay consistent.
When a single-elimination round is complete, winners move into the next round.
When the final or round-robin schedule is complete, the tournament is marked as completed with a winner.

Success:

- redirects to `/tournaments`
- shows `Tournament match recorded successfully!`

## Elo Rules

Every new player starts with a rating of `1200`.

Expected winner score:

```text
expectedWinner = 1 / (1 + 10 ^ ((loserRating - winnerRating) / 400))
```

The app uses K-factor `30`:

```text
winnerRatingChange = 30 * (1 - expectedWinner)
loserRatingChange = -winnerRatingChange
```

Ratings are stored as `NUMERIC(10, 2)` in PostgreSQL. This avoids floating point issues and keeps values readable.

## Match Cancellation

Elo is order-sensitive, so cancelling an old match is not just deleting a row.

The service:

1. Loads the cancelled match.
2. Reverts its rating delta for both players.
3. Recalculates later matches involving either affected player.
4. Deletes the cancelled match.

This keeps the current leaderboard consistent with visible match history.

## Auditing

Player and match changes are audited with Hibernate event listeners.

Each audit row stores:

- entity name
- entity id
- operation: `INSERT`, `UPDATE`, or `DELETE`
- JSONB snapshot
- actor from the authenticated user, or the request header when no user is available
- timestamp with time zone

If neither authentication nor a usable actor header is present, the configured fallback actor is used.
Tournament setup is not audited yet because the current audit feature was scoped to players and matches.

## Error Handling

This is an MVC app, so errors usually redirect back to a page with flash messages.

Redirect targets:

| Error area | Redirect |
| --- | --- |
| Players | `/players` |
| Match reporting | `/players` |
| Tournament engine | `/tournaments` |
| Match history or cancellation | `/matches` |

Clients should not expect JSON error bodies from these MVC endpoints.
