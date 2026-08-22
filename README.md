# PipelineCRM

A deliberately small sales-pipeline CRM, built as a demonstration of Clean Architecture,
Clean Code and tool-driven test discipline (Robert C. Martin's rules, enforced by the
build rather than by good intentions).

The binding rules for this repository are in **[CONSTITUTION.md](CONSTITUTION.md)**.
Read that first — it defines the layers, the dependency rule, and every quality gate.

## Stack

| Concern | Choice |
|---------|--------|
| Database | PostgreSQL 16 |
| Backend | Spring Boot 3.4 · Java 21 · Spring Data JPA · Bean Validation |
| Frontend | Svelte 5 · TypeScript · Vite |
| API | REST/JSON |
| Auth | JWT (HS256) — demo grade, see CONSTITUTION.md §6 |

## Module layout

```
backend/
  domain/               layer 1 — entities, value objects, domain services. Zero dependencies.
  application/          layer 2 — use-case interactors, input/output ports. Depends on domain.
  adapter-persistence/  layer 3 — JPA implementations of the repository ports.
  adapter-web/          layer 3 — REST controllers, DTOs, security filter.
  bootstrap/            layer 4 — Spring Boot app, wiring, migrations, architecture tests.
  config/checkstyle/    Clean Code limits, mechanically enforced.
  tools/crap.py         CRAP metric gate over the JaCoCo XML report.
frontend/               layer 4 — Svelte UI, talks only to the public HTTP API.
docs/reviews/           stage hand-offs and adversarial review records.
docs/qa/                human-followable QA procedures (and their Playwright equivalents).
```

Dependencies point inward only. The direction is enforced three times over: by Maven
module dependencies, by `maven-enforcer-plugin` banned-dependency rules, and by ArchUnit
fitness functions.

## Running the tests

```bash
# Everything: unit, Gherkin acceptance, architecture, integration, mutation, CRAP
mvn -f backend/pom.xml verify

# Fast loop (skips mutation testing)
mvn -f backend/pom.xml verify -Dpit.skip=true

# Frontend
cd frontend && npm ci && npm run check && npm run test:unit
```

Integration tests start a real PostgreSQL via Testcontainers, so Docker must be running.
The CRAP gate shells out to `python3`.

## Quality gates

| Gate | Threshold | Where |
|------|-----------|-------|
| Line coverage (domain, application) | ≥ 95 % | JaCoCo `check` |
| Branch coverage (domain, application) | ≥ 95 % | JaCoCo `check` |
| Mutation score (domain, application) | ≥ 90 % | PIT |
| CRAP per method (domain, application) | ≤ 6 | `backend/tools/crap.py` |
| Cyclomatic complexity per method | ≤ 6 | Checkstyle |
| Dependency direction, no package cycles | 0 violations | ArchUnit |

Each gate fails the build. None of them can be waived without an amendment recorded in
`docs/reviews/`.

## Status

Built stage by stage; each stage is gated by an adversarial review recorded in
`docs/reviews/`. See that directory for what has been approved so far.
