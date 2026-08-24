# PipelineCRM

A deliberately small sales-pipeline CRM used to demonstrate Clean Architecture,
Clean Code, and enforceable quality gates.

Law of the repository: [`CONSTITUTION.md`](CONSTITUTION.md).

## Stack

- Java 21, Maven, Spring Boot 3.5.x (wired from Stage 2)
- PostgreSQL
- Svelte 5 + TypeScript + Vite
- JWT auth (demo only)

## Modules

```
inner-parent           shared quality gates for domain + application (POM only)
domain                 pure entities and value objects
application            use cases and ports
adapter-persistence    JPA implementations of output ports
adapter-web            REST / security adapters
bootstrap              Spring Boot composition root
tools/crap-check       CRAP metric gate
frontend               Svelte UI (consumes the public API only)
```

## Prerequisites

- JDK **21** (not 22+). The build enforces `[21,22)`.
- Node 22+ (frontend)
- Docker (Testcontainers / later compose)

macOS:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
```

## Build

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # macOS; Linux: point at JDK 21
./mvnw verify

cd frontend
npm install
npm run build
```

### Quality commands

| Gate | Command |
| --- | --- |
| Compile, unit/architecture tests, JaCoCo, CRAP | `./mvnw verify` |
| Mutation testing (domain + application) | `./mvnw -Pmutation -pl domain,application -am verify` |
| Frontend production build | `cd frontend && npm run build` |
| Frontend type-check | `cd frontend && npm run check` |

Default `verify` does **not** run PIT (too slow for every local change).
Stage reviews and CI run `-Pmutation` once inner-layer production code exists.

## Current quality metrics

Stage 0 has no domain/use-case production methods yet. Inner-layer coverage,
mutation score, and CRAP summaries will be published here from Stage 3 onward.

| Metric | Target | Current (Stage 0) |
| --- | --- | --- |
| Line coverage (`domain`, `application`) | ≥ 95% | n/a (no executable production methods) |
| Branch coverage (`domain`, `application`) | ≥ 95% | n/a |
| Mutation score (PIT, inner layers) | 100% | n/a (`failWhenNoMutations=false` until Stage 3) |
| CRAP per method (inner layers) | ≤ 6 | n/a |

## Running the app

Application wiring, PostgreSQL, and JWT land in Stage 2+.
Docker Compose for the full demo lands in Stage 8.

## Tests

- Unit + architecture: Maven Surefire during `verify`
- Gherkin (from Stage 1/3): `application/src/test/resources/features`
- QA UI procedures (Stage 7): `docs/qa/`

## Reviews

Builder/Reviewer stage records live in [`docs/reviews/`](docs/reviews/).
