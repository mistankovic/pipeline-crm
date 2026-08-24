# PipelineCRM

A deliberately small sales-pipeline CRM used to demonstrate Clean Architecture,
Clean Code, and enforceable quality gates.

Law of the repository: [`CONSTITUTION.md`](CONSTITUTION.md).

## Stack

- Java 21, Maven, Spring Boot 3.5.x
- PostgreSQL 16
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
- Docker (Testcontainers / Compose)

macOS:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
```

## How to run (Docker Compose)

```bash
docker compose up --build
```

- UI: http://localhost:8081
- API: http://localhost:8080
- Postgres is internal to the Compose network (`postgres:5432`). It is not published on the host.

Demo login:

| Email | Password | Role |
| --- | --- | --- |
| sales@pipelinecrm.demo | password | SALES |
| manager@pipelinecrm.demo | password | MANAGER |

Local (without Compose): run PostgreSQL 16 on `localhost:5432` with database/user/password `pipelinecrm`, then

```bash
./mvnw -pl bootstrap -am package -DskipTests
java -jar bootstrap/target/bootstrap-0.1.0-SNAPSHOT.jar
cd frontend && npm ci && npm run dev
```

Vite proxies `/api` to `http://localhost:8080`.

## Build and test

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # macOS; Linux: point at JDK 21
./mvnw verify

cd frontend
npm ci
npm run build
npm run check
```

### Quality commands

| Gate | Command |
| --- | --- |
| Compile, unit/architecture/Gherkin tests, JaCoCo, CRAP | `./mvnw verify` |
| Mutation testing (domain + application) | `./mvnw -Pmutation -pl domain,application -am verify` |
| Persistence IT (Testcontainers Postgres) | included in `./mvnw verify` (`adapter-persistence`) |
| Frontend production build | `cd frontend && npm run build` |
| Frontend type-check | `cd frontend && npm run check` |
| UI QA procedures | [`docs/qa/ui-procedures.md`](docs/qa/ui-procedures.md) |

Default `verify` does **not** run PIT (too slow for every local change). CI and stage reviews run `-Pmutation`.

## Quality metrics (Stage 7, `./mvnw -Pmutation -pl domain,application -am verify`)

| Metric | Target | Current |
| --- | --- | --- |
| Domain line coverage | ≥ 95% | 334/335 (99.7%) |
| Domain branch coverage | ≥ 95% | 91/92 (98.9%) |
| Application line coverage | ≥ 95% | 134/134 (100%) |
| Application branch coverage | ≥ 95% | 29/30 (96.7%) |
| PIT mutation score (domain) | 100% | 140/140 killed |
| PIT mutation score (application) | 100% | 54/54 killed |
| CRAP per inner-layer method | ≤ 6 | 0 violations (137 domain + 54 application methods) |
| Gherkin | all scenarios | 48 Surefire-counted scenarios |

## Tests

- Unit + architecture: Maven Surefire during `verify`
- Gherkin: `application/src/test/resources/features` (use-case layer, not UI)
- Persistence IT: `adapter-persistence` + Testcontainers
- API: `bootstrap` `ApiEndToEndTest` + thin controller tests
- QA UI procedures: [`docs/qa/`](docs/qa/)

## Reviews

Builder/Reviewer stage records live in [`docs/reviews/`](docs/reviews/).
