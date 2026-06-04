# API and Domain Notes

The app currently uses Spring MVC and Thymeleaf. Most endpoints return HTML pages or redirects, not JSON responses.

OpenAPI is enabled only as a local helper. Since there are no REST controllers yet, `/v3/api-docs` mainly shows project metadata.
The actual MVC behavior is documented here.

## Local URLs

| Page | URL |
| --- | --- |
| Players | `http://localhost:8080/players` |
| Match history | `http://localhost:8080/matches` |
| Tournaments | `http://localhost:8080/tournaments` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:9090/actuator/health` |

## Headers

| Header | Required | Description |
| --- | --- | --- |
| `X-Actor` | No | Used by auditing. If it is missing, the app uses the configured fallback actor, currently `system`. |

The header name can be changed with `AUDIT_ACTOR_HEADER`.

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

## Players

### `GET /players`

Shows the leaderboard page.

The model contains registered players sorted by Elo rating, the player registration form, and the match reporting form.

### `POST /players/register`

Creates a player.

Form fields:

| Name | Required | Notes |
| --- | --- | --- |
| `nickname` | Yes | Must be unique. Validation rules are defined in `CreatePlayerRequest`. |

Success:

- redirects to `/players`
- shows `Player added successfully!`

Example:

```bash
curl -i -X POST \
  -H 'X-Actor: demo-user' \
  -d 'nickname=Alice' \
  http://localhost:8080/players/register
```

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

Form fields:

| Name | Required | Notes |
| --- | --- | --- |
| `winnerId` | Yes | Match winner. |
| `loserId` | Yes | Match loser. Must be different from winner. |

Success:

- redirects to `/players`
- shows `Match reported successfully!`

Example:

```bash
curl -i -X POST \
  -H 'X-Actor: match-reporter' \
  -d 'winnerId=1&loserId=2' \
  http://localhost:8080/matches/report
```

### `POST /matches/cancel`

Cancels a match and repairs affected ratings.

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
- existing tournament setups with seed order

### `POST /tournaments`

Creates a tournament setup.

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

Example:

```bash
curl -i -X POST \
  -d 'name=Friday Finals' \
  -d 'playerCount=2' \
  -d 'seedingMode=MANUAL' \
  -d 'gameFormat=BO3' \
  -d 'winningPoints=11' \
  -d 'bracketType=SINGLE_ELIMINATION' \
  -d 'playerIds=1' \
  -d 'playerIds=2' \
  http://localhost:8080/tournaments
```

Current limitation: tournaments store setup and participants only.
Match generation, results, and bracket progression are planned separately.

## Elo Rules

Every new player starts with rating `1200`.

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
- actor from request header
- timestamp with time zone

Tournament setup is not audited yet because the current audit feature was scoped to players and matches.

## Error Handling

This is an MVC app, so errors usually redirect back to a page with flash messages.

Redirect targets:

| Error area | Redirect |
| --- | --- |
| Players | `/players` |
| Match reporting | `/players` |
| Match history or cancellation | `/matches` |
| Tournaments | `/tournaments` |

Clients should not expect JSON error bodies from these MVC endpoints.
