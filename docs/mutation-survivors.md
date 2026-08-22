# Surviving mutants

Constitution §3 requires every mutant that survives PIT to be justified here in writing.
This is a review-gate obligation: no tool reads this file, so an empty list here is a claim
the reviewer must check against the PIT report.

## Current state

| Module | Mutants | Killed | Score | Survivors |
|--------|--------:|-------:|------:|-----------|
| `domain` | 125 | 125 | **100 %** | none |
| `application` | 78 | 78 | **100 %** | none |

There are no survivors to justify. Reproduce with `mvn -f backend/pom.xml verify`; the
reports land in `backend/*/target/pit-reports/`.

## Survivors that existed and how they were killed

Kept because the *reason* a mutant survived is usually a defect in the tests, and that is
worth remembering.

### `Deal.hashCode` → `return 0` (Stage 1)

Survived because nothing asserted what a deal hashes on. Killed by specifying the actual
requirement — a deal hashes by identity, so it stays findable in a `HashSet` while its own
state changes — rather than by excluding `hashCode` from mutation.

### `DealMutations.mutate` and `DealHistory.save` → removed call to `save` (Stage 3)

Both survived for the same reason, and it was not a missing assertion: `InMemoryDeals`
stored and returned **the same object** the caller had just mutated. Every test passed with
the save deleted, because the change was already visible in the store. The fix was in the
test double, which now stores and returns copies. A fake that cannot report a missing write
is worse than no fake, and only mutation testing was going to say so.

### `DealHistory.everyDeal` → `NO_COVERAGE` (Stage 3)

Not a test gap. The method had no callers at all: dead code, deleted.
