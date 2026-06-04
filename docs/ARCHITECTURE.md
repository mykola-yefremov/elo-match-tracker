# Architecture

This project is built as a small layered Spring Boot application.
The UI is MVC/Thymeleaf, but the domain logic is kept in services so it can be reused if a REST API is added later.

## Layers

| Layer | Responsibility |
| --- | --- |
| Controller | Handles web requests, prepares model attributes, returns views or redirects. |
| Service | Contains business rules and transaction boundaries. |
| Mapper | Converts between entities, requests, and response models. |
| Repository | Reads and writes database rows through Spring Data JPA. |
| Entity | Represents persisted data and relationships. |

## Player Flow

1. `PlayerController` receives the registration form.
2. `PlayerService` checks nickname uniqueness.
3. `PlayerMapper` creates the entity.
4. `PlayerRepository` saves it.
5. The controller redirects back to `/players`.

Players start with a rating of `1200`.

## Match Flow

1. `MatchController` receives winner and loser ids.
2. `MatchService` checks that both ids are different.
3. The service loads both players with write locks.
4. Elo rating changes are calculated.
5. Both player ratings and the match row are saved in one transaction.
6. The controller redirects back to the leaderboard.

Match history queries use fetch joins for winner and loser. This avoids N+1 queries when rendering the history page.

## Match Cancellation

Cancelling a match is handled as a data correction.

The service reverts the cancelled match delta, recalculates later matches for the affected players,
updates stored deltas, and removes the cancelled match.
This is more work than a delete, but it keeps Elo ratings correct because Elo depends on match order.

## Tournament Setup

Tournaments follow the same structure as players and matches.

`TournamentController` renders the page and submits create requests.
`TournamentService` validates the roster and applies seeding rules.
`TournamentMapper` builds the `Tournament` entity and maps saved tournaments back to response models.

Validation rules:

- player count must be one of `2`, `4`, `8`, or `16`
- selected players must not be empty
- selected players must be unique
- selected roster size must match `playerCount`

Manual seeding keeps the submitted player order.
Random seeding is intentionally non-deterministic; once saved, seed numbers are the source of truth.

The current feature stores setup data only.
Tournament match generation and bracket progression are future work.

## Auditing

Player and match inserts, updates, and deletes are audited through Hibernate event listeners.

The audit listener writes a separate `audit_revision` row with entity name, entity id, operation,
actor, timestamp, and a JSONB snapshot.
For matches, player references are stored as ids instead of nested objects to keep snapshots stable.

The actor comes from the configured request header, currently `X-Actor`.
If the header is missing, the fallback actor is used.

## Request Correlation And Filtering

`CorrelationIdFilter` runs before application filters.
It reads `X-Correlation-Id` when present, generates one when missing, adds it to the response,
and stores it in the logging MDC.

`HeaderRestrictionFilter` runs after correlation id setup.
It checks configured header-value rules and rejects matching requests with `403 Forbidden`.
Rejected requests increment a business metric.

The rules list is empty by default, which keeps local development simple.
Deployments can add rules through configuration without changing Java code.

## Persistence

The app uses PostgreSQL and Flyway.

Important database choices:

- ratings use `NUMERIC(10, 2)` instead of floating point numbers
- player rows have a version column for persistence-level concurrency tracking
- rating updates use pessimistic locks for the affected players
- match history fields are indexed for filtering and recalculation
- audit revisions are indexed for lookup by entity, operation, and creation time
- tournament participants have unique constraints for player membership and seed number per tournament

## Profiles

The default profile is for local development.

The `prod` profile disables Swagger UI and keeps actuator exposure limited.
This is safer for a deployed environment while still allowing local API discovery during development.

## CI And Build Validation

GitHub Actions runs the Gradle quality gate on pull requests to `main`.
The workflow also reviews dependency changes, runs end-to-end tests, and validates that Jib can build the Docker image.

Spring Boot build info is generated during the Gradle build and exposed through `/actuator/info`.
The Jib base image is pinned by digest so container builds are more reproducible.

## Testing Strategy

The project has several test layers:

- unit tests for service behavior and small components
- integration tests with Testcontainers PostgreSQL
- end-to-end tests for the containerized app stack
- JaCoCo coverage verification with a 70% minimum instruction coverage rule

The usual command before opening a PR is:

```bash
./gradlew clean check
```

For infrastructure or container changes, also run:

```bash
./gradlew end2end
./gradlew jibDockerBuild
```
