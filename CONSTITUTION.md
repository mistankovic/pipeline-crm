# PipelineCRM — Project Constitution

This document is **binding**. Every rule below is stated so that it can be checked
mechanically. A rule that cannot be enforced by a tool is a wish, not a rule; where a
rule is currently enforced by review only, it says so explicitly and names the review
gate that checks it.

Version: 1.0 · Status: ratified at Stage 0 · Amendment: only via a numbered entry in
`docs/reviews/` recording who demanded the change and why.

---

## 1. Layers and the Dependency Rule

Source code dependencies point **strictly inward**. Outer layers know inner layers;
inner layers never know outer layers — not by import, not by annotation, not by string
name, not by reflection.

| # | Layer | Maven module | May depend on | Contains |
|---|-------|--------------|---------------|----------|
| 1 | Enterprise Business Rules | `domain` | *(nothing but the JDK)* | Entities, value objects, domain services, domain exceptions |
| 2 | Application Business Rules | `application` | `domain` | Use-case interactors, input ports, output ports, repository ports |
| 3 | Interface Adapters (web) | `adapter-web` | `application`, `domain` | REST controllers, DTOs, mappers, HTTP error translation |
| 3 | Interface Adapters (persistence) | `adapter-persistence` | `application`, `domain` | JPA entities, Spring Data repositories, port implementations, mappers |
| 4 | Frameworks & Drivers | `bootstrap` | all of the above | Spring Boot application, bean wiring, security config, Flyway, properties |
| 4 | Frameworks & Drivers | `frontend` | the public HTTP API only | Svelte 5 + TypeScript UI |

### 1.1 Enforced by the build (hard failure)

* `domain/pom.xml` declares **zero** compile-scope dependencies. Verified by
  `maven-enforcer-plugin` (`banTransitiveDependencies` + explicit `bannedDependencies`)
  and by the fact that Maven cannot compile against a module it does not depend on.
* `application/pom.xml` declares exactly one compile-scope dependency: `domain`.
* `adapter-web` and `adapter-persistence` do **not** depend on each other. An adapter
  that needs another adapter is a design error; it goes through a port.
* No module may declare a compile-scope dependency on `bootstrap`.

### 1.2 Enforced by architecture fitness functions (ArchUnit, `bootstrap` test scope)

* `LayeredArchitecture` check over `com.pipelinecrm..` packages.
* `domain` classes carry **no** annotation from `org.springframework..`,
  `jakarta.persistence..`, `jakarta.validation..`, `com.fasterxml.jackson..`.
* `domain` and `application` classes import nothing from `org.springframework..`,
  `jakarta..`, `com.fasterxml..`, `java.sql..`, `javax.sql..`.
* `SlicesRuleDefinition.slices().matching("com.pipelinecrm.(*)..").should().beFreeOfCycles()`
  — no package cycles anywhere.
* Controllers may not reference persistence types; persistence may not reference web types.
* Use-case interactors are reachable from adapters **only** through an input port interface.

### 1.3 Enforced by review (gate: Stage 2 and Stage 5 reviews)

* Ports are named from the perspective of the **inner** layer, not the technology
  (`DealRepository`, not `DealJpaGateway`).

---

## 2. Where business rules live

Every rule in §2.1 is implemented in `domain` or `application` and is covered by at
least one unit test **and** at least one Gherkin scenario that runs against the
use-case layer.

### 2.1 The rules

1. A `Deal` may only follow the stage machine
   `LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON | CLOSED_LOST`.
   Backward and skipping transitions are rejected. Closed deals are terminal.
2. A `Deal` cannot enter `CLOSED_WON` without a **positive value** and at least one
   linked `Activity` of type `MEETING` or `CALL`.
3. Only the deal's **owner** or a user with role `MANAGER` may change its stage.
4. Entering `CLOSED_WON` forces probability to 100; entering `CLOSED_LOST` forces it to 0.
5. Forecast = `Σ (value × probability)` over **open** deals only (neither closed state),
   grouped by owner or by stage.

### 2.2 Prohibited locations

* No business rule in a controller, DTO, mapper, JPA entity, SQL, or in the frontend.
  The frontend may **mirror** a rule for affordance (e.g. graying out an illegal drop
  target) but the server is the only authority; every mirrored rule must have a
  server-side test proving the server rejects the illegal case.
* Checked at the Stage 5 and Stage 6 review gates by reading every controller and by
  the ArchUnit rule that controllers contain no conditional business vocabulary beyond
  translating a port result.

---

## 3. Test obligations

A stage is not done until **all** of the following are green.

