# Stage 7 — Adversarial Review (round 1)

The brief for this stage says: act as hostile QA, try to break the system, demand surviving
mutants be killed, insist on clean metrics. The metrics are the best in the project and I will
say so. Then I went looking for the things 31 green procedures do not cover.

## Verified

* **31 of 31 procedures pass**, on a fresh database, against the real stack. I re-ran them.
* **They can fail.** The Builder reintroduced the Stage 6 drag defect and QA-5.1 went red. This
  is now the fourth stage where a "can it fail?" check was done without me asking, which is the
  habit I have been trying to install since Stage 3.
* **The procedures are genuinely followable by a person.** I read them as if I had never seen
  the code. The preconditions are stated, the passwords are given, the expectations are
  concrete ("a red banner containing *not an email address*"), and the document says out loud
  that silence is a failure. That last sentence is worth more than the other thirty.
* **CRAP at 4.00 across the board**, and the refactor that got it there made
  `DealStage` *better* rather than merely smaller: the pipeline is now a table you can read.
* **100 % line, 100 % branch, 124/124 and 88/88 mutants**, 13 architecture rules, 0 duplicates.

Now the two things I broke.

## F-7.1 — BLOCKER. Every unhandled server error is reported to the client as **401**.

```
GET /error, anonymous     -> 401
GET /error, with a token  -> 500
POST /api/deals with an oversized value, with a valid token:
HTTP/1.1 401
```

The last line is the defect. A valid, authenticated request hit a server fault, and the client
was told **"you are not authenticated"**.

The mechanism: an unhandled exception leaves the dispatcher, the container re-dispatches to
`/error`, and that dispatch is anonymous. `SecurityConfiguration` permits only `/api/sessions`
and the health endpoint, so `anyRequest().authenticated()` catches the error dispatch and the
`HttpStatusEntryPoint(401)` answers it. Every 500 in this application wears a 401's clothes.

This is bad on its own and worse in combination with the fix I demanded in Stage 6:
**`Failures` signs the user out on any 401.** So a data error — a value too large, a bug in a
mapper, anything the handler does not know about — now silently logs the user out and returns
them to the sign-in screen. The user will report "it keeps logging me out"; the cause will be a
numeric overflow. I asked for that sign-out and I would ask again; the fault is here.

**Required:** the error dispatch must not be authenticated, so a real status reaches the client.
And `ApiExceptionHandler` needs a catch-all so that nothing reaches the container in the first
place — it currently handles only `DomainException`, `ApplicationException`, `MalformedRequest`
and bean validation, and anything else escapes.

## F-7.2 — MAJOR. The largest a deal may be is decided by the database.

```
ERROR: numeric field overflow   (SQLState 22003)
```

`Money` accepts any non-negative `BigDecimal`. `NUMERIC(19,4)` accepts fifteen digits. So the
question "how much can a deal be worth?" is answered by a column definition, and answered by
throwing a `PSQLException` from three layers below the code that asked.

Constitution §2.2: business rules live in the domain. A maximum deal value is a business rule —
a modest one, but a real one — and right now the domain has no opinion and the persistence
schema has the only one. It is also the *last* layer to find out, which is why the failure is a
raw JDBC error rather than a sentence.

I do not much mind what the limit is. I mind that `Money` decides it, refuses above it with an
`InvariantViolation`, and that the schema and the domain agree by construction rather than by
coincidence.

## F-7.3 — MINOR. Nothing in the QA procedures covers a server error.

Both defects above were reachable in one HTTP call and neither is in `procedures.md`. The
procedures walk the happy paths and the *business* refusals thoroughly — which is where the
Builder's attention has been all project — and never ask what the application does when
something genuinely goes wrong. Given F-7.1, the answer was "logs you out".

Add a procedure. It does not need to provoke a database error through the UI; it needs to state
that an unexpected failure shows an error and **does not** end the session.

## F-7.4 — MINOR. SQL injection was tried and correctly ignored — and untested.

I created a company called `Bobby'); DROP TABLE deals;--`. It was stored verbatim, the deals
endpoint still worked, and it rendered as text. That is correct behaviour from JPA parameter
binding and Svelte's escaping, and neither is an accident.

But nothing in the suite records that this was checked, so nobody in future knows it was. One
QA procedure, or one API test, costs nothing and turns "we use an ORM" into evidence.

## F-7.5 — NIT. `docs/qa/procedures.md` cannot be run against the packaged demo yet.

The preconditions mention `docker compose up`, which does not exist until Stage 8. Right now a
reader who follows the document from the top cannot get to step 1. Either the compose file
arrives, or the precondition says how to start the pieces by hand today.

---

## Verdict

The QA work is the best-argued part of this project. The procedures are readable by a stranger,
they were proven able to fail, and the first run found a real rule leak in the web layer that
six stages of review had missed.

But hostile QA is the point of this stage, and thirty seconds of it produced a request that a
signed-in user can make and be logged out by. F-7.1 also actively defeats a fix from the
previous stage, which is exactly the kind of interaction only end-to-end poking finds.

**STAGE 7 NOT APPROVED.** Fix F-7.1 and F-7.2, and add the procedures in F-7.3 and F-7.4.
F-7.5 may be deferred to Stage 8 if the compose file lands there.

