# Stage 1 — Re-review

STAGE 1 APPROVED

Scoped re-review of `00fc3d2` (`fix: keep win evidence and currency on the Deal aggregate`) against `docs/reviews/stage-01-review.md` and `docs/reviews/stage-01-fixes.md`. Branch `local-grok-pipeline-crm`, working tree clean aside from this report. Did not inspect other git branches.

Command: `export JAVA_HOME="$(/usr/libexec/java_home -v 21)" && ./mvnw -B verify` — BUILD SUCCESS. Domain JaCoCo check met, CRAP 137 methods 0 violations; application CRAP 12 methods 0 violations.

---

## Previous Important findings

### 1. `Forecast.byOwner` ignores the requested currency and will mix currencies across owners

**Verdict: ADDRESSED**

- `Forecast.openDealsIn` now calls `requireCurrency` on every **open** deal (`Forecast.java:33-44`, `46-50`). Mismatch throws `MixedCurrencyException` (domain exception: “forecast requires every open deal to use the requested currency”).
- `byOwner` and `byStage` share that filter. Alice USD + Bob EUR no longer yields a mixed list; requesting EUR for a USD-only pipeline throws.
- Same-owner two-currency open deals are rejected in `ForecastTest.mixedOpenCurrenciesAreRejected` (USD+EUR, both `LEAD`, same `UserId`); both grouping methods asserted.
- Gherkin: `forecast.feature` “mixed currencies are rejected” (alice USD, bob EUR, grouped by owner in USD) and “requesting a currency none of the open deals use is rejected” (USD deal, request EUR).

Closed deals are still skipped before the currency check, which matches “open deals only.”

### 2. `Deal.restore` (and `revalue`) can create a closed deal that violates §7 and then cannot be repaired

**Verdict: ADDRESSED**

- Private constructor always `recordAll` then `assertConsistent` (`Deal.java:40-41`, `164-174`). `CLOSED_WON` requires `assertWinnable()` (positive value + qualifying activity) and probability 100. `CLOSED_LOST` requires probability 0.
- `restoreClosedWonWithoutMeetingIsRejected` and `restoreRejectsIllegalClosedWon` (probability 25 on won) fail as required. Foreign-deal activity on restore is rejected in `recordActivity` before consistency.
- `revalue` and `changeProbability` both go through `assertNotTerminal` (`Deal.java:101-111`, `145-148`). `closedDealLocksValue` covers revalue after won.
- Gherkin: `deal_probability_on_close.feature` locks probability **and** value after `CLOSED_WON`.

### 3. Closed-won “has a meeting/call on this deal” is not a Deal invariant

**Verdict: ADDRESSED**

- `Deal` owns `List<Activity>` (`Deal.java:22`). `recordActivity` accepts only `activity.target().isDeal(id)`. Contact or other-deal activities throw; they never enter the list.
- `changeStage(User, DealStage)` no longer takes a `Collection<Activity>`. Win check uses the owned list (`assertWinnable` / `hasQualifyingActivity`).
- `restore` reconstitutes from the same list and still refuses `CLOSED_WON` without a qualifying activity.
- `ChangeDealStageUseCase.Command` still has no activity dump; Stage 3 must load the deal and call `changeStage` on its state.

Fabricating a meeting now means recording it **on the Deal**. Persistence of the aggregate persists the evidence. That was the required model.

---

## Previous Minors the Builder claimed fixed

| # | Claim | Verdict | Evidence |
| --- | --- | --- | --- |
| 4 | Terminal / same-stage Gherkin expanded | **ADDRESSED** | `deal_stage_transitions.feature`: terminal outline includes `CLOSED_WON → CLOSED_LOST`, `CLOSED_WON → QUALIFIED`, same-stage on both terminals; same-stage outline covers all four open stages. Probability lock after `CLOSED_WON` is in `deal_probability_on_close.feature`. Skip table is still not a full cartesian product (`NEGOTIATION → LEAD` omitted); the first review allowed “at least one other-terminal and one same-stage-on-terminal example.” |
| 5 | `RecordActivityUseCase.Command` XOR | **ADDRESSED** | Compact constructor: exactly one of `dealId` / `contactId` (`RecordActivityUseCase.java:14-18`). `PortTypesTest` asserts both-null and both-set throw. |

Minors 6 (port-package ArchUnit) and 7 (some rejection `Then`s still omit “stage is still X”: no-activities closed-won, same-stage open outline) were not claimed. They do not block Stage 1.

---

## New issues from the fix diff

No new Critical or Important findings.

### Residual Minors (do not block Stage 1)

1. `Deal.restore` of `CLOSED_WON` with value `0` and a **valid** meeting on this deal is implemented (`assertWinnable`) but the named test uses `meetingOn(DealId.generate())`, so that path fails as `IllegalArgumentException` first. Not a production hole.
2. `recordActivity` throws `IllegalArgumentException` for a foreign target, not a domain exception.
3. `ViewDealUseCase.Result` still carries a separate `List<Activity>` now that `Deal.activities()` exists. Harmless duplication until Stage 3.
4. Gherkin mixed-currency scenarios only exercise **owner** grouping; `byStage` is covered by the unit test only.

---

Stage 2 may start. Further stages still need `STAGE N APPROVED` in `docs/reviews/` before N+1.
