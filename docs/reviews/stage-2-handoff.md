# Stage 2 hand-off — Clean Architecture skeleton and technology wiring

## Architecture fitness functions (11 rules, all green)

In `bootstrap/src/test/java/com/pipelinecrm/architecture/`, run by `mvn verify`.

| Rule | What it forbids |
|------|-----------------|
| `dependencies_point_inward` | ArchUnit `layeredArchitecture` over all five layers. Bootstrap may be accessed by nobody; each adapter only by bootstrap; application only by the adapters and bootstrap. |
| `the_two_adapters_do_not_know_each_other` | web ↔ persistence dependencies in either direction. |
| `no_package_anywhere_takes_part_in_a_cycle` | any package cycle, at any depth. |
| `the_domain_knows_no_framework` | domain → Spring, Jakarta, javax, Jackson, Hibernate, JDBC, slf4j, JUL. |
| `the_use_cases_know_no_framework` | the same, for `application`. |
| `the_domain_carries_no_annotations_from_outside` | a Spring or Jakarta annotation on an entity or use case. |
| `domain_state_is_private_and_mostly_final` | any non-private instance field in the domain. |
| `nothing_outside_the_web_adapter_is_a_controller` | a `*Controller` anywhere else. |
| `persistence_types_stay_in_the_persistence_adapter` | Spring Data referenced from any other module. |
| `nothing_outside_the_web_adapter_speaks_http` | `org.springframework.web` / `.http` referenced from any other module. |
| `ports_are_interfaces` | a class in `port.in` / `port.out` that is not an interface. |

Two rules were **rewritten during this stage** because they were vacuous. ArchUnit fails a
rule that matches no classes, and it caught two of mine:

* `controllers should reside in the web adapter` matched nothing (no controllers until Stage
  5). Rewritten as the prohibition `nothing outside the web adapter is a controller`, which
  has classes to check from day one and protects the same thing.
* `the two adapters do not know each other` matched nothing because both adapters were
  empty — which is what pushed this stage to build them rather than defer them.

The `layeredArchitecture` rule also failed with *"Layer 'Web adapter' is empty"* until the
adapters existed. A rule that cannot see anything is now a build failure, not a pass.

## Outer adapters built

**`adapter-web`** — `SecurityConfiguration` (stateless, CSRF off, `/api/sessions` and
health public, everything else authenticated, 401 rather than 403 for anonymous callers),
`JwtAuthenticationFilter` (decides *who* is calling and nothing about what they may do),
`JwtAccessTokenIssuer` (implements the `AccessTokenIssuer` output port), `JwtSettings`
(refuses to start on a secret under 32 characters), `SignedInUser`, `JwtKeys`.

**`adapter-persistence`** — `SpringTransactions` implementing the `Transactions` output
port, and `PersistenceConfiguration`. `SpringTransactions` is the only class in the
codebase that knows what "atomically" is made of.

No controllers and no JPA entities yet: those are Stages 5 and 4.

## Integration proof

`ApplicationWiringTest` (`bootstrap`, `@SpringBootTest` with a random port) asserts:
the context starts; `select version()` returns a string starting with `PostgreSQL`;
`Transactions` executes work through a real transaction manager; `AccessTokenIssuer` has an
implementation; `/actuator/health` is public; `/api/deals` returns **401** to an anonymous
caller. It found the 403-instead-of-401 bug, which is now fixed with an explicit
`HttpStatusEntryPoint`.

Run: 16 tests in `bootstrap`, all green.

## Database policy

`PostgresDatabase` is a sealed choice between `ContainerisedPostgres` (Testcontainers, the
default) and `ProvidedPostgres` (an environment that already runs one, named by
`PIPELINECRM_TEST_DB_URL`). Substituting a different engine is not one of the options —
there is no H2 path and no way to add one without editing a sealed interface. The harness
lives in `adapter-persistence`'s test-jar so `bootstrap` shares it rather than copying it.

## A limitation I am flagging rather than hiding

**In this build environment the Testcontainers path could not be executed.** Container
image layers are blocked by the egress policy (`403 Forbidden` from Docker Hub's and ECR's
CDNs, for `postgres:16-alpine` from both). The Docker daemon runs; pulls do not.

So the integration tests above ran against a real PostgreSQL 16.13 started locally, via
`PIPELINECRM_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5433/pipelinecrm_test`. That is a real
PostgreSQL and the tests genuinely pass against it — but the `ContainerisedPostgres` branch
is **unexecuted code** here, and it is the default branch. CI (`quality-gates.yml`) exercises
it on a runner with registry access. I am not claiming it works because I wrote it; I am
saying which half was proven where.

