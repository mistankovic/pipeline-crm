# Stage 3 — Adversarial Re-Review

STAGE 3 REJECTED

Reviewed on branch `local-grok-pipeline-crm` at `4f450fd` (`fix: make closed-deal updates atomic and drop cucumber.features workaround`; working tree clean aside from this report). Scope: git range `a78a8a9`..`4f450fd` plus remaining Stage 3 code at HEAD (use cases, Gherkin, tests). Prior REJECTED review: `docs/reviews/stage-03-review.md`. Builder claims: `docs/reviews/stage-03-fixes.md`. Did not inspect other git branches.

Commands run (JDK 21 via `/Users/mislav/Library/Java/JavaVirtualMachines/jdk-21.0.11+10/Contents/Home`):

- `./mvnw -B verify` — BUILD SUCCESS (8 reactor modules)
- `./mvnw -B -Pmutation -pl domain,application -am verify` — BUILD SUCCESS
- `jdeps --multi-release 21 -s application/target/application-0.1.0-SNAPSHOT.jar` — `java.base` + domain (`not found` without `-cp`)
- `./mvnw -B -pl application help:effective-pom` — `failIfNoTests` absent; no JUnit 5 tree reporter

Pretty plugin printed **48** Gherkin scenarios against use-case implementations (in-memory ports, not UI). Application JaCoCo 125/125 lines, 27/28 branches (96%); CRAP 51 methods / 0 violations. Domain CRAP 137 / 0. PIT this run: domain 140/140 killed, application 53/53 killed, `failWhenNoMutations=true`. Enforcer/ArchUnit inner-layer bans still green. **Important 2 is fixed.** **Important 1 is not.** Surefire still records **0** Cucumber tests; discovering no scenarios is still BUILD SUCCESS. `docs/reviews/stage-03-fixes.md` documented only the `cucumber.features` deletion and claimed Surefire counting as a side-effect. That side-effect did not happen.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| All input ports implemented | **MET** | 14 `port.in` types have interactors in `com.pipelinecrm.application.usecase`. No `@Service` / Spring in application. |
| Use cases depend on ports, not adapters | **MET** | Constructors take `*Repository` / `PasswordHasher` / `TokenIssuer` / `Clock`. Test doubles live under `src/test`. |
| Gherkin against use-case layer, not UI | **RAN, still not a gate** | Glue calls `ChangeDealStageService`, `RecordActivityService`, `ForecastService`, `UpdateDealService`. Pretty output has no failed steps. **Not a gate** — see Important 1. |
| Unit tests for use cases | **ADEQUATE for Important 2** | `UseCaseTest.updateOpenDeal` now asserts a rejected closed-deal update leaves the title unchanged on identity-map `findById`. |
| JaCoCo ≥ 0.95 line/branch | **MET** | Application 100% line / 96% branch. Domain 99% line / 98% branch. |
| CRAP ≤ 6 | **MET** | Domain 137 methods 0 violations; application 51 methods 0 violations. |
| PIT 100%, `failWhenNoMutations=true` | **MET** | `inner-parent/pom.xml:201`. Domain 140/140, application 53/53. No `SURVIVED`. |
| Inner layers: zero Spring / JPA / Jackson / Lombok | **MET** | See leakage attack below. |
| Surefire counts nested Gherkin / fail-on-zero-scenarios | **UNMET** | Application `Tests run: 12`. `CucumberTest` `Tests run: 0`. `failIfNoTests` not configured. |

---

## What changed since `a78a8a9` (and what did not)

| Prior required fix | Claimed | Actual |
| --- | --- | --- |
| 1a. Remove `cucumber.features` / `FEATURES_PROPERTY_NAME` | Yes | **Done.** `CucumberTest` keeps `@SelectClasspathResource("features")` + glue. `junit-platform.properties` no longer sets `cucumber.glue`. Verify log no longer prints the Cucumber 7 “Discovering tests using the cucumber.features property” warning. |
| 1b. Make Surefire count nested scenarios (~48, not 12) | “Discovery is only `@SelectClasspathResource`” (implied) | **Not done.** See Important 1. |
| 1c. Fail the module when 0 cucumber tests are discovered | Not mentioned in `stage-03-fixes.md` | **Not done.** See Important 1. |
| 2. Atomic closed-deal update + unit test that title is unchanged | Yes | **Done.** See Important 2 close-out. |
| 2 (extra). Use or drop unused `actorId` | Required in first review, not claimed | **Not done.** Residual, not re-raised as a Stage 3 blocker. |

---

## Leakage attack

- Application production POM: `domain` only. Tests: JUnit, AssertJ, ArchUnit, Cucumber, `crap-check`.
- Grep of `domain/src/main/java` and `application/src/main/java`: no `org.springframework`, `jakarta.*`, `org.hibernate`, `com.fasterxml`, `org.projectlombok`, `javax.persistence`.
- `jdeps` application jar → `java.base` + domain.
- `ApplicationIndependenceTest` imports `target/classes` and allowlists domain + JDK `javax.*` namespaces. Green.
- Bootstrap still does not `@Bean`-wire interactors (Stage 2 residual). No `@Service` on application types. Not a Stage 3 reject.

