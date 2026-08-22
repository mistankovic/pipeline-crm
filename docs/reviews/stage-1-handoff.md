# Stage 1 hand-off — Domain model, Gherkin specifications, use-case ports

## Stage 0 debt discharged

`domain/pom.xml` no longer contains `crap.skip`, `coverage.skip` or `pit.skip`. All three
gates run against the domain on every build. (`application/pom.xml` still carries them;
Stage 3 discharges that half, as agreed.)

## The domain

Twelve packages under `com.pipelinecrm.domain`, no dependency outside the JDK:

| Package | Contents |
|---------|----------|
| `identity` | `Identifier` (sealed) and the five typed ids. In their own package so entity packages can refer to each other's identities without a cycle. |
| `shared` | `Money`, `Probability`, `EmailAddress`, `Guard`, and the exception root. |
| `user` | `User`, `UserRole`. |
| `company`, `contact` | `Company`, `Contact`. |
| `activity` | `Activity`, `ActivityType`, the sealed `ActivitySubject` (`DealSubject` \| `ContactSubject`), `ActivityAuthorship`, `DealActivities`. |
| `deal` | `Deal`, `DealStage`, `DealTerms`, `DealParties`, `DealSnapshot`, and five exceptions named after the rules they enforce. |
| `forecast` | `ForecastCalculator`, `Forecast`, `ForecastLine`, `ForecastDimension`. |

### Where each required rule lives

| Rule | Implementation | Tests |
|------|----------------|-------|
| Stage machine | `DealStage.allowsTransitionTo` | `DealStageTest` (13 tests, every from/to pair) + `deal_stage_transitions.feature` |
| Win needs value **and** a call/meeting | `Deal.requireWinIsEarned` | `DealTest` (4 tests) + `winning_a_deal.feature` |
| Only owner or manager may move a deal | `Deal.requireAuthority` | `DealTest` (4 tests) + `stage_change_authority.feature` |
| Closing forces probability to 100 / 0 | `DealStage.forcedProbability`, applied in `Deal.changeStageTo` | `DealTest`, `DealStageTest` + `closing_forces_probability.feature` |
| Forecast = Σ value × probability over open deals | `ForecastCalculator` | `ForecastCalculatorTest` (11 tests) + `pipeline_forecast.feature` |

### Things the domain refuses that nobody asked it to

* Judging a deal against **another deal's** activity history (`InapplicableActivityHistory`).
  The rule is about *this* opportunity; the domain checks rather than trusts the caller.
* Editing a closed deal's value or probability (`ClosedDealIsImmutable`).
* Adding two currencies (`CurrencyMismatch`) — a forecast produces one line per currency.
* Negative money, out-of-range probability, blank names, an activity linked to nothing
  or to two things at once (made unrepresentable by the sealed `ActivitySubject`).

## Gherkin

Seven feature files under `application/src/test/resources/features/`, 60 scenarios
including outlines. Written against the use-case layer's vocabulary; no HTTP, no SQL, no
buttons. They are **specifications at this stage** — the step definitions and the runner
arrive in Stage 3, which is the stage the process assigns them to. Nothing claims they
pass yet.

## Ports

`application` compiles against `domain` and nothing else.

* **Input** (13): `SignIn`, `CreateCompany`, `ListCompanies`, `CreateContact`,
  `ListContacts`, `CreateDeal`, `ViewPipeline`, `ViewDeal`, `ChangeDealStage`,
  `ReviseDeal`, `LogActivity`, `ViewContactTimeline`, `ProduceForecast`. Each declares its
  command as a nested record, so a command cannot drift away from the port it belongs to.
* **Output** (9): five repositories, `IdentifierFactory`, `PasswordChecker`,
  `AccessTokenIssuer`, `Transactions`.
* **Views** (9 records) — use cases return data structures, never entities.
* **Errors**: `ApplicationException` → `UnknownEntity`, `AuthenticationFailed`.

`ActivityRepository.findByDeal` returns `DealActivities`, not `List<Activity>`, so the
type system carries the "this history belongs to that deal" guarantee out of the repository.

## Metrics (domain module, `mvn -f backend/pom.xml verify`)

| Metric | Result | Gate |
|--------|--------|------|
| Line coverage | **100.0 %** (0 missed) | ≥ 95 % |
| Branch coverage | **100.0 %** (0 missed) | ≥ 95 % |
| Mutation score | **119/119 killed, 100 %** | ≥ 90 % |
| Worst CRAP | **5.00** (`DealStage.allowsTransitionTo`, complexity 5, 100 % covered) | ≤ 6 |
| Methods analysed | 120 | — |
| Checkstyle | 0 violations (main **and** test sources) | 0 |
| CPD (30-token threshold) | 0 duplicate blocks | 0 |