## Verification run

```
mvn -f backend/pom.xml verify                        → BUILD SUCCESS, all modules
  domain:    100% line, 100% branch, 124/124 mutants, worst CRAP 5.00
  bootstrap: 16 tests (11 architecture + 5 wiring), 0 failures
```

## Deliberately not done

* JPA entities, mappers, Flyway schema — Stage 4.
* Controllers, DTOs, the exception handler — Stage 5.
* Use-case implementations and Cucumber glue — Stage 3.
* `application` still carries the Stage 0 gate opt-out; Stage 3 discharges it.

---

# Stage 2 hand-off — round 2 (response to review)

| Finding | Disposition |
|---------|-------------|
| F-2.1 architecture tests can pass against stale bytecode | **Fixed.** New `AnalysedCodeTest`: every analysed `com.pipelinecrm` class must come from inside this checkout, found by walking up to the `.mvn` marker Stage 0 added. Verified both ways — the reactor build passes, `mvn -pl bootstrap test` now **fails** with the location of every class resolved from the local repository and a message telling the reader to run from the reactor root. The first version of this rule was too crude (it rejected all jars, including the fresh reactor ones a `verify` build legitimately produces); the second distinguishes *where the artefact came from* rather than *what shape it is*. |
| F-2.2 the default database path was never executed | **Partly fixed, honestly.** `PostgresDatabase.chooseFrom(UnaryOperator<String>)` extracts the selection from the environment lookup, and six tests pin it: absent variable, blank variable, set variable, and both credential fallbacks including the blank case. Starting a container still cannot be executed here and the hand-off still says so. What is now proven is the branch *choice*; what remains unproven is the container *start*. |
| F-2.3 the security skeleton had no tests | **Fixed.** 23 tests across `JwtAccessTokenIssuerTest` (6), `JwtAuthenticationFilterTest` (11), `JwtSettingsTest` (6): expired token, foreign secret, foreign issuer, missing header, non-bearer header, gibberish, non-UUID subject, role granted, credentials not retained, request always continues, and the issuer's subject/role/issuer/expiry/no-email-leak. |
| F-2.4 the adapters were coupled through the Spring context | **Fixed.** The `Clock` bean moved from `PersistenceConfiguration` to a new `CompositionRoot` in `bootstrap`. The comment there explains the failure mode so it does not come back. |
| F-2.5 `token()` returning `"n/a"` | **Fixed.** Credentials are `null`, with a comment saying why keeping the token would be a leak. |
| F-2.6 `JwtSettings` mixes validation and defaulting | **Refused, with reason.** Both are constructor responsibilities for a settings record, and splitting them would mean either a builder or a second type for four lines of code. The magic number is now a named constant. I would rather leave this than add indirection to satisfy a symmetry argument. |
| F-2.7 an invented route | **Fixed.** `SecurityConfiguration.SIGN_IN_ROUTE` is public, and the comment tells Stage 5 what breaks if the controller disagrees with it. |
| F-2.8 no CORS | **Deferred to Stage 6, deliberately.** The Vite proxy covers development. The decision belongs with the frontend, not before it. |

## Two real defects these tests found

1. **`JwtAuthenticationFilter` judged expiry against the host clock**, not the injected one,
   so token expiry was untestable and unrepeatable. It now parses with
   `.clock(() -> Date.from(clock.instant()))`. Found only because F-2.3 forced the tests.
2. **A token with a non-UUID subject crashed the filter.** Stage 1's fix for F-1.3 made
   `UserId.fromString` raise `InvariantViolation`, which the filter did not catch — so a
   malformed token would have produced **500 Internal Server Error** instead of being read
   as "I do not know who you are". The catch is now `JwtException | DomainException`.
   This is a regression that Stage 1 introduced and Stage 1's review did not catch; it
   surfaced two stages later because a reviewer insisted on tests for an untested class.

## Verification run (round 2)

```
PIPELINECRM_TEST_DB_URL=… mvn -f backend/pom.xml verify   → BUILD SUCCESS, all six modules
  domain:              100% line, 100% branch, 124/124 mutants, worst CRAP 5.00
  adapter-persistence:  6 tests
  adapter-web:         23 tests
  bootstrap:           17 tests (12 architecture + 5 wiring)
mvn -pl bootstrap test -Dtest=AnalysedCodeTest              → BUILD FAILURE, as designed
```
