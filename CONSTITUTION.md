# PipelineCRM Constitution

This document is the project's law. Build plugins, ArchUnit tests, and the
CRAP gate enforce it. A rule without an automated check is not a rule; it is
a wish. If a later stage cannot satisfy a clause, change the constitution
in the same commit that changes the gate, and record why in `docs/reviews/`.

## 1. Purpose

PipelineCRM is a **demo** sales-pipeline CRM. Scope stays deliberately small.
Craftsmanship is not optional: Clean Architecture, Clean Code, SOLID, and
measurable quality gates are the product as much as the features.

## 2. Clean Architecture — Dependency Rule

Source-code dependencies point **strictly inward**.

```
Frameworks & Drivers          bootstrap, Spring, JPA, PostgreSQL, Svelte, JWT
        ↓
Interface Adapters            adapter-web, adapter-persistence
        ↓
Application (use cases)       application (ports + interactors)
        ↓
Domain (entities)             domain
```

### 2.1 Layers and Maven modules

| Layer | Module | Java package root | Allowed compile-time dependencies |
| --- | --- | --- | --- |
| Domain | `domain` | `com.pipelinecrm.domain` | JDK only (production). Tests may use JUnit, AssertJ, ArchUnit, and `crap-check`. |
| Application | `application` | `com.pipelinecrm.application` | `domain` + JDK (production). Tests may use JUnit, AssertJ, ArchUnit, Mockito, Cucumber, and `crap-check`. |
| Persistence adapter | `adapter-persistence` | `com.pipelinecrm.adapter.persistence` | `application` (and thus `domain`), JPA/Spring Data/Postgres drivers |
| Web adapter | `adapter-web` | `com.pipelinecrm.adapter.web` | `application` (and thus `domain`), Spring Web/Security/Validation |
| Composition root | `bootstrap` | `com.pipelinecrm.bootstrap` | both adapters + Spring Boot |
| Frontend | `frontend/` (npm) | n/a | public HTTP API only |
| Inner-layer parent | `inner-parent` | n/a (POM only) | Shares banned-dependency, JaCoCo check, CRAP, and PIT configuration for `domain` and `application` |
| Quality tool | `tools/crap-check` | `com.pipelinecrm.tools.crap` | JDK only |

`adapter-web` **must not** depend on `adapter-persistence`.
`adapter-persistence` **must not** depend on `adapter-web`.
Neither adapter may depend on `bootstrap`.
`domain` and `application` **must not** depend on any adapter, Spring,
Jakarta Persistence, Jakarta Servlet, Hibernate, Jackson, or the frontend.

The composition root (`bootstrap`) is the only place that may wire adapters
to use-case implementations.

### 2.2 Forbidden in `domain` and `application` (compile + bytecode)

These package prefixes are illegal in inner-layer production code:

- `org.springframework..`
- `jakarta.persistence..`
- `jakarta.servlet..`
- `jakarta.ws.rs..`
- `org.hibernate..`
- `com.fasterxml.jackson..`
- `org.springframework.data..`
- `org.springframework.boot..`
- `org.mockito..` (production code; tests may mock ports)
- `org.projectlombok..`

Lombok is banned in **every** module. Generate nothing that hides structure.

### 2.3 Ports

- **Input ports** live in `com.pipelinecrm.application.port.in`.
  Controllers call these. They accept/return application-layer types, never
  HTTP or JPA types.
- **Output ports** live in `com.pipelinecrm.application.port.out`.
  Use cases call these. Persistence adapters implement them.
- Use-case implementations live in `com.pipelinecrm.application.usecase`
  and depend on ports, not on adapters.

### 2.4 Enforcement

| Check | Where |
| --- | --- |
| Maven module graph | each module `pom.xml` — no illegal `<dependency>` |
| Banned artifacts | `maven-enforcer-plugin` `bannedDependencies` on `domain` and `application` |
| Bytecode dependencies | ArchUnit tests in each module + layered architecture test in `bootstrap` |
| No cycles | ArchUnit slice rules in `bootstrap` |
| Java 21 only | `maven-enforcer-plugin` `requireJavaVersion` `[21,22)` |

A green `./mvnw verify` **is** the architecture review for dependency direction.

## 3. Domain richness (not anemic)

Inner-layer types protect invariants. Constructors and mutating methods
reject illegal states by throwing domain exceptions.

Required properties (Stage 1+):

- Entities are not JavaBean bags. No public setters for invariant fields.
- Value objects are immutable (Java `record` is preferred).
- Deal stage changes go through a domain state machine with guards.
- Business rules listed in §7 live in domain or use-case code, **never** in
  controllers, JPA entities, or the Svelte UI.

The Adversarial Reviewer rejects anemic models.

## 4. Clean Code and SOLID

- Small functions. One level of abstraction per function.
- Meaningful names. No `data`, `info`, `manager`, `util` dumping grounds.
- No duplication. Three copies is a missed type or policy.
- Prefer polymorphism over switch-on-type when adding a new kind would
  otherwise touch every case.
- Dependency inversion: use cases depend on output ports, not Spring Data.
- Interface segregation: ports are small and use-case specific.

