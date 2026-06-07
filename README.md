# Elo Match Tracker

Elo Match Tracker is a small Spring Boot project for registering players, reporting 1v1 match results,
and keeping player ratings updated with the Elo formula.

I built it as an MVC application first, so the main interface is server-rendered with Thymeleaf.
The same service layer now also powers a JSON REST API under `/api/v1`.

## What The App Does

- Register players with an initial Elo rating of `1200`.
- Report match results and update both players in one transaction.
- Cancel matches and repair later rating history.
- Filter match history by one player or by a pair of players.
- Create tournaments, generate brackets, record results, and determine winners.
- Require login and admin permissions for write actions.
- Store audit revisions for player and match changes.
- Add correlation ids to requests for easier log tracing.
- Expose basic business metrics through Actuator.
- Use row locking for safer concurrent Elo rating updates.
- Optionally block requests by configured header-value pairs.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring MVC, REST controllers, and Thymeleaf
- Spring Data JPA
- Spring Security
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
├── controller      # Thymeleaf MVC endpoints and REST API controllers
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
http://localhost:8080/login
http://localhost:8080/api/v1/players
http://localhost:8080/api/v1/matches
http://localhost:8080/api/v1/tournaments
http://localhost:8080/swagger-ui.html
http://localhost:9090/actuator/health
http://localhost:9090/actuator/info
http://localhost:9090/actuator/metrics
```

Local development credentials are configured through `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`,
`APP_USER_USERNAME`, and `APP_USER_PASSWORD`. The default local values live in `application.yml`;
set real values in your shell or deployment environment before sharing the app.

## Documentation

- [API and Domain Notes](docs/API.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [Support](SUPPORT.md)

## Roadmap

- Add player search and pagination.
- Add match notes and optional game modes.
