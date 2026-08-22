# Stage 3 — Use cases + tests

## Builder hand-off

- Use cases for profile, company, contact, deal, activity, forecast, bootstrap, workspace
- 80 Vitest tests including 28 Gherkin scenarios, all green
- Line coverage on domain + application ≈ 99% (barrels excluded)

## Adversarial review

**Finding 3.1** — Missing not-found paths and unknown currency.

**Fix** — `src/application/more-coverage.test.ts`

**Finding 3.2** — Seed data must still go through use cases so close-won cannot cheat.

**Fix** — `seedIfEmpty` calls `createDeal` / `createActivity` / `changeDealStage`

## STAGE 3 APPROVED
