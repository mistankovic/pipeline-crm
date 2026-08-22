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
