# Stage 7 — Adversarial Review

STAGE 7 APPROVED

Reviewed on branch `local-grok-pipeline-crm` at `f6747cb` (`docs: add UI QA procedures and keep cucumber XML gate off PIT`; working tree clean aside from this report). Scope: Stage 7 contract (detailed QA procedures for the real Svelte UI, demonstrate they pass, PIT 100%, JaCoCo ≥ 95%, CRAP ≤ 6, no architectural violations), `docs/qa/ui-procedures.md`, cucumber JUnit XML gate phase move in `application/pom.xml`, inner-layer quality gates at HEAD. Builder claims: `docs/reviews/stage-07-builder-handoff.md`. Did not inspect other git branches.

Commands / probes (JDK 21 via `/Users/mislav/Library/Java/JavaVirtualMachines/jdk-21.0.11+10/Contents/Home`):

- `./mvnw -B -Pmutation -pl domain,application -am verify` — BUILD SUCCESS
- `./mvnw -B -pl application -am test` — BUILD SUCCESS (cucumber XML gate on a non-PIT test phase)
- `jdeps --multi-release 21 -s domain/target/domain-0.1.0-SNAPSHOT.jar` — `java.base` only
- `jdeps --multi-release 21 -s -cp domain/target/domain-0.1.0-SNAPSHOT.jar application/target/application-0.1.0-SNAPSHOT.jar` — `java.base` + domain
- Grep of `domain/src/main` and `application/src/main` — no Spring / Jakarta / Hibernate / Jackson / Lombok
- Grep of `frontend/src` for `canTransitionTo`, `qualifiesCloseWon`, `passwordHash` — none; forecast caption describes `value × probability / 100` and does not compute it
- Isolated Chrome context `stage7-qa` against live Vite `127.0.0.1:5173` → API `:8080` → Postgres 16 (`pipelinecrm-pg`). Procedures P1–P6 executed against the real Svelte UI.

Inner-layer numbers from this mutation verify (match the hand-off):

| Metric | Target | This run |
| --- | --- | --- |
| Domain line / branch | ≥ 95% | 334/335 line (99.7%), 91/92 branch (98.9%) |
| Application line / branch | ≥ 95% | 134/134 line (100%), 29/30 branch (96.7%) |
| PIT domain | 100% | 140/140 killed |
| PIT application | 100% | 54/54 killed |
| CRAP | ≤ 6 | domain 137 methods / 0; application 54 / 0 |

Cucumber JUnit XML gate ran in the `test` phase **before** PIT (`exec:java (cucumber-junit-xml-gate)` with empty stderr, then `pitest:mutationCoverage`). Application Surefire **Tests run: 66** including `CucumberTest` **48**. A subsequent `./mvnw -B -pl application -am test` rewrote `application/target/cucumber-junit.xml` to **48** `<testcase>` / **0** failures and the gate succeeded again.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| Detailed QA procedures in `docs/qa/` for the real Svelte UI | **MET, thin** | `docs/qa/ui-procedures.md` is six procedures (P1–P6) plus a one-paragraph pass log. Steps map to real routes, labels, and API error strings. Not a script, not screenshots, no Pass column despite the intro claiming one. See Important 1 and residuals. |
| Cover login | **MET** | P1: `/#/login`, wrong password, sales login → `#/board`, anonymous `#/board` → login. Live: bad password prints `user` and stays on login; good password navigates to board; isolated `stage7-anon` `#/board` landed on `#/login`. |
| Cover board DnD / stage | **MET** | P3 create + owner filter; P4 skip / legal move. Live HTML5 drag of `Sales pipeline` LEAD → NEGOTIATION heading: `cannot move from LEAD to NEGOTIATION`, card stayed in LEAD. Drag to QUALIFIED: card regrouped under QUALIFIED. |
| Cover closed-won guards | **MET** | P4.1 owner-or-manager; P4.4 Call/Meeting; P4.5 win + probability 100. Live: Sales on Manager-owned `Acme expansion` → `only the deal owner or a manager may change deal stage`, still LEAD. Drag QUALIFIED → CLOSED WON without meeting → `closed-won requires a call or meeting on the deal`. After MEETING, `CLOSED_WON · 1000.00 USD · 100%`. |
| Cover forecast | **MET** | P6: Forecast page copies API buckets; closed deals drop out; empty open stages at 0.00. Live `GET /api/forecast`: owner Manager `250.00` (open `Acme expansion` 1000×25/100); stages LEAD 250.00 + QUALIFIED/PROPOSAL/NEGOTIATION 0.00; no CLOSED_* rows. UI matched. |
| Cover CRUD | **MET** | P2 company create + Rename; contact create + Edit; P3 deal create; P5 deal title Save + NOTE/MEETING. Live: second Acme row then rename to `Acme QA7`; second Pat Lee then Edit → `Pat Lee QA7` / `pat-qa7@acme.com` via `PUT /api/contacts/{id}`; deal title `Sales pipeline QA7` after Save deal. Activities append-only (no PUT to invent). |
| Demonstrate procedures pass | **MET (reviewer re-ran)** | Builder pass log is one sentence. This review re-executed P1–P6 on the live stack; all steps that the UI can perform **PASS**. |
| Mutation 100% | **MET** | PIT domain 140/140, application 54/54. `failWhenNoMutations=true`, `mutationThreshold=100`. |
| Coverage ≥ 95% line/branch | **MET** | JaCoCo inner-bundle check green. Domain 99.7%/98.9%, application 100%/96.7%. |
| CRAP ≤ 6 | **MET** | Both inner modules 0 violations. |
| No architectural violations | **MET** | Inner-layer ArchUnit ran in this mutation verify. `jdeps` domain → `java.base`; application → `java.base` + domain. No Spring/JPA/Jackson/Lombok in inner `src/main`. Frontend still public `/api` only. |
| Cucumber XML gate still works | **MET** | Bound to `test` (not `verify`) so PIT cannot fail the gate by overwriting `cucumber-junit.xml`. Mutation verify ran Surefire + gate first (48 scenarios), then PIT. Non-PIT `application -am test` gate green; XML 48/0. |

