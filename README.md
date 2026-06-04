# Elo Match Tracker

Elo Match Tracker is a small Spring Boot project for registering players, reporting 1v1 match results,
and keeping player ratings updated with the Elo formula.

I built it as an MVC application first, so the main interface is server-rendered with Thymeleaf.
The service layer is separated from controllers, so adding a REST API later should not require rewriting the core logic.

## What The App Does

- Register players with an initial Elo rating of `1200`.
- Report match results and update both players in one transaction.
- Cancel matches and repair later rating history.
- Filter match history by one player or by a pair of players.
- Create tournament setups with roster size, seeding mode, game format, scoring, and bracket type.
- Store audit revisions for player and match changes.
- Add correlation ids to requests for easier log tracing.
- Expose basic business metrics through Actuator.
- Use row locking for safer concurrent Elo rating updates.
- Optionally block requests by configured header-value pairs.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring MVC and Thymeleaf
- Spring Data JPA
- PostgreSQL
- Flyway
- Gradle
- JUnit 5, Mockito, AssertJ, Testcontainers
- JaCoCo, Checkstyle, PMD
- Micrometer and Spring Boot Actuator
- Jib for Docker image builds
- GitHub Actions
- Spring Boot build info

## Project Structure

```text
src/main/java/com/emt
├── audit           # Hibernate audit listener and actor resolution
├── configuration   # MVC errors, OpenAPI, request filtering
├── controller      # Thymeleaf MVC endpoints
├── entity          # JPA entities
├── mapper          # entity/request/response mapping
├── model           # requests, responses, enums, exceptions
├── repository      # Spring Data repositories
└── service         # business rules and transactions
```

## Run Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./gradlew bootRun
```

Open the app:

```text
http://localhost:8080/players
```

Useful local links:

```text
http://localhost:8080/matches
http://localhost:8080/tournaments
http://localhost:8080/swagger-ui.html
http://localhost:9090/actuator/health
http://localhost:9090/actuator/info
http://localhost:9090/actuator/metrics
```

## Configuration

Local defaults are defined in `src/main/resources/application.yml`.

Common environment variables:

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=elt_database
DB_USERNAME=elt_user
DB_PASSWORD=elt_pass
DB_SCHEMA=app_elo_match_tracker
AUDIT_ACTOR_HEADER=X-Actor
AUDIT_FALLBACK_ACTOR=system
REQUEST_HEADER_RESTRICTIONS_ENABLED=true
```

The request filter is enabled by default, but there are no blocked headers configured locally.

Example blocked header rule:

```yaml
request-filter:
  header-restrictions:
    enabled: true
    rules:
      - header-name: X-Blocked-Client
        header-value: legacy-importer
```

Run with the production profile:

```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

The production profile disables Swagger UI and keeps actuator exposure limited.

## Tests And Quality Gate

Run unit tests:

```bash
./gradlew unit
```

Run integration tests:

```bash
./gradlew integration
```

Run the full quality gate:

```bash
./gradlew check
```

Run end-to-end tests:

```bash
./gradlew end2end
```

The `check` task includes Checkstyle, PMD, tests, and a JaCoCo coverage rule with a minimum of 70% instruction coverage.

GitHub Actions runs the quality gate on pull requests and validates that the Docker image can be built with Jib.

## Database

Flyway migrations are in:

```text
src/main/resources/db/migration
```

The schema currently includes players, matches, audit revisions, tournaments, and tournament participants.
Rating changes are stored on matches so cancellation can repair Elo history later.
Players also have a version column, and rating updates lock the affected player rows to avoid lost updates.

## Docker Image

Build a local image with Jib:

```bash
./gradlew jibDockerBuild
```

The image name comes from `repository` and `serviceName` in `gradle.properties`.
The Jib base image is pinned by digest to keep local and CI builds reproducible.

## Documentation

- [API and Domain Notes](docs/API.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [Support](SUPPORT.md)

## Roadmap

- Add a separate JSON REST API for external clients.
- Add player search and pagination.
- Add match notes and optional game modes.
- Add tournament result tracking and bracket progression.
- Add authentication for admin actions.
