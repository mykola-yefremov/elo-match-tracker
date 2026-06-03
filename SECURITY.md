# Security

## Supported Versions

The `develop` branch is the active development line.

## Reporting Issues

If you find a vulnerability, avoid opening a public issue with exploit details. Share the minimum reproducible information with the repository owner so the fix can be prepared safely.

## Production Notes

- Run with `SPRING_PROFILES_ACTIVE=prod`.
- Provide database credentials through environment variables or a secret manager.
- Keep actuator exposure limited to health and info endpoints.
- Do not expose the management port publicly without authentication.
- Disable Swagger UI in production unless it is protected.