---

## Are the procedures followable through the real Svelte UI?

**Mostly yes**, if the operator already has companies/deals from Stage 6 (this database did) or is willing to invent a Manager-owned deal. Mapping from the document to the UI:

| Procedure text | Actual UI | Followable? |
| --- | --- | --- |
| Open `/#/login` | Hash router `App.svelte`; Vite `/` | Yes |
| Wrong password → API error | `p.error` shows `user` (404 `NotFoundException`) | Yes; copy is ugly, not missing |
| sales@… / password → `#/board` | Login stores JWT, `location.hash = '#/board'` | Yes |
| Private window `#/board` → login | `onHash` redirects if no token | Yes (isolated context) |
| Companies → create `Acme` → Rename | Nav **Companies**, Create, **Rename** (`prompt`) | Yes; names are not unique so re-runs duplicate rows |
| Contacts → `Pat Lee` / `pat@acme.com` → Edit | Nav **Contacts**, company select, Create, **Edit** (`prompt` ×2) | Yes; table still has no company column |
| Add deal title `Sales pipeline`, owner **Sales**, 1000.00 / 25 | Board form; owner select already **Sales** for a sales session | Yes; second card with the same title if one already exists |
| Owner filter → Sales | Combobox **Owner filter** / option **Sales** | Yes |
| Click `CLOSED_WON` (or drag) | Stage **buttons live on deal detail**, not the board. Board is HTML5 DnD onto `section[aria-label=…]`. Column heading is **CLOSED WON** (spaces). | Yes if the tester opens the deal **or** drags. “Click on the board” would miss. |
| Error “only owner or manager may change stage” | Actual: `only the deal owner or a manager may change deal stage` | Close enough |
| Skip to NEGOTIATION → conflict | `cannot move from LEAD to NEGOTIATION` | Yes |
| Closed-won without Call/Meeting | Exact server string | Yes |
| Forecast `value × probability / 100` as returned by the API | Caption + tables copy `row.amount` | Yes; independent arithmetic is optional (see residual) |
| Save | Button is **Save deal** | Yes |

Demo users in the table match `DemoDataSeeder` (`sales@pipelinecrm.demo` / `manager@pipelinecrm.demo` / `password`). Start line (Postgres 5432, API 8080, `npm run dev` 5173, Vite `/api` proxy) matches the running stack. README still defers “Running the app” to Stage 8; a new tester has to already know how to boot Boot + Vite.

---

## Attack results

| Attack | Result |
| --- | --- |
| Procedures that cannot be executed on the Svelte UI | **Miss on the happy path.** Every P1–P6 action exists as a real control (login form, nav, Create/Rename/Edit, board form, HTML5 DnD, deal-detail stage buttons, activity Record, Forecast tables). |
| Missing login / board / DnD / closed-won / forecast / CRUD | **Miss.** All six themes are present and passed live. Contact edit is in P2 (the Stage 6 hole). |
| Procedures smuggle §7 into the browser | **Miss.** They tell the tester to expect **server** errors and API forecast numbers. Frontend still POSTs illegal stages. |
| Quality gates quietly dropped | **Miss.** Mutation verify was green: PIT 140/140 + 54/54, JaCoCo minima, CRAP 0, ArchUnit, cucumber XML gate. |
| Cucumber XML gate broken by the PIT workaround | **Miss on the build.** Phase `test` runs after Surefire and before PIT. Mutation verify: gate then PIT. Non-PIT test: 48/0. PIT **does** still smash the file afterwards (see residual). |
| Inner-layer framework leakage | **Miss.** `jdeps` + grep + ArchUnit in this run. |
| Forecast math in the UI | **Miss.** Live UI amounts === `GET /api/forecast`. Closed sales deals vanished from owner buckets (Sales row gone; only Manager 250.00). |
| Pass log is fiction | **Miss this run.** Re-execution passed. The log itself is still not evidence (one sentence, no per-step table). |

---

## Leakage attack

