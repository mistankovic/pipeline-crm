# Stage 8 — Adversarial Review

STAGE 8 REJECTED

Reviewed at `ebd2555` (`feat: add docker-compose demo stack and document quality metrics`; working tree clean aside from this report). Scope: Stage 8 contract (working Compose, clear README, all source/tests/Gherkin/QA present, system genuinely clean), `README.md`, `docker-compose.yml`, `Dockerfile.backend`, `Dockerfile.frontend`, `frontend/nginx.conf`, `docs/qa/`, quality-metric claims. Builder claims: `docs/reviews/stage-08-builder-handoff.md`. Did not inspect other git branches.

Commands / probes (JDK 21 via `/Users/mislav/Library/Java/JavaVirtualMachines/jdk-21.0.11+10/Contents/Home`):

- `docker compose config` — parsed (podman-compose 1.6.0). Services: `postgres` (postgres:16-alpine, healthcheck), `backend` (Dockerfile.backend, `8080:8080`, waits for healthy Postgres), `frontend` (Dockerfile.frontend, `8081:80`).
- `docker build -f Dockerfile.frontend` — **succeeds**. Image contains `/usr/share/nginx/html/index.html` + hashed assets; `frontend/nginx.conf` lands at `/etc/nginx/conf.d/default.conf`.
- `docker build -f Dockerfile.backend` — **fails** at `RUN ./mvnw -B -pl bootstrap -am package -DskipTests`. See Critical 1.
- Host `./mvnw -B -pl bootstrap -am package -DskipTests` — BUILD SUCCESS only because `application/target/cucumber-junit.xml` already existed (48/0 from 19:02Z). Docker `.dockerignore` excludes `**/target`.
- `jdeps --multi-release 21 -s domain/target/domain-0.1.0-SNAPSHOT.jar` — `java.base` only.
- `jdeps --multi-release 21 -s -cp domain/target/domain-0.1.0-SNAPSHOT.jar application/target/application-0.1.0-SNAPSHOT.jar` — `java.base` + domain.
- Grep of `domain/src/main` and `application/src/main` — no Spring / Jakarta / Hibernate / Jackson / Lombok. Application production has no `@Service` / `@Component` / `@Transactional`.
- On-disk JaCoCo/PIT (Stage 7 mutation verify leftovers; Stage 8 did not change inner modules): domain 334/335 line, 91/92 branch; application 134/134 line, 29/30 branch; PIT 140/140 + 54/54 KILLED.

`docker compose up --build` was not brought all the way up: the backend image does not build.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| README: how to run | **MET, with a lie** | `docker compose up --build`; UI `:8081`, API `:8080`; demo users match `DemoDataSeeder`. Local-without-Compose path (`java -jar bootstrap/target/bootstrap-0.1.0-SNAPSHOT.jar` + `npm run dev`) is present. Compose section also lists `Postgres: localhost:5432` — **that port is not published**. See Important 1. |
| README: how to run all test suites | **MET** | Quality table: `./mvnw verify` (unit/architecture/Gherkin/JaCoCo/CRAP + persistence IT), `-Pmutation` for PIT, frontend `build`/`check`, pointer to `docs/qa/ui-procedures.md`. Default verify still does not run PIT; that is documented. |
| Quality metrics present and not Stage 0 leftovers | **MET** | Table is labeled Stage 7 and matches on-disk JaCoCo/PIT/CRAP/Gherkin 48. Commit dropped the Stage 0 `n/a (no executable production methods)` rows and the “Compose lands in Stage 8” deferral. |
| Compose: Postgres + backend + frontend | **SHAPE MET, STACK NOT BUILDABLE** | Three services, healthchecked Postgres, JDBC `jdbc:postgresql://postgres:5432/pipelinecrm`, nginx proxies `/api/` to `http://backend:8080`. Backend image build fails (Critical 1). |
| `docker compose config` | **MET** | Valid. Frontend `depends_on: backend` is `service_started` only (no backend healthcheck). |
| Dockerfiles not obviously broken | **FAILED** | Frontend is fine (`COPY mvnw` N/A; nginx path correct; `COPY --from=build /web/dist`). Backend copies `mvnw` + `.mvn` (only-script wrapper 3.3.2, no missing `maven-wrapper.jar`), copies every Maven module, copies `bootstrap-0.1.0-SNAPSHOT.jar` under the name the Spring Boot plugin actually produces. The **Maven line** is broken against the Stage 7 cucumber XML gate. |
| Gherkin, QA, source, reviews exist | **MET** | Five features under `application/src/test/resources/features` (CucumberTest Surefire 48). `docs/qa/ui-procedures.md`. All modules have production source. `docs/reviews/` has STAGE 0–7 APPROVED plus this stage’s hand-off. |
| Architecture still holds (no Spring in domain) | **MET** | `jdeps` + import grep + ArchUnit tests still in tree. Use cases still `@Bean`-wired in `UseCaseConfiguration`. Frontend still relative `/api` only; no `canTransitionTo` / `qualifiesCloseWon` / `passwordHash` in `frontend/src`. |
| Working compose / system genuinely clean | **NOT MET** | The documented demo command cannot produce images. That is the Stage 8 product. |

