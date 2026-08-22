# PipelineCRM

A small sales CRM with a real pipeline, not a bag of forms. Deals move through
Lead → Qualified → Proposal → Negotiation → Won / Lost. The domain — not the
UI — decides whether a move is legal.

## What you can do

- Sign in with email and password, Google, or X
- Work a Kanban board with drag-and-drop stage changes
- Open a deal, read the activity timeline, log a note / call / meeting
- Create and edit companies, contacts, and deals
- Read a forecast: open-deal value × probability, by owner or by stage, never mixing currencies

## Business rules

- A deal cannot be marked won without a **positive value** and at least one **Call or Meeting on that deal**
- Only the **owner or a MANAGER** may change stage (or edit the deal)
- Won forces probability **100**; lost forces **0**
- Closed deals do not reopen
- The first person in an empty workspace is a manager and receives a seeded board

## Architecture

Clean Architecture, inward-only dependencies:

`src/domain` → `src/application` → `src/adapters` + `src/lib/crm-api.ts` → `src/routes`

See [CONSTITUTION.md](CONSTITUTION.md).

This App Builder host runs TanStack Start, React, and Postgres (Neon in
production, embedded PGLite in preview). The constitution maps the original
Spring / Svelte / PIT brief onto the tools that actually run here.

## Quality (measured)

| Gate | Result |
| ---- | ------ |
| Unit + Gherkin | 81 tests green (28 Gherkin scenarios) |
| Line coverage (domain + application) | 99.4% |
| CRAP | 132 methods, all ≤ 4 (budget 6) |
| Mutation | 7 relevant domain mutants, 0 survivors |
| Architecture | Dependency rule holds |
| UI QA | Sign-in, board, close-won rejection, win after meeting, forecast, companies |

```
npm run test:unit
npm run test:gherkin
npm run test:arch
npm run test:crap
npm run test:mutation
npm run typecheck
npm run build
```

Gherkin lives in `features/`. QA procedures for the real UI live in
`docs/qa/QA-PROCEDURES.md`. Review notes for each stage are in `docs/reviews/`.

## Compose (optional local Postgres)

The hosted preview does not need Docker. For a standalone Postgres:

```yaml
# docker-compose.yml
```

See `docker-compose.yml`. Set `DATABASE_URL` only in a real deployment — never
commit a `.env`.
