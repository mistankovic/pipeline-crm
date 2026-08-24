# Stage 2 — Adversarial Review

STAGE 2 APPROVED

Reviewed on branch `local-grok-pipeline-crm` at `64a7ac8` (`feat: wire Spring Boot, JWT skeleton, and Testcontainers Postgres`; working tree clean aside from this report). Scope: `CONSTITUTION.md` §2, module POMs, `bootstrap`, `adapter-web` security, `adapter-persistence`, `frontend/`, `docs/reviews/stage-02-builder-handoff.md`, inner-layer ArchUnit/enforcer, and bytecode of `domain` / `application`. Did not inspect other git branches.

Commands run (JDK 21 via `/usr/libexec/java_home -v 21`):

- `./mvnw -B verify` — BUILD SUCCESS (8 reactor modules)
- `./mvnw -B -pl domain,application,adapter-web,adapter-persistence,bootstrap dependency:tree`
- `jdeps --multi-release 21 -s domain/target/domain-0.1.0-SNAPSHOT.jar`
- `javap -v` over `domain/target/classes` and `application/target/classes` for Spring/JPA/Hibernate/Jackson/Lombok/Postgres descriptors

Inner-layer enforcer (`ban-non-jdk-artifacts-on-inner-layers`, `ban-frameworks-on-inner-layers`) passed on `domain` and `application`. Domain CRAP 137 methods / 0 violations; application CRAP 12 methods / 0 violations. Bootstrap `PipelineCrmApplicationTest` started Testcontainers `postgres:16-alpine`, Hikari obtained a `PgConnection`, Hibernate 6.6 initialized persistence unit `default` (0 JPA repositories), Spring Boot 3.5.16 context loaded.

No Critical or Important findings. Stage 2 may close.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| Full module graph + Dependency Rule | **MET** | Modules `domain` → `application` → (`adapter-web` \| `adapter-persistence`) → `bootstrap`. Adapters do not depend on each other or on bootstrap. `LayerDependencyRulesTest` (6 tests) and per-module independence tests passed. |
| Spring Boot composition root | **MET** | `bootstrap` `@SpringBootApplication`, `spring-boot-maven-plugin` repackage, `application.yml`. |
| Postgres + Testcontainers | **MET** | `adapter-persistence` has `spring-boot-starter-data-jpa` + `postgresql` (runtime). Bootstrap test uses `@Testcontainers` + `@ServiceConnection` `PostgreSQLContainer("postgres:16-alpine")`. Verify connected to Postgres 16.14. |
| Svelte npm project | **MET** | `frontend/package.json` (Svelte 5.38 + Vite 6 + TypeScript), `package-lock.json`, `npm run build` / `check`. Unchanged from Stage 0; still a real npm project, not a Maven afterthought. |
| Basic security skeleton | **MET** | `SecurityFilterChain` (stateless, CSRF off, login/health permitAll), BCrypt `PasswordEncoder`, `BcryptPasswordHasher` (output port), `JwtTokenIssuer` (output port), `pipelinecrm.jwt` settings. |
| Outermost adapters, thin | **MET** | Web adapter translates to `PasswordHasher` / `TokenIssuer` only. Persistence adapter is JPA/Postgres on the classpath with no mappings (handoff: intentional). No §7 rules in adapters. |
| Domain / application: zero Spring / Postgres / Svelte | **MET** | See leakage attack below. |

---

## Leakage attack

### Maven graph

- `domain` production: **no compile dependencies**. Test: JUnit, AssertJ, ArchUnit, `crap-check`.
- `application` production: `domain` only.
- `adapter-web`: `application` + Spring Web/Security/Validation + JJWT. **Not** `adapter-persistence`.
- `adapter-persistence`: `application` + `spring-boot-starter-data-jpa` + `postgresql`. **Not** `adapter-web`.
- `bootstrap`: both adapters + Spring Boot + Testcontainers (test).

### Bytecode

- `jdeps` domain jar → `java.base` only.
- `jdeps` application jar → `java.base` + domain (`not found` without `-cp`; not Spring).
- No class-file references in inner-layer production output to `org/springframework`, `jakarta/persistence`, `org/hibernate`, `jakarta/servlet`, `com/fasterxml/jackson`, `org/projectlombok`, or `org/postgresql`.

### Source

Grep of `domain/` and `application/` production Java finds none of those prefixes. Ports (`DealRepository`, `LoginUseCase`, …) use domain types and JDK only — no `Pageable`, `EntityManager`, `ResponseEntity`.

A green `./mvnw verify` remains the architecture review for dependency direction (`CONSTITUTION.md` §2.4).

---

## Critical

None.

## Important

None.

---

## Residual minors (do not block Stage 2)

1. `@SpringBootApplication(scanBasePackages = "com.pipelinecrm")` also visits `domain` and `application`. Inner layers cannot become beans without a Spring compile dependency, which enforcer/ArchUnit already forbid. Prefer `com.pipelinecrm.bootstrap` + `com.pipelinecrm.adapter` so the scan matches the composition root.

2. JWT skeleton issues tokens (`JwtTokenIssuer`) but does not authenticate them (no filter / `oauth2ResourceServer`). Stateless `anyRequest().authenticated()` is therefore deny-by-default until a later stage adds login + Bearer validation. Acceptable for “skeleton”; do not ship API controllers against this chain as if JWT already works.

3. `SecurityConfig` permits `/actuator/health` without `spring-boot-starter-actuator` on the classpath. Dead matcher.

4. Adapters self-register with `@Component`. `CONSTITUTION.md` §2.1 says bootstrap is the only place that may **wire adapters to use-case implementations**. No interactors exist yet (`com.pipelinecrm.application.usecase` is empty). When Stage 3 adds them, wire with bootstrap `@Bean` methods — do not put `@Service` on the application module.

5. `README.md` still speaks as Stage 0 (“wiring, PostgreSQL, and JWT land in Stage 2+”; coverage table “Current (Stage 0)”). Update when convenient.

6. Persistence IT stays in bootstrap Surefire, not `adapter-persistence` + Failsafe. Matches constitution §5.4 (Stage 3+).

---

Stage 3 may start. Further stages still need `STAGE N APPROVED` in `docs/reviews/` before N+1.
