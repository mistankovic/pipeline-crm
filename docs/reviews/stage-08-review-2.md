# Stage 8 — Adversarial Re-Review

STAGE 8 APPROVED

Scoped re-review of `4d8dffc` (`fix: skip cucumber XML gate when tests are skipped`) against prior REJECTED `docs/reviews/stage-08-review.md` (`ebd2555`) and builder claims `docs/reviews/stage-08-fixes.md`. Branch `local-grok-pipeline-crm`; working tree clean aside from this report. Stage 8 contract (working Compose, clear README, source/tests/Gherkin/QA present, system genuinely clean) plus the two prior findings. Did not inspect other git branches. Did not rebuild the backend image (optional); the Maven line that failed in Docker was re-run on a tree with no `cucumber-junit.xml`.

Commands / probes (JDK 21 via `/Users/mislav/Library/Java/JavaVirtualMachines/jdk-21.0.11+10/Contents/Home`):

- `docker compose config` — parsed (podman-compose 1.6.0). Services: `postgres` (postgres:16-alpine, healthcheck, **no host ports**), `backend` (`Dockerfile.backend`, `8080:8080`, `service_healthy` on Postgres), `frontend` (`Dockerfile.frontend`, `8081:80`, `service_started` on backend).
- Deleted `application/target/cucumber-junit.xml`, then `./mvnw -B -pl bootstrap -am package -DskipTests` — **BUILD SUCCESS**. Application log: `Tests are skipped.` then `exec:3.5.0:java (cucumber-junit-xml-gate)` → **`skipping execute as per configuration`**. XML still absent after the run. This is the clean-tree condition Docker sees (`.dockerignore` excludes `**/target`).
- `Dockerfile.backend` line 13 is still `RUN ./mvnw -B -pl bootstrap -am package -DskipTests`. The gate was not moved off `test`. `target/` is still not copied into the image.

---

## Previous findings

### Critical 1. `Dockerfile.backend` cannot `package -DskipTests`

**Verdict: ADDRESSED**
**Confidence:** 95

`application/pom.xml` execution `cucumber-junit-xml-gate` now has `<skip>${skipTests}</skip>`. `-DskipTests` skips Surefire **and** this exec. Host evidence matches the first review’s prescribed proof: no leftover XML, `skipping execute as per configuration`, BUILD SUCCESS.

The gate remains bound to `test` (PIT on `verify` still cannot use a smashed report as the cucumber gate). That was the constraint. They did not paper over Docker by copying `target/` or by deleting the gate.

Full `docker build -f Dockerfile.backend` was not repeated. The only failure mode in the first review was this exec after Surefire skip; that path is green.

### Important 1. README Compose advertises Postgres on `localhost:5432`

**Verdict: ADDRESSED, with a leftover**
**Confidence:** 90

Compose bullets now say Postgres is internal (`postgres:5432`) and **not published on the host**. `docker compose config` agrees: only `8080:8080` and `8081:80`.

The local-without-Compose sentence still says “start Postgres with those credentials.” “Those” now sits under the **demo login** table (`sales@pipelinecrm.demo` / `password`), not `pipelinecrm` / `pipelinecrm` / `pipelinecrm` on host `5432`. Host JDBC remains `bootstrap/src/main/resources/application.yml`. See remaining Important 2.

### Important 2. QA start is Vite-only; P4.1 not executable from empty DB

**Verdict: PARTIAL**
**Confidence:** 90

Start line now lists `docker compose up --build` → http://localhost:8081 **or** local Vite `:5173`. The Compose-port documentation hole is closed.

P4.1 (Sales must open a **Manager**-owned deal) is **not** closed. See remaining Important 1.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| README: how to run | **MET** | `docker compose up --build`; UI `:8081`, API `:8080`; demo users match `DemoDataSeeder`. Compose no longer claims host `5432`. Local jar + `npm run dev` path remains. |
| README: how to run all test suites | **MET** | Unchanged quality table / `./mvnw verify` / `-Pmutation` / frontend `build`/`check` / `docs/qa/ui-procedures.md`. |
| Quality metrics present and not Stage 0 leftovers | **MET** | Unchanged Stage 7 numbers. This commit did not retouch inner modules. |
| Compose: Postgres + backend + frontend | **SHAPE MET.** Backend **Maven line MET** on a clean XML tree. Image not rebuilt this pass. | Three services; JDBC `jdbc:postgresql://postgres:5432/pipelinecrm`; nginx `/api/` → `http://backend:8080`. |
| `docker compose config` | **MET** | Valid. Frontend `depends_on` still `service_started` only. |
| Dockerfiles not obviously broken | **MET** | The Stage 7 cucumber gate no longer fires under `-DskipTests`. |
| Gherkin, QA, source, reviews exist | **MET** | Unchanged presence. QA start now names Compose `:8081`. |
| Architecture still holds | **MET** | This commit is pom skip + docs. No inner-layer / Svelte change. |
| Working compose / system genuinely clean | **MET on the reject criterion** | The documented `package -DskipTests` invocation no longer requires leftover cucumber XML. |

