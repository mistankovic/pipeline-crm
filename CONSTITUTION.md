# PipelineCRM Constitution

This document is the project’s enforceable quality law. Code that violates it
does not ship. Fitness functions in `tests/architecture` and `scripts/` fail
the build when a rule is broken.

## 1. Purpose

PipelineCRM is a deliberately small sales-pipeline CRM used to demonstrate
professional craftsmanship: rich domain model, Clean Architecture, SOLID,
and quality gates that are measured rather than hoped for.

## 2. Runtime platform (sandbox contract)

The original brief named Spring Boot 3 / Java 21, JPA, Svelte 5, JWT, JUnit,
Cucumber, Testcontainers, PIT, and JaCoCo.

This workspace is an App Builder sandbox whose **non-negotiable preview
contract** is:

- the UI must be a TanStack Start + React application
- it must bind `0.0.0.0:8080` via `npm run dev`
- platform files (`vite.config.ts`, `src/lib/auth/*`, `src/lib/db.ts`,
  `public/__grok/`, `server/`, `scripts/grok-pwa-*`) must be kept
- Postgres is Neon in production and embedded PGLite in preview
- authentication is Better Auth (Google, X, email/password) — not a
  hand-rolled JWT

Java 17 is present; Maven, Docker, Java 21, and a Svelte host are not.
A parallel Spring/Svelte stack would not run in the live preview and
would not be verifiable.

**Therefore the architecture and quality bar of the brief are implemented
in TypeScript**, with equivalent tools:

| Brief                 | This repository                          |
| --------------------- | ---------------------------------------- |
| Spring Boot / JPA     | TanStack Start server functions + SQL    |
| Svelte 5              | React 19 + TanStack Router               |
| JWT username/password | Better Auth email/password + Google + X  |
| JUnit / AssertJ       | Vitest                                   |
| Cucumber              | Gherkin features + Vitest step runner    |
| Testcontainers        | In-memory ports + PGLite Postgres        |
| JaCoCo                | V8 coverage via Vitest                   |
| PIT                   | `scripts/mutation.mjs` on domain code    |
| CRAP tooling          | `scripts/crap.mjs`                       |

The **Dependency Rule, domain richness, and test kinds are not relaxed.**

## 3. Clean Architecture / Dependency Rule

Source code dependencies point strictly inward.

```
Frameworks & Drivers   src/routes, src/components, src/lib/db, src/lib/auth
        ↓
Interface Adapters     src/adapters, src/lib/crm-api.ts
        ↓
Application            src/application  (use cases + ports)
        ↓
Domain                 src/domain       (entities, value objects, domain services)
```

Rules:

1. `src/domain` imports nothing outside `src/domain`. No React, TanStack,
   Kysely, `pg`, Better Auth, Zod, DOM, or HTTP.
2. `src/application` may import `src/domain` only. Ports are interfaces.
   Use cases orchestrate; they do not contain HTTP, SQL, or UI.
3. `src/adapters` may import application ports and domain types. They
   translate. They do not contain business rules.
4. `src/routes` and `src/components` may call server functions (adapters)
   only. They never encode stage-transition guards, close-won rules,
   ownership rules, or forecast formulae.
5. No circular imports across these layers.

Enforced by `tests/architecture/dependency-rule.test.ts`.

## 4. Domain richness

Entities are not bags of getters. Invariants live on the objects that own
them:

- A `Deal` is the only place a stage may change.
- Close-won requires a positive value and at least one Call or Meeting
  **on that deal**.
- Only the owner or a MANAGER may change stage.
- CLOSED_WON forces probability 100; CLOSED_LOST forces 0.
- Terminal stages do not leave.
- Forecast is `sum(value × probability)` of **open** deals, grouped by
  owner or stage, and never mixes currencies.

## 5. SOLID

- One reason to change per use case class.
- Ports (interfaces) for repositories, clock, and ids.
- New activity types or stages extend value objects, not controllers.
- Use cases depend on ports, not SQL.
- Domain services do not know about persistence.

## 6. Test requirements

| Kind            | Location                         | Gate                                      |
| --------------- | -------------------------------- | ----------------------------------------- |
| Unit            | `src/domain/**/*.test.ts`, `src/application/**/*.test.ts` | ≥ 95% line coverage on domain + application |
| Gherkin         | `features/*.feature` + `tests/acceptance` | All scenarios green against use cases     |
| Architecture    | `tests/architecture`             | Dependency rule + no cycles               |
| Mutation        | `scripts/mutation.mjs`           | No surviving relevant mutants in domain   |
| CRAP            | `scripts/crap.mjs`               | Every domain + application method ≤ 6     |
| QA procedures   | `docs/qa/QA-PROCEDURES.md`       | Exercised through the real UI             |

Write the failing test first when adding a rule or guard.

## 7. CRAP metric

```
CRAP(m) = comp(m)^2 * (1 - cov(m))^3 + comp(m)
```

- `comp` is cyclomatic complexity.
- `cov` is line coverage of that method (0..1).
- Target: **CRAP ≤ 6** (prefer ≤ 4) for every method in `src/domain` and
  `src/application`.
- Methods that exceed the budget must be split before merge.

## 8. DRY and size

- Functions stay small enough that their name is the documentation.
- No copy-paste of stage tables, currency math, or authorization checks.
- Duplication is removed when it is real, not when it is coincidental.

## 9. Security

- Every server function that reads or writes CRM data uses `authMiddleware`.
- The actor is always `context.userId`. Client-supplied actor ids are ignored.
- Authorization (owner vs MANAGER) is decided in the use-case / domain layer.
- This is a single-tenant team CRM: authenticated users share the pipeline,
  but mutations are authorized.

## 10. Quality commands (must stay green)

```
npm run test:unit
npm run test:gherkin
npm run test:arch
npm run test:crap
npm run test:mutation
npm run typecheck
npm run build
```

`STAGE X APPROVED` in `docs/reviews/` is required before the next stage
is considered done. Findings and fixes are recorded there.
