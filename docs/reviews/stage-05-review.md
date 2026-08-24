# Stage 5 — Adversarial Review

STAGE 5 APPROVED

Reviewed on branch `local-grok-pipeline-crm` at `b35e0ba` (`feat: add thin REST adapters and JWT Bearer auth`; working tree clean aside from this report). Scope: `CONSTITUTION.md` §2 (controllers call input ports only; bootstrap wires use cases; no business rules in web), §7 domain rules stay out of the web layer; REST controllers, DTOs, `RestExceptionHandler`, JWT Bearer filter, `UseCaseConfiguration`, `ApiEndToEndTest`, adapter-web controller tests. Builder claims: `docs/reviews/stage-05-builder-handoff.md`. Did not inspect other git branches.

Commands run (JDK 21 via `/Users/mislav/Library/Java/JavaVirtualMachines/jdk-21.0.11+10/Contents/Home`):

- `./mvnw -B verify` — BUILD SUCCESS (8 reactor modules)
- Grep of `adapter-web/src/main` for `DealStage.canTransitionTo`, `CLOSED_WON`, forecast math, `Deal.changeStage` — no stage machine, no win-guard, no Σ(value × probability)
- Grep of `application/src` for `@Service` — none (only `@ServiceConnection` on bootstrap/persistence tests)
- `jdeps --multi-release 21 -s domain/target/domain-0.1.0-SNAPSHOT.jar` — `java.base` only
- `jdeps --multi-release 21 -s application/target/application-0.1.0-SNAPSHOT.jar` — `java.base` + domain (`not found` without `-cp`)

Inner-layer JaCoCo/CRAP still green (domain 334/335 lines, 91/92 branches; application 134/134 lines, 29/30 branches; CRAP 137 / 54 methods, 0 violations). `WebIndependenceTest` 4/4. `LayerDependencyRulesTest` 6/6. Adapter-web Surefire **7** tests; bootstrap Surefire **9** including `ApiEndToEndTest` Tests run **2** against Testcontainers `postgres:16-alpine` (PostgreSQL **16.14**, Hikari `PgConnection`). Enforcer/ArchUnit inner-layer bans still green.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| REST controllers + DTOs + exception handling + security | **MET** | Seven `@RestController`s under `/api/*`. `ApiDtos` / `Responses`. `RestExceptionHandler`. `SecurityConfig` + `JwtBearerFilter`. |
| Controllers thin: HTTP ↔ input ports only | **MET** | Constructors take `*UseCase` ports. `DealController.changeStage` is `DealStage.valueOf` + `ChangeDealStageUseCase.Command`. No `canTransitionTo`, no `Deal.changeStage`, no closed-won / probability forcing. |
| Use cases wired as bootstrap `@Bean`, not `@Service` on application | **MET** | `UseCaseConfiguration` `@Bean` methods for login, users, companies, contacts, deals, stage, activity, forecast. Zero Spring in `application/src/main`. |
| JWT Bearer filter; unauthenticated API → 401 | **MET** | Filter `verifyWith` + `parseSignedClaims`. Chain `anyRequest().authenticated()` + `HttpStatusEntryPoint(UNAUTHORIZED)`. E2E `unauthenticatedDealsAreRejected` is 401. Login + Bearer creates a company. |
| Domain rules stay in domain / use cases | **MET** | Handler maps `DealNotWinnableException` / `IllegalDealStageException` / `DealStageNotAuthorizedException`; it does not reimplement them. `ChangeDealStageService` loads the actor from `UserRepository` and calls `deal.changeStage`. |
| `GET /api/users` without password hashes | **MET** | `ListUsersService` returns `PublicUser(id, email, name, role)`. `UserResponse` has those four fields. `User.passwordHash()` is never mapped. |
| `adapter-web` must not depend on `adapter-persistence` | **MET** | Web POM has `application` + Spring Web/Security/Validation + JJWT only. ArchUnit forbids `com.pipelinecrm.adapter.persistence..`. |

---

## Attack results

| Attack | Result |
| --- | --- |
| Closed-won / stage guards / forecast math in controllers | **Miss.** Controllers do not reference `DealStage.canTransitionTo`, `CLOSED_WON`, `Deal.changeStage`, or `Forecast.by*`. Stage change is port delegation. Forecast controller maps `groupBy` to `ForecastUseCase.byStage` / `byOwner` and copies bucket totals already computed in domain. `Probability.of` / `Money.of` / `DealStage.valueOf` are HTTP → command parsing, not §7 rules. |
| JWT not actually validated | **Miss on production code.** `JwtBearerFilter` uses JJWT 0.12 `parser().verifyWith(hmacShaKeyFor(secret)).parseSignedClaims(token)`. Invalid tokens throw, context is cleared (fail-closed), then `authenticated()` fires the 401 entry point. Issued tokens from `JwtTokenIssuer` use the same key. E2E proves missing Authorization is 401 and a login-issued Bearer is accepted. Tests do **not** lock a forged / unsigned / expired token — see residuals. |
| Exception handler swallows domain rules | **Miss.** Specific types → 404 / 403 / 409 / 400; remaining `DomainException` → 422. Message is `ex.getMessage()`. Closed-won still throws in `Deal`; the web layer only chooses a status. No catch-all `Exception` that returns 200. |
| `@Service` on application | **Miss.** Interactors are plain `final` classes. Composition root `@Bean`s. Component scan of `com.pipelinecrm` cannot instantiate them. |
| Web depends on persistence | **Miss.** POM + `WebIndependenceTest` + layered ArchUnit. |
| Password hashes on `GET /api/users` | **Miss.** `ListUsersUseCase.PublicUser` has no hash field. Controller never returns `User`. |

