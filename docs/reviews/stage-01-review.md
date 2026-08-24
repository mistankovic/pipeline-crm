# Stage 1 — Adversarial Review

STAGE 1 REJECTED

Reviewed on branch `local-grok-pipeline-crm` at `0548e92` (`feat: add domain model, Gherkin specs, and use-case ports`; working tree clean aside from this report). Scope: `CONSTITUTION.md` §3 and §7, `domain/src/main/java`, `application/src/main/java` ports, `application/src/test/resources/features`, domain unit tests, `docs/reviews/stage-01-builder-handoff.md`. Did not inspect other git branches.

Commands run (JDK 21 via `/usr/libexec/java_home -v 21`):

- `./mvnw -B verify` — BUILD SUCCESS (domain JaCoCo 100% line / 100% branch, CRAP 129 methods 0 violations; application JaCoCo 100% line, CRAP 12 methods 0 violations)
- `./mvnw -B -Pmutation -pl domain,application -am verify` — BUILD SUCCESS (domain 125/125 mutants killed; application 0 mutants, 100% because ports are interfaces/records and `failWhenNoMutations=false`)

Deal is not a JavaBean bag. `changeStage` is a real state machine with authorization, happy-path guards, closed-won activity/value guards, and forced close probabilities. That is necessary and not sufficient. Stage 1 still ships a public reconstitution hole, a forecast API that does not mean what Gherkin says, and a Gherkin table that under-specifies terminal behavior while claiming to be the source of truth.

---

## Important

### 1. `Forecast.byOwner` ignores the requested currency and will mix currencies across owners

**Severity:** Important
**Confidence:** 92

**CONSTITUTION.md / Gherkin:** §7.5 (open-deal weighted sum). `forecast.feature` lines 4–5: “Closed deals are excluded. … Amounts in different currencies are not mixed.” Scenarios call “grouped by owner **in USD**”.

**Evidence:**

`Forecast.byOwner` null-checks `currency` and never uses it:

```18:22:domain/src/main/java/com/pipelinecrm/domain/forecast/Forecast.java
    public static List<ForecastBucket> byOwner(Collection<Deal> deals, Currency currency) {
        Guards.notNull(currency, "currency");
        Map<UserId, WeightedValue> totals = emptyOwnerTotals(openDeals(deals));
        return toOwnerBuckets(totals);
    }
```

`byStage` *does* seed zeros in the requested currency, then `WeightedValue.plus` rejects a mismatch (`WeightedValue.java:27-32`). Same pipeline, two policies:

| Call | Alice 1000 USD @ 50%, Bob 200 EUR @ 25% |
| --- | --- |
| `byOwner(..., USD)` | Two buckets: 500.00 USD and 50.00 EUR. No exception. |
| `byStage(..., USD)` | Throws `IllegalArgumentException` when merging EUR into the USD zero. |
| `byOwner(..., EUR)` with only USD deals | Silently returns USD totals. |

`ForecastTest` never covers mixed currencies or a request/deal currency mismatch (`cannotAddDifferentCurrencies` only hits `WeightedValue.plus` directly). Gherkin states the currency rule and then has **zero** scenarios for it, so Stage 3 glue cannot fail a mixed-currency case that the feature text forbids.

`ForecastUseCase.byOwner(Currency)` will inherit this: the web adapter will ask for USD and can receive EUR amounts.

**Required fix:**

1. In both `byOwner` and `byStage`, reject (domain exception) any open deal whose money currency is not the requested currency. Do not return a multi-currency list.
2. Add Gherkin: mixed-currency pipeline is rejected; requesting EUR when every deal is USD is rejected; same-owner two-currency deals are rejected. “Not mixed” must mean the whole result, not “plus() throws only when two amounts land in one map entry.”

---

### 2. `Deal.restore` (and `revalue`) can create a closed deal that violates §7 and then cannot be repaired

**Severity:** Important
**Confidence:** 88

**CONSTITUTION.md §3:** “Constructors and mutating methods reject illegal states by throwing domain exceptions.” §7.2: closed-won requires `value > 0`. §7.4: closed-won probability is 100, closed-lost is 0.

**Evidence:**

`Deal.restore` is a public factory that only null-checks (`Deal.java:43-52`). This is legal today:

```java
Deal.restore(id, companyId, ownerId, title, Money.of("0.00", "USD"), Probability.of(25), DealStage.CLOSED_WON);
```

Result: `CLOSED_WON`, value 0, probability 25. `changeProbability` then throws because the stage is terminal (`Deal.java:94-99`). `changeStage` throws because the stage is terminal. The illegal state is sticky.

