# PipelineCRM

A deliberately small sales-pipeline CRM, built as a demonstration of Clean Architecture,
Clean Code and tool-driven test discipline — Robert C. Martin's rules, enforced by the build
rather than by good intentions.

The binding rules for this repository are in **[CONSTITUTION.md](CONSTITUTION.md)**. Read that
first: it defines the layers, the dependency rule, and every quality gate. Every stage of the
work was gated by an adversarial review; those reviews, and every finding they raised, are in
**[docs/reviews/](docs/reviews/)**.

> **Another implementation of the same brief** lives on the
> [`grok-pipeline-crm`](https://github.com/mistankovic/pipeline-crm/tree/grok-pipeline-crm)
> branch, which is where this repository's `main` pointed before this one landed. Both were
> built from the same specification; neither is a fork of the other.

## What it does

Sales people keep companies, contacts and deals. A deal moves through
`LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON`, and can be lost from anywhere. The
rules that make it a CRM rather than a table editor all live in the domain and use-case layers,
where they are tested without a database, a browser or a web server:

* A deal cannot be **won** without a positive value and at least one Call or Meeting logged
  against it.
* Only the deal's **owner or a MANAGER** may change its stage, reprice it, or reweight it.
* Closing a deal **forces** its probability to 100 (won) or 0 (lost).
* The **forecast** sums `value × probability` over open deals only, grouped by owner or by
  stage, and never adds two currencies together.

Companies and contacts can be corrected after the fact: a company can be renamed without
becoming a second company, and a contact's name and email can be fixed. What a contact cannot
do is change employer, and activities cannot be edited at all — both deliberate, recorded as
D-20 and D-21 in `docs/domain-decisions.md`.

The UI knows none of this. It draws what the API tells it, including which moves are legal for
the person looking — see `youMayChangeThis` and `allowedTransitions` on the deal view.

## Stack

| Concern | Choice |
|---------|--------|
| Database | PostgreSQL 16 |
| Backend | Spring Boot 3.4 · Java 21 · Spring Data JPA · Bean Validation |
| Frontend | Svelte 5 (runes) · TypeScript · Vite |
| API | REST/JSON |
| Auth | JWT, HMAC-SHA — the strength follows the secret's length, so the 48-character demo secret yields HS384. Demo grade; see CONSTITUTION.md §6 |

## Module layout

```
backend/
  domain/               layer 1 — entities, value objects, domain services. Zero dependencies.
  application/          layer 2 — use-case interactors, input/output ports. Depends on domain.
  adapter-persistence/  layer 3 — JPA implementations of the repository ports, Flyway migrations.
  adapter-web/          layer 3 — REST controllers, DTOs, security filter, HTTP error translation.
  bootstrap/            layer 4 — Spring Boot app, wiring, architecture tests, API tests.
  config/checkstyle/    Clean Code limits, mechanically enforced.
  tools/crap.py         CRAP metric gate over the JaCoCo XML report.
frontend/               layer 4 — Svelte UI, talks only to the public HTTP API.
  qa/                   the QA procedures, scripted with Playwright.
docs/reviews/           stage hand-offs and adversarial review records.
docs/qa/                human-followable QA procedures, and recorded runs.
docs/domain-decisions.md  every domain question that had more than one defensible answer.
```

Dependencies point inward only. The direction is enforced three times over: by the Maven module
graph, by `maven-enforcer-plugin` banned-dependency rules, and by ArchUnit fitness functions.

---

## How to run it

### With Docker

```bash
docker compose up --build
```

Then open **http://localhost:8080**. This brings up PostgreSQL, the API, and nginx serving the
built frontend and proxying `/api` to the API.

> **Honesty note.** `docker compose config` validates this stack, and the Dockerfiles are
> ordinary multi-stage builds, but **the compose stack has never been executed**: the
> environment this project was built in cannot pull container images (every registry returns
> 403). It is offered as the conventional path, not as a tested one. The path below *is* tested,
> and is the one everything in `docs/qa/runs/` was run against.

### Without Docker

```bash
scripts/run-locally.sh          # start PostgreSQL, the API, and the Vite dev server
scripts/run-locally.sh --stop   # stop all three
```

Then open **http://localhost:5173**. Needs Java 21, Maven, Node 22 and a PostgreSQL 16 server
binary on the machine. The script refuses to start if something already holds port 8080 or 5173,
rather than waiting on a health check that a stranger would satisfy.

### The demo users

Seeded by `V2__demo_users.sql`. This is a demo and the passwords are deliberately public.

| Who | Email | Password | Role |
|-----|-------|----------|------|
| Sam Sales | `sam@pipelinecrm.demo` | `sam-password` | SALES |
| Robin Reid | `robin@pipelinecrm.demo` | `robin-password` | SALES |
| Mo Mancini | `mo@pipelinecrm.demo` | `mo-password` | MANAGER |

There is no sign-up. Users are seeded, deliberately — see `docs/domain-decisions.md` D-13.

---

## How to run the tests

### Everything in the backend

```bash
mvn -f backend/pom.xml verify
```

That single command runs the unit tests, the Gherkin acceptance suite, the architecture fitness
functions, the persistence integration tests against a real PostgreSQL, the REST API tests over
real HTTP, mutation testing, and the coverage, CRAP, Checkstyle and duplication gates. Any one
of them failing fails the build.

```bash
mvn -f backend/pom.xml verify -Dpit.skip=true    # fast loop: same, minus mutation testing
```

Integration tests run against a **real PostgreSQL** — never an in-memory substitute. By default
they start one with Testcontainers, so Docker must be running. If the environment already has a
PostgreSQL, point the tests at it and no container is started:

```bash
export PIPELINECRM_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5432/pipelinecrm_test
export PIPELINECRM_TEST_DB_USER=pipelinecrm
export PIPELINECRM_TEST_DB_PASSWORD=pipelinecrm
mvn -f backend/pom.xml verify
```

That escape hatch is not a convenience — it is how this project's own integration tests were
ever run, because images could not be pulled here. The CRAP gate shells out to `python3`.

### Individual backend suites

```bash
# unit tests of the two inner layers
mvn -f backend/pom.xml test -pl domain,application -am

# the Gherkin acceptance suite alone (83 scenarios, through the input ports)
mvn -f backend/pom.xml test -pl application -am \
    -Dtest=AcceptanceTest -Dsurefire.failIfNoSpecifiedTests=false

# the architecture fitness functions alone (13 rules)
mvn -f backend/pom.xml test -pl bootstrap -am \
    -Dtest='com.pipelinecrm.architecture.*Test' -Dsurefire.failIfNoSpecifiedTests=false

# coverage, mutation and CRAP for the inner layers, without the integration suites
mvn -f backend/pom.xml verify -pl domain,application -am
```

The `-am` is not optional. `AnalysedCodeTest` refuses to certify the architecture against
already-installed jars, so running `-pl bootstrap` on its own fails on purpose rather than
quietly checking a stale copy of the code. That guard exists because an earlier version of
these tests did exactly that — see F-2.1 in `docs/reviews/stage-2-review.md`.

### Frontend

```bash
cd frontend
npm ci
npm run check       # svelte-check: types and unused/unsafe markup
npm run test:unit   # vitest
```

### The QA procedures

`docs/qa/procedures.md` holds 40 numbered procedures written to be followed **by a person with
a browser and no access to the code**. Each one is also scripted, one Playwright test per
numbered step. Where the two disagree, the document is the specification.

```bash
# with the application already running (see "How to run it")
cd frontend && npm run test:qa
```

If Playwright cannot download its pinned browser, point it at one you already have —
otherwise every procedure fails at launch, which says nothing about the application:

```bash
PLAYWRIGHT_CHROMIUM_PATH=/path/to/chrome npm run test:qa
```

---

## Final quality metrics

Measured by `mvn -f backend/pom.xml verify` on the current commit, not from memory. Coverage and
mutation gates apply to `domain` and `application` — the layers that hold the business rules.
The outer layers are covered by integration and API tests instead, which is a deliberate choice
recorded in CONSTITUTION.md §3.

### Coverage and mutation, domain + application

| Module | Line | Branch | Method | Class | Mutation score |
|--------|------|--------|--------|-------|----------------|
| `domain` | **100.00 %** (305/305) | **100.00 %** (69/69) | 100.00 % (131/131) | 100.00 % (42/42) | **100.00 %** — 126/126 killed |
| `application` | **100.00 %** (297/297) | **100.00 %** (16/16) | 100.00 % (136/136) | 100.00 % (58/58) | **100.00 %** — 96/96 killed |

**Zero surviving mutants.** The gates require 95 % coverage and a 90 % mutation score; the
survivors that existed along the way, and what each one exposed, are recorded in
`docs/mutation-survivors.md`. Two of them were real defects in the tests, not noise.

### CRAP

| Module | Methods | Worst CRAP | Worst method |
|--------|---------|-----------|--------------|
| `domain` | 131 | **4.00** | `Deal.requireWinIsEarned` |
| `application` | 136 | **2.00** | `ActivityViews.dealIn` |

The Constitution's limit is 6 and its preference is 4. Every method in both layers is at or
under 4, so the preference is met, not merely the limit.

### Tests

| Suite | Count | What it proves |
|-------|-------|----------------|
| `domain` unit | 273 | The business rules, with no framework in sight |
| `application` unit + Gherkin | 171 (of which **83 Gherkin scenarios** across 10 feature files) | The use cases, driven through their input ports |
| `adapter-persistence` integration | 47 | Mapping, updates and constraints against a real PostgreSQL |
| `adapter-web` unit | 28 | HTTP translation, JWT handling, error shape |
| `bootstrap` architecture + API | 81 (13 ArchUnit rules) | The dependency rule, the wiring, and the API over real HTTP |
| frontend unit | 26 | The client's own logic, in isolation |
| QA procedures | **40 / 40 passing** | The application as a person meets it |
| **Total automated** | **600 backend + 26 frontend + 40 QA** | |

The most recent recorded QA run is `docs/qa/runs/2026-08-22-stage-8-round-2.md`.

### Gates that fail the build

| Gate | Threshold | Enforced by |
|------|-----------|-------------|
| Line coverage (domain, application) | ≥ 95 % bundle, ≥ 90 % per class | JaCoCo `check` |
| Branch coverage (domain, application) | ≥ 95 % bundle, ≥ 85 % per class | JaCoCo `check` |
| Mutation score (domain, application) | ≥ 90 % | PIT |
| CRAP per method (domain, application) | ≤ 6 | `backend/tools/crap.py` |
| Cyclomatic complexity per method | ≤ 6 | Checkstyle |
| Method length / parameters / nesting | ≤ 20 lines / ≤ 4 / ≤ 2 | Checkstyle |
| Duplicated code | ≥ 30 tokens (inner), ≥ 100 (outer) | PMD CPD |
| Dependency direction, no package cycles | 0 violations | ArchUnit (13 rules) |
| Every output port wired, every domain failure translated | 0 gaps | `EveryPortIsWiredTest`, `EveryFailureIsTranslatedTest` |

None of them can be waived without an amendment recorded in `docs/reviews/`.

## Status

Complete. Stages 0–8, each gated by an adversarial review recorded in `docs/reviews/`.

**Sixty-eight review findings** were raised and resolved across those gates — 11, 9, 8, 7, 7, 6,
5, 6 and 9 at stages 0 through 8. Every one is written down with the fix that answered it,
including the embarrassing ones: a QA suite whose setup was undocumented, two Gherkin scenarios
that could not fail, a timing side channel that leaked which email addresses exist, and a
catch-all error handler that turned every client mistake into a 500.