---

## Attack results

| Attack | Result |
| --- | --- |
| README still Stage 0 metrics / “running the app lands later” | **Miss.** Those paragraphs are gone. Numbers match Stage 7 inner-layer reports. |
| Compose file is theatre (missing service, bad ports, invalid YAML) | **Miss on shape.** Config is three real services. UI/API publish 8081/8080. |
| Dockerfiles missing `mvnw`, wrong jar name, wrong nginx path | **Miss on those three.** Wrapper is only-script; jar name matches `0.1.0-SNAPSHOT` + Boot repackage; nginx.conf is copied and `proxy_pass http://backend:8080` keeps `/api/...`. Frontend image built. |
| `docker compose up --build` actually works | **Hit.** Backend `package -DskipTests` dies on `cucumber-junit-xml-gate` in a clean tree. Host success is leftover XML. |
| Inner-layer framework leakage in this pass | **Miss.** Domain jar → `java.base`. Application → domain + `java.base`. No Spring in inner `src/main`. |
| Frontend smuggles §7 | **Miss.** Relative `fetch('/api/...')`; compose nginx is the proxy (this closes the old `vite preview` hole **for the demo path only**). |
| CORS 8081 is required for compose | **Dead config.** Browser talks same-origin to `:8081`; nginx proxies. Adding `localhost:8081` to `SecurityConfig` is unused, not harmful. |

---

## Leakage attack

- `domain/src/main` and `application/src/main`: no `org.springframework`, `jakarta.*`, Hibernate, Jackson, Lombok.
- Bootstrap still wires interactors with `@Bean` (`UseCaseConfiguration`). No `@Service` on application types.
- Frontend `api.ts` paths are `/api/...`. `Dockerfile.frontend` does not bake an API origin. Correct.
- Gherkin remains use-case layer (`CucumberTest` glue `com.pipelinecrm.application.acceptance`), not UI. Stage 8 did not relocate it.

Do not “fix” the Docker failure by dropping the cucumber XML gate or by encoding stage rules in Svelte.

---

## Critical

### 1. `Dockerfile.backend` cannot `package -DskipTests` — Compose demo does not build

**Severity:** Critical
**Confidence:** 95

`Dockerfile.backend` line 13:

```
RUN ./mvnw -B -pl bootstrap -am package -DskipTests
```

`package` still runs the `test` phase. `-DskipTests` skips **Surefire**, not other `test`-phase plugins.

Stage 7 bound `exec-maven-plugin` `cucumber-junit-xml-gate` to `test` (so PIT on `verify` cannot clobber `cucumber-junit.xml`). That execution still runs under `package`. With Surefire skipped it looks for a file that was never written:

```
[INFO] --- surefire:3.5.4:test (default-test) @ application ---
[INFO] Tests are skipped.
[INFO] --- exec:3.5.0:java (cucumber-junit-xml-gate) @ application ---
Missing cucumber JUnit XML: /src/application/target/cucumber-junit.xml (empty features/ or a Surefire that never ran Cucumber)
Error: building at STEP "RUN ./mvnw -B -pl bootstrap -am package -DskipTests": while running runtime: exit status 1
```

`.dockerignore` correctly excludes `**/target`. A clean Docker context therefore has no stale XML. This review’s host `./mvnw -B -pl bootstrap -am package -DskipTests` was **green** because `application/target/cucumber-junit.xml` from 2026-08-24T19:02:59Z (48 tests / 0 failures) was already on disk. That is not what `docker compose up --build` sees.

COPY of `mvnw` / modules / `bootstrap-0.1.0-SNAPSHOT.jar` is not the bug. The documented Maven invocation is incompatible with the cucumber gate on a clean tree.

Fix in the image (or an equivalent skip): do not run the gate when tests are skipped, e.g. `<skip>${skipTests}</skip>` on that `exec` execution, or `-DskipTests -Dexec.skip=true` if that property is wired. Do **not** move the gate back to `verify` without a PIT-safe report path (Stage 7 residual 5). Do not copy `target/` into the image to paper over it.

