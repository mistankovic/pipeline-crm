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
