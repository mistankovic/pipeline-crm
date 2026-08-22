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