`DealTest.restoreRebuildsExistingState` restores `PROPOSAL` at probability 20 and never asserts closed-state alignment. `ForecastTest.deal(...)` restores `CLOSED_WON` / `CLOSED_LOST` with whatever probability the caller passes.

The transition path *does* force probability (`Deal.java:108-113`) and reject non-positive value. Persistence Stage 3 will call `restore`, not `changeStage`. A mapping bug or a later `revalue(Money.of("0.00", "USD"))` on an already-won deal (`Deal.java:90-92` has no terminal/positive guard) produces a `CLOSED_WON` with value 0 while `changeProbability` is locked. Asymmetric: probability is frozen, value is not.

**Required fix:**

1. `restore` (or the private constructor) must reject `CLOSED_WON` unless `value.isPositive()` and probability is 100, and `CLOSED_LOST` unless probability is 0.
2. `revalue` must reject non-positive value on `CLOSED_WON`, and should reject mutation of value on a terminal deal unless Gherkin explicitly allows it (add that scenario either way).
3. Tests for both factories and mutators; Gherkin for “cannot zero-out a won deal.”

Activity membership cannot be checked inside `restore` while activities live outside `Deal`. That is finding 3’s problem, not an excuse to skip the checks restore *can* make.

---

### 3. Closed-won “has a meeting/call on this deal” is not a Deal invariant; it is an argument the caller can fabricate

**Severity:** Important
**Confidence:** 86

**CONSTITUTION.md §7.2:** a deal cannot move to `CLOSED_WON` unless it **has** at least one Meeting or Call. §3: the state machine’s guards live on the entity.

**Evidence:**

`Deal` stores no activities and no “has qualifying engagement” flag. `changeStage(User, DealStage, Collection<Activity>)` trusts the collection. `hasQualifyingActivity` only checks `activity.qualifiesDealWin(id)` (`Deal.java:139-146`).

A Stage 3 interactor (or a test) can:

```java
deal.changeStage(owner, DealStage.CLOSED_WON, List.of(meetingOn(deal.id())));
dealRepository.save(deal);
// never ActivityRepository.save
```

The deal is `CLOSED_WON` with no durable activity. Conversely, omitting `ActivityRepository.findByDeal` when calling `changeStage` rejects a deal that *does* have a meeting. The domain cannot tell the difference between “this deal’s activities” and “a list I built.”

`Deal.restore(..., CLOSED_WON)` skips the activity guard entirely, so a persistence round-trip of a won deal never re-validates §7.2.

Gherkin is actually clear that the activity must be **linked to that deal** (`deal_closed_won_guards.feature` lines 3–4, contact vs other-deal scenarios). The entity does not *have* that link.

This is the anemic-adjacent hole: behavior is on `Deal`, the fact the rule talks about is not.

**Required fix:**

Choose one aggregate that owns the win evidence, then keep it there:

- Preferred: `Deal.recordActivity(...)` (or equivalent) that accepts only a deal-targeted Call/Meeting/Note, remembers that a qualifying engagement exists, and `changeStage` takes no `Collection<Activity>`. `restore` must take the flag or the activity list and still refuse `CLOSED_WON` without it.
- Or: keep `Activity` as its own aggregate but persist the derived `hasQualifyingEngagement` on `Deal` only through a domain method, never as a naked boolean on `restore` without a guard.

`ChangeDealStageUseCase.Command` correctly omits activities (the interactor should load them). That does not make the entity honest.

---

## Minor

### 4. Gherkin “terminal” and “same-stage” tables are narrower than the feature text

**Severity:** Minor
**Confidence:** 84

`deal_stage_transitions.feature`: “Terminal stages are terminal,” but the outline only moves a closed deal **to LEAD**. `CLOSED_WON → CLOSED_LOST`, `CLOSED_WON → QUALIFIED`, `CLOSED_LOST → CLOSED_WON`, same-stage on a terminal stage are unspecified. Same-stage is only `QUALIFIED → QUALIFIED`. Skip/back examples omit `NEGOTIATION → LEAD` / `PROPOSAL → LEAD`.

CONSTITUTION.md §7: “Gherkin wins over this summary.” A Stage 3 glue author who implements only the examples still has a green feature file if they allow `CLOSED_WON → CLOSED_LOST`. Domain `DealStage.ALLOWED` currently forbids it (`DealStage.java:14-20`); the acceptance spec does not.

**Required fix:** Exhaustive outlines (or a documented “every pair not listed as allowed is rejected” rule with at least one other-terminal and one same-stage-on-terminal example). Same for probability lock after `CLOSED_WON` (`deal_probability_on_close.feature` only locks after `CLOSED_LOST`).

### 5. `RecordActivityUseCase.Command` does not encode deal XOR contact

**Severity:** Minor
**Confidence:** 82

