# Stage 3 — Adversarial Review

STAGE 3 REJECTED

Reviewed on branch `local-grok-pipeline-crm` at `a78a8a9` (`feat: implement use cases and Gherkin acceptance tests`; working tree clean aside from this report). Scope: `CONSTITUTION.md` §2.3, §4, §5, §7; `application/src/main/java` use cases and ports; Cucumber runner/glue/features; in-memory test adapters; unit tests; inner-layer JaCoCo/CRAP/PIT; `docs/reviews/stage-03-builder-handoff.md`. Did not inspect other git branches.

Commands run (JDK 21 via `/usr/libexec/java_home -v 21`):

- `./mvnw -B verify` — BUILD SUCCESS (8 reactor modules)
- `./mvnw -B -Pmutation -pl domain,application -am verify` — BUILD SUCCESS
- `jdeps --multi-release 21 -s application/target/application-0.1.0-SNAPSHOT.jar` — `java.base` + domain

Pretty plugin printed **48** Gherkin scenarios against use-case implementations (in-memory ports, not UI). Application JaCoCo 125/125 lines, 27/28 branches (96%); CRAP 51 methods / 0 violations. Domain CRAP 137 / 0. PIT: domain 140/140 killed, application 53/53 killed, `failWhenNoMutations=true`. Enforcer/ArchUnit inner-layer bans still green. That is necessary and not sufficient. Surefire still records **0** Cucumber tests, and `UpdateDealService` is not an atomic use case on the Stage 3 persistence model.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| All input ports implemented | **MET** | 14 `port.in` types have interactors in `com.pipelinecrm.application.usecase`. No `@Service` / Spring in application. |
| Use cases depend on ports, not adapters | **MET** | Constructors take `*Repository` / `PasswordHasher` / `TokenIssuer` / `Clock`. Test doubles live under `src/test`. |
| Gherkin against use-case layer, not UI | **RAN** | Glue calls `ChangeDealStageService`, `RecordActivityService`, `ForecastService`, `UpdateDealService`. Pretty output has no failed steps. **Not a gate** — see Important 1. |
| Unit tests for use cases | **THIN** | One `UseCaseTest` (6 methods) plus `PortTypesTest`. CRUD/not-found covered; closed-deal update atomicity is not. |
| JaCoCo ≥ 0.95 line/branch | **MET** | Application 100% line / 96% branch. Domain 99% line / 98% branch. |
| CRAP ≤ 6 | **MET** | Domain 137 methods 0 violations; application 51 methods 0 violations. |
| PIT 100%, `failWhenNoMutations=true` | **MET** | `inner-parent/pom.xml:201`. Domain 140/140, application 53/53. No `SURVIVED` in `mutations.xml`. |
| Inner layers: zero Spring / JPA / Jackson / Lombok | **MET** | See leakage attack below. |

---

## Leakage attack

- Application production POM: `domain` only. Tests: JUnit, AssertJ, ArchUnit, Cucumber, `crap-check`.
- Grep of `domain/src/main/java` and `application/src/main/java`: no `org.springframework`, `jakarta.*`, `org.hibernate`, `com.fasterxml`, `org.projectlombok`, `javax.persistence`.
- `jdeps` application jar → `java.base` + domain (`not found` without `-cp`).
- `ApplicationIndependenceTest` imports `target/classes` and allowlists domain + JDK `javax.*` namespaces. Green.
- Bootstrap still does not `@Bean`-wire interactors (Stage 2 residual). No `@Service` on application types, so component scan cannot pick them up. Not a Stage 3 reject; wire in the composition root when HTTP lands.

---

## Important

### 1. Surefire counts 0 Gherkin tests; discovering no scenarios is still BUILD SUCCESS

**Severity:** Important
**Confidence:** 91

**CONSTITUTION.md §5.4:** Gherkin acceptance lives in `application/src/test/resources/features` and must exercise the use-case layer. A green `./mvnw verify` is the gate.

**Evidence:**

