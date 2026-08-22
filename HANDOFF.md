# Handoff — pick this up cold

You are continuing PipelineCRM. This file is the state of the work, written so you do not
have to reconstruct it from the transcript. Read this, then `CONSTITUTION.md`, then work.

**Status: Stages 0–8 complete. Every gate approved. The build is green and pushed.**
Nothing is half-finished. If you have been handed new work, section 7 tells you how to do it
here without breaking what exists.

---

## 1. What this project is

A sales-pipeline CRM demonstrating Clean Architecture, Clean Code and tool-driven test
discipline, built through a **nine-stage gated workflow** by two roles played by one agent:

- **BUILDER** — implements a stage, then writes a hand-off package.
- **ADVERSARIAL REVIEWER** — reviews it, genuinely hostile. *"Polite rubber-stamping is
  forbidden."* The next stage may not begin until the Reviewer writes literally
  **`STAGE X APPROVED`**.

Both roles are you. Play them honestly — the Reviewer's job is to find what the Builder
missed, and it has done so at every single gate. Every finding and its fix is written down in
`docs/reviews/`.

Standing rules from the original brief, still binding:

- Never skip a review gate.
- Document every review finding and the subsequent fix.
- When in doubt, add a test.
- After every significant change, re-run the relevant quality tools.

## 2. Where the work lives

| Thing | Path |
|---|---|
| Binding rules — read before changing anything | `CONSTITUTION.md` |
| How to run and test everything, final metrics | `README.md` |
| Every stage hand-off and review, all 67 findings | `docs/reviews/` |
| Domain decisions D-1 … D-22, each with "what would change it" | `docs/domain-decisions.md` |
| 40 human-followable QA procedures | `docs/qa/procedures.md` |
| Recorded QA runs | `docs/qa/runs/` |
| Mutation survivor history | `docs/mutation-survivors.md` |

**Repository:** `mistankovic/pipeline-crm` (renamed from `uncle-bob-agentic-principles`).
**Branches:** this work is on `main` and on `claude/pipelinecrm-clean-demo-13nqf4`;
`grok-pipeline-crm` is a separate implementation of the same brief by another agent — do not
touch it.

> PR #1 was closed without merging when the repo was repurposed to host two implementations.
> The user then asked for this work on `main` directly, which is what the original brief said
> anyway. `main` had been force-pushed to an unrelated signpost commit, so this history was
> **merged** into it rather than force-pushed over it: that commit's `.gitignore` entries and
> its pointer to `grok-pipeline-crm` are both preserved. Do not force-push `main`.

## 3. Stage checklist — all closed

| Stage | What it delivered | Findings | Gate |
|---|---|---|---|
| 0 | Constitution, module skeleton, every quality gate wired to the build | 11 | ✅ APPROVED |
| 1 | Domain: entities, value objects, the stage machine | 9 | ✅ APPROVED |
| 2 | Architecture fitness functions, dependency rule enforced 3 ways | 8 | ✅ APPROVED |
| 3 | Use cases behind input/output ports + Gherkin acceptance suite | 7 | ✅ APPROVED |
| 4 | Persistence adapter against real PostgreSQL, Flyway, bcrypt | 7 | ✅ APPROVED |
| 5 | REST API, JWT security, HTTP error translation | 6 | ✅ APPROVED |
| 6 | Svelte 5 frontend that holds no business rules | 5 | ✅ APPROVED |
| 7 | QA procedures a person can follow, and that can fail | 6 | ✅ APPROVED |
| 8 | Docker/compose, run script, README, final metrics | 9 | ✅ APPROVED |

**68 findings total.** Stage 8 needed two rounds: round 1 blocked because re-reading the
original brief revealed company/contact editing had never been implemented *or* declared out
of scope. It was implemented, not waived.

## 4. Baseline you must not regress

Measured on `13a5b6d`. If a change moves any of these the wrong way, that is a defect.

| Module | Line | Branch | Mutation | Methods | Worst CRAP |
|---|---|---|---|---|---|
| `domain` | 100.00 % (305/305) | 100.00 % (69/69) | **126/126** | 131 | 4.00 |
| `application` | 100.00 % (297/297) | 100.00 % (16/16) | **96/96** | 136 | 2.00 |

**Zero surviving mutants out of 222.** Gates *require* 95 % coverage / 90 % mutation / CRAP ≤ 6;
the project actually sits at 100 % / 100 % / ≤ 4. New code gets the achieved bar, not the
required one — that was an explicit Stage 8 review ruling.

Tests: 273 domain · 171 application (incl. **83 Gherkin scenarios**, 10 features) · 47
persistence · 28 web · 81 bootstrap (13 ArchUnit rules) = **600 backend**. Plus 26 frontend
unit tests and **40 QA procedures**. `svelte-check`: 322 files, 0 errors.

## 5. Environment — the traps, all hit at least once

This environment is unusual. Every item below cost real time to discover.

**Container images cannot be pulled.** Every registry returns 403. So:
- `docker compose up` **has never been run** and cannot be. Say so honestly; do not claim it works.
- Testcontainers is unusable. Use the env-provided-database escape hatch instead:
  ```bash
  export PIPELINECRM_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5433/pipelinecrm_test
  export PIPELINECRM_TEST_DB_USER=pipelinecrm
  export PIPELINECRM_TEST_DB_PASSWORD=pipelinecrm
  ```
  A local PostgreSQL 16 runs on **port 5433** (not 5432). `scripts/run-locally.sh` starts it.