## 5. Quality gates (numeric, failing the build)

These apply to **production** code in `domain` and `application` once those
modules contain executable methods. `package-info.java` is not executable.
`tools/crap-check` follows the same CRAP cap and is covered by its own unit tests.

### 5.1 Line and branch coverage — JaCoCo

- **Minimum LINE covered ratio: 0.95**
- **Minimum BRANCH covered ratio: 0.95**
- Counter scope: each inner module (`BUNDLE` of that module).
- Plugin: `jacoco-maven-plugin` `check` bound to `verify`.

### 5.2 CRAP — custom gate

For every executable method in packages
`com.pipelinecrm.domain..` and `com.pipelinecrm.application..`:

```
CRAP(m) = comp(m)^2 * (1 - cov(m))^3 + comp(m)
```

- `comp(m)` = JaCoCo `COMPLEXITY` counter (`missed + covered`)
- `cov(m)` = JaCoCo `LINE` covered ratio for that method
- **Maximum CRAP: 6.0** (prefer ≤ 4.0 when refactoring)
- At 100% line coverage, CRAP equals cyclomatic complexity, so complexity
  itself may not exceed 6 on a fully covered method.
- Methods with no executable lines (e.g. empty markers) are skipped.
- If `jacoco.xml` is missing **and** the module still has production
  `.class` files other than `package-info` / `module-info`, the gate **fails**.
- If the module has no production classes, the gate **passes**.

Tool: `tools/crap-check`, bound to `verify` on `domain` and `application`.

### 5.3 Mutation testing — PIT

- Target classes: `com.pipelinecrm.domain.*` and `com.pipelinecrm.application.*`
- **Mutation score minimum: 100%** of mutants PIT considers relevant
- Command: `./mvnw -Pmutation -pl domain,application -am verify`
- `failWhenNoMutations` is `false` only while an inner module has no
  mutable production code. Stage 3 **must** set it to `true` for any module
  that has executable production methods.
- Surviving mutants are either killed by a new test or recorded in
  `docs/reviews/` with a reason that the mutant is equivalent / invalid.
  Unexplained survivors are a failed stage.

### 5.4 Complementary tests (required by Stage 3+)

| Kind | Location | Exercises |
| --- | --- | --- |
| Unit | `domain` and `application` `src/test/java` | entities, VOs, use cases |
| Gherkin acceptance | `application/src/test/resources/features` | use-case layer, **not** UI |
| Architecture | each module + `bootstrap` | Dependency Rule |
| Persistence IT | `adapter-persistence` + Testcontainers Postgres | mapping, adapters |
| API tests | `adapter-web` / `bootstrap` | HTTP adapters stay thin |
| QA procedures | `docs/qa/` | real Svelte UI (Stage 7) |

Stack versions are locked in the parent POM: JUnit 5, AssertJ, Mockito,
Cucumber (JUnit Platform engine), Testcontainers, PIT, JaCoCo, ArchUnit.

Frontend never contains the business rules in §7. It may disable a button
for UX, but the server remains the authority.

## 6. Code style

- Java 21. `maven.compiler.release=21`. No preview features.
- Indentation and charset: `.editorconfig`.
- No wildcard imports.
- Tests: Arrange-Act-Assert. AssertJ fluent assertions.
- Gherkin: business language, not UI clicks, not Java method names.

## 7. Domain rules (implemented from Stage 1; enforced by tests from Stage 3)

These rules belong in domain / use-case code.

1. Deal stages: `LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON | CLOSED_LOST`.
   Forward along the happy path, or to a terminal closed state from a
   non-terminal stage, as specified by Gherkin. No skipping happy-path
   stages. No leaving a terminal stage.
2. A deal **cannot** move to `CLOSED_WON` unless `value > 0` **and** it has
   at least one activity of type Meeting or Call.
3. Only the deal owner or a user with role `MANAGER` may change stage.
4. Moving to `CLOSED_WON` forces probability to 100. Moving to `CLOSED_LOST`
   forces probability to 0.
5. Forecast for open deals = Σ (`value × probability / 100`), grouped by
   owner or by stage. Open = every non-terminal stage.

Exact transition tables and edge cases are Gherkin in Stage 1. Gherkin wins
over this summary if they ever diverge; update this section in the same
change.

## 8. Process

Work proceeds Stage 0 … Stage 8 as specified by the project supervisor.
The Builder does not start Stage N+1 until the Adversarial Reviewer has
written `STAGE N APPROVED` in `docs/reviews/`. Findings and fixes are
recorded in that directory.

## 9. What Stage 0 guarantees

Stage 0 does **not** implement the domain. It guarantees that:

1. The module graph exists and compiles.
2. Inner modules have zero framework dependencies.
3. Architecture tests fail if the Dependency Rule is broken.
4. Coverage, CRAP, and mutation tools are wired and documented.
5. Java 21 is required.
6. The frontend toolchain (Svelte 5 + TypeScript + Vite) builds a scaffold.

## 10. Changing this constitution

Amendments require:

1. An updated clause in this file.
2. An updated automated gate (or an explicit, reviewed exception).
3. A note in `docs/reviews/`.
