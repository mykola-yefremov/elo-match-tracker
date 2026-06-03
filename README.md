# Elo Match Tracker

Elo Match Tracker is a Java 17 Spring Boot application for tracking 1v1 matches and keeping player ratings consistent with the Elo ranking system.

The project is intentionally small, but built like a production service: database migrations, layered tests, Testcontainers integration checks, container image support, health endpoints, and a 70% JaCoCo coverage gate.

## Highlights

- Server-rendered UI for player rankings and match history
- Elo rating calculation with match cancellation and rating rollback
- JSONB audit revisions for player and match mutations
- PostgreSQL persistence with Flyway migrations
- Integration tests backed by Testcontainers
- Gradle quality gate with JaCoCo coverage verification
- Docker Compose setup for local development
- Production profile with restricted actuator and disabled Swagger UI

## Tech Stack

- Java 17
- Spring Boot 3
- Spring MVC and Thymeleaf
- Spring Data JPA
- PostgreSQL
- Flyway
- Gradle
- JUnit 5, Mockito, AssertJ, Testcontainers
- Jib for container image builds

## Project Structure

```text
src/main/java/com/emt
├── configuration   # MVC exception handling and OpenAPI configuration
├── audit           # entity mutation audit listener and actor resolution
├── controller      # UI endpoints
├── entity          # JPA entities
├── mapper          # entity/DTO mapping
├── model           # requests, responses, exceptions
├── repository      # Spring Data repositories
└── service         # business logic and Elo rating updates
```

## Getting Started

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./gradlew bootRun
```

Open the UI:

```text
http://localhost:8080/players
```

Swagger UI is available locally at:

```text
http://localhost:8080/swagger-ui.html
```

## Configuration

The application reads database settings from environment variables with local defaults:

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=elt_database
DB_USERNAME=elt_user
DB_PASSWORD=elt_pass
DB_SCHEMA=app_elo_match_tracker
AUDIT_ACTOR_HEADER=X-Actor
AUDIT_FALLBACK_ACTOR=system
```

For production, run with:

```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

The production profile disables Swagger UI and keeps actuator exposure limited to health and info endpoints.

## Tests

Run unit tests:

```bash
./gradlew unit
```

Run integration tests:

```bash
./gradlew integration
```

Run the quality gate:

```bash
./gradlew check
```

The quality gate verifies unit tests and enforces at least 70% JaCoCo instruction coverage.

## Database

Flyway migrations live in:

```text
src/main/resources/db/migration
```

Current migrations create player, match, and audit revision tables, store Elo rating deltas for match cancellation, and add indexes for match history and audit lookups.

## Container Image

Build a local Docker image with Jib:

```bash
./gradlew jibDockerBuild
```

The image name is assembled from `repository` and `serviceName` in `gradle.properties`.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [Support](SUPPORT.md)

## Roadmap

- Add a separate REST API layer for external clients
- Add player search and pagination
- Add match notes and optional game modes
- Add authentication for administrative actions
