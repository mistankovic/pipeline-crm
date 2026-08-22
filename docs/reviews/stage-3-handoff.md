# Stage 3 hand-off — Use cases, unit tests, acceptance tests

## Stage 0 debt discharged, in full

`application/pom.xml` no longer contains `crap.skip`, `coverage.skip` or `pit.skip`. Both
inner modules are now fully gated. No module in the build carries a temporary opt-out.

## The use cases

Fourteen interactors in `com.pipelinecrm.application.usecase`, one per input port. Every
one of them reads the same way: translate the request, ask the domain, write back, describe
the result. `ChangeDealStageInteractor` is four lines of body, and none of them is a rule.

### Collaborators the constructor limit found

The Constitution allows four parameters. Three use cases would have needed six, and rather
than raise the limit each missing concept was named:

| Collaborator | What it is |
|--------------|------------|
| `Parties` | Turning an id from outside into a company or a user, and saying which was missing when it cannot. |
| `DealHistory` | A deal together with what has been recorded against it — they travel together because the domain's own rules need both at once. |
| `ActivitySubjects` | Translating the port's sealed "what is this about" into the domain's sealed version, checking existence on the way. Both sides sealed, so the translation is exhaustive by construction. |
| `Timelines` | Activities plus who recorded them, with authors read once per timeline rather than once per entry. |
| `DealMutations` | Find the deal, find who is asking, let the deal decide, write it back, describe the result — inside one unit of work. |
| `WritingPorts` | The identity source and unit of work every writing use case needs. |

`DealMutations` was not designed up front: **the duplication gate found it.** CPD reported
five duplicate blocks over 30 tokens, three of which were the three deal-changing use cases
being the same method with a different middle line.

### Presenters became pure functions

`DealViews`, `ActivityViews`, `ForecastViews` and friends are static. They hold no state,
reach no port, and there is nothing a test would substitute; injecting them would have added
a constructor parameter to every use case to make a function replaceable by another function
computing the same thing. Recorded as a decision in the class comment of `MoneyViews`.

## Acceptance tests

**All 69 Gherkin scenarios execute and pass** against the use-case layer — no HTTP, no
database, no browser. `AcceptanceTest` is a JUnit Platform suite; the glue is seven small
step classes sharing a `World` through PicoContainer.

Two things worth stating plainly:

* **No scenario was edited to make it pass.** The Stage 1 review said it would treat any such
  edit as a Stage 1 defect found late. There were none. Scenarios moved between files
  (`revising_a_deal.feature` was split out) and nine were added, but no existing scenario's
  text changed.
* **Fixtures drive the real use cases.** A Given that says "a deal in NEGOTIATION" produces
  one that got there legally, through `CreateDeal` and `ChangeDealStage`. Nothing writes to a
  repository behind the use cases' backs, so a scenario cannot set up a state the system
  could never reach.

Cucumber steps take one argument per captured value, which would have blown the four
parameter limit. Fixed with custom `@ParameterType`s (`{money}`, `{amount}`, `{stage}`,
`{dimension}`) rather than with a suppression: a step now takes one argument for one idea.

## Metrics

| Metric | `domain` | `application` | Gate |
|--------|---------:|--------------:|------|
| Line coverage | **100.0 %** | **100.0 %** | ≥ 95 % |
| Branch coverage | **100.0 %** | **100.0 %** | ≥ 95 % |
| Mutation score | **125/125 (100 %)** | **78/78 (100 %)** | ≥ 90 % |
| Worst CRAP | 5.00 | **2.00** | ≤ 6 |
| Methods | 126 | 115 | — |
| CPD @ 30 tokens | 0 | 0 | 0 |
| Checkstyle | 0 | 0 | 0 |

Test count: 133 in `application` (64 unit + 69 acceptance), 231 in `domain`.

## What mutation testing found that nothing else did

Three survivors, all real:

1. & 2. Deleting `history.save(deal)` from `DealMutations` and from `DealHistory` broke
   nothing, because `InMemoryDeals` returned **the same object** the caller had just mutated.
   Every test passed with the write removed. The fake now stores and returns copies. This is
   the most valuable thing this stage produced: a test double that could not detect a missing
   write, found by a tool rather than by luck.
3. `DealHistory.everyDeal` had no callers. Dead code, deleted.

All recorded in `docs/mutation-survivors.md`.

## Deliberately not done

* JPA entities, mappers, Flyway schema — Stage 4.
* Controllers, DTOs, HTTP error mapping — Stage 5.
* The Svelte UI — Stage 6.
