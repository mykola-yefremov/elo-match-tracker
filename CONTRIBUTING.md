# Contributing

Thanks for taking a look at Elo Match Tracker. The project is intentionally compact, so contributions should keep the code easy to read and close to the existing Spring Boot style.

## Local Setup

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./gradlew bootRun
```

## Quality Checks

Before opening a pull request, run:

```bash
./gradlew check
```

For integration coverage, also run:

```bash
./gradlew integration
```

## Commit Style

Use Conventional Commits:

```text
feat: add match notes
fix: keep ratings atomic when reporting matches
test: cover match cancellation recalculation
docs: refresh architecture notes
```

## Code Style

- Keep controllers thin and move business rules into services.
- Keep database changes in Flyway migrations.
- Prefer explicit tests for business rules before changing Elo behavior.
- Avoid broad refactors unless they make a current change easier to reason about.