---

## Critical

None.

---

## Important

### 1. Surefire still counts 0 Gherkin tests; discovering no scenarios is still BUILD SUCCESS

**Severity:** Important (carried from `docs/reviews/stage-03-review.md`; **not waived**)
**Confidence:** 95
**Status:** **OPEN** — sub-item (a) only.

**CONSTITUTION.md §5.4:** Gherkin acceptance lives in `application/src/test/resources/features` and must exercise the use-case layer. A green `./mvnw verify` is the gate. The first review’s required fix was explicit: Surefire must own the 48 examples, and a vanished `features/` directory must be a red build. Pretty-print this run is not a substitute.

**Evidence from this `./mvnw -B verify`:**

| Report | Tests run |
| --- | --- |
| `application/target/surefire-reports/com.pipelinecrm.application.acceptance.CucumberTest.txt` | **0** (`failures=0`, `errors=0`, time 0.159 s) |
| `TEST-com.pipelinecrm.application.acceptance.CucumberTest.xml` | `tests="0"` — **zero `<testcase>` elements** |
| Application module Surefire summary | **Tests run: 12** (5 ArchUnit + 1 `PortTypesTest` + 6 `UseCaseTest`) |
| Pretty plugin during the same Surefire fork | **48** scenarios, 0 failed / 0 undefined steps |

Maven log is the contradiction in two consecutive lines:

```
[INFO] Running com.pipelinecrm.application.acceptance.CucumberTest
… 48 pretty scenarios …
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.159 s -- in com.pipelinecrm.application.acceptance.CucumberTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

JUnit Platform **does** discover the nested tree. PIT’s slowest test this run was `[engine:junit-platform-suite]/[suite:com.pipelinecrm.application.acceptance.CucumberTest]/[engine:cucumber]/[feature:classpath%3Afeatures%2Fdeal_closed_won_guards.feature]/[scenario:9]`. Surefire 3.5.3’s default reporter does not flatten that tree into `testcase` rows. Nothing in `pom.xml`, `inner-parent/pom.xml`, or `application/pom.xml` enables `JUnit5StatelessTestsetInfoTreeReporter` (or `cucumber.plugin=junit:…` consumed by verify). `cucumber.junit-platform.naming-strategy=long` only names tests that Surefire already counts.

**Mental check: if `features/` vanished, would verify still succeed?** Yes.

1. `CucumberTest` (`application/src/test/java/com/pipelinecrm/application/acceptance/CucumberTest.java:11-16`) is an empty `@Suite` with `@SelectClasspathResource("features")`. An empty resource selection is a suite with no children, not an error.
2. That is already the Surefire result **with features present**: `tests="0" failures="0"`.
3. Effective POM: `maven-surefire-plugin` 3.5.3 with **no** `failIfNoTests`, no per-suite execution, no cucumber XML consumer. Default `failIfNoTests` is `false`.
4. Even `failIfNoTests=true` on the **module** would not help: 12 Jupiter tests would still satisfy it. The first review asked for a cucumber-specific fail-on-zero (`failIfNoTests` **on that suite**, or an assertion on scenario count). Neither exists.

Stage 3’s acceptance suite remains stdout, not a failing gate. Removing `cucumber.features` was necessary and not sufficient.

**Required fix (unchanged from review 1, items 2–3):**

1. Make Surefire count nested scenarios (JUnit 5 tree reporter and/or `cucumber.plugin=junit:target/cucumber-junit.xml` that verify consumes). Application `Tests run:` must include the 48 examples, not 12.
2. Fail the module when the cucumber engine discovers 0 tests (`failIfNoTests` on a dedicated cucumber Surefire execution, or a JUnit/ArchUnit assertion on scenario count). A vanished `features/` directory must be a red build. Module-level `failIfNoTests` alone is not enough while Jupiter tests exist.

Do not request re-review until a fresh `./mvnw -B verify` shows `CucumberTest` `Tests run:` ≥ 48 (or an equivalent consumed JUnit XML with that many cases) **and** a proof that empty discovery fails the build.

### 2. `UpdateDealService` closed-deal title leak — **CLOSED**

**Severity:** Important (prior)
**Confidence:** 88
**Status:** **FIXED** with a residual note.

`UpdateDealService.execute` now runs the guarded mutators first:

```17:23:application/src/main/java/com/pipelinecrm/application/usecase/UpdateDealService.java
    public void execute(Command command) {
        Deal deal = deals.findById(command.dealId()).orElseThrow(() -> new NotFoundException("deal"));
        deal.revalue(command.value());
        deal.changeProbability(command.probability());
        deal.rename(DealTitle.of(command.title()));
        deals.save(deal);
    }