- `application/target/surefire-reports/com.pipelinecrm.application.acceptance.CucumberTest.txt`: **Tests run: 0**.
- `io.cucumber.junit.platform.engine.CucumberTestEngine.txt`: **Tests run: 0**.
- Application module summary: **Tests run: 12** (5 ArchUnit + 1 `PortTypesTest` + 6 `UseCaseTest`). Zero of those are scenarios.
- `CucumberTest` sets `cucumber.features=classpath:features`. Cucumber 7 prints:

  > Discovering tests using the cucumber.features property. Other discovery selectors are ignored!
  > If you are using the JUnit 5 Suite Engine … you should not use this property.

- Pretty plugin (junit-platform.properties + suite `PLUGIN_PROPERTY_NAME`) did print all 48 scenarios during verify, and PIT listed nested IDs such as `[suite:CucumberTest]/[engine:cucumber]/[feature:classpath%3Afeatures%2Fdeal_closed_won_guards.feature]/[scenario:9]`. They **ran this time**. Surefire does not own them.
- Empty discovery is success: both cucumber containers already report `tests="0"` with `failures="0"`. Delete the feature files (or break classpath resource selection) and `CucumberTest` stays a green 0-test suite. `failIfNoTests` is not set.

Stage 3’s acceptance suite is stdout, not a failing gate. That is the “Gherkin not actually executing (Surefire 0 tests)” hole, even though pretty output currently proves they execute.

**Required fix:**

1. Remove `FEATURES_PROPERTY_NAME` / `cucumber.features` from the `@Suite` class. Keep `@SelectClasspathResource("features")` + glue only (Cucumber’s documented Suite setup).
2. Make Surefire count nested scenarios (JUnit 5 tree reporter and/or `cucumber.plugin=junit:target/cucumber-junit.xml` that verify consumes). Application `Tests run:` must include the 48 examples, not 12.
3. Fail the module when the cucumber engine discovers 0 tests (`failIfNoTests` on that suite, or an ArchUnit/JUnit assertion on scenario count). A vanished `features/` directory must be a red build.

---

### 2. `UpdateDealService` is not atomic; a rejected closed-deal update still changes the title

**Severity:** Important
**Confidence:** 90

**CONSTITUTION.md §4:** small, correct use cases. §7.4 / `deal_probability_on_close.feature`: value and probability are locked on a terminal deal. The only update API always sends title + value + probability together.

**Evidence:**

```17:23:application/src/main/java/com/pipelinecrm/application/usecase/UpdateDealService.java
    public void execute(Command command) {
        Deal deal = deals.findById(command.dealId()).orElseThrow(() -> new NotFoundException("deal"));
        deal.rename(DealTitle.of(command.title()));
        deal.revalue(command.value());
        deal.changeProbability(command.probability());
        deals.save(deal);
    }
```

`Deal.rename` has no terminal guard (`Deal.java:97-99`). `revalue` / `changeProbability` throw `IllegalDealStageException` on a closed deal (`Deal.java:101-110`) **after** rename has already mutated the entity.

`InMemoryDealRepository.findById` returns the same instance it stored. Gherkin and `UseCaseTest` use that adapter. Sequence on a `CLOSED_WON` deal with a new title:

1. `rename("Hacked")` succeeds.
2. `revalue(...)` throws (locked).
3. `save` is not called.
4. `findById` still returns the mutated object. Title is `"Hacked"`.

Gherkin “probability and value are locked after closed-won” resubmits the **current** title, so the leak is invisible. `UseCaseTest.updateOpenDeal` never hits a terminal deal. PIT kills `save()` removal via `saveCount` on the happy path; it does not kill “rename applied, then throw.”

The same identity-map leak is what JPA will do inside a persistence context if the adapter does not roll back.

`Command.actorId` is also unused (no owner/manager check, no `UserRepository` lookup). §7.3 is stage-only, so that is not a constitution miss by itself; it is the same “command is a bag the interactor only partially honors” pattern.

**Required fix:**

1. Treat `execute` as one transaction at the domain boundary: reject a terminal deal **before** any mutation, or lock `rename` on closed deals and add Gherkin either way (“title locked” or “title may change; value/probability may not, and a rejected value change must not keep a new title”).
2. Unit test: closed deal, `UpdateDealUseCase` with a different title + illegal probability → exception **and** title unchanged on a subsequent `ViewDealUseCase` / `findById`.
3. Either use `actorId` (load user, authorize) or drop it from the command so the web adapter cannot pretend the field is enforced.

