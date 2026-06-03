# API and Domain Behavior

Elo Match Tracker currently exposes a server-rendered MVC interface. The endpoints below return HTML views or redirects rather than JSON response bodies. This keeps the UI simple today while leaving the service layer ready for a dedicated REST API in the future.

## Base URLs

- Application UI: `http://localhost:8080`
- Management server: `http://localhost:9090`
- Swagger UI in local profile: `http://localhost:8080/swagger-ui.html`

## OpenAPI and Swagger

OpenAPI documentation is enabled for local development:

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui.html`

Because the current application is server-rendered MVC, the generated OpenAPI document is mainly a local discovery placeholder with project metadata. The MVC routes are documented manually in this file. A future REST layer should expose JSON endpoints through `@RestController` and become the primary OpenAPI contract.

The `prod` profile disables both OpenAPI docs and Swagger UI. This keeps local discovery convenient while avoiding public API documentation exposure in production.

## Request Headers

| Header | Required | Description |
| --- | --- | --- |
| `X-Actor` | No | Optional actor identifier used by auditing. The header name is configurable through `AUDIT_ACTOR_HEADER`. Missing or blank values fall back to `AUDIT_FALLBACK_ACTOR`, which defaults to `system`. |

## Request Filtering

Incoming requests can be rejected before controller handling when they contain a configured restricted header-value pair. The filter is enabled by default, but the default rules list is empty, so local behavior is unchanged until rules are added.

Example configuration:

```yaml
request-filter:
  header-restrictions:
    enabled: true
    rules:
      - header-name: X-Blocked-Client
        header-value: legacy-importer
```

Filtering semantics:

- any matching rule rejects the request with `403 Forbidden`
- header names follow servlet container matching behavior
- header values are matched exactly
- repeated header values are rejected when any value matches the configured value
- set `REQUEST_HEADER_RESTRICTIONS_ENABLED=false` to disable the filter without removing rules

## Player Endpoints

### `GET /players`

Renders the leaderboard page.

The page includes:

- all registered players sorted by Elo rating descending
- the player registration form
- the match reporting form

Successful response:

- Status: `200 OK`
- View: `elo-ranking`

### `POST /players/register`

Registers a new player.

Form parameters:

| Name | Required | Description |
| --- | --- | --- |
| `nickname` | Yes | Unique player nickname. Must satisfy the validation constraints from `CreatePlayerRequest`. |

Successful response:

- Status: `3xx redirect`
- Redirect: `/players`
- Flash message: `Player added successfully!`

Error behavior:

- duplicate nickname redirects back with an error flash message
- validation errors redirect back with field-level flash errors

Example:

```bash
curl -i -X POST \
  -H 'X-Actor: portfolio-demo' \
  -d 'nickname=Alice' \
  http://localhost:8080/players/register
```

## Match Endpoints

### `GET /matches`

Renders the match history page.

Query parameters:

| Name | Required | Description |
| --- | --- | --- |
| `playerId` | No | Filters history to matches where the selected player was either winner or loser. |
| `opponentId` | No | When used with `playerId`, filters to head-to-head matches between both players. When used alone, behaves like a single-player filter. |

Filtering semantics:

- no filters: show all matches
- only `playerId`: show all matches involving that player
- only `opponentId`: show all matches involving that player
- both ids and different values: show head-to-head matches in either winner/loser direction
- both ids and the same value: show that player's full match history

Successful response:

- Status: `200 OK`
- View: `match-history`

Examples:

```bash
curl -i http://localhost:8080/matches
curl -i 'http://localhost:8080/matches?playerId=1'
curl -i 'http://localhost:8080/matches?playerId=1&opponentId=2'
```

### `POST /matches/report`

Reports a match result and updates both players' Elo ratings in one transaction.

Form parameters:

| Name | Required | Description |
| --- | --- | --- |
| `winnerId` | Yes | Player id of the match winner. |
| `loserId` | Yes | Player id of the match loser. |

Successful response:

- Status: `3xx redirect`
- Redirect: `/players`
- Flash message: `Match reported successfully!`

Error behavior:

- identical winner and loser ids are rejected
- unknown player ids redirect back with an error flash message
- validation errors redirect back with field-level flash errors

Example:

```bash
curl -i -X POST \
  -H 'X-Actor: match-reporter' \
  -d 'winnerId=1&loserId=2' \
  http://localhost:8080/matches/report
```

### `POST /matches/cancel`

Cancels a recorded match and repairs affected Elo ratings.

Form parameters:

| Name | Required | Description |
| --- | --- | --- |
| `matchId` | Yes | Match id to cancel. |

Successful response:

- Status: `3xx redirect`
- Redirect: `/matches`
- Flash message: `Match cancelled successfully!`

Error behavior:

- unknown match ids redirect back with an error flash message

Example:

```bash
curl -i -X POST \
  -H 'X-Actor: admin-user' \
  -d 'matchId=42' \
  http://localhost:8080/matches/cancel
```

## Management Endpoints

### `GET /actuator/health`

Returns application health from the management server.

Successful response:

- Status: `200 OK`
- Body includes status such as `UP`

Example:

```bash
curl -i http://localhost:9090/actuator/health
```

## Elo Rating Model

Every player starts with an Elo rating of `1200`.

When a match is reported, the service calculates the winner's expected score using the standard Elo probability formula:

```text
expectedWinner = 1 / (1 + 10 ^ ((loserRating - winnerRating) / 400))
```

The project uses a constant K-factor of `30`:

```text
winnerRatingChange = 30 * (1 - expectedWinner)
loserRatingChange = -winnerRatingChange
```

Then both ratings are updated in the same transaction:

```text
winner.rating = winner.rating + winnerRatingChange
loser.rating = loser.rating - winnerRatingChange
```

Rating values are persisted as `NUMERIC(10, 2)` in PostgreSQL to avoid floating-point drift.

## Match Cancellation and Rating Repair

A match cancellation is treated as a data correction rather than a simple delete.

The cancellation flow does three things:

1. Loads the match being cancelled.
2. Reverts the stored rating delta for the winner and loser.
3. Recalculates later matches involving either affected player, then deletes the cancelled match.

This matters because Elo is order-sensitive. Removing an old result can change the rating context for later matches, so the service repairs the downstream rating history instead of only deleting one row.

## Match History Filtering

Match history queries use fetch joins for `winner` and `loser` to keep the rendered page from triggering N+1 lazy loading.

The head-to-head filter is direction-agnostic. A request for `playerId=1&opponentId=2` returns both:

- matches where player `1` beat player `2`
- matches where player `2` beat player `1`

## Auditing Behavior

Mutations for `Player` and `Match` are audited through Hibernate event listeners.

Each audit revision stores:

- entity name
- entity id
- operation (`INSERT`, `UPDATE`, `DELETE`)
- JSONB snapshot of entity state
- actor from the configured request header
- timestamp with time zone

Audit writes participate in the same transactional flow as the entity mutation. Match snapshots store player references as ids instead of nested entity graphs, keeping audit payloads compact and stable.

## Error Handling

The current MVC controllers use redirect-based error handling:

- validation errors are stored as flash attributes
- domain exceptions are shown as flash error messages
- `/matches/report` errors redirect to `/players`
- other `/matches` errors redirect to `/matches`
- player errors redirect to `/players`

Because this is currently an MVC-first application, clients should not expect JSON error payloads from these endpoints.

## Future REST API Notes

The service and repository layers are intentionally separated from MVC controllers. A future REST API can reuse the same business logic while adding:

- JSON request and response models
- explicit HTTP status codes for domain errors
- pagination for player and match history queries
- authentication and authorization controls
