# Stage 4 — Adversarial Review

STAGE 4 APPROVED

Reviewed on branch `local-grok-pipeline-crm` at `7606369` (`feat: map domain aggregates through JPA and Flyway`; working tree clean aside from this report). Scope: `CONSTITUTION.md` §2 (Dependency Rule), §5.4 persistence IT, §7 domain rules stay in domain; Flyway `V1__init.sql`; JPA entities, mappers, `Jpa*Repository` adapters, `JpaRepositoryIT`; inner-layer purity. Builder claims: `docs/reviews/stage-04-builder-handoff.md`. Did not inspect other git branches.

Commands run (JDK 21 via `/Users/mislav/Library/Java/JavaVirtualMachines/jdk-21.0.11+10/Contents/Home`):

- `./mvnw -B verify` — BUILD SUCCESS (8 reactor modules)
- Grep of `domain/src/main/java` and `application/src/main/java` — no `jakarta.persistence`, `org.hibernate`, `org.springframework`, `javax.persistence` (also no `jakarta.*` / Jackson / Lombok)
- `jdeps --multi-release 21 -s domain/target/domain-0.1.0-SNAPSHOT.jar` — `java.base` only
- `jdeps --multi-release 21 -s application/target/application-0.1.0-SNAPSHOT.jar` — `java.base` + domain (`not found` without `-cp`; with `-cp` domain jar: domain + `java.base`)

Failsafe ran `JpaRepositoryIT` against Testcontainers `postgres:16-alpine` (JDBC `jdbc:postgresql://localhost:42575/test`, PostgreSQL **16.14**, Hikari `PgConnection`). Flyway applied `V1__init` to an empty schema **before** Hibernate started the EMF. `ddl-auto=none` in both adapter-persistence IT yaml and bootstrap `application.yml`. Inner-layer JaCoCo/CRAP still green (domain 334/335 lines, 91/92 branches; application 125/125 lines, 27/28 branches; CRAP 137 / 51 methods, 0 violations). Enforcer/ArchUnit inner-layer bans still green.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| JPA adapters implement application output ports | **MET** | `JpaUserRepository`, `JpaCompanyRepository`, `JpaContactRepository`, `JpaDealRepository`, `JpaActivityRepository` implement the five `port.out` repository interfaces. Spring Data types stay in `…persistence.spring`. IT autowires `com.pipelinecrm.application.port.out.*`, not `Spring*Repository`. |
| Map domain ↔ persistence; tx outside domain | **MET** | Mappers in `adapter-persistence`. `@Transactional` only on JPA adapters (and the IT). Domain/application have zero `@Transactional`. |
| Domain remains pure (no JPA annotations) | **MET** | See leakage attack. Entities live under `…persistence.entity`. |
| Repository implementations are thin | **MET** | Adapters delegate + map. No stage machine, no win-guard duplication, no authorization. |
| Persistence IT + Testcontainers Postgres | **MET** | Failsafe `JpaRepositoryIT` Tests run **2**. Not H2. Not bootstrap-only. |
| Schema via Flyway; `ddl-auto` is none | **MET** | `V1__init.sql`; IT log: empty schema → migrate v1, then Hibernate. `spring.jpa.hibernate.ddl-auto: none`. |
| Closed-won guards stay in domain | **MET** | `DealMapper.toDomain` calls `Deal.restore(…, activities)`. `restore` → `assertConsistent` → `assertWinnable`. IT reloads through the port then `changeStage(CLOSED_WON)`. |

---

## Attack results

| Attack | Result |
| --- | --- |
| Anemic mapping bypasses `Deal.restore` / win guards | **Miss.** `DealMapper.toDomain` is `Deal.restore` with owned activities, not `Deal.open` plus field writes. `DealEntity` has no activities collection and no stage mutators that skip the domain. Loading `CLOSED_WON` without a qualifying activity would throw in the domain, not silently hydrate. |
| Dual-write (deal activities vs `ActivityRepository`) | **Smell, not a split-brain.** One `activities` table. `JpaDealRepository.save` upserts `deal.activities()` onto that table; `JpaActivityRepository.save` upserts the same row type. `findById` reconstitutes from `findByDealId`. `RecordActivityService` still writes both ports (in-memory identity-map leftover). Same PK → merge, not two sources of truth. See residuals. |
| Missing XOR target on activities | **Miss.** Flyway `CONSTRAINT activities_one_target CHECK ((deal_id IS NULL) <> (contact_id IS NULL))`. Domain `ActivityTarget.deal` / `.contact` and `RecordActivityUseCase.Command` already XOR. Mapper prefers deal if both columns were set; the CHECK makes that row illegal. |
| Fat adapters with business rules | **Miss.** No `DealStage` / `assertWinnable` / role checks in adapters. `groupDealActivities` is a query join in Java. |
| Domain leakage | **Miss.** See leakage attack. |
| Tests that do not hit Postgres | **Miss.** Failsafe + `postgres:16-alpine` + Flyway + `PgConnection`. Constitution §5.4 location is `adapter-persistence`, not bootstrap. |
| Tx boundary loses a closed-won activity | **Miss on the adapter.** `JpaDealRepository.save` writes deal **and** owned activities in one adapter transaction. `ChangeDealStageService` reloads via `findById` which attaches `findByDealId` before `changeStage`. The IT’s closed-won after reload would fail if activities were dropped on the way back. Use-case-level wrapping still belongs in bootstrap (Stage 2 residual). |

