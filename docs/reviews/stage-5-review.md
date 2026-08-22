# Stage 5 — Adversarial Review (round 1)

The stage's own claim is "reject fat controllers or business logic in the web layer". I could
not find any, and the fitness function the Builder wrote to enforce it is the right one. So I
went after the translation table instead, which is where this layer's real risk lives.

## Verified

* **No business logic in a controller.** The only conditionals in the whole `rest` package are
  the two the hand-off names, and both are about JSON shape. `DealController` contains no `if`.
* **`no_controller_ever_touches_an_entity` is real** and it already found something — its own
  first version, which correctly failed on an output-port implementation. Scoping it to the
  controllers rather than deleting it was the right response.
* **Tokens are real.** `ApiTest` signs in over HTTP; nothing is mocked. A test that used
  `@WithMockUser` would have proven nothing about the filter.
* **`EveryPortIsWiredTest` refuses to pass on an empty scan.** That is the F-3.1 lesson applied
  before I had to point it out, which I note with some satisfaction.

## F-5.1 — MAJOR. The status table is matched by exact class, and nothing checks it is complete.

```java
return STATUSES.get(failure.getClass());
```

Exact-class lookup, over a hand-written `Map.of` of seven entries. Everything else becomes
**500**. So:

* `InapplicableActivityHistory` — a real `DomainException`, thrown by `Deal` — is not in the
  table. It is arguably a programming error and arguably *should* be 500, but nothing says so
  and no test asserts it. It is unmapped by omission, not by decision.
* The next `DomainException` anybody adds becomes a silent 500. Not a compile error, not a test
  failure — a 500 in production, discovered by a user.
* Subclassing any mapped exception also silently degrades to 500.

You wrote `EveryPortIsWiredTest` to protect the hand-wiring against exactly this failure mode —
a hand-maintained list falling behind the code — and then left an equally hand-maintained list
of exceptions unprotected two files away.

**Required:** a test that enumerates every concrete subclass of `DomainException` and
`ApplicationException` on the classpath and asserts each is either in the table or on an
explicit, named list of "deliberately a server fault". Same shape as the port test.

## F-5.2 — MINOR, but it will bite. A constant named for probability is used for money.

```java
private static final String LOWEST_PROBABILITY = "0";
…
@NotNull @DecimalMin(LOWEST_PROBABILITY) BigDecimal value,
…
@NotNull @DecimalMin(LOWEST_PROBABILITY) BigDecimal amount,
```

The floor on a deal's **value** and on a repricing **amount** is a constant called
`LOWEST_PROBABILITY`. It is right by accident — both floors happen to be zero. The day
somebody decides probabilities start at 5, they will change that constant and silently change
the minimum value of a deal. This is the kind of defect that Clean Code's naming chapter exists
for, and it is in a file whose Javadoc explains how careful it is being.

## F-5.3 — MINOR. A request DTO reaches into the security layer.

`NewDealRequest.ownerOr(SignedInUser caller)` — a record describing the JSON body knows about
authentication. The *controller* has the caller; the request should not. `request.ownerId()`
and a `Optional.ofNullable(...).orElse(caller...)` in the controller says the same thing
without coupling the body to the principal.

## F-5.4 — MINOR. `GET /api/users/me` reads every user to find one.

```java
return users.handle().stream().filter(user -> user.id().equals(caller.id().value())).findFirst()
```

The caller's identity is already in the token. Either add a port that fetches one user, or —
better — recognise that this endpoint mostly exists so the browser can show a name, and that
the sign-in response already contains the whole `UserView`. It may not need to exist at all.

## F-5.5 — MINOR. Anyone may create a deal owned by somebody else.

The hand-off raises this, which I credit, and then leaves it open. `POST /api/deals` accepts
`ownerId`, and nothing checks the caller may assign it. A salesperson can create a deal owned
by a colleague — and then, by rule 2, be unable to touch it again. That is a strange thing to
be able to do.

The domain has rules about *changing* a deal and none about *creating* one for someone else.
Either that asymmetry is deliberate — in which case it is a decision, in `domain-decisions.md`
— or the rule is missing from the domain. It must not be settled by a validation annotation in
the web layer.

## F-5.6 — NIT. The 500 path is right, and untested.

`unexpected()` logs the failure and returns `{"error":"InternalError","message":"the request
could not be completed"}` — correct, and the only place in the API where a caller learns
nothing. No test covers it, so nothing stops a future edit from putting `failure.getMessage()`
in that body. One test with a deliberately unmapped exception would pin it.

---

## Verdict

Thin controllers, an honest translation table, a fitness function that enforces the stage's
actual rule, and API tests that exercise the refusals harder than the successes. The layer is
right.

But F-5.1 is a hand-maintained list with no completeness check, in a project that added a
completeness check for the *other* hand-maintained list in the same stage. Fix it, and the
naming defect in F-5.2 which is going to cause a bug.

**STAGE 5 NOT APPROVED.** Fix F-5.1 and F-5.2. F-5.3 through F-5.6 may be fixed or refused in
writing, but F-5.5 must become a written decision either way.

---

# Stage 5 — Adversarial Review (round 2)

| Finding | Verified |
|---------|----------|
| F-5.1 | Yes, by my own probe. I added a new `DomainException` subclass to the domain, rebuilt, and the build failed: *"com.pipelinecrm.domain.shared.ProbeFailure must be given a status in HttpTranslation, or listed there as a deliberate server fault."* Reverted. Forgetting to map a failure is now a build failure rather than a production 500. |
| — | And the test earned its keep before I got to it: it failed on its **first** run, on `CurrencyMismatch`, which nothing had mapped. That is a defect the Builder did not know about, found by a test written because a reviewer complained about a *different* list. |
| F-5.2 | Yes. `NOT_NEGATIVE` and `LOWEST_PROBABILITY` now mean what they say, and the bare `@Min(0)` is gone with them. |
| F-5.3 | Yes. |
| F-5.4 | Yes, **by deletion**, which was the better of the two options I offered. An endpoint that existed because it sounded like one should is the cheapest thing in any codebase to remove and the most expensive to keep. |
| F-5.5 | Yes. D-18 is a real decision: it says what the rule protects, why creating differs from changing, and names the two changes that would reverse it. |
| F-5.6 | Yes, and the third assertion is the one I wanted without asking for it — the 500 body must not contain the deal id that appeared in the exception's own message. |
| Build | `mvn clean install`, six modules, green. 125/125 and 88/88 mutants. |

## Verdict

Controllers that hold no rules, a fitness function that makes it structurally impossible for
them to acquire any, and a translation table that can no longer fall behind the code. The
pattern of this stage is worth naming: **two hand-maintained lists, two completeness tests,
and the second one found a real defect the first day it ran.**

**STAGE 5 APPROVED**
