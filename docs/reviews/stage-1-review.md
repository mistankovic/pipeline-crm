# Stage 1 — Adversarial Review (round 1)

I attacked the domain for anemia, leaked framework concepts, missing invariants and
ambiguous scenarios, and I checked the hand-off's numbers rather than believing them.

## What survives attack

* **Not anemic.** `Deal` has 24 methods and no setters; the four required rules are private
  `require…` methods inside the entity, not a service operating on data. Nothing in
  `domain/src/main` imports Spring, Jakarta, Jackson or JDBC — I grepped.
* **`DealActivities` is the best idea in this stage.** Passing a bare `List<Activity>` would
  have let a caller hand deal A's meeting to deal B and win a deal nobody has spoken to.
  The type refuses. That is an invariant I would otherwise have demanded.
* **Sealed `ActivitySubject`** makes "linked to exactly one thing" unrepresentable rather
  than validated. Correct instinct.
* **The metrics are real.** 100 % line, 100 % branch, 119/119 mutants killed, worst CRAP
  5.00, and the one survivor was killed by *specifying* behaviour, not by excluding it.
  I re-ran `mvn verify` myself.

Now the defects.

---

## F-1.1 — BLOCKER. Anyone at all may reprice or reweight somebody else's deal.

`Deal.changeStageTo` demands the actor be the owner or a manager. `Deal.reprice` and
`Deal.reweight` demand nothing:

```java
public void reprice(Money newValue) {
    requireStillOpen();
    terms = terms.pricedAt(newValue);
}
```

A rival salesperson cannot move my deal to CLOSED_LOST, but they can set its value to zero
and its probability to 0 %, which removes it from the forecast just as effectively and is
harder to notice. The requirement's rule 2 names the stage because that is the case the
author thought of; the *intent* is that a deal belongs to its owner. Meanwhile
`ReviseDeal.DealRevision` already carries an `actorId` that the domain gives it nowhere to
spend — the port author clearly expected a check that the entity does not perform.

Either the entity enforces authority on every mutation, or the hand-off must argue in
writing why value is less protected than stage. I do not believe that argument exists.

## F-1.2 — BLOCKER. `ReviseDeal` cannot express the money it is asked to change.

```java
record DealRevision(UUID dealId, UUID actorId, BigDecimal value, Integer probability) {}
```

There is no currency. `Money` requires one. The scenario
*"When Sam tries to reprice 'Acme renewal' to 20000 EUR"* is therefore unimplementable
against this port as written. The implementer in Stage 3 will have exactly two options,
both bad: silently reuse the deal's existing currency (a business decision smuggled into a
mapper), or invent one. Fix the port now, before an implementation hides the problem.

While you are there: `Integer probability` is a boxed nullable used to mean "leave it
alone". Constitution §4 forbids null as a domain/application signal. A partial update needs
either two ports or an explicit "unchanged" representation, not a null.

## F-1.3 — MAJOR. The domain throws exceptions that are not domain exceptions.

Every failure in this module is supposed to be a `DomainException`, so the web layer can
map one hierarchy to 4xx and everything else to 500. It is not true. I compiled a probe
against `domain/target/classes`:

```
Money.of("10", "NOTACURRENCY") -> java.lang.IllegalArgumentException
DealId.fromString("nope")      -> java.lang.IllegalArgumentException
Money.of("not-a-number","EUR") -> java.lang.NumberFormatException
```

All three are reachable from user input the moment a controller exists. All three will be
mapped to **500 Internal Server Error** by any sane exception handler, for what is plainly
a 400. Worse, the Builder's own `IdentifierTest.refuses_text_that_is_not_a_uuid` *asserts*
`IllegalArgumentException` — the test enshrines the leak instead of catching it.

Wrap them. `Currency.getInstance`, `UUID.fromString` and `new BigDecimal(String)` are the
three places.

## F-1.4 — MAJOR. Grouping a forecast by owner produces a stringified UUID.

`ForecastDimension.OWNER.keyOf` returns `deal.parties().owner().value().toString()`. The
forecast's group column is therefore a UUID rendered as text, which the application layer
must parse back into a `UserId` to look up a name for `ForecastLineView.label`. A type
was destroyed and will have to be reconstructed by string parsing one layer out. That is
the definition of primitive obsession, and it is in the innermost layer.

`ForecastLine` should carry something typed. I will accept a small `ForecastGroup` value
object, or the calculator returning a `Map<UserId, Money>` for the owner dimension — but
not a `String` that is secretly a UUID.

## F-1.5 — MAJOR. Nothing in this design says where users come from.

`signing_in.feature` opens with *"Given a salesperson 'Sam' with password 'correct-horse'"*.
There is no `CreateUser` port, no `UserRepository.save`, and no mention of seeding in the
Constitution's non-goals. So either user creation is a use case that was forgotten, or it is
a seeded fixture that nobody has written down. Right now the Gherkin depends on a mechanism
that does not exist anywhere in the design. Decide and write it down.

## F-1.6 — MINOR. Two ports throw away type safety at the boundary for no stated reason.

* `ProduceForecast.handle(String dimension)` and `ChangeDealStage.StageChange.targetStage`
  are strings, when `ForecastDimension` and `DealStage` are enums the application already
  depends on.
* `ListContacts.handle(Optional<UUID>)` and `ViewPipeline.handle(Optional<UUID>)` take
  `Optional` as a **parameter**, which is a documented misuse of the type.
* `LogActivity.NewActivity.About(UUID dealId, UUID contactId)` reintroduces exactly the
  "both or neither" hole that the domain's sealed `ActivitySubject` was built to close.

Strings at an HTTP boundary are defensible. These are not at the HTTP boundary — they are
the *use-case* boundary, one layer in, and the web adapter is supposed to have already
translated. If the Builder wants string-typed ports, the justification goes in the hand-off.

## F-1.7 — MINOR. The hand-off overstates the work.

* "Twelve packages under `com.pipelinecrm.domain`" — there are **eight**. I counted them.
* "60 scenarios including outlines" — expanding every `Examples` table gives **54**.

Neither number changes anything technically. Both damage the document that a reviewer is
supposed to be able to trust instead of re-deriving. If the hand-off is wrong about the
things I can check in ten seconds, I have to check everything.

## F-1.8 — MINOR. No scenario covers rejected input at the use-case boundary.

Every feature file exercises the happy path and the *domain* rejections. Nothing covers
"create a deal for a company that does not exist", "log an activity with no author",
"forecast by a dimension that is not a dimension". Those are use-case behaviours and Stage 3
will implement them; the specifications should say what they do first.

## F-1.9 — NIT. `Examples.dealAt` drives the entity in a `while` loop.

The test builder advances a deal stage by stage using `DealStage.values()[ordinal + 1]`.
It works, but it couples every test that uses it to the *order of the enum constants*.
Reorder the enum and dozens of unrelated tests start failing for a reason that has nothing
to do with what they test.

---

## Verdict

The domain itself is the strongest part of this project so far, and I want that on the
record. But F-1.1 leaves a deal's value unprotected while its stage is guarded, and F-1.2
describes a port that cannot implement its own acceptance scenario. Both are design faults
that get much more expensive after Stage 3 exists on top of them.

**STAGE 1 NOT APPROVED.** Fix F-1.1 through F-1.5. F-1.6 through F-1.9 may be fixed or
refused in writing.
