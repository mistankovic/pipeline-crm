# Stage 0 hand-off — Project Constitution & Quality Gates

## What was built

| Artefact | Purpose |
|----------|---------|
| `CONSTITUTION.md` | The binding rules. Layers, dependency rule, business-rule locations, test obligations, Clean Code limits, per-stage definition of done, explicit non-goals. |
| `README.md` | How to run the build and the test suites; the gate table. |
| `.editorconfig` | Encoding, line endings, indentation, line length. |
| `backend/pom.xml` | Parent POM: dependency management, and every quality gate bound to a build phase. |
| `backend/{domain,application,adapter-persistence,adapter-web,bootstrap}/pom.xml` | The five modules in dependency order. |
| `backend/config/checkstyle/checkstyle.xml` | Mechanical form of Constitution §4. |
| `backend/config/checkstyle/suppressions.xml` | One suppression, justified inline. |
| `backend/tools/crap.py` | CRAP gate. Parses JaCoCo XML, fails the build above the threshold, writes a Markdown report. |
| `backend/tools/test_crap.py` | Five tests for the gate itself. |
| `frontend/` | Vite + Svelte 5 + TypeScript scaffold with strict `tsconfig`, `svelte-check` in the build. |

## How the dependency rule is enforced at Stage 0

Three independent mechanisms, so no single mistake defeats it:

1. **Maven module graph.** `domain` declares no compile dependency at all, so it *cannot*
   compile against anything else. `application` declares only `domain`.
2. **`maven-enforcer-plugin` banned dependencies.** `domain` bans every compile/runtime/
   provided dependency, transitively. `application` bans everything except
   `com.pipelinecrm:domain`. Verified working: the build passes today and would fail the
   moment a Spring jar appeared on either module's compile classpath.
3. **ArchUnit fitness functions.** Deferred to Stage 2, where there are classes to check.
   Listed here so the gap is visible, not hidden.

## Gates that are live now

| Gate | Bound to | Proven |
|------|----------|--------|
| Checkstyle (complexity ≤ 6, method ≤ 20 lines, ≤ 4 params, nesting ≤ 2, no star imports, …) | `validate`, all modules | Yes — it rejected the first version of `PipelineCrmApplication` |
| Enforcer: Java ≥ 21, Maven ≥ 3.9, no duplicate dependency versions, upper-bound deps | `validate` | Yes |
| Enforcer: domain/application dependency bans | `validate` | Yes |
| JaCoCo report + 95 % line/branch check | `test`, domain + application only | Configured; nothing to measure yet |
| CRAP ≤ 6 | `verify`, domain + application only | Gate script proven by its own unit tests; not yet applied to production code |
| PIT ≥ 90 % mutation score | `verify`, domain + application only | Configured; nothing to mutate yet |

## Verification run

```
mvn -f backend/pom.xml -Dpit.skip=true -Dcrap.skip=true verify   → BUILD SUCCESS (6 modules)
python3 -m unittest discover -s backend/tools -p 'test_*.py'     → 5 tests, OK
cd frontend && npm install && npm run build                      → svelte-check 0 errors, vite build OK
```

`-Dcrap.skip` and `-Dpit.skip` were passed only because `domain` and `application` contain
no production code yet; both gates are unskipped by default in those two modules' POMs.

## Deliberately deferred

* ArchUnit rules — Stage 2 (no classes to inspect yet).
* PMD/CPD duplication gate — Stage 3 (Constitution §4 promises it; it is not wired yet).
* Testcontainers, Flyway migrations, security config — Stages 2 and 4.
* `docker-compose.yml` — Stage 8.
* Any domain code, any UI. Stage 0 is scaffolding and gates only.

## Known weaknesses I expect to be attacked

1. The CRAP gate depends on `python3` being on `PATH`. That is an undeclared build
   prerequisite outside Maven.
2. `coverage.skip`/`crap.skip` default to `true` in the parent and are switched off per
   module. A new module would silently inherit *no* coverage gate.
3. The Constitution promises a CPD duplication gate that Stage 0 does not wire.

---

# Stage 0 hand-off — round 2 (response to review)

| Finding | Disposition |
|---------|-------------|
| F-0.1 build only works from one directory | **Fixed.** Added `backend/.mvn/extensions.xml` as a reactor-root marker. Re-verified: `cd backend/domain && mvn validate` now passes and `maven.multiModuleProjectDirectory` resolves to `backend/`. |
| F-0.2 CRAP gate passes on an empty report | **Fixed.** `crap.py` exits 3 (`EXIT_UNUSABLE_REPORT`) when the report contains no methods, or when any method has a complexity counter but no line counter. An empty report is accepted only behind an explicit `--allow-empty`. Three new tests cover all three paths; 8 tests total, green. |
| F-0.3 gates opt-in by default | **Fixed.** `coverage.skip` and `crap.skip` now default to `false` in the parent. Every module that opts out states its reason in its own POM. A new module inherits every gate. |
| F-0.4 promised CPD gate missing | **Fixed.** `maven-pmd-plugin:cpd-check` bound to `verify` for all modules, failing the build. 30 tokens in `domain`/`application`, 100 elsewhere; the Constitution now states both numbers. Confirmed running (`domain/target/cpd.xml`). |
| F-0.5 Constitution vs `.gitignore` | **Fixed.** §5.2 no longer asks for committed reports; the numbers go in the hand-off, the artefacts stay out of git. |
| F-0.6 bundle-only coverage rule | **Fixed.** Added a `CLASS`-element rule at 0.90 line / 0.85 branch beneath the 0.95 bundle rule. No single class can be dark. |
| F-0.7 test sources unchecked | **Fixed.** `includeTestSourceDirectory=true`. Complexity, nesting, method length, star imports and braces now apply to tests. Four checks lifted for tests only, each with a written reason in `suppressions.xml`. §3.2 now separates the mechanical rules from the review-only ones. |
| F-0.8 `docs/mutation-survivors.md` legislated but unenforced | **Fixed by amendment.** §3 now says plainly that the file is a review-gate obligation and that no tool reads it. |
| F-0.9 no CI | **Fixed.** `.github/workflows/quality-gates.yml` runs the CRAP gate's own tests, the full `mvn verify`, and the frontend type-check/tests/build on every push and PR, and uploads the reports. |
| F-0.10 `.gitignore` would swallow a Maven wrapper | **Fixed.** Negation added ahead of the change that would need it. |
| F-0.11 `vite.config.ts` unchecked | **Fixed.** Added to `tsconfig` `include`, plus `/// <reference types="vitest/config" />` and `@types/node`. `svelte-check` now covers 284 files, 0 errors. |

## The one thing I did not remove

`domain`, `application` set `crap.skip`, `coverage.skip` and `pit.skip` to `true`, with a
comment saying which stage must delete them. Reason: those modules contain no production
code yet, and after the F-0.2 fix the gates now *correctly* fail loudly rather than
passing silently on nothing — PIT likewise aborts with "No mutations found". Rather than
weaken a gate to accommodate an empty module, the module declares itself not-yet-measured
in a way that is impossible to miss on review. Stage 1 (domain) and Stage 3 (application)
delete those lines; the corresponding reviews must check that they are gone.

## Verification run (round 2)

```
mvn -f backend/pom.xml verify                                → BUILD SUCCESS, 6 modules, no -D flags
cd backend/domain && mvn validate                            → SUCCESS (was broken before)
python3 -m unittest discover -s backend/tools -p 'test_*.py' → 8 tests, OK
cd frontend && npm run check                                 → 284 files, 0 errors
cd frontend && npm run test:unit && npm run build            → OK
```
