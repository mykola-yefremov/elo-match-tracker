# Elo Match Tracker

This repository contains "Elo Match Tracker" — a Java Spring Boot service that manages players and 1v1 matches and calculates Elo rating changes. It includes REST endpoints, Thymeleaf views for basic UI pages, Flyway database migrations, and Gradle tasks for unit/integration/end-to-end suites.

**Description**
- **Purpose:** Track player registrations and matches, compute Elo rating updates, and expose a simple web UI + API for integration.
- **Key features:** REST API, server-side rendered templates (`elo-ranking.html`, `match-history.html`), Flyway migrations, OpenAPI / Swagger UI, health and metrics endpoints.

**Tech Stack**
- **Language & Framework:** Java 17, Spring Boot 3
- **Build:** Gradle (wrapper included)
- **DB:** PostgreSQL (Flyway migrations in `src/main/resources/db/migration`)
- **API docs:** springdoc / OpenAPI (Swagger UI)

**Prerequisites**
- **Java 17** (Adoptium or other JDK)
- **Gradle wrapper** (included: use `./gradlew` on Unix or `.` `\`gradlew.bat` on Windows)
- **Docker** (optional — used for local Postgres or building images)

**Quick Start (Windows PowerShell)**
- Run a local Postgres (docker-compose):

```powershell
docker compose up -d
```

- Start the app using the Gradle wrapper:

```powershell
.\gradlew.bat bootRun
```

- Build artifact:

```powershell
.\gradlew.bat clean build
```

- Run tests (unit/integration/end2end):

```powershell
.\gradlew.bat test            # runs default test task (JUnit Platform)
.\gradlew.bat integration     # run integration suite
.\gradlew.bat end2end         # run end-to-end suite (will build docker image via jib)
```

**Docker / Image**
- Local Docker image build via Jib (configured in `build.gradle`):

```powershell
.\gradlew.bat jibDockerBuild
```

- The `jib.to.image` is assembled using values from `gradle.properties` (`repository` and `serviceName`).

**Configuration**
- Main config file: `src/main/resources/application.yml`.
- DB connection is configured by properties at the top of `application.yml` (`dbHost`, `dbPort`, `dbName`, `dbUsername`, `dbPassword`). Adjust there or provide overrides via environment variables or OS-specific property files.
- Flyway migrations live in `src/main/resources/db/migration` and are applied on app startup.

**API / UI**
- OpenAPI / Swagger UI is enabled (springdoc). The Swagger UI path is configured in `application.yml`.
- Thymeleaf templates are in `src/main/resources/templates` (`elo-ranking.html`, `match-history.html`).
- Actuator endpoints (health, info, metrics) are exposed; management port is configured (`management.server.port: 9090`).

**Project Structure (high level)**
- `src/main/java/com/emt`: main application packages
	- `controller` — REST controllers and UI controllers
	- `service` — business logic (Elo calculation, match handling)
	- `repository` — JPA repositories
	- `entity` / `model` — domain objects and DTOs
- `src/main/resources` — configuration, templates, and DB migrations
- `src/test` — unit, integration, end-to-end test suites

**Developer Notes & Suggestions**
- Add a `CONTRIBUTING.md` describing code style, commit/message guidelines, and how to run tests locally.
- Add CI badges (GitHub Actions, GitLab CI or similar) to README once CI workflows are available.
- Consider adding an example `.env.example` or `application-local.yml` to make local overrides explicit.
- If you plan to publish an image, update `gradle.properties` with a real repository and consider using a CI job to build & push images.

**Running in production / Docker**
- Configure DB credentials securely (secrets manager / environment variables). Do not commit secrets.
- Use an orchestrator (Kubernetes) or a proper service (container registry) for production images.

**License & Contact**
- No license specified. Add a `LICENSE` file if you intend to make this open source.
- Questions / help: open an issue or contact the repository owner.