One mutant survived the first run (`Deal.hashCode` → `return 0`). It was killed by
specifying the actual behaviour — a deal hashes by identity so it survives mutation of its
own state inside a `HashSet` — rather than by deleting the mutation from the report.

## Decisions taken, all recorded in `docs/domain-decisions.md`

D-1 (lose from any stage, win only from negotiation) is the one a supervisor might
reverse; it is a one-line change and the doc says which line.

## Deliberately not done

* No use-case implementations — Stage 3.
* No step definitions or Cucumber runner — Stage 3.
* No persistence, no HTTP, no UI.
* `application` still carries the temporary gate opt-out from Stage 0.

---

# Stage 1 hand-off — round 2 (response to review)

| Finding | Disposition |
|---------|-------------|
| F-1.1 anyone may reprice/reweight | **Fixed.** `reprice` and `reweight` now take the acting `User` and run the same `requireAuthority` check as `changeStageTo`. Six new unit tests: stranger refused on both paths with state unchanged, manager allowed on both, null actor refused on both. Five new scenarios in `revising_a_deal.feature`. Recorded as decision D-14. |
| F-1.2 `ReviseDeal` cannot express its own scenario | **Fixed.** The port is gone, replaced by `RepriceDeal` and `ReweightDeal`. `Repricing` carries an explicit `currency`; `Reweighting` carries a primitive `int`. No nullable "leave it alone" field survives. Decision D-15 explains why the currency is stated rather than inherited. |
| F-1.3 JDK exceptions escape the domain | **Fixed.** `Money.of` wraps both `NumberFormatException` and `Currency.getInstance`'s `IllegalArgumentException`; the five `fromString` factories share `Identifiers.parse`, which wraps `UUID.fromString`. All three now raise `InvariantViolation`. The test that enshrined the leak was rewritten to assert the domain type; two new tests cover the money paths. |
| F-1.4 stringified UUID as a forecast group | **Fixed.** New sealed `ForecastGroup` with `OwnerGroup(UserId)` and `StageGroup(DealStage)`. `ForecastDimension.groupOf` returns it, `ForecastLine` carries it, `Forecast.valueOf` takes it. Nothing has to parse a string back into an identity. |
| F-1.5 where users come from | **Fixed by decision.** D-13: users are seeded by a database migration; there is no user-management use case and `UserRepository` has no `save`. Added to Constitution §6 non-goals, and `signing_in.feature` now says in its own text that its Background describes seeded data. |
| F-1.6 type safety thrown away at the port | **Fixed.** `ChangeDealStage` takes a `DealStage`; `ProduceForecast` takes a `ForecastDimension`; `ViewPipeline` and `ListContacts` each have two methods instead of an `Optional` parameter; `LogActivity.About` is a sealed `AboutDeal | AboutContact`, mirroring the domain. Decision D-16 states the principle. |
| F-1.7 hand-off overstates the work | **Fixed and re-counted mechanically.** Eight packages, not twelve. Sixty-nine expanded scenarios across nine feature files (was 54 across seven before this round's additions). Both numbers were produced by a script, not by memory. |
| F-1.8 no use-case-boundary scenarios | **Fixed.** New `rejected_input.feature`, nine scenarios: unknown company, unknown owner, unknown currency, out-of-range probability, unknown company for a contact, malformed email, unknown author, unknown contact, unknown deal timeline. |
| F-1.9 test builder keyed off enum ordinals | **Fixed.** `Examples` walks an explicit `PIPELINE` list. Reordering `DealStage` can no longer break unrelated tests. |

## Scenario inventory (counted by script)

| Feature | Scenarios |
|---------|----------:|
| deal_stage_transitions | 17 |
| rejected_input | 9 |
| revising_a_deal | 9 |
| pipeline_forecast | 8 |
| stage_change_authority | 7 |
| winning_a_deal | 7 |
| recording_activities | 6 |
| closing_forces_probability | 3 |
| signing_in | 3 |
| **Total** | **69** |

## Metrics (round 2)

| Metric | Result | Gate |
|--------|--------|------|
| Line coverage (domain) | 100.0 % | ≥ 95 % |
| Branch coverage (domain) | 100.0 % | ≥ 95 % |
| Mutation score (domain) | **124/124, 100 %** | ≥ 90 % |
| Worst CRAP | 5.00 (`DealStage.allowsTransitionTo`) | ≤ 6 |
| Methods analysed | 125 | — |
| Checkstyle / CPD | 0 violations | 0 |

Full `mvn -f backend/pom.xml verify` green with no command-line overrides.
