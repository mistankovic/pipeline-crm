# Stage 3 — Adversarial Review (round 1)

The numbers in this hand-off are the best in the project: 100 % line, 100 % branch and
100 % mutation on both inner modules, worst CRAP 2.00 in the application layer, 69 executable
scenarios. So I spent this review asking the only question worth asking about numbers like
that: **which of these tests would still pass if the code were wrong?**

I found two that would.

## What survives attack

* `ChangeDealStageInteractor` is four lines and contains no rule. I read all fourteen
  interactors looking for a business decision that had leaked upward. There is not one.
* `DealMutations` was extracted **because the duplication gate reported it**, not because
  somebody felt like refactoring. That is the tool doing the design work it was installed to do.
* The `InMemoryDeals` fix is the best work in this stage. A test double that returns the very
  object the caller mutated cannot detect a missing write, and 133 tests did not notice. PIT
  did. The hand-off says so plainly instead of quietly fixing it.
* Custom `@ParameterType`s instead of a Checkstyle suppression: the limit was respected and
  the steps read better for it.

## F-3.1 — BLOCKER. Two scenarios pass whatever the system does.

`World.attempt` swallows any `RuntimeException` and stores it. That is right for a step that
says "tries to". It is applied to steps that state a **plain action**, and in two scenarios
nothing ever looks at what was swallowed.

`recording_activities.feature`:

```gherkin
Scenario: An activity against a contact does not appear on a deal timeline
  When Sam logs a MEETING "introductions" against the contact "Cara"
  Then the timeline of "Acme renewal" has 0 entries
```

If logging against the contact throws — for any reason, today or after a future change — the
exception is swallowed and the deal timeline still has 0 entries. **The scenario passes
because nothing happened.** It is testing that a failed operation does not have an effect.

```gherkin
Scenario: An activity against a contact is not evidence for winning a deal
  And Sam logs a MEETING "introductions" against the contact "Cara"
  When Sam tries to move "Acme renewal" to CLOSED_WON
  Then the move is rejected because the deal has not earned a win
```

Same hole, and worse: a deal with **no** activity at all also fails to be won, so the
scenario cannot distinguish "the contact meeting was correctly ignored" from "the contact
meeting was never recorded". This is the scenario that proves decision D-4 — the rule that a
contact's meeting is not evidence for a deal — and right now it proves nothing.

Two fixes, and I want both:

1. Steps that state a plain action must **not** swallow. `attempt` belongs only to steps whose
   sentence says "tries to". Where a feature file uses a plain phrasing for a rejection
   scenario, change the *sentence* — "Sam tries to create a deal…" — not the step.
2. Add an `@After` hook that fails the scenario if a swallowed failure was never asserted.
   Then this class of hole cannot come back, in any scenario anybody writes later.

## F-3.2 — MAJOR. Two views disagree about what a missing user is.

I probed it:

```
PROBE viewPipeline -> java.lang.NullPointerException
PROBE viewDeal     -> com.pipelinecrm.application.error.UnknownEntity
```

`ViewPipelineInteractor.viewsOf` reads `owners.get(...)` and `companies.get(...)` straight out
of a map and hands the result to `DealViews.of`, which dereferences it. `ViewDealInteractor`
asks `Parties`, which raises `UnknownEntity`. Same missing datum, two behaviours: one becomes
a clean 404-shaped error, the other a `NullPointerException` that the web layer will report as
**500 Internal Server Error**.

I accept this is only reachable through inconsistent data, and there is no delete in the
system today. That is an argument about likelihood, not about correctness — and the whole
point of `Required.found` is that the application layer never lets a missing thing turn into
an NPE. One of the two code paths did not get the memo.

## F-3.3 — MAJOR. The `Timelines` collaborator reads every user in the system, per call.

`Timelines.of` calls `parties.everyone()` — a full `findAll()` over users — for **every**
timeline, including an empty one. `ViewDealInteractor` therefore issues a full user table scan
to render a deal with no activities. The hand-off presents "authors are read once per timeline
rather than once per entry" as an optimisation; it is one, compared to the alternative it
replaced, but it is still an unbounded read to resolve at most a handful of authors.

I am not asking for a cache. I am asking that the port express what the use case actually
needs — the authors of *these* activities — rather than the use case asking for everything and
throwing most of it away.

## F-3.4 — MINOR. Deal-creation steps live in `ContactSteps`.