**Playwright's pinned browser (build 1234) is absent.** Build 1194 is installed. Always:
```bash
PLAYWRIGHT_CHROMIUM_PATH=/opt/pw-browsers/chromium npm run test:qa
```
`/opt/pw-browsers/chromium` is a **symlink to the binary**, not a directory. Without this every
procedure fails at launch — which is not a QA result. This is documented in the procedures now.

**Maven:**
- `mvn -f backend/pom.xml clean verify` takes **~2.5 minutes**. Run it with
  `run_in_background: true` and poll, or you will hit tool timeouts.
- **`-am` is mandatory** when running a subset. `AnalysedCodeTest` deliberately fails if it
  finds itself analysing already-installed jars instead of reactor output — that guard is
  finding F-2.1 and it works.
- But `mvn test -pl bootstrap -am` **breaks Spring API tests**: without reaching `package`,
  Maven puts adapter-persistence's *unfiltered* `target/test-classes` on the classpath and
  `PersistenceTestApplication` clashes with `CompositionRoot` over the `passwordEncoder` bean.
  To run bootstrap API tests, `install -DskipTests` first, then run **without** `-am`.
- The surefire property is `surefire.failIfNoSpecifiedTests`, **not** `failIfNoSpecifiedTests`.

**`/tmp` can be cleared mid-session.** It happened here: it deleted a build log being written
and, worse, `/tmp/pipelinecrm/pg.log`, which PostgreSQL needs to exist and be writable by the
`postgres` user. Keep logs you care about in the scratchpad directory, not `/tmp`.

**Shell:**
- `cd` **persists between Bash tool calls.** This caused several "could not find the selected
  project in the reactor" errors. Use absolute paths or `mvn -f`.
- `cd X && python3 <<EOF` silently skips the heredoc if the `cd` fails.
- `curl` needs `--noproxy '*'` for localhost.
- **Do not `pkill -f "<string>"`** — it matches your own shell's command line and kills the
  tool call (exit 144). Find the listener by port instead.
- Python heredocs break on Java text blocks (nested `"""`). Write the Java snippet to a file
  and splice it in.

## 6. Running it

```bash
scripts/run-locally.sh          # Postgres + API (:8080) + Vite (:5173). Exits in ~15s.
scripts/run-locally.sh --stop   # stops all three, leaves no stragglers
```
Refuses to start if something already holds 8080 or 5173 — that guard exists because the
script once reported a healthy stack it had never started (F-8.1).

Demo users (deliberately public): `sam@` / `robin@` / `mo@pipelinecrm.demo`, passwords
`sam-password` / `robin-password` / `mo-password`. Mo is the MANAGER.

Every test command is in `README.md` under "How to run the tests" and **each one has been
executed exactly as written**. Do not trust a command that has not been.

## 7. If you are given new work

Follow the same protocol. It is the point of the project, not ceremony.

1. **BUILDER**: implement it. Domain rules go in `domain` or `application` — never in a
   controller, never in the frontend. Write the tests as you go.
2. Re-run the gates: `mvn -f backend/pom.xml clean verify`, then `npm run check`,
   `npm run test:unit`, and the QA suite against a running stack.
3. **Watch your new checks fail.** Deliberately break the thing and confirm the test goes red,
   then restore. This project's three worst defects were all checks that could not fail. A
   check nobody has seen fail is not a check.
4. Write `docs/reviews/stage-N-handoff.md`, including a section naming what you want attacked.
5. **ADVERSARIAL REVIEWER**: re-derive every claimed number from the reports rather than
   believing the hand-off. Write `docs/reviews/stage-N-review.md`. Only write
   `STAGE N APPROVED` if it genuinely deserves it.
6. If you find problems: fix, resubmit as round 2, review again. Stage 8 took two rounds;
   Stages 5, 6 and 7 took two or three.
7. Commit with a message that says what was found, not just what was added. Push to the
   designated branch and update PR #1.

## 8. Deliberately not done — do not "fix" these

Each is a recorded decision with reasoning and a stated trigger that would reverse it.
Implementing one without that trigger is scope creep.

- **No deletion** of anything — companies, contacts, deals or activities (D-22, round-1 review).
- **Activities are immutable**; the timeline is append-only. The win rule depends on evidence
  that cannot be rewritten afterwards (**D-20**).
- **A contact cannot change employer** — correcting a name is not the same event as moving a
  person between companies (**D-21**). A test proves `companyId` in the PATCH body is ignored.
- **No authority rule on companies/contacts** — they are shared reference data with no owner,
  so there is no rival's number to protect (**D-22**). Both endpoints still require a session.
- **No user provisioning, multi-tenancy, refresh tokens, password reset, i18n, or FX
  conversion** (`CONSTITUTION.md` §6).
- **No CORS configuration** — the browser and API share an origin by design (**D-19**).
- **JWT is demo grade.** §6 states the rule (strength follows secret length, 32 chars minimum)
  rather than naming an algorithm, because naming one is how that sentence was wrong until
  Stage 8 (F-8.7).

## 9. Habits that actually caught things

Not advice in the abstract — each of these found a real defect here.

- **Re-read the original brief at the end.** Eight gates passed over a missing requirement
  because every stage reviewed its own hand-off and none re-read the spec (F-8.6).
- **Re-derive numbers, never recall them.** Three README figures were wrong when checked
  against the reports.
- **Run the documented command verbatim.** Three of four "run one suite" commands did not work.
- **Start from a genuinely cold shell.** The QA suite could not be run from its own
  instructions; an env var set in an earlier shell had been hiding it.
- **Sabotage the thing and watch the check go red.** The only technique that has caught every
  "passed without checking anything" defect. There are three in the record.
