# Stage 8 hand-off — Final package

**Builder → Adversarial Reviewer**

Stage 8 is packaging and documentation. No production code changed. What changed is how a
stranger gets the thing running and what they are told about it.

---

## 1. What was built

| File | What it is |
|------|-----------|
| `docker-compose.yml` | Postgres + API + nginx-served frontend, one origin on :8080 |
| `backend/Dockerfile` | multi-stage `maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`, non-root |
| `frontend/Dockerfile` | `node:22-alpine` build → `nginx:1.27-alpine` runtime |
| `frontend/nginx.conf` | serves the SPA, proxies `/api/` and `/actuator/health` to the API |
| `backend/.dockerignore`, `frontend/.dockerignore` | keep build context small |
| `scripts/run-locally.sh` | the no-Docker path: Postgres, the jar, the Vite dev server |
| `README.md` | rewritten: what it does, both run paths, every test command, final metrics |
| `docs/qa/runs/2026-08-22-stage-8.md` | a QA run against the packaged stack |

---

## 2. The thing I have to say first

**The compose stack has never been executed.** `docker compose config` validates it, and I can
read the Dockerfiles and believe they are correct, but this environment cannot pull container
images — every registry answers 403:

```
failed to solve: maven:3.9-eclipse-temurin-21: ... 403 Forbidden
16-alpine: Pulling from library/postgres ... 403 Forbidden
```

So `docker compose up` is **unverified**. I have said so in the README, in the section that
tells people to run it, rather than in a footnote. The Constitution's definition of done does
not have a category for "probably works", and I am not going to invent one at the last stage.

What *is* verified is `scripts/run-locally.sh`, and every QA run in `docs/qa/runs/` was made
against a stack it started.

## 3. What Stage 8 itself found

Packaging turned out not to be clerical. Four things came out of it.

### F-8.1 — `run-locally.sh` reported a stack it had not started

The first version waited on `curl http://127.0.0.1:8080/actuator/health` with no check that the
port was free. Orphaned processes from earlier manual work were still holding 8080 and 5173, so
both health checks passed **instantly against strangers**, the script's own children died on a
port conflict, and it printed "healthy" and "PipelineCRM is running".

It looked like a success. It was a script that had started nothing.

Fixed three ways:
* `require_free_port` refuses to start when something already answers on 8080 or 5173, naming
  the port and how to override it.
* `await` gives up if the child process dies, printing the last 20 lines of its log, and gives
  up after 120 s rather than waiting forever.
* each service starts under `setsid` with stdin and stdout detached, so it leads its own
  process group.

Proven in both directions: with a decoy listener on 8080 the script exits 1 with the message;
with the port free it starts and exits 0.

### F-8.2 — the script never exited

Children inherited the script's stdout, so anything reading that stdout waited for the
services rather than for the script. `</dev/null` and `setsid` fixed it. It now returns in
about 15 s.

### F-8.3 — `--stop` left node processes running

`kill <pid>` killed `npx` and orphaned the `node` it had spawned; the port stayed bound. Now
each service leads a process group and `--stop` signals the group. Verified: after `--stop`,
`ps` shows no `pipelinecrm.jar` and no `vite`, and both ports are closed.

### F-8.4 — the QA suite could not be run from the instructions

Running `npm run test:qa` from a cold shell gave **34 of 34 failing**. Not one of those was the
application: Playwright could not launch a browser, and `PLAYWRIGHT_CHROMIUM_PATH` — which every
previous run had set from my shell — appeared in no document. A reader following
`docs/qa/procedures.md` exactly would have hit a wall of red and had nothing to go on.

Now documented in the procedures and in the README, with the warning that a launch failure is
not a QA result. After the fix: **34 of 34 pass, 40.5 s.**

### F-8.5 — the two run paths signed tokens differently

The compose secret was 41 characters and the local one 48. jjwt derives HMAC strength from key
length, so compose would have signed **HS256** and the local path **HS384** — same demo, two
different cryptographic behaviours, and the README asserted a single answer. The secrets are now
byte-identical, with a comment in the compose file saying why they must stay that way.

Also corrected: the old README said the auth was HS256. The token header actually reads HS384.

---

## 4. Claims I checked rather than remembered

Writing the metrics table, I re-derived every number from the reports on this commit instead of
copying my own earlier notes. Three were wrong:

* backend test total: I wrote **589**; it is **563** (267 + 151 + 43 + 28 + 74).
* review findings: I wrote **22**; there are **59** (11, 9, 8, 7, 7, 6, 5, 6 across stages 0–7).
* three of the four "run one suite" commands I drafted did not exist or did not work:
  `RunAcceptanceTest` is not a class, `-Dtest='*ArchitectureTest,*RuleTest'` matches nothing, and
  the surefire property is `surefire.failIfNoSpecifiedTests`, not `failIfNoSpecifiedTests`.
  Every command in the README has now been executed exactly as written.

Running the architecture tests alone also surfaced something worth keeping: `-pl bootstrap`
without `-am` **fails**, because `AnalysedCodeTest` refuses to certify architecture against
already-installed jars. That is the F-2.1 guard working. The README documents the `-am` and says
why.

---

## 5. Final metrics

From `mvn -f backend/pom.xml clean verify` on this commit — BUILD SUCCESS.

| Module | Line | Branch | Mutation | Methods | Worst CRAP |
|--------|------|--------|----------|---------|-----------|
| `domain` | 100.00 % (303/303) | 100.00 % (69/69) | 100.00 % (124/124) | 129 | 4.00 |
| `application` | 100.00 % (277/277) | 100.00 % (16/16) | 100.00 % (88/88) | 127 | 2.00 |

Zero surviving mutants. Every method in both layers is at or under CRAP 4 — the Constitution's
*preference*, not merely its limit of 6.

| Suite | Count |
|-------|-------|
| `domain` unit | 267 |
| `application` unit + Gherkin (72 scenarios, 9 features) | 151 |
| `adapter-persistence` integration, real PostgreSQL | 43 |
| `adapter-web` unit | 28 |
| `bootstrap` architecture (13 ArchUnit rules) + API over HTTP | 74 |
| frontend unit | 26 |
| QA procedures | 34 / 34 |

`svelte-check`: 322 files, 0 errors, 0 warnings.

---

## 6. What I want attacked

1. **The compose stack.** It is the headline instruction in the README and it is unrun. Is
   admitting that enough, or does an unexecuted primary path fail the definition of done?
2. **`run-locally.sh`.** It creates a Postgres cluster, `su`s to `postgres`, and writes to
   `/var/lib`. Is that acceptable in a script a reader is told to run?
3. **The metrics table.** I claim these were re-derived. Re-derive them yourself.
4. **Whether the README over-sells.** 100 % coverage and a 100 % mutation score are true of two
   modules out of five. Does the document make that boundary clear enough, or does it read as
   though the whole system is at 100 %?
5. **F-8.1.** A script that reported a running stack it had not started is the same class of
   defect as the two Gherkin scenarios that could not fail (F-3.1) and the QA step that only
   passed on an empty database (Stage 7). Third occurrence of "the check passed without checking
   anything". Is there a systemic answer I keep missing?