---

## Residual minors (do not by themselves block, but do not ignore on the fix pass)

1. **`a manager user {string} exists` does not create a manager.** `DealSteps.managerExists` calls `world.user(name)`; `AcceptanceWorld.createUser` assigns `MANAGER` only when the name is `"boss"`. Authorization scenarios pass because the feature uses `"boss"`. `Given a manager user "gina"` would register SALES.

2. **Contact-meeting Given is a no-op for `ChangeDealStageService`.** `meetingOnContact` saves a contact-targeted `Activity` straight to `ActivityRepository` and never calls `RecordActivityUseCase`. `extraContact()` does not persist the contact. `ChangeDealStageService` only reads `Deal.activities()`. The When still correctly fails closed-won (no qualifying activity on the deal); the Given does not lock “record-on-contact must not attach to the deal.”

3. **`RecordActivityService` does not require the contact (or actor) to exist.** Company is checked on create-contact; deal is checked on deal-targeted activity. Phantom `contactId` is stored. `targetOf` then `ActivityTarget.contact` is fine given the XOR on the command.

4. **`ContactServices` update never takes a non-null email.** JaCoCo: `1 of 2 branches missed` at `ContactServices.java:37`. Bundle branch coverage still 96%. Create-with-email and update-to-null are tested; update-to-an-email is not.

5. **`CompanyServices` / `ContactServices` each implement three input ports.** The ports stay small; the classes are not CRAP-fat. Prefer one interactor per use case when touching this.

6. **§2.3 port-package ArchUnit still missing** (Stage 0 item 6, Stage 1 residual). `ApplicationIndependenceTest` only requires `com.pipelinecrm.application..`.

7. **`ViewDealUseCase.Result` still duplicates `deal.activities()`** (Stage 1 residual).

8. **Incomplete rejection `Then`s** remain (`no activities is not enough`, contact/other-deal meetings, same-stage open outline). Glue can treat throw-and-advance as pass.

9. **Bootstrap does not wire use cases.** Correct for “no Spring in application”; required before HTTP. Do not add `@Service` on interactors.

10. **README** still describes Stage 0 coverage/PIT rows.

---

## Attack-question notes (not extra findings)

| Attack | Result |
| --- | --- |
| Fat use cases? | **Mostly thin.** `ChangeDealStageService` / `ForecastService` / `LoginService` are load-delegate-save. `CompanyServices`/`ContactServices` bundle CRUD (minor 5). `UpdateDealService` is wrong, not fat (Important 2). |
| Missing edge cases? | Closed-deal update atomicity (Important 2). Unused `actorId` on create/update deal. No contact existence on record-activity. Contact email update branch. Manager glue keyed off `"boss"`. |
| Gherkin executing? | **Yes, this run** (48 pretty scenarios, PIT nested cucumber tests). **Surefire 0 / fail-open discovery** (Important 1). Glue is use-case layer, not UI. |
| Framework leakage? | **No.** |
| Coverage / CRAP / PIT? | **Green.** Application 100% line / 96% branch, CRAP 0, PIT 53/53. Domain PIT 140/140, `failWhenNoMutations=true`. Missed application branch is contact email update. |

---

## What is already in good shape (do not rip out)

- All 14 input ports have interactors; closed-won evidence lives on `Deal` and `changeStage` does not take a fabricated activity list.
- In-memory adapters are test-scoped. `saveCount` exists to kill PIT `VOID_METHOD_CALLS` on `save`.
- Gherkin When/Then for stage, auth, forecast currency, and close-probability go through use cases.
- Login maps unknown user and bad password to the same `NotFoundException`.
- Inner-layer purity and numeric gates are real, not skipped.

Fix Important 1–2 (Surefire-visible Gherkin gate with fail-on-zero-scenarios, atomic `UpdateDealService` with a test that a rejected closed-deal update does not keep a new title). Then request re-review. Do not start Stage 4 until this file contains the line `STAGE 3 APPROVED`.
