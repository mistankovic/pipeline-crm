# Stage 2 — Builder hand-off

## Delivered

- Spring Boot 3.5 composition root in `bootstrap`
- Web adapter: Spring Web/Security/Validation, JWT token issuer, BCrypt hasher
- Persistence adapter: Spring Data JPA + PostgreSQL driver (no mappings yet)
- `application.yml` datasource + JWT settings
- Testcontainers PostgreSQL context-load test

Domain and application modules still have zero Spring/JPA dependencies (enforcer + ArchUnit).
