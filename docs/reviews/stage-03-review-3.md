# Stage 3 — Adversarial Re-Review (third pass)

STAGE 3 APPROVED

Reviewed on branch `local-grok-pipeline-crm` at `7f0e02a` (`docs: record Stage 3 re-review rejection`; parent `a70a87e` `fix: count Gherkin scenarios in Surefire and fail on empty discovery`; working tree clean aside from this report). Scope: git range `4f450fd`..HEAD plus remaining Stage 3 code at HEAD (use cases, Gherkin, tests, gates). Prior REJECTED reviews: `docs/reviews/stage-03-review.md` at `a78a8a9`, `docs/reviews/stage-03-review-2.md` at `4f450fd`. Builder claims: `docs/reviews/stage-03-fixes.md`. Did not inspect other git branches. Mutation profile was **not** re-run this pass (no production code in `4f450fd`..HEAD; prior pass domain 140/140, application 53/53).

Commands run (JDK 21 via `/Users/mislav/Library/Java/JavaVirtualMachines/jdk-21.0.11+10/Contents/Home`):

- `./mvnw -B verify` — BUILD SUCCESS (8 reactor modules)
- `jdeps --multi-release 21 -s application/target/application-0.1.0-SNAPSHOT.jar` — `java.base` + domain (`not found` without `-cp`)
- Inner-layer grep of `domain/src/main/java` and `application/src/main/java` — no Spring / Jakarta / Hibernate / Jackson / Lombok / `javax.persistence`

Pretty plugin printed **48** Gherkin scenarios against use-case implementations (in-memory ports, not UI). This time Surefire owns them: `CucumberTest` **Tests run: 48**, application module **Tests run: 66**. `application/target/cucumber-junit.xml` has **48** `<testcase>` elements; verify executed `cucumber-junit-xml-gate` with no error. Application JaCoCo 125/125 lines, 27/28 branches (96%); CRAP 51 methods / 0 violations. Domain JaCoCo 334/335 lines, 91/92 branches (99% / 98%); CRAP 137 / 0. Enforcer/ArchUnit inner-layer bans still green. **Important 1 is closed. Important 2 stays closed.**

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| All input ports implemented | **MET** | 14 `port.in` types have interactors in `com.pipelinecrm.application.usecase`. No `@Service` / Spring in application. |
| Use cases depend on ports, not adapters | **MET** | Constructors take `*Repository` / `PasswordHasher` / `TokenIssuer` / `Clock`. Test doubles live under `src/test`. |
| Gherkin against use-case layer, not UI | **MET (now a gate)** | Glue calls `ChangeDealStageService`, `RecordActivityService`, `ForecastService`, `UpdateDealService`. Pretty output has no failed steps. Surefire + XML + discovery gates own the 48 examples. |
| Unit tests for use cases | **ADEQUATE for Important 2** | `UseCaseTest.updateOpenDeal` still asserts a rejected closed-deal update leaves the title unchanged on identity-map `findById`. |
| JaCoCo ≥ 0.95 line/branch | **MET** | Application 100% line / 96% branch. Domain 99% line / 98% branch. `inner-parent` bundle minima 0.95 / 0.95. |
| CRAP ≤ 6 | **MET** | Domain 137 methods 0 violations; application 51 methods 0 violations. |
| PIT 100%, `failWhenNoMutations=true` | **MET on prior pass; not re-run** | `inner-parent/pom.xml:201`. Last mutation verify (review 2): domain 140/140, application 53/53. No production bytecode in this range. |
| Inner layers: zero Spring / JPA / Jackson / Lombok | **MET** | See leakage attack below. |
| Surefire counts nested Gherkin / fail-on-zero-scenarios | **MET** | `CucumberTest` Tests run **48**. Application Tests run **66**. Three independent empty-discovery fails (see Important 1 close-out). |

---

## What changed since `4f450fd`

| Prior required fix | Claimed in `stage-03-fixes.md` | Actual |
| --- | --- | --- |
| 1b. Make Surefire count nested scenarios (~48, not 12) | `CucumberTest` Surefire Tests run 48; application 66 | **Done.** Surefire 3.5.4 + `JUnit5Xml30StatelessReporter` + `cucumber.junit-platform.naming-strategy=long`. Fresh verify: `CucumberTest.txt` Tests run **48**; XML `tests="48"` with 48 `<testcase>` rows named after scenarios. Module total 66 = 48 Gherkin + 5 ArchUnit + 1 `PortTypesTest` + 6 `UseCaseTest` + 2 discovery-gate + 4 XML-gate. |
| 1c. Fail the module when 0 cucumber tests are discovered | `@Suite(failIfNoTests=true)`, `CucumberDiscoveryGateTest`, `CucumberJunitXmlGate` on verify | **Done.** See Important 1. Vanished `features/` is a red build. |
| 2. Atomic closed-deal update + unit test | Closed in re-review 2 | **Still fixed.** `UpdateDealService` and `UseCaseTest.updateOpenDeal` unchanged in `4f450fd`..HEAD. |
| Unused `actorId` | Not claimed | **Not done.** Residual, not a Stage 3 blocker. |

