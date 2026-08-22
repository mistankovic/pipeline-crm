# Stage 7 hand-off — QA procedures and end-to-end hardening

## The QA procedures

`docs/qa/procedures.md` — **31 numbered procedures in six sections**, written to be followed by
a person with a browser and no access to the code. Each says what to do and what you must see.
"You must see" is literal: if the screen does something else, *including doing nothing*, the
procedure has failed. Two of this project's worst defects were the application quietly doing
nothing, so silence is a failure condition rather than a neutral outcome.

Every procedure is also scripted, in `frontend/qa/`, one spec per section and one test per
numbered step. The document says which is authoritative: **the document**.

| Section | What it covers | # |
|---------|----------------|--:|
| QA-1 Signing in | correct, wrong password, unknown address (identical message), reload, sign out, tampered token | 6 |
| QA-2 Companies and contacts | create, blank name, filter, malformed email, contact timeline | 5 |
| QA-3 The pipeline board | new deal and its weighted value, drag one stage, skipping refused, offered moves, losing forces 0%, owner filter | 6 |
| QA-4 Winning a deal | refused without evidence, allowed with a meeting, won deal frozen, timeline order and attribution | 4 |
| QA-5 Who may move a deal | rival not invited and told why, owner may, manager may, rival may still read | 4 |
| QA-6 Revising and forecasting | reprice, reweight, by owner, by stage, a lost deal leaves the forecast, two currencies | 6 |

## They pass, and the run is recorded

`docs/qa/runs/2026-08-22.md`. **31 of 31**, in 35.6 s, against PostgreSQL 16.13, the packaged
jar and the Vite server, in a headless Chromium, on a database created fresh for the run.

## They can fail

A passing suite proves nothing about tests that cannot fail, so the suite was sabotaged:
`DealCard` was set back to `draggable="true"` — the Stage 6 defect, reintroduced — and QA-5.1
went red. Reverted.

## What the first QA run found

Two procedures failed initially. Neither was flaky; both said something true.

**A business rule had leaked into the web layer.** `NewContactRequest` carried `@Email`, so a
malformed address was rejected by Jakarta validation before the domain saw it, and the user got
a framework's sentence instead of the domain's. Two definitions of "email address", in two
layers, free to disagree — precisely what Constitution §2.2 forbids. The annotation is gone:
presence and length are shape, validity is the domain's business. The API test now pins the
domain's wording as part of the contract.

**A procedure only passed on an empty database**, then only passed by luck of timing. Both were
the procedure's fault and both are written up in the run record, because a QA procedure that
passes for the wrong reason is the same defect as a unit test that cannot fail.

The fix for the second is worth naming: the forecast heading is now rendered **from the
response** and carries `data-testid="forecast-grouping"`, so a script waits for the server's
answer rather than for the dropdown it just changed. *A script that waits for its own input is
not waiting for anything.*

## Hardening: CRAP driven to the target, not just the limit

The Constitution sets the CRAP **limit** at 6 and the **target** at 4. One method sat at 5.00:
`DealStage.allowsTransitionTo`, three conditionals about closed-ness, losing from anywhere, and
next-in-line. The pipeline was inferable from the method body rather than visible.

It is now a table:

```java
private static final Map<DealStage, Set<DealStage>> ALLOWED_NEXT = Map.of(
        LEAD,        Set.of(QUALIFIED,   CLOSED_LOST),
        QUALIFIED,   Set.of(PROPOSAL,    CLOSED_LOST),
        PROPOSAL,    Set.of(NEGOTIATION, CLOSED_LOST),
        NEGOTIATION, Set.of(CLOSED_WON,  CLOSED_LOST),
        CLOSED_WON,  Set.of(),
        CLOSED_LOST, Set.of());
```

The whole state machine reads in one glance, and the method is complexity 2.

**Every method in `domain` and `application` is now at CRAP ≤ 4.** Not one is between 4 and 6.

## Final metrics

| Metric | `domain` | `application` | Gate |
|--------|---------:|--------------:|------|
| Line coverage | **100.0 %** | **100.0 %** | ≥ 95 % |
| Branch coverage | **100.0 %** | **100.0 %** | ≥ 95 % |
| Mutation score | **124/124 (100 %)** | **88/88 (100 %)** | ≥ 90 % |
| Worst CRAP | **4.00** | **2.00** | ≤ 6, target 4 |
| Methods analysed | 128 | 127 | — |
| CPD duplicates @ 30 tokens | 0 | 0 | 0 |
| Checkstyle (main **and** test) | 0 | 0 | 0 |

Architecture: **13 ArchUnit rules**, 0 violations, including the guard that refuses to evaluate
them against a stale artefact.

Tests: `domain` 254 · `application` 151 (79 unit + **72 Gherkin scenarios**) ·
`adapter-persistence` 43 against a real PostgreSQL · `adapter-web` 28 ·
`bootstrap` 46 (13 architecture, 5 wiring, 2 port completeness, 26 API over HTTP) ·
`frontend` 26 unit · **31 QA procedures**.

