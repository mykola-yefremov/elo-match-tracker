# Architecture

Elo Match Tracker is a layered Spring Boot application. The current interface is server-rendered MVC, while the core business logic is kept in services so a REST API can be added later without rewriting the domain flow.

## Main Flow

1. A player is registered through `PlayerController`.
2. `PlayerService` validates nickname uniqueness and persists the player.
3. A match is reported through `MatchController`.
4. `MatchService` loads both players, calculates the Elo delta, updates both ratings, and saves the match in one transaction.
5. Match history is read with a fetch join to avoid lazy-loading each player row individually.

## Match Cancellation

Cancelling a match is treated as a data correction. The service reverses the cancelled match rating delta, recalculates later matches involving either player, updates stored deltas, and removes the cancelled match.

This keeps current player ratings consistent with the visible match history.

## Persistence

The application uses PostgreSQL with Flyway migrations. Rating values are stored as `NUMERIC(10, 2)` to keep deterministic decimal behavior and avoid floating point drift.

Indexes exist for the match fields used by history and recalculation queries:

- `winner_id`
- `loser_id`
- `created_at`

## Profiles

The default profile is optimized for local development. The `prod` profile disables Swagger UI and keeps actuator exposure limited.

## Testing Strategy

- Unit tests cover service behavior and Elo calculations.
- Integration tests use Testcontainers PostgreSQL for controller and service flows.
- End-to-end tests exercise the containerized application stack.
- JaCoCo enforces a minimum 70% unit-test coverage gate.