---

## Attack results

| Attack | Result |
| --- | --- |
| Skip is unconditional / gate dead when tests actually run | **Miss on wiring.** `<skip>${skipTests}</skip>` only. Property is unset on a normal `test`/`verify`. Direct `exec:java@cucumber-junit-xml-gate` from the reactor root is the wrong probe (root POM has no `mainClass`); the skip log appears **only** under `-DskipTests`. Gate still lives on `test`, not `verify`. |
| Docker “fixed” by copying `target/` or dropping the gate | **Miss.** `.dockerignore` still `**/target`. Execution still present. |
| Gate moved back to `verify` (Stage 7 residual 5) | **Miss.** Phase is still `test`. |
| README still lies about host `5432` on Compose | **Miss.** Internal-only wording matches config. |
| QA still only documents Vite | **Miss.** Compose `:8081` is first. |
| P4.1 now works on a volume-less Compose DB | **Hit (carried).** Seeder still users-only. See remaining Important 1. |

---

## Remaining Important

### 1. P4.1 is still not executable from the documented empty-database start (Stage 7 Important 1 / Stage 8 Important 2)

**Severity:** Important
**Confidence:** 90

Unchanged product: `DemoDataSeeder` writes Manager + Sales and returns. P2 creates `Acme`. P3 creates a **Sales**-owned deal and leaves the owner filter on Sales. P4.1 still needs a Manager-owned LEAD.

The board form **can** select Manager (`Board.svelte` owner `<select>` lists all users). The procedure still does not say to do that, and after P3 the filter hides Manager cards. Compose as the **first** start path is a fresh volume-less Postgres — worse ambient state than a leftover Stage 6 DB, not better.

**Not a re-reject.** First Stage 8 review already declined to block on this. The control exists; a tester who improvises a Manager-owned deal can complete the step. Do not encode the owner-or-manager rule in Svelte.

### 2. Local-without-Compose “those credentials” no longer names the database

**Severity:** Important
**Confidence:** 80

README Compose fix removed `pipelinecrm` / `pipelinecrm` / `pipelinecrm` and host `5432`. The next paragraph still says “start Postgres with those credentials.” The nearest credentials are now demo **login** emails. Real local JDBC is still `localhost:5432` / `pipelinecrm` in `application.yml`.

Compose is the Stage 8 product and is now honest. The secondary local path is vaguer than before the fix. Name the DB user/password/port on the local paragraph, not under Compose ports.

**Not a re-reject.**

---

## Residual minors (do not by themselves block)

Carried from the first Stage 8 review; none were the skip-gate bug:

1. No backend healthcheck. Frontend `depends_on` is `service_started`. First `/api` hits can 502 while Boot is Flyway-ing.
2. Standalone `nginx -t` on the frontend image fails (`host not found in upstream "backend"`). Expected outside Compose DNS.
3. `Dockerfile.backend` has no `chmod +x mvnw`. Fragile on a Windows checkout.
4. CORS 8081 / 5173 / 4173 unused under both documented proxies. Harmless.
5. PIT still overwrites `application/target/cucumber-junit.xml` after mutation verify. Gate is on `test`; leave it there.
6. Carried UI residuals: contacts table omits company; company/contact edit is `window.prompt`; login error copy is `user`; `DealDetail` `onMount`-only; activity timestamps raw ISO; all six stage buttons stay enabled; `@SpringBootApplication(scanBasePackages = "com.pipelinecrm")`.
7. This re-review did not bring `docker compose up --build` all the way to a live login. The Maven skip is the closed Critical; a first-boot 502 (residual 1) is still possible.

---

## What is already in good shape (do not rip out)

- `<skip>${skipTests}</skip>` on `cucumber-junit-xml-gate` is the right skip. Do not move the gate to `verify` without a PIT-safe report path. Do not COPY host `target/`.
- Compose Postgres unpublished on the host. Keep saying that.
- Frontend relative `/api` + nginx `proxy_pass http://backend:8080`. Do not hard-code `localhost:8080` in Svelte.
- Wrapper only-script, jar name `bootstrap-0.1.0-SNAPSHOT.jar`, `.dockerignore` excluding `**/target`.
- Inner-layer purity and `UseCaseConfiguration` `@Bean` wiring were not touched this commit.

Critical 1 is closed. Stage 8 is approved with the two remaining Important items above (P4.1 empty-DB setup, local Postgres credentials dangling). They are documentation/procedure, not a missing backend image.