---

## Leakage attack

- Application production POM: `domain` only. Tests: JUnit, AssertJ, ArchUnit, Cucumber, `crap-check`, `junit-platform-suite` / `launcher`.
- Grep of `domain/src/main/java` and `application/src/main/java`: no `org.springframework`, `jakarta.*`, `org.hibernate`, `com.fasterxml`, `org.projectlombok`, `javax.persistence`.
- `jdeps` application jar → `java.base` + domain.
- `ApplicationIndependenceTest` imports `target/classes` and allowlists domain + JDK `javax.*` namespaces. Green this run (5 tests).
- Bootstrap still does not `@Bean`-wire interactors (Stage 2 residual). No `@Service` on application types. Not a Stage 3 reject.

---

## Critical

None.

---

## Important

### 1. Surefire counts 0 Gherkin tests; discovering no scenarios is still BUILD SUCCESS — **CLOSED**

**Severity:** Important (carried from reviews 1 and 2; **not waived**)
**Confidence:** 93
**Status:** **FIXED**

**CONSTITUTION.md §5.4:** Gherkin acceptance lives in `application/src/test/resources/features` and must exercise the use-case layer. A green `./mvnw verify` is the gate. Reviews 1–2 required Surefire to own the 48 examples **and** a vanished `features/` directory to be a red build. Pretty-print is not a substitute. Both bars are now met.

**Evidence from this `./mvnw -B verify`:**

| Report | Tests run |
| --- | --- |
| `application/target/surefire-reports/com.pipelinecrm.application.acceptance.CucumberTest.txt` | **48** (`failures=0`, `errors=0`, time 0.183 s) |
| `TEST-com.pipelinecrm.application.acceptance.CucumberTest.xml` | `tests="48"` — **48** `<testcase>` elements (first: `Closed-won guards - meeting on the deal allows closed-won`) |
| `application/target/cucumber-junit.xml` | **48** `<testcase>` elements, `failures="0"` `errors="0"` |
| Application module Surefire summary | **Tests run: 66** |

Maven log is no longer a contradiction:

```
[INFO] Running com.pipelinecrm.application.acceptance.CucumberTest
… 48 pretty scenarios …
[INFO] Tests run: 48, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.183 s -- in com.pipelinecrm.application.acceptance.CucumberTest
[INFO] Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- exec:3.5.0:java (cucumber-junit-xml-gate) @ application ---
```

Feature files expand to the same 48 (20 plain scenarios + 28 outline examples). `jst.version` is **3.5.4** (SUREFIRE-2298 nested Cucumber XML). Application Surefire uses `JUnit5Xml30StatelessReporter` with phrased names.

**Mental check: if `features/` vanished, would verify fail?** Yes. Three independent trips, not one:

1. **`CucumberDiscoveryGateTest`** (`application/src/test/java/.../CucumberDiscoveryGateTest.java`) launches the cucumber engine against `classpath:features` and asserts `countTestIdentifiers(isTest) >= 48`. A missing resource is independently shown to discover **0** (`missingFeaturesResourceDiscoversNothing`). Vanished features → 0 < 48 → Surefire failure in the **test** phase. This is not a comment; it ran this verify (Tests run: 2).
2. **`@Suite(failIfNoTests = true)`** on `CucumberTest`. JUnit Platform Suite 1.12.2 default is already `true`; the engine’s `SuiteTestDescriptor.execute` throws `NoTestsDiscoveredException` (`Suite [%s] did not discover any tests`) when `failIfNoTests && testsFoundCount == 0`. Empty cucumber selection is therefore a failed suite, not a green 0-test class. Review 2’s “empty suite is success” inference came from Surefire’s **reporter** counting 0 **while 48 tests actually ran**, which is not the vanished-features case.
3. **`CucumberJunitXmlGate`** bound to **verify** (`application/pom.xml` `cucumber-junit-xml-gate`). `check()` returns a failure string (process exit 1) for a missing file, `< 48` `<testcase>` nodes, or any `<failure>`/`<error>`. Unit tests cover those four outcomes (`CucumberJunitXmlGateTest`, 4 tests this run). This run wrote `application/target/cucumber-junit.xml` (not repo-root `target/`) and the exec goal succeeded.

Module-level `failIfNoTests` is still not required: Jupiter tests would satisfy it alone. The cucumber-specific gates above are what review 1 asked for.

Residual (do not reopen Important 1): `io.cucumber.junit.platform.engine.CucumberTestEngine.txt` is still Tests run **0** — the engine class is not the suite. Stale `target/test-classes/features` after a delete-without-`clean` is the usual Maven resources footgun; `clean verify` and the live discovery assertion cover the constitution gate. The XML gate does not fail on 48 skipped cases (current XML has 0 skipped). `MINIMUM_SCENARIOS = 48` is duplicated on the two gate classes.