```13:13:application/src/main/java/com/pipelinecrm/application/port/in/RecordActivityUseCase.java
    record Command(UserId actorId, ActivityType type, String body, DealId dealId, ContactId contactId) {}
```

Both IDs nullable, both allowed together. Domain `ActivityTarget` is already XOR (`ActivityTarget.java:18-24`). Closed-won Gherkin hangs on that distinction. Prefer `ActivityTarget` (or `onDeal` / `onContact` factories) on the command so Stage 3 cannot “record on both.”

### 6. §2.3 port-package ArchUnit still missing after ports landed

**Severity:** Minor
**Confidence:** 90

Stage 0 review item 6 said add the `port.in` / `port.out` / `usecase` rules in the same commit as the first ports. Ports exist; `ApplicationIndependenceTest` only requires `com.pipelinecrm.application..`. A port in `com.pipelinecrm.application.api` still verifies.

### 7. Incomplete `Then`s on rejection paths

**Severity:** Minor
**Confidence:** 80

`deal_closed_won_guards.feature` “no activities is not enough” does not assert the stage is still `LEAD`. `deal_stage_transitions.feature` same-stage scenario does not assert stage is still `QUALIFIED`. Other rejections do. Glue can treat a mutation that still throws but also advances stage as passing.

---

## Attack-question notes (not extra findings)

| # | Question | Result |
| --- | --- | --- |
| 1 | Anemic Deal? Invariants in constructors/methods? | **Mostly rich, with holes.** No public setters. `changeStage` + `DealStage.ALLOWED` is a real machine. `Deal.open` always starts `LEAD`. `restore` / `revalue` / external activity list are findings 2–3. Company/Contact/User are thin (rename) and that is acceptable. |
| 2 | `CLOSED_WON` without meeting/call on **this** deal? | **Not via `changeStage` if the collection is honest.** Notes, contact meetings, other-deal meetings, empty list, zero value are all rejected in domain tests and Gherkin. Bypass: fabricate the collection or `restore` (findings 2–3). |
| 3 | Peer sales user change stage? | **Rejected.** `assertAuthorized`: owner id or `User.isManager()`. Gherkin `deal_stage_authorization.feature` covers owner / manager / peer. |
| 4 | Probability on close? | **Forced on the transition** (`Probability.closedWon()` / `closedLost()`). Locked afterwards. Not forced on `restore` (finding 2). Gherkin does not lock after `CLOSED_WON` (minor 4). |
| 5 | Forecast formula and closed-deal exclusion? | **Formula is `value × probability/100`** (`WeightedValue.of`). Open = `!stage.isTerminal()`. Closed excluded in both groupings. Stage grouping seeds empty open stages. **Currency is broken** (finding 1). |
| 6 | Gherkin unambiguous (contact vs deal, skip, same-stage, terminal)? | Contact vs deal: **yes**, dedicated scenarios. Skip: **yes** for listed pairs. Same-stage: **one** example. Terminal: **only to LEAD** (minor 4). Currency rule: **text without examples** (finding 1). |
| 7 | Framework types in domain? | **No.** JDK only (`UUID`, `Instant`, `Currency`, `BigDecimal`). ArchUnit + enforcer green. No Lombok, Spring, JPA, Jackson. |
| 8 | Ports leak HTTP/JPA? | **No.** Commands/results use domain IDs, `DealStage`, `Money`, `Probability`, `String` token. `TokenIssuer` / `PasswordHasher` are application ports, not JWT/JPA types. |
| 9 | Coverage / CRAP / PIT green? | **Yes** on `verify` and `-Pmutation`. Domain 100/100 JaCoCo, CRAP 0 violations, PIT 125/125. Application ports 100% line, 0 PIT mutants (`failWhenNoMutations=false` is still the Stage 0/3 escape; not a Stage 1 reject). |

---

## What is already in good shape (do not rip out)

- Happy-path table `LEAD → QUALIFIED → PROPOSAL → NEGOTIATION` with no skips, close-from-any-open, notes-do-not-count, contact/other-deal meetings do not count, owner vs manager vs peer.
- Value objects (`Money`, `Probability`, `DealTitle`, `Email`, `ActivityTarget`) reject garbage in compact constructors.
- Input ports are use-case sized; `ChangeDealStageUseCase` does not take HTTP or a raw activity dump.
- Output ports are persistence/identity abstractions, not Spring Data.
- Inner-layer purity gates still hold.

Fix findings 1–3 (currency-honest forecast with Gherkin, restore/revalue closed-state guards, make qualifying engagement part of `Deal`’s state). Then request re-review. Do not start Stage 2 until this file contains the line `STAGE 1 APPROVED`.
