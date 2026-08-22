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
