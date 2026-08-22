# Stage 5 — API adapters

## Builder hand-off

- Thin `createServerFn` handlers in `src/lib/crm-api.ts`
- All CRM functions use `authMiddleware`
- Results are `{ ok, data | code, message }` — domain errors are not HTTP in the domain

## Adversarial review

**Finding 5.1** — Controllers that call `deal.stage =` would be a fail.

**Fix** — Handlers only call use cases.

**Finding 5.2** — Client-supplied actor ids.

**Fix** — `actorId` is always `context.userId`.

## STAGE 5 APPROVED
