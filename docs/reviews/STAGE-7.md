# Stage 7 — QA + hardening

## Builder hand-off

- `docs/qa/QA-PROCEDURES.md`
- CRAP: 132 methods, all ≤ 4
- Mutation: 7 killed, 0 survivors
- Coverage: 99.4% lines on domain + application
- Live UI: account create → board → new deal → rejected won → meeting → won at 100% → forecast → companies

## Adversarial review

**Finding 7.1** — `minorUnits > 0` → `> 1` survived.

**Fix** — One-cent close-won unit test.

**Finding 7.2** — Draggable `<Link>` swallowed clicks, so deal detail never opened.

**Fix** — Card is an `<article>` with `onClick` navigation; drag still uses HTML5 DnD.

**Finding 7.3** — Close-won QA against a seeded deal failed for a second SALES user (not owner).

**Fix** — QA creates a deal owned by the signed-in user.

## STAGE 7 APPROVED