---

## Leakage attack

- `domain` production POM: no compile dependencies. `application` production: `domain` only.
- Grep of `domain/src/main/java` and `application/src/main/java`: no `org.springframework`, `jakarta.*`, `org.hibernate`, `javax.persistence`, Jackson, Lombok.
- `jdeps` domain jar → `java.base`. Application jar → `java.base` + domain.
- `adapter-web/pom.xml` still does not depend on `adapter-persistence`. `LayerDependencyRulesTest` 6/6.
- `PersistenceIndependenceTest` 4/4: production classes in `com.pipelinecrm.adapter.persistence..`, no web/bootstrap, no Lombok.
- No `@OneToMany` / `CascadeType` / `orphanRemoval` on domain types (none on JPA entities either). Activities are rows + mapper, not a Hibernate collection sneaking into `Deal`.

A green `./mvnw verify` remains the architecture review for dependency direction (`CONSTITUTION.md` §2.4).

---

## Critical

None.

---

## Important

None.

---

## Residual minors (do not by themselves block)

1. **`RecordActivityService` still dual-writes deal activities** (`deals.save` then `activities.save`). With JPA both land on `activities`. Harmless merge today; a later “thin deal.save” plus a dropped second call would lose evidence. Bootstrap should wrap the use case in **one** transaction when interactors are wired — not `@Transactional` on the application module.

2. **`JpaRepositoryIT` is `@Transactional` (rollback).** Flush-visible in one persistence context, not commit-then-new-session. Still Postgres; still Failsafe. Does not prove a second HTTP request sees the meeting.

3. **IT is thin (2 methods).** It never asserts amount, currency, owner, or company on reload (closed-won needs `value > 0`, so amount is not *zero*, but it could be the wrong money). It never records a deal-targeted activity *only* through `ActivityRepository` (the production `RecordActivityService` path). It never inserts a both-null / both-set activity to trip `activities_one_target`. XOR is in Flyway; it is not an IT.

4. **Assigned UUIDs without `Persistable` / `@Version`.** Spring Data `save` uses `merge` (extra SELECT). First insert still worked in the IT. Concurrent stage changes are last-write-wins.

5. **`JpaDealRepository.save` never deletes activities absent from `deal.activities()`.** Domain is append-only, so this matches today. Do not add “replace aggregate” semantics later without orphan handling.

6. **`ActivityMapper.targetOf` is deal-first, not XOR.** Corrupt rows with both FKs would silently drop the contact. The CHECK is the real gate.

7. **`findAll` loads every activity then groups in a `HashMap`.** Contact notes are scanned and discarded; deal activity order is not stable. Fine at demo scale.

8. **`CHAR(3)` currency vs Hibernate `String`.** Round-trip of `CLOSED_WON` in the IT went through `Currency.getInstance(entity.currency())`, so this run did not see padded `"USD "`. Keep an eye on JDBC `bpchar` if a later driver/mapping change appears.

9. **Bootstrap still does not `@Bean`-wire use cases.** Stage 2 residual. Do not put `@Service` on interactors. Persistence adapters `@Component` + `PersistenceJpaConfig` self-register; same pattern as Stage 2.

10. **In-memory vs JPA semantic split** (Stage 3 leftover). `InMemoryDealRepository` is an identity map and does **not** reconstitute from `ActivityRepository`. JPA does. Gherkin stays green because `RecordActivityService` writes both. Production closed-won evidence is the `activities` table.

11. **FK vs in-memory phantoms.** `activities.created_by` / `contact_id` / `deal_id` are real FKs. Stage 3 residual “phantom contactId is stored” will become `DataIntegrityViolationException` on this adapter. Better, but unmapped in the use case.

12. **No `hibernate.ddl-auto=validate`.** Constitution asked for `none`; Flyway owns the schema. Entity/column drift would show up as SQL errors, not a boot failure.

---

## What is already in good shape (do not rip out)

- Flyway `V1` matches the five aggregates; XOR CHECK on activities; FKs to users/companies/contacts/deals; `ddl-auto=none`.
- JPA model is anemic **on purpose**. Richness stays in `Deal.restore` / `changeStage` / `recordActivity`.
- `JpaDealRepository.findById` / `findAll` pass owned activities into `Deal.restore`. Closed-won after reload is a real port test, not a Spring Data `existsById`.
- Failsafe, not Surefire, owns `JpaRepositoryIT`. The IT injects output ports. Postgres 16.14, not an embedded substitute.
- Inner layers still compile and `jdeps` to JDK + domain only.

Stage 4 is approved. Residuals above are not blockers for Stage 5; do not treat this file as a waiver of bootstrap `@Bean` wiring or of a use-case-level transaction around `RecordActivityService`.