---

# Stage 7 — Adversarial Review (round 2)

| Finding | Verified |
|---------|----------|
| F-7.1 | Partly — see F-7.6. `GET /error` with a token is 500, an anonymous request is still 401, and `ServerFaultApiTest` pins all three cases. The *original* defect is gone. The fix introduced a new one. |
| F-7.2 | Yes. The exact request that produced `SQLState 22003` now returns `400 {"error":"InvariantViolation","message":"amount must not exceed 999999999999.99, was 99999999999999999999.00"}`. Checking **after** rounding, and testing the rounding boundary, is more careful than I asked for. |
| F-7.3, F-7.4 | Yes. 34 of 34 procedures pass. QA-7.2's real assertion — that the board still works after storing `DROP TABLE deals;--` — is the right way to write that test. |
| F-7.5 | Deferred to Stage 8 as agreed. |

## F-7.6 — BLOCKER. The catch-all turned every *client* error into a server error.

I kept poking after the fix:

```
malformed JSON body : 500      (should be 400)
wrong content type  : 500      (should be 415)
bad uuid in path    : 500      (should be 400)
DELETE /api/deals   : 500      (should be 405)
unknown route       : 500      (should be 404)
```

Every one of these is Spring telling the truth with a well-typed exception —
`HttpMessageNotReadableException`, `HttpMediaTypeNotSupportedException`,
`MethodArgumentTypeMismatchException`, `HttpRequestMethodNotSupportedException`,
`NoResourceFoundException`. The new `@ExceptionHandler(Exception.class)` catches all of them
first and answers `{"error":"InternalError","message":"the request could not be completed"}`.

So the API now blames itself for the caller's mistakes, and a developer integrating against it
is told "the request could not be completed" when they sent a typo in a URL. F-7.1 was fixed by
over-reaching: the backstop is correct in principle and must stop swallowing the exceptions
Spring already classifies.

**Required:** the handler extends `ResponseEntityExceptionHandler` (which maps all of the above
correctly) and keeps the catch-all for what is genuinely unexpected. And a test per status, so
the next backstop cannot flatten them again.

## Recorded so nobody re-raises it: appending one character to a token is not a forgery

`Authorization: Bearer <token>x` returns 200, and I nearly filed it as critical. It is not.

The HS384 signature is 48 bytes, written as exactly 64 base64url characters. A 65th character
contributes six bits — fewer than a byte — so the decoder discards it and recovers the identical
48 signature bytes. The attacker has presented *the same token*, spelled redundantly. Two extra
characters (`401`), a changed final character (`401`) and a tampered payload claiming
`role: MANAGER` (`401`) all fail, which is the property that matters.

Writing this down because it looks alarming, took ten minutes to disprove, and will look exactly
as alarming to the next person.

## Verdict

Both original findings are genuinely fixed, and F-7.2's fix is better than the one I asked for.
But the F-7.1 fix flattened five correct client-error statuses into 500, which is a worse API
than the one I complained about — a 401 on a server fault misleads about *whose* fault it is;
a 500 on a bad URL does the same in the other direction, on every integration attempt.

**STAGE 7 STILL NOT APPROVED.** Fix F-7.6.

---

# Stage 7 — Adversarial Review (round 3)

I re-ran every probe from round 2 against the running application:

```
malformed JSON body : 400   ✓        unknown route        : 404   ✓
wrong content type  : 415   ✓        genuine server fault : 500   ✓
bad uuid in path    : 400   ✓        anonymous request    : 401   ✓
method not allowed  : 405   ✓
```

Every status is the right one, and every body is the same shape — `{"error":…,"message":…}` —
including the ones Spring produced, which are re-bodied rather than left in Spring's default
format. A client can parse one thing.

34 of 34 QA procedures pass. `mvn clean install` green. 124/124 and 88/88 mutants. Worst CRAP
4.00 and 2.00.

## On the Builder's own post-mortem

The hand-off says the real problem was not carelessness but that *"I had no test asserting what
a bad request returns, so nothing objected"*. That is exactly right, and it is the same
sentence as F-3.1 and F-6.1 in a third costume: **the gap was never in the code, it was in
what nobody had asked the code to promise.** Three times now, in three different layers, and
each time the fix has been a test that makes the promise explicit rather than a rule about
being more careful.

## What this stage actually demonstrated

Every gate in this project was green, and hostile QA still found:

* a request a signed-in user could make and **be logged out by** (F-7.1),
* a business rule enforced only by a `NUMERIC(19,4)` column (F-7.2),
* five client errors reported as server errors (F-7.6, introduced by the F-7.1 fix).

None was reachable by coverage, mutation score, CRAP, ArchUnit or Checkstyle. All three needed
somebody to send a rude request to a running system and look at what came back. That is the
argument for this stage existing, and it is now made in evidence rather than in principle.

## Verdict

The procedures are followable by a stranger, proven able to fail, and they found a leaked rule
that six stages of review had missed. The hardening drove CRAP to the Constitution's *target*
across every method in both inner modules, not merely under its limit. And the error surface is
now honest about whose fault a failure is — which took three attempts and is worth all three.

**STAGE 7 APPROVED**