---

## Leakage attack

- `domain` production: no compile dependencies. `application` production: `domain` only. Grep of both `src/main/java`: no `org.springframework`, `jakarta.*`, `org.hibernate`, Jackson, Lombok.
- `jdeps` domain jar → `java.base`. Application jar → `java.base` + domain.
- `adapter-web/pom.xml` does not depend on `adapter-persistence`. `LayerDependencyRulesTest` 6/6. `WebIndependenceTest` 4/4.
- Controllers inject `port.in` types, not JPA repositories, not `EntityManager`, not bootstrap types.
- `BcryptPasswordHasher` / `JwtTokenIssuer` implement application **output** ports in the web adapter (drivers). That is the right place; it is not a persistence leak.

A green `./mvnw verify` remains the architecture review for dependency direction (`CONSTITUTION.md` §2.4).

---

## Critical

None.

---

## Important

None.

---

## Residual minors (do not by themselves block)

1. **API tests do not lock JWT signature verification.** `ApiEndToEndTest` is two methods: no `Authorization` → 401, and login-issued Bearer → `POST /api/companies` 201. Both still pass if the filter treated *any* `Bearer` payload as authenticated (or parsed claims without `verifyWith`). Production code does verify. Add a forged / unsigned / expired Bearer → 401 case before treating the filter as regression-proof. Standalone `DealControllerTest` / `AuthControllerTest` never install `JwtBearerFilter` or `SecurityConfig`.

2. **Exception mapping and most resources are untested at HTTP.** No `RestExceptionHandler` test. No controller test for users, contacts, activities, forecast, or deal create/list/view. E2E never hits `GET /api/users` (hash omission is source-review only), never triggers `DealNotWinnableException` / `DealStageNotAuthorizedException` over HTTP, never records an activity. Thinness of `DealController.changeStage` is proven by the mock verify; closed-won-over-HTTP is not.

3. **`RecordActivityService` is still two adapter transactions.** Stage 4 residual. Bootstrap now `@Bean`-wires the interactor (that residual is closed) but does not wrap `deals.save` + `activities.save` in one composition-root transaction. HTTP `POST /api/activities` can commit the deal write and fail the activity write. Do not put `@Transactional` on the application module.

4. **Bad login is HTTP 404.** `LoginService` throws `NotFoundException("user")` for missing user *and* wrong password. `RestExceptionHandler` maps that to 404. Avoids enumeration; it is not 401. Fine for a demo; do not document it as “unauthenticated → 401” — that status is for missing/invalid Bearer, not failed password.

5. **`Actors.require` maps a missing principal to `IllegalArgumentException` → 400.** The filter chain should never let that through (`authenticated()` + 401 entry point). If it ever did, the client would see `bad_request` / “unauthenticated” instead of 401.

6. **`ForecastController` treats every `groupBy` other than `"stage"` as owner.** Typos (`Stage`, `owner `) silently call `byOwner`. That is sloppy HTTP translation, not forecast math. 400 on unknown `groupBy` would be thinner.

7. **JWT `role` claim is unused for authorization.** Filter sets `ROLE_*` from the token; no `hasRole` / `@PreAuthorize`. Stage change reloads `User` from the database (correct; do not trust the claim). Stale JWT role vs DB role cannot widen stage rights today.

8. **`JwtBearerFilter` swallows every `RuntimeException` from parse.** Fail-closed, so invalid tokens become anonymous rather than 500. It also hides a too-short HMAC secret on Bearer requests (login `signWith` would still blow up). Demo secret is 40 ASCII bytes; keep it ≥ 32.

9. **`@SpringBootApplication(scanBasePackages = "com.pipelinecrm")` still visits domain and application.** Stage 2 residual. Inner layers still cannot become beans. Prefer `com.pipelinecrm.bootstrap` + `com.pipelinecrm.adapter`.

10. **No `UserControllerTest` asserting the JSON has no `passwordHash` key.** `PublicUser` / `UserResponse` make a leak a deliberate field addition, not an accidental getter. A one-line E2E assertion would still be cheaper than a later UI surprise.

---

## What is already in good shape (do not rip out)

- Controllers are translators. `DealController` does not implement §7. Stage change, win-guards, owner-or-manager, and probability-on-close stay in `Deal` / `ChangeDealStageService`.
- `UseCaseConfiguration` is the composition root the constitution asked for. Do not “simplify” by putting `@Service` on interactors.
- JWT is real HMAC-SHA via JJWT `verifyWith` + `parseSignedClaims`, not a prefix check. Stateless session, CSRF off, login `permitAll`, everything else authenticated, 401 entry point.
- `ListUsersService` exists specifically so the web layer never sees `User.passwordHash()`.
- Domain exceptions are mapped, not reimplemented. `DealStageNotAuthorizedException` → 403; not-winnable / illegal stage / mixed currency → 409.
- `adapter-web` still does not compile against persistence. Inner layers still `jdeps` to JDK + domain.

Stage 5 is approved. Residuals above are not blockers for Stage 6; do not treat this file as a waiver of a forged-token 401 test or of a use-case-level transaction around `RecordActivityService`.
