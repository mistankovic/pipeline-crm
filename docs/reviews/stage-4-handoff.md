# Stage 4 hand-off — Persistence and infrastructure

## The domain did not change

Not one file under `domain/` was touched in this stage. No annotation, no accessor added for
a mapper's convenience, no `Serializable`. The ArchUnit purity rules and the enforcer bans
still pass, and `mvn verify` is green across all six modules.

That is the whole claim of this stage, so here is the evidence rather than the assertion:
`git diff --stat` for Stage 4 touches `adapter-persistence`, `bootstrap` and two POMs. The
`domain` module appears nowhere in it.

## Rows are not entities

Five `@Entity` classes in `adapter-persistence/row/`, named `UserRow`, `CompanyRow`,
`ContactRow`, `DealRow`, `ActivityRow`. They carry the shape the database needs — including
`password_hash`, which the domain has no concept of — and they contain **no behaviour at all**.

Five mappers in `mapping/` do the translating. `DealMapping` goes through `DealSnapshot` in
both directions, so it never reaches inside the aggregate and `Deal` needed no new accessor.
`ActivityMapping` flattens the sealed `ActivitySubject` into two nullable columns and rebuilds
it on the way back; a row with neither column set is rejected there rather than turned into a
half-built domain object.

## Repositories are thin

Five implementations of the output ports. Every method is query, map, return. The longest is
four lines. `JpaActivityRepository.findByDeal` returns a `DealActivities` carrying the deal it
was asked about — the guarantee is created at the only place that knows the query was for
that deal.

## Schema

Two Flyway migrations, living in **`adapter-persistence`** rather than in `bootstrap`: the
schema is that adapter's own business, and Flyway finds them on the classpath. Constraints:
foreign keys throughout, role and stage and type restricted to known values, probability
between 0 and 100, value not negative, and

```sql
CONSTRAINT activities_have_exactly_one_subject CHECK (
    (deal_id IS NOT NULL AND contact_id IS NULL)
    OR (deal_id IS NULL AND contact_id IS NOT NULL))
```

which is the database saying, in its own words, what the domain's sealed type says in Java.
Neither is the place the rule is *decided*; both refuse to store data the rule forbids.

`V2` seeds three demo users (decision D-13). Their bcrypt hashes are real, and the passwords
are in the README, which is exactly as insecure as it sounds and exactly what a demo wants.

## Tests — 36 in this module, all against a real PostgreSQL

| Test | What it pins |
|------|--------------|
| `SchemaMatchesTheMappingTest` | Hibernate runs with `ddl-auto: validate`, so the context starting **is** the assertion that every mapped column exists with a compatible type. Plus: the tables exist, the seed ran, and the database refuses an activity about nothing. |
| `RepositoryRoundTripTest` (18) | What goes in comes out — asserted on whole domain objects and whole `DealSnapshot`s, not on a field or two, because the defect a mapper produces is the field nobody thought to check. Money scale and currency, stage after a change, ordering oldest-first, find-by-owner and find-by-company both ways. |
| `BcryptPasswordCheckerTest` (6) | Against the real seeded hashes: right password, wrong password, another user's password, empty, null, unknown user. |
| `SpringTransactionsTest` (3) | Commit, **rollback of two writes when the work throws**, and the returned value. The `Transactions` port promised atomicity in Stage 2; this is where a real database is asked to keep the promise. |
| `PostgresDatabaseTest` (6) | Unchanged from Stage 2. |

## Three defects the gates caught during this stage

1. **`CHAR(3)` vs `VARCHAR(3)`.** `ddl-auto: validate` refused to start:
   *"wrong column type encountered in column [value_currency]"*. A silent `bpchar`
   space-padding difference, found before a single row was written.
2. **The Stage 2 review's prediction came true.** It warned that implementing
   `PasswordChecker` in the persistence adapter would recreate finding F-2.4 by injecting the
   `PasswordEncoder` bean that `adapter-web` declared. It would have. The bean moved to
   `CompositionRoot` before the code was written, and the persistence tests had to supply
   their own — which is the correct signal that the adapter does not own it.
3. **The shared test-jar published too much.** It shipped this module's whole test tree,
   including its `@SpringBootApplication`, onto `bootstrap`'s test classpath, where component
   scanning found it and its beans collided with the real composition root. The jar now
   publishes only `testing/**`: a shared artefact should contain exactly what is shared.

## Metrics

| Metric | `domain` | `application` | Gate |
|--------|---------:|--------------:|------|
| Line / branch coverage | 100 % / 100 % | 100 % / 100 % | ≥ 95 % |
| Mutation score | 125/125 | 86/86 | ≥ 90 % |
| Worst CRAP | 5.00 | 2.00 | ≤ 6 |

`adapter-persistence` is exempt from the coverage and CRAP gates by the opt-out granted in
Stage 0, on the stated grounds that it is held to account by integration tests against a real
database. It now has 36 of them, which is the promise being kept rather than cashed in.

## Deliberately not done

* Controllers, DTOs, HTTP error mapping — Stage 5.
* The Svelte UI — Stage 6.
* No optimistic locking. Two managers moving the same deal at once, last write wins. This is a
  gap, not an oversight: it needs a version column and a conflict response, and I would rather
  the reviewer decide whether a demo needs it than discover I silently skipped it.
