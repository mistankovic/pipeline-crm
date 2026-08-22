# Stage 0 — Project Constitution & Quality Gates

## Builder hand-off

- `CONSTITUTION.md` — Clean Architecture rules, CRAP formula, test kinds,
  sandbox stack mapping, dependency rule.
- `.editorconfig`
- Quality commands named so they can be wired as fitness functions.

## Adversarial review

**Finding 0.1 (medium)** — The brief asked for Spring Boot + Svelte.
Shipping TypeScript without documenting the constraint would be a silent
scope change.

**Fix** — Constitution §2 records the sandbox preview contract and maps
every quality tool to an equivalent. The Dependency Rule is not relaxed.

**Finding 0.2 (high)** — A constitution that cannot fail the build is
theatre.

**Fix** — §3 and §10 name the architecture test and the quality scripts
that will exist from Stage 2 onward. Stage 0 only defines the law; later
stages implement the gates. This is accepted because empty fitness
functions that pass are worse than named, not-yet-wired ones.

**Finding 0.3 (low)** — CRAP target of 6 without a formula is unenforceable.

**Fix** — Formula and coverage definition are in §7.

## STAGE 0 APPROVED

The constitution is precise enough to reject later work that leaks
framework types inward or hides business rules in controllers/UI.
