# Stage 5 — Builder hand-off

## Delivered

- Thin REST controllers under `/api/*` that call input ports only
- JWT Bearer filter; unauthenticated API calls return 401
- Domain exceptions mapped in `RestExceptionHandler` (no business rules in controllers)
- Use cases wired as bootstrap `@Bean` methods (`UseCaseConfiguration`)
- Demo users seeded: `sales@pipelinecrm.demo` / `manager@pipelinecrm.demo` (password `password`)
- `GET /api/users` via `ListUsersUseCase` (no password hashes)
- Controller tests (standalone MockMvc) and `ApiEndToEndTest` on Testcontainers Postgres

Controllers do not implement stage guards, closed-won rules, or forecast math.
