# Stage 4 — Persistence

## Builder hand-off

- `createSqlPorts()` maps rows ↔ domain snapshots
- UPSERT adapters; no business rules in SQL
- Clock and ids live in `src/adapters/system.ts`

## Adversarial review

**Finding 4.1** — JPA-style annotations on entities would pollute the domain.

**Fix** — Domain snapshots are plain data; mapping is adapter-only.

**Finding 4.2** — `timestamptz` may arrive as Date or string.

**Fix** — `occurredAt()` normalizes both.

## STAGE 4 APPROVED