```

`Deal.revalue` / `changeProbability` call `assertNotTerminal` **before** assigning (`Deal.java:101-110`, `145-148`). `rename` is still unguarded (`Deal.java:97-99`), but it is no longer reached on a closed deal.

The unit test is on the **identity-map** adapter, which is the leak model the first review described (and the model JPA will use inside a persistence context):

```17:20:application/src/test/java/com/pipelinecrm/application/support/InMemoryDealRepository.java
    public Optional<Deal> findById(DealId id) {
        return Optional.ofNullable(store.get(id));
    }
```

```111:117:application/src/test/java/com/pipelinecrm/application/usecase/UseCaseTest.java
        recordActivity.execute(new RecordActivityUseCase.Command(ownerId, ActivityType.MEETING, "hi", dealId, null));
        changeStage.execute(new ChangeDealStageUseCase.Command(ownerId, dealId, DealStage.CLOSED_WON));
        String closedTitle = viewDeal.execute(dealId).deal().title().value();
        assertThatThrownBy(() -> updateDeal.execute(new UpdateDealUseCase.Command(
                        ownerId, dealId, "Hacked", Money.of("1.00", "USD"), Probability.of(1))))
                .isInstanceOf(com.pipelinecrm.domain.deal.IllegalDealStageException.class);
        assertThat(viewDeal.execute(dealId).deal().title().value()).isEqualTo(closedTitle);
```

If `rename("Hacked")` still ran before the throw, `viewDeal` would see `"Hacked"` without `save`. A copy-on-find adapter would hide that bug; this one does not. The test therefore proves the original leak is gone on the Stage 3 persistence double.

Residual (do not reopen as Important 2): `rename` is still unlocked, so a later reorder reintroduces the leak; `DealTitle.of` now runs last, so an illegal title on an **open** deal can mutate value/probability on the identity map before throwing; `Command.actorId` is still unused.

---

## Residual minors (do not by themselves block)

Unchanged from review 1 except as noted. Not promoted to blockers.

1. **`a manager user {string} exists` still keys off `"boss"`.** `AcceptanceWorld.createUser` (`AcceptanceWorld.java:121-122`). Authorization Gherkin uses `"boss"`, so it stays green.
2. **Contact-meeting Given is a no-op for `ChangeDealStageService`.** `DealSteps.meetingOnContact` writes a contact-targeted `Activity` straight to `ActivityRepository`; `extraContact()` does not persist the contact. The When still correctly fails closed-won.
3. **`RecordActivityService` does not require the contact (or actor) to exist.** Phantom `contactId` is stored.
4. **`ContactServices` update never takes a non-null email.** JaCoCo still `1 of 2 branches missed` at `ContactServices.java:37`. Bundle 96%.
5. **`CompanyServices` / `ContactServices` each implement three input ports.**
6. **§2.3 port-package ArchUnit still missing.**
7. **`ViewDealUseCase.Result` still duplicates `deal.activities()`.**
8. **Incomplete rejection `Then`s** remain (`no activities is not enough`, contact/other-deal meetings, same-stage open outline).
9. **Bootstrap does not wire use cases.** Do not add `@Service` on interactors.
10. **README** still describes Stage 0 coverage/PIT rows.
11. **Unused `actorId` on `UpdateDealUseCase` / `CreateDealUseCase`.** First-review required-fix item 3; still a bag field the web adapter could pretend is enforced.
12. **Closed-deal atomicity is order-dependent**, not a use-case-level “reject terminal before any mutation” / locked `rename`.

---

## Attack-question notes (not extra findings)

| Attack | Result |
| --- | --- |
| Fat use cases? | **Mostly thin.** Same as review 1. |
| Missing edge cases? | Closed-deal title leak **closed** on the identity map. Unused `actorId`, contact existence, manager glue, contact-email branch remain residual. |
| Gherkin executing? | **Yes, this run** (48 pretty scenarios; PIT nested cucumber IDs). **Surefire 0 / fail-open discovery still Important 1.** Glue is use-case layer, not UI. |
| Framework leakage? | **No.** |
| Coverage / CRAP / PIT? | **Green this run.** Application 100% line / 96% branch, CRAP 0, PIT 53/53. Domain PIT 140/140. Missed application branch is still contact email update. |

---

## What is already in good shape (do not rip out)

- All 14 input ports have interactors; closed-won evidence lives on `Deal` and `changeStage` does not take a fabricated activity list.
- `cucumber.features` workaround is gone; Suite discovery is the documented `@SelectClasspathResource("features")` path.
- Closed-deal update no longer mutates title before the lock throw; the identity-map unit test would fail if that order were restored.
- In-memory adapters are test-scoped. `saveCount` exists to kill PIT `VOID_METHOD_CALLS` on `save`.
- Gherkin When/Then for stage, auth, forecast currency, and close-probability go through use cases.
- Login maps unknown user and bad password to the same `NotFoundException`.
- Inner-layer purity and numeric gates are real, not skipped.

Fix remaining Important 1 (Surefire-visible Gherkin counts **and** fail-on-zero-scenarios). Then request re-review. Do not start Stage 4 until a review file contains the line `STAGE 3 APPROVED`.