| Kind | Tool | Scope | Gate |
|------|------|-------|------|
| Unit | JUnit 5 + AssertJ + Mockito | `domain`, `application` | ≥ 95 % line **and** ≥ 95 % branch, enforced by JaCoCo `check` — build fails below |
| Acceptance (Gherkin) | Cucumber JVM | use-case layer, no HTTP, no DB | every rule in §2.1 has ≥ 1 scenario; runner is part of `mvn verify` |
| Architecture | ArchUnit | all modules | build fails on any violation |
| Integration | Testcontainers (PostgreSQL) | `adapter-persistence`, `bootstrap` | real Postgres, no H2 substitution |
| API | Spring `MockMvc` / `@SpringBootTest` | `adapter-web`, `bootstrap` | every endpoint, incl. 401/403/404/409/422 paths |
| Mutation | PIT | `domain`, `application` | mutation score ≥ 90 %, enforced by the build. Every surviving mutant is additionally justified in `docs/mutation-survivors.md` — **review gate**, no tool reads that file |
| CRAP | `tools/crap.py` over JaCoCo XML | `domain`, `application` | **every** method CRAP ≤ 6; build fails otherwise |
| UI / QA | Playwright + written manual procedure | full stack | every procedure in `docs/qa/` executed and recorded |

### 3.1 CRAP

`CRAP(m) = comp(m)² × (1 − cov(m))³ + comp(m)`, where `comp` is cyclomatic complexity
and `cov` is the fraction of the method's lines covered by tests.

* Hard limit: **6**. Target: **4**.
* Consequence: a method with complexity 6 must be **100 %** covered (CRAP = 6); a method
  with complexity 3 may be at worst ~78 % covered. Complexity above 6 is impossible to
  satisfy at any coverage, so it must be refactored. This is intentional.

### 3.2 Test style

Structural limits (complexity, nesting, method length, star imports) are enforced on test
sources by Checkstyle exactly as on production code. The judgement-based rules below are
**review gate** obligations:

* One reason to fail per test. Test names read as sentences describing behaviour.
* Arrange–Act–Assert, visually separated.
* No test asserts on a mock that was not part of the behaviour under test.
* Test data is built through named builders / object mothers, never by copy-paste.

---

## 4. Clean Code limits

| Property | Limit | Enforcement |
|----------|-------|-------------|
| Cyclomatic complexity per method | ≤ 6 | implied by the CRAP gate; checked directly by the CRAP report |
| Method length | ≤ 20 lines (target ≤ 8) | Checkstyle |
| Parameters per method | ≤ 4 (prefer a parameter object) | Checkstyle |
| Class length | ≤ 200 lines | Checkstyle |
| Nesting depth | ≤ 2 | Checkstyle |
| Public class per file | 1 | Checkstyle |
| `import *` | forbidden | Checkstyle |
| Duplicated blocks | 0 above 30 tokens in `domain`/`application`; 0 above 100 tokens elsewhere | PMD CPD (`cpd-check`), build fails |
| Mutable public state | forbidden — domain fields are `private final`; value objects are `record`s | review gate + ArchUnit `GeneralCodingRules` |
| `null` returned from a domain/application method | forbidden — use `Optional` or throw | review gate |
| Comments | a comment explaining *what* the code does is a defect; extract a named method instead | review gate |

---

## 5. Definition of Done for a stage

1. Code compiles; `mvn -f backend/pom.xml verify` is green from a clean state.
2. All gates in §3 that apply to the stage pass. Generated reports are build output and
   are **not** committed; the resulting numbers are recorded in the stage hand-off.
3. The Builder writes a hand-off note in `docs/reviews/stage-N-handoff.md` listing what
   was built, what was deliberately deferred, and the metric numbers.
4. The Adversarial Reviewer writes `docs/reviews/stage-N-review.md` with numbered
   findings; each finding is either fixed (with the commit noted) or refused with a
   written justification.
5. The Reviewer writes the literal line `STAGE N APPROVED`. Nothing in stage N+1 starts
   before that line exists.

## 6. Non-goals

Explicitly out of scope, so nobody reviews us for missing them: **user provisioning**
(users are seeded by a database migration; there is no user-management use case, see
decision D-13), multi-tenancy, refresh
tokens/token revocation, password reset, audit trail, i18n, pagination beyond a simple
limit, real money arithmetic across currencies (a deal's value is stored with its
currency and forecasts are grouped per currency; no FX conversion), and horizontal
scaling concerns.

**JWT in this demo is deliberately simple** (HS256, single shared secret, 8 h expiry, no
refresh). This is documented as a demo limitation, not presented as production security.