### 2. `UpdateDealService` closed-deal title leak — **CLOSED** (unchanged)

**Severity:** Important (prior)
**Confidence:** 88
**Status:** **FIXED** with the same residual note as review 2.

`4f450fd`..HEAD does not touch production use cases. `UpdateDealService.execute` still runs the guarded mutators first:

```17:23:application/src/main/java/com/pipelinecrm/application/usecase/UpdateDealService.java
    public void execute(Command command) {
        Deal deal = deals.findById(command.dealId()).orElseThrow(() -> new NotFoundException("deal"));
        deal.revalue(command.value());
        deal.changeProbability(command.probability());
        deal.rename(DealTitle.of(command.title()));
        deals.save(deal);
    }
```

`Deal.revalue` / `changeProbability` still call `assertNotTerminal` before assigning. `rename` is still unguarded (`Deal.java:97-99`) and is still not reached on a closed deal. `InMemoryDealRepository.findById` is still an identity map. `UseCaseTest.updateOpenDeal` still asserts the closed title is unchanged after a rejected `"Hacked"` update.

Residual (do not reopen as Important 2): `rename` is still unlocked, so a later reorder reintroduces the leak; `DealTitle.of` now runs last, so an illegal title on an **open** deal can mutate value/probability on the identity map before throwing; `Command.actorId` is still unused.

---

## Residual minors (do not by themselves block)

Unchanged from review 2 except as noted. Not promoted to blockers.

1. **`a manager user {string} exists` still keys off `"boss"`.** `AcceptanceWorld.createUser` (`AcceptanceWorld.java:121-122`). Authorization Gherkin uses `"boss"`, so it stays green.
2. **Contact-meeting Given is a no-op for `ChangeDealStageService`.** `DealSteps.meetingOnContact` writes a contact-targeted `Activity` straight to `ActivityRepository`; `extraContact()` does not persist the contact. The When still correctly fails closed-won.
3. **`RecordActivityService` does not require the contact (or actor) to exist.** Phantom `contactId` is stored.
4. **`ContactServices` update never takes a non-null email.** JaCoCo still `1 of 2 branches missed` at `ContactServices` (bundle 96%). Domain still misses 1 line / 1 branch on `Deal`.
5. **`CompanyServices` / `ContactServices` each implement three input ports.**
6. **§2.3 port-package ArchUnit still missing.**
7. **`ViewDealUseCase.Result` still duplicates `deal.activities()`.**
8. **Incomplete rejection `Then`s** remain (`no activities is not enough`, contact/other-deal meetings, same-stage open outline).
9. **Bootstrap does not wire use cases.** Do not add `@Service` on interactors.
10. **README** still describes Stage 0 coverage/PIT rows.
11. **Unused `actorId` on `UpdateDealUseCase` / `CreateDealUseCase`.** First-review required-fix item 3; still a bag field the web adapter could pretend is enforced.
12. **Closed-deal atomicity is order-dependent**, not a use-case-level “reject terminal before any mutation” / locked `rename`.
13. **`CucumberTestEngine` Surefire file is still Tests run 0.** Harmless; the suite class is the gate.
14. **PIT not re-run this pass.** Prior pass was 100% on unchanged production bytecode.

---

## Attack-question notes (not extra findings)

| Attack | Result |
| --- | --- |
| Fat use cases? | **Mostly thin.** Same as review 2. |
| Missing edge cases? | Closed-deal title leak **still closed** on the identity map. Unused `actorId`, contact existence, manager glue, contact-email branch remain residual. |
| Gherkin executing? | **Yes, and now a gate.** 48 Surefire cases + 48 cucumber JUnit XML cases + discovery `>= 48`. Glue is use-case layer, not UI. |
| Framework leakage? | **No.** |
| Coverage / CRAP / PIT? | JaCoCo and CRAP **green this run**. PIT **green last pass**, skipped this pass. Missed application branch is still contact email update. |

---

## What is already in good shape (do not rip out)

- All 14 input ports have interactors; closed-won evidence lives on `Deal` and `changeStage` does not take a fabricated activity list.
- `cucumber.features` workaround is gone; Suite discovery is `@SelectClasspathResource("features")` + glue.
- Surefire 3.5.4 flattens nested Cucumber into 48 `testcase` rows; verify consumes `cucumber-junit.xml` and live discovery of `classpath:features`.
- Closed-deal update no longer mutates title before the lock throw; the identity-map unit test would fail if that order were restored.
- In-memory adapters are test-scoped. `saveCount` exists to kill PIT `VOID_METHOD_CALLS` on `save`.
- Gherkin When/Then for stage, auth, forecast currency, and close-probability go through use cases.
- Login maps unknown user and bad password to the same `NotFoundException`.
- Inner-layer purity and numeric coverage/CRAP gates are real, not skipped.

Stage 3 is approved. Residual minors above are not blockers for Stage 4; do not treat this file as a waiver of later-stage wiring (`@Bean` in bootstrap, not `@Service` on interactors).
