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

Useful local links:

```text
http://localhost:8080/matches
http://localhost:8080/tournaments
http://localhost:8080/swagger-ui.html
http://localhost:9090/actuator/health
http://localhost:9090/actuator/info
http://localhost:9090/actuator/metrics
```

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