Until `docker build -f Dockerfile.backend` is green on a clean context, Stage 8 has no working compose.

---

## Important

### 1. README Compose section advertises Postgres on `localhost:5432`; Compose does not publish it

**Severity:** Important
**Confidence:** 90

README “How to run (Docker Compose)” lists:

- Postgres: localhost:5432 (`pipelinecrm` / `pipelinecrm` / `pipelinecrm`)

`docker-compose.yml` has **no** `ports:` on `postgres` and comments “Postgres is only on the compose network.” Config agrees: only `8080:8080` and `8081:80` are published.

The credentials match `POSTGRES_*` and `application.yml`. The host port claim does not. A reader following the Compose bullets with `psql` or a GUI will fail. The local-without-Compose paragraph (“start Postgres with those credentials”) is the place that port belongs.

Not the reject reason. Still a false statement in the primary run doc.

### 2. QA procedures still cannot execute P4.1 from the documented empty-database start (Stage 7 Important 1, unfixed)

**Severity:** Important
**Confidence:** 90

Carried. `DemoDataSeeder` still writes only Manager + Sales. P2/P3 still do not create a Manager-owned deal or reset the owner filter. P4.1 still needs that row. Stage 8’s compose stack, **if it built**, would be a **fresh** volume-less Postgres every `up` — worse than Stage 7’s leftover DB, not better.

Also: QA start line is still Vite `:5173`, not Compose `:8081`. Hash routes would work on 8081; the document does not say so.

Not a new hole, but Stage 8 is the final package and the QA start-state bug is still there.

---

## Residual minors (do not by themselves block)

1. **No backend healthcheck.** Frontend `depends_on` is `service_started`. Nginx can bind `:80` while Boot is still Flyway-ing; first `/api` hits 502. Demo-annoying, not the build break.
2. **Standalone `nginx -t` on the frontend image** fails with `host not found in upstream "backend"`. Expected outside Compose DNS. Not a compose defect. Variable `proxy_pass` + `resolver` would make `nginx -t` work in isolation; unnecessary if Compose starts all containers first.
3. **`Dockerfile.backend` has no `chmod +x mvnw`.** Host mode is `755`; Linux COPY kept it (the failed build got as far as running `./mvnw`). Fragile on a Windows checkout.
4. **CORS 8081 / 5173 / 4173 is unused** in both documented run modes (Vite proxy and nginx proxy are same-origin). Harmless.
5. **PIT still overwrites `application/target/cucumber-junit.xml` after mutation verify** (Stage 7 residual 5). The gate is on `test`. Do not “fix” Docker by moving the gate onto `verify` without a separate path.
6. **Carried UI residuals** (not re-opened): contacts table omits company; company/contact edit is `window.prompt`; login error copy is `user`; `DealDetail` `onMount`-only; activity timestamps raw ISO; all six stage buttons stay enabled; `@SpringBootApplication(scanBasePackages = "com.pipelinecrm")`.
7. **Handoff is six lines** and does not mention a successful `docker compose up --build`. The image build is the missing evidence.

---

## What is already in good shape (do not rip out)

- README quality table is real Stage 7 numbers, not Stage 0 `n/a`. Keep attributing the measurement command.
- Frontend image: Node 22 alpine `npm ci` + `vite build`, nginx 1.27, `try_files` for the hash router, `/api/` reverse proxy to the compose service name. Relative `fetch` is the right composition. Do not hard-code `localhost:8080` in Svelte.
- Wrapper is `distributionType=only-script` (3.3.2) with `apache-maven-3.9.11`. There is no missing `maven-wrapper.jar` to COPY.
- Jar name `bootstrap-0.1.0-SNAPSHOT.jar` matches Boot `repackage` on this version. `.dockerignore` excluding `**/target` is correct — do not COPY host jars.
- Inner-layer purity and `UseCaseConfiguration` `@Bean` wiring still hold. Postgres 16 + Flyway `V1__init` + `ddl-auto: none` unchanged.
- Gherkin, ArchUnit, JaCoCo 95%, PIT 100%, CRAP ≤ 6 are still in the tree. Stage 8 did not need to re-prove them; it needed a stack that builds.

Fix Critical 1 with a clean-context `docker build -f Dockerfile.backend` (and ideally `docker compose up --build` far enough to log in as `sales@pipelinecrm.demo`). Then request re-review. Do not treat Important 1–2 as waived; they are documentation, not a substitute for a backend image.