`docs/mutation-survivors.md`: no survivors to justify, with the history of the ones there were.

## Deliberately not done

* Docker compose, the final README and the packaging — Stage 8.
* The QA procedures cover the flows the requirement names. They are not exhaustive; they are the
  set a reviewer can run in half an hour and believe.

---

# Stage 7 hand-off — round 2 (response to review)

| Finding | Disposition |
|---------|-------------|
| F-7.1 every server error reported as **401** | **Fixed, both ends.** `SecurityConfiguration` permits the `ERROR` dispatcher type, so the container's re-dispatch to `/error` is no longer treated as an anonymous request. And `ApiExceptionHandler` gained a catch-all, so nothing reaches the container to begin with. Verified against the running app: `GET /error` with a token is now **500**, an anonymous `GET /api/deals` is still **401**. Three tests in `ServerFaultApiTest` — via a controller that exists only to fail, because provoking a genuine one means finding a bug and the point is that there is not one to find. |
| F-7.2 the database decided how much a deal could be worth | **Fixed in the domain.** `Money.LARGEST` is 999,999,999,999.99, checked **after** rounding so an amount that rounds *up* past the limit is over it. Verified live: the request that produced `SQLState 22003 numeric field overflow` now returns `400 {"error":"InvariantViolation","message":"amount must not exceed 999999999999.99, was 99999999999999999999.00"}`. Four new domain tests including the rounding boundary. |
| F-7.3 no procedure covered a server error | **Fixed.** New section **QA-7 — When things go wrong**, three procedures. 7.1 is the one that matters: an absurd value must produce a message **and leave you signed in**. |
| F-7.4 injection was checked but not recorded | **Fixed.** QA-7.2 stores `Bobby'); DROP TABLE deals;--` and then asserts the board still works — the table still existing is the actual assertion. QA-7.3 does the same for markup, checking that nothing executed. An API test pins both server-side. |
| F-7.5 procedures reference a compose file that does not exist | **Deferred to Stage 8**, as the review allows. The compose file lands there and the precondition becomes true. |

## The interaction worth remembering

F-7.1 is the only defect in this project produced by two *correct* decisions meeting:

* Stage 6 made the browser sign out on any 401 — right, and I would do it again.
* Stage 2 authenticated every request that was not explicitly public — right, and the error
  dispatch was never thought about.

Together they meant a numeric overflow logged the user out. Neither code review nor any gate
would have found it, because neither piece is wrong. Only sending a hostile request to a
running system did.

## Final metrics (round 2)

| Metric | `domain` | `application` | Gate |
|--------|---------:|--------------:|------|
| Line / branch coverage | **100 % / 100 %** | **100 % / 100 %** | ≥ 95 % |
| Mutation score | **124/124** | **88/88** | ≥ 90 % |
| Worst CRAP | **4.00** | **2.00** | ≤ 6, target 4 |
| Methods | 129 | 127 | — |
| CPD @ 30 tokens · Checkstyle | 0 · 0 | 0 · 0 | 0 |

**34 of 34 QA procedures pass**, recorded in `docs/qa/runs/2026-08-22.md`.

---

# Stage 7 hand-off — round 3 (response to review round 2)

| Finding | Disposition |
|---------|-------------|
| F-7.6 the catch-all flattened five client errors into 500 | **Fixed.** `ApiExceptionHandler` extends `ResponseEntityExceptionHandler`, so the failures Spring already classifies keep their statuses, and `createResponseEntity` is overridden to re-body them into this project's `{error, message}` shape — one format for every failure. The catch-all stays, for what is genuinely unexpected. Five new tests, one per status. |

Verified against the running application, every case:

```
malformed JSON body : 400        unknown route        : 404
wrong content type  : 415        genuine server fault : 500
bad uuid in path    : 400        anonymous request    : 401
method not allowed  : 405
```

And the bodies keep one shape: `{"error":"BAD_REQUEST","message":"Bad Request"}`,
`{"error":"NOT_FOUND","message":"Not Found"}`.

## What I got wrong, and what it cost

The F-7.1 fix was a one-line catch-all, and it was too broad by exactly one line. It fixed the
finding and broke five things the reviewer had not asked about — a strictly worse API than the
one being complained about, since a wrong status on *every integration attempt* beats a wrong
status on a rare server fault.

The lesson is not "be more careful". It is that **I had no test asserting what a bad request
returns**, so nothing objected. There are eight now, and the tests are the reason a third
version of this handler cannot quietly flatten them again.

## Final state

| Metric | `domain` | `application` |
|--------|---------:|--------------:|
| Line / branch coverage | 100 % / 100 % | 100 % / 100 % |
| Mutation score | 124/124 | 88/88 |
| Worst CRAP | 4.00 | 2.00 |

**34 of 34 QA procedures pass.** `mvn clean install` green across six modules.
