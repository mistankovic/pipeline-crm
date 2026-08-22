# Stage 1 — Domain Model + Gherkin

## Builder hand-off

- Rich aggregates: `Deal`, `Activity`, `CrmUser`, `Company`, `Contact`
- Value objects: `Money` (minor units), `Probability`, `DealStage`, `Role`, `EmailAddress`, `ActivityType`, `Actor`
- Gherkin: stage transitions, close-won guards, ownership, forecasting, activities
- Use-case ports in `src/application/ports.ts`

## Adversarial review

**Finding 1.1** — Anemic `Deal` with public setters would push rules into services.

**Fix** — `Deal.changeStage` / `closeWon` / `closeLost` own the guards.

**Finding 1.2** — Mixed-currency forecast summed into one number is a lie.

**Fix** — `forecastOpenDeals` keeps currencies separate.

**Finding 1.3** — Close-won could be satisfied by a meeting on another deal.

**Fix** — `Activity.countsTowardCloseWon(dealId)` requires the same deal.

**Finding 1.4** — Gherkin “sales user” was ambiguous vs first-user-is-manager.

**Fix** — Acceptance world forces the stated role after profile creation.

## STAGE 1 APPROVED