- `frontend/src`: no `canTransitionTo`, no `qualifiesCloseWon`, no `passwordHash`. `STAGES` is a column/button list. Illegal drops still 409 from the server.
- Forecast caption describes the formula; Svelte does not `reduce` deals. P6 correctly says “as returned by the API”.
- Inner production code: no Spring / Jakarta / Hibernate / Jackson / Lombok. Domain jar → `java.base`. Application jar → `java.base` + domain.
- Cucumber still hits use cases, not the UI (`CucumberTest` glue `com.pipelinecrm.application.acceptance`). Stage 7 did not relocate acceptance tests into the browser.

Do not “fix” 409/403 by encoding the stage machine in Svelte. The QA doc is right to treat the server as authority.

---

## Critical

None.

---

## Important

### 1. P4.1 is not executable from the documented empty-database start state

**Severity:** Important
**Confidence:** 90

The procedures’ start state is: seeded **users only** (`DemoDataSeeder` writes Manager + Sales and returns). There is no seeded company, contact, or deal.

Sequential execution as written:

1. P2 creates company `Acme`.
2. P3 creates a **Sales**-owned deal and **leaves the owner filter on Sales**.
3. P4.1: “Logged in as Sales, open a **Manager**-owned deal and click `CLOSED_WON`”.

Nothing in P1–P3 creates a Manager-owned deal. The seeder does not. After P3 the board, if the filter is followed, does not even **show** Manager cards. A first-time tester on an empty DB cannot perform P4.1 without inventing a setup step (create a deal with owner **Manager**, then clear the filter). If they try P4.1 on their own P3 deal they get the closed-won/meeting 409 instead of the owner-or-manager 403 and will think the expected error is wrong.

This review could run P4.1 only because Stage 6 leftover `Acme expansion` (LEAD, Manager) was still in Postgres. That is not a procedure; it is ambient state. The pass log does not mention it.

**Not a Stage 7 reject.** The control exists, the server rule is enforced, and a tester who already has mixed-owner data (or who improvises a Manager deal from the same board form) can complete the step. The QA document still claims to start from an empty database and then requires a row that empty-database seed does not provide. Fix: add an explicit setup step (create a Manager-owned LEAD, reset owner filter to All) **or** seed a manager deal. Do not encode the authorization rule in Svelte to make the step “easier”.

---

## Residual minors (do not by themselves block)

1. **Intro mentions a Pass column that does not exist.** “each step’s **Pass** column holds” — the markdown is a numbered list, not a table. The “Pass log” is one paragraph dated 2026-08-24. Weak demonstration; this review is the actual evidence.

2. **Hardcoded entity names are not idempotent.** Re-running P2/P3 on this DB created a second `Acme`, a second `Pat Lee`, and a second `Sales pipeline` (one already CLOSED_WON from Stage 6). Rename/Edit then have two identical rows. Fine for a demo; bad as a repeatable script.

3. **P3 → P4 sequencing.** P3 ends on owner filter = Sales. P4.1 needs a Manager card. The doc never says to set the filter back to All. Human-obvious, still omitted.

4. **P6 does not tell the tester to independently multiply `value × probability / 100`.** “As returned by the API” always passes if the table renders JSON. Live check: 1000.00 × 25/100 = 250.00 matched both API and UI. Keep the math on the server; add one arithmetic example so a human can catch a wrong API.

5. **PIT still overwrites `application/target/cucumber-junit.xml`.** After mutation verify the file was **1** testcase / **1** failure (`NoSuchElementException` in a mutant forecast scenario). The gate no longer sees that because it runs in `test`. Inspecting the leftover file after `-Pmutation` is misleading. Pointing the Cucumber JUnit plugin at a PIT-safe path (or deleting the report in `mutationCoverage`) would be cleaner than relying on phase order. The gate itself is not broken.

6. **Board has no stage click targets.** P4’s “click `CLOSED_WON` (or drag)” is deal-detail buttons **or** DnD. Column labels are `CLOSED WON` not `CLOSED_WON`. Drag onto the heading worked in Chrome.

7. **Save control is “Save deal”, not “Save”.** P5. Followable.

8. **Carried Stage 6 UI residuals** (unchanged, not re-opened): contacts table omits company; `DealDetail` still `onMount`-only; company/contact edit is `window.prompt`; login error copy is `user`; activity timestamps are raw ISO; `vite preview` has no `/api` proxy; all six stage buttons stay enabled.

---

## What is already in good shape (do not rip out)

- QA procedures exercise the **public API through the Svelte UI**, not a second copy of §7. Illegal stage / win-guard / owner checks are expected as HTTP error text. Keep it that way.
- HTML5 DnD is real and is the board’s stage-change path. Live skip and closed-won-without-meeting both 409’d and left the card in place.
- Cucumber XML gate on `test` is the correct workaround for PIT clobbering `junit:target/cucumber-junit.xml`. Do not move it back to `verify` without a separate report path.
- Inner-layer PIT 100%, JaCoCo ≥ 95%, CRAP 0. Stage 7 did not need production Java changes to hold those numbers.

Stage 8 may start. Important 1 is a documentation fix, not a waiver of owner-or-manager coverage (that coverage exists and passed on leftover data).
