# Stage 2 — Skeleton + wiring

## Builder hand-off

- `src/domain`, `src/application`, `src/adapters`, `src/lib/crm-api.ts`, `src/routes`
- Spring/JPA replaced by TanStack server functions + SQL adapters as required by the host
- Auth: Better Auth email/password + Google + X
- Postgres schema in `migrations/0002_crm.sql`

## Adversarial review

**Finding 2.1** — Domain must not import `@/lib/db` or React.

**Fix** — `tests/architecture/dependency-rule.test.ts` fails the build on leaks.

**Finding 2.2** — JWT-from-scratch would fight the host auth broker.

**Fix** — Constitution §2. Actor identity is still `context.userId` only.

## STAGE 2 APPROVED