`ContactSteps` contains `aDealIsCreatedWithAnUnknownOwner` and
`createsADealForAnUnknownCompany`. Both are about deals. The class is named for contacts. A
step definition is found by searching for its sentence, so this costs nothing to run and a
minute every time somebody looks for it.

Both also use fully-qualified `com.pipelinecrm.application.port.in.CreateDeal` and
`java.math.BigDecimal` inline instead of importing them. `ActivitySubjects` does the same with
`java.util.UUID`. Checkstyle bans star imports; it does not ban this, and it should not have to.

## F-3.5 — MINOR. `Required.found` is stringly typed.

Every call site passes a literal: `"deal"`, `"company"`, `"user"`, `"contact"`. Four literals
repeated across nine files, and the reviewer of the eventual HTTP error mapping will have to
grep for them to know the vocabulary. The `Identifier` argument already knows what it is —
`DealId` is not ambiguous.

## F-3.6 — MINOR. Two people with the same name break the acceptance suite silently.

`PeopleSteps.emailFor` derives the address from the name, and `EmailAddress` is unique per
user in `InMemoryUsers.findByEmail` (first match wins). A future feature file with two people
called "Sam" would get one user and no warning. Cheap to defend against; expensive to debug.

## F-3.7 — NIT. `UseCases` exposes nine public mutable fields.

It is a test fixture and I am not going to insist. But it is the object every acceptance test
and every unit test touches, and `application.users.forget(...)` — which I added to probe
F-3.2 — went in without any friction at all. That is a lot of leverage for something with no
encapsulation.

---

## Verdict

The engineering here is the strongest so far and the mutation-testing find is genuinely
excellent work. But F-3.1 means two of the 69 scenarios are decoration, and one of them is the
*only* executable evidence for a documented domain decision. A suite that reports 69/69 while
two of them cannot fail is exactly the failure mode this whole project claims to be about.

**STAGE 3 NOT APPROVED.** Fix F-3.1 through F-3.3. F-3.4 through F-3.7 may be fixed or refused
in writing.

---

# Stage 3 — Adversarial Review (round 2)

| Finding | Verified |
|---------|----------|
| F-3.1 | Yes, three ways. (1) The reworded sentences are in the feature files. (2) I re-ran the Builder's sabotage and confirmed `recording_activities.feature:21` and `:26` now fail. (3) **I wrote my own probe scenario** that calls a "tries to" step and asserts nothing, and the hook caught it: *"a step was refused and no Then examined the refusal, so this scenario passed because nothing happened: no company with id …"*. Reverted. |
| F-3.2 | Yes. Both paths go through `PartyIndex`, which goes through `Required.found`. |
| F-3.3 | Yes, and fixed at the port as I asked rather than with a cache. `everyone()` and `everyCompany()` no longer exist — I grepped. |
| F-3.4, F-3.5, F-3.6 | Yes. Removing the `kind` literal in favour of deriving it from the identifier's type is a better fix than the one I described, and `UnknownEntityTest` pins the wording for the layer that has not been written yet. |
| F-3.7 | **Refusal accepted.** The argument is a good one and it is evidenced: my own F-3.2 probe was a one-line change *because* the fixture is open. I was applying a production rule to a diagnostic tool. |
| Metrics | Re-ran `mvn verify`: 100 % line and branch on both inner modules, **125/125 and 86/86 mutants killed**, worst CRAP 5.00 and 2.00, CPD and Checkstyle clean, six modules green. |

## The finding I want to record for the rest of this project

F-3.1 is the most important defect found so far, and none of the tooling could have found it.
Coverage was 100 %: the code *was* executed. Mutation score was 100 %: every mutant was
killed — by other tests. CRAP was 2.00. Every gate in the Constitution was green while two
scenarios were incapable of failing, one of them the sole executable evidence for a documented
domain decision.

The lesson is not "add another tool". It is that **a metric tells you what was executed, never
what was depended upon**, and the only defence is someone asking of each test: what would have
to break for this to go red? The `@After` hook now automates that question for one specific
failure mode. The question itself still has to be asked by a person, and I will keep asking it
in Stages 4 to 8.

## Verdict

Fourteen use cases holding no business rules, 69 scenarios that can all now fail, both inner
modules at 100 % across coverage and mutation, and the two most valuable defects of this stage
found by a tool and by a review respectively — each fixed at the root rather than at the
symptom.

**STAGE 3 APPROVED**
