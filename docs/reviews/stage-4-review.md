# Stage 4 — Adversarial Review (round 1)

## Verified

* **The domain really was untouched.** `git diff --stat HEAD~1 HEAD -- domain/` is empty. The
  central claim of this stage is true and I checked it rather than believing the hand-off.
* **The repositories really are thin.** Longest file 45 lines including imports and Javadoc;
  the only conditionals anywhere in the adapter outside the mappers are one null check in the
  password checker and the sealed-subject switch. No business logic.
* **The round trips really assert.** I corrupted `DealMapping` to write probability 0, and
  `a_deal_round_trips_with_every_field_intact` failed. Asserting on the whole `DealSnapshot`
  rather than on chosen fields is what made that work.
* **`ddl-auto: validate` caught `CHAR(3)`.** The hand-off reports this as a find; it is a good
  one, and it is the kind of defect that otherwise surfaces as a mysterious trailing-space bug
  months later.
* **The Stage 2 prediction was honoured** before the code that would have triggered it existed.

## F-4.1 — MAJOR. The constraint test passes when the insert fails for the wrong reason.

```java
private boolean insertActivityWith(String deal, String contact) {
    try { jdbc.update("insert into activities …"); return true; }
    catch (RuntimeException refused) { return false; }
}
```

`an_activity_about_nothing_is_refused_by_the_database` asserts this returns `false`. It
returns `false` for **any** failure. I changed the literal `'NOTE'` in the statement to
`'NOT_A_TYPE'` — so the insert is now rejected by the *type* constraint, not the *subject*
constraint — and:

```
Tests run: 3, Failures: 0, Errors: 0
BUILD SUCCESS
```

The test claims to prove that the database enforces "exactly one subject". It proves that the
database rejects something. This is finding F-3.1 again, in a new module, three days later:
a helper that swallows every failure, and an assertion that cannot tell the failures apart.

Two things are needed, the same two as last time:
1. Assert on **which** constraint fired, not merely that something threw.
2. Assert the **positive** case too — a valid insert with exactly one subject must succeed —
   or the helper could return `false` unconditionally and nobody would know.

And test the "both set" case, which nothing currently covers at all.

## F-4.2 — MAJOR. Sign-in leaks which accounts exist, through timing.

Decision D-12 says failures are indistinguishable so that nobody can enumerate the user list,
and `AuthenticationFailed` carries one message for every cause. The *message* is identical.
The *time* is not.

`SignInInteractor` throws at `users.findByEmail(...).orElseThrow(AuthenticationFailed::new)`,
so an unknown address never reaches `PasswordChecker`. A known address does, and bcrypt at
cost 10 takes on the order of 100 ms. An attacker times the two and reads the answer off a
stopwatch. This is the textbook user-enumeration side channel, and it defeats a property this
project explicitly set out to have.

The fix is standard: when no user matches, still perform one bcrypt comparison against a
fixed dummy hash, and then fail. `BcryptPasswordChecker` is the natural place for it since it
already owns the encoder — its current early `orElse(false)` has the same shape of problem.

I would let this go in a demo that had not made a point of it. This one made a point of it.

## F-4.3 — MAJOR. Three tests assert on the state of a shared database.

`PostgresBackedTest` resolves **one** database for the whole JVM, and nothing cleans between
classes. Against that:

* `a_deal_owned_by_somebody_else_is_not` asserts `deals.findOwnedBy(MO)` is **empty**.
* `the_demo_users_were_seeded` asserts `count(*) from users` is exactly **3**.
* `the_migrations_created_every_table…` is fine, but the pattern is set.

These pass today because no test happens to create a deal for MO or a fourth user. The first
test that does will break a test in another class, for reasons that have nothing to do with
the change being made — and the developer will spend an hour on it. Assert what *this* test
did: `doesNotContain(saved.id())`, and `contains` the three seeded addresses.

## F-4.4 — MINOR. A defensive branch nobody tests.

`ActivityMapping.subjectOf` throws `InvariantViolation` for a row with neither subject set.
The database forbids such a row, so no integration test can produce one — and
`adapter-persistence` is exempt from the coverage gate, so nothing notices the branch is dead
weight. It is either worth testing (a plain unit test on the mapper, no database needed, is
five lines) or it is not worth having. Right now it is untested code that claims to handle a
case, which is the worst of both.

## F-4.5 — MINOR. Tests enable Flyway `clean`.

`adapter-persistence/src/test/resources/application.yml` sets `clean-disabled: false`. Nothing
uses it. It is a loaded gun pointed at whatever database `PIPELINECRM_TEST_DB_URL` names —
and F-4.3 has already established that this is a shared, uncleaned database that a developer
may well point at something they care about. Delete it.

## F-4.6 — MINOR. No optimistic locking, and no decision saying so.

The hand-off raises this itself, which I credit. But "I would rather the reviewer decide" is
not a decision, and `docs/domain-decisions.md` is where decisions live. Concretely: `save`
writes the whole row, so a manager closing a deal while its owner reprices it loses one of the
two changes silently. For a demo board with drag-and-drop that is defensible — but write it
down as D-17 with the consequence spelled out, or add the version column.

## F-4.7 — NIT. Every `save` costs an extra SELECT.

`rows.save(mapper.toRow(entity))` on a detached row with an assigned id makes Spring Data call
`merge`, which SELECTs before it writes — on inserts too. Harmless at this scale, and worth
knowing before somebody benchmarks it and blames the mappers.

---

## Verdict

The architecture held: the domain did not move an inch to accommodate a database, the mappers
carry all the translation, and the constraints are a genuine second line of defence. But
F-4.1 is the *second* occurrence of a test that cannot distinguish success from a different
kind of failure, and F-4.2 undoes a security property the project deliberately built.

**STAGE 4 NOT APPROVED.** Fix F-4.1 through F-4.3. F-4.4 through F-4.7 may be fixed or refused
in writing — but F-4.6 must at least become a written decision.
