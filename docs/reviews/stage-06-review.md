# Stage 6 — Adversarial Review

STAGE 6 REJECTED

Reviewed on branch `local-grok-pipeline-crm` at `7f8fbc2` (`feat: add Svelte pipeline board, forms, and forecast UI`; working tree clean aside from this report). Scope: `CONSTITUTION.md` §2 (frontend consumes public HTTP API only), §5.4 / §7 (no domain rules in the Svelte UI), Stage 6 UI contract (login, Kanban + drag-and-drop, deal detail + activity timeline, create/edit Company/Contact/Deal/Activity, list/filter, forecast), `frontend/src/**`, Vite proxy, live `/api` against the already-running demo. Builder claims: `docs/reviews/stage-06-builder-handoff.md`. Did not inspect other git branches.

Commands / probes:

- `cd frontend && npm run check` — 0 errors, 0 warnings
- `cd frontend && npm run build` — Vite production build succeeded (`dist/` is gitignored)
- `POST /api/auth/login` as `sales@pipelinecrm.demo` / `password` — 200 JWT; wrong password — 404 `{"error":"not_found","message":"user"}`
- `GET /api/users` with Bearer — JSON keys `id`, `email`, `name`, `role` only (no `passwordHash`)
- Unauthenticated `GET /api/users` — 401
- Browser (isolated Chrome context) against `http://127.0.0.1:5173` with Vite proxy → `:8080`, Postgres `pipelinecrm-pg` up

Live UI session (sales user unless noted): login failure then success; Kanban render; HTML5 drag of a manager-owned card → 403; create deal; drag to `CLOSED_WON` without Call/Meeting → 409; skip `LEAD → NEGOTIATION` → 409; legal `LEAD → QUALIFIED` → 204 and card moved; deal detail edit; NOTE does not unlock win; MEETING then `CLOSED_WON` (probability 100 from server); company create + rename; contact create; forecast by owner/stage; owner filter `GET /api/deals?ownerId=…`; forged token → `#/login`; anonymous `#/board` → `#/login`.

---

## Contract checklist

| Requirement | Verdict | Evidence |
| --- | --- | --- |
| Login | **MET** | `Login.svelte` posts `/api/auth/login`, stores JWT, routes to `#/board`. Bad password surfaces API `message` (`user`). Anonymous `#/board` forced to login. |
| Pipeline board, columns by stage, DnD stage changes | **MET** | `Board.svelte` renders `STAGES` as drop columns. `ondragstart` / `ondrop` call `POST /api/deals/{id}/stage`. Live: 403 / 409 / 204 all fired; rejected cards stayed put; legal drop moved `Review deal` to QUALIFIED. |
| Deal detail + activity timeline | **MET** | `DealDetail.svelte` loads `GET /api/deals/{id}`, lists `deal.activities`, records `POST /api/activities`. Timeline showed NOTE then MEETING after live records. |
| Create/edit Company | **MET (crude)** | Create form + `PUT` via `prompt()` rename. Live create Globex + rename to Globex Ltd returned 201/204. Rename has no `try/catch` (residual). |
| Create/edit Contact | **NOT MET** | Create only. No `client.updateContact`, no edit control. Public `PUT /api/contacts/{id}` is unused. Builder hand-off only claimed “list/create”. |
| Create/edit Deal | **MET** | Board create + detail `PUT` title/amount/probability. Live title edit 204. |
| Create/edit Activity | **MET for the public API** | Activities are append-only: `ActivityController` is `POST` only; no `UpdateActivity` port. Deal-detail Record form is the write path. Do not invent a PUT. |
| Simple list/filter views | **MET, thin** | Company/contact tables; board owner filter hits the list API (`ownerId` query). Contacts table omits company and has no filter. |
| Basic forecast summary | **MET** | `GET /api/forecast?groupBy=owner\|stage&currency=USD`. UI copies `row.amount`. After closing the sales deal, only Manager / LEAD `250.00 USD` remained (open Acme expansion). |
| Consume public API only | **MET** | Single `fetch` helper; paths all `/api/…`. Vite `server.proxy['/api'] → localhost:8080`. No domain/application Java types. |
| No §7 rules in the UI (server is authority) | **MET** | No `canTransitionTo`, no Call/Meeting win-guard, no Σ(value × probability). Illegal drops/buttons POST and display the server message. Closed-won probability 100 appeared after reload, not a client force. |

---

## Attack results

| Attack | Result |
| --- | --- |
| Closed-won logic in Svelte | **Miss.** `DealDetail` and `Board` POST whatever stage the user picked. NOTE on the deal still 409 `closed-won requires a call or meeting on the deal`. After MEETING, `CLOSED_WON` 204 and subtitle `CLOSED_WON · 1000.00 USD · 100%`. Frontend does not call `ActivityType.qualifiesCloseWon`. |
| Forecast math in the client | **Miss.** `client.forecast` returns server buckets. Caption *describes* `value × probability / 100` but does not compute it. Closed deals vanished from the table after the live close; zeros on empty open stages match `GET /api/forecast`. |
| Missing main flows | **Hit — contact edit.** Login, board, DnD, deal CRUD, activity record, company create/rename, contact create, owner filter, forecast all ran. Contact update is specified and implemented on the server; the UI never calls it. |
| Unusable drag-and-drop | **Miss.** Chrome DnD of `article.deal-card` onto `section[aria-label=…]` issued `POST /stage`. 403/409 left the card in place and showed `p.error`. 204 then `load()` re-grouped the card. |
| Leaking password hashes | **Miss.** `User` has no hash field. Live `GET /api/users` body: two objects with `id,email,name,role`. UI prints `user.name` only. |
| Ignoring API errors | **Partial hit.** Board, login, deal detail, forecast `catch` and render `payload.message`. Company/contact `load()` and company `rename()` do not. 401 is still handled in `api.ts` (clear token, `#/login`) before the throw. |

---

## Leakage attack (business rules + dependency direction)

- Grep of `frontend/src` for `canTransitionTo`, `qualifiesCloseWon`, `value *`, `passwordHash`: none of the domain rules. `CLOSED_WON` appears as a column label, a CSS modifier, and a stage button — display, not a guard.
- `STAGES` in `types.ts` is a UI column list matching the public enum names. Drops/clicks still go to the server; illegal transitions 409 `cannot move from LEAD to NEGOTIATION`.
- `frontend/` compile deps are Svelte/Vite/TypeScript only. Runtime talks JSON over `/api`. No import of Java modules.
- `npm run check` clean under `strict`.

A Svelte UI that POSTs and displays 409/403 is the right shape. Do not “fix” that by encoding the stage machine in the browser.

---

## Critical

None.

---

## Important

### 1. Contact edit is missing; the public API already has it

**Severity:** Important
**Confidence:** 95

Stage 6 requires create **and** edit for Company, Contact, Deal, and Activity. Company rename, deal save, and activity record exist. Contact is create-only.

**Evidence:**

- `frontend/src/lib/api.ts` exports `createContact` / `contacts` and **no** `updateContact`. `updateCompany` sits on the adjacent lines.
- `frontend/src/pages/Contacts.svelte` is a create form + a two-column table (name, email). No rename, no inline edit, no row action. Company id is not even shown.
- `adapter-web/.../ContactController.java` `PUT /{id}` → `UpdateContactUseCase.Command(id, name, email)` (HTTP 204).
- `ContactServices.execute(UpdateContactUseCase.Command)` already does `rename` + `changeEmail` + `save`.
- Builder hand-off: “Company and contact list/create” — they did not claim the missing edit.

This is not “edit is optional because the demo is small”. The same page pattern as Companies (`Rename`) would have been enough. Activity has no update port, so append-only Record is acceptable there. Contact is not append-only.

**Required fix:**

1. Add `client.updateContact` → `PUT /api/contacts/{id}`. The write DTO currently reuses `ContactRequest` (`companyId` `@NotBlank`); send the existing `companyId` even though the use case ignores it, or thin the DTO. Do not invent a company-change rule in Svelte.
2. Surface edit in the contacts list (Companies-style rename, or an inline form) for **name and email**.
3. Show company on the row (the list is otherwise unusable once two companies exist — live: Pat Lee / Ada Globex with no company column).

Until a user can change a contact without curling the API, Stage 6 is not done.

---

## Residual minors (do not by themselves block)

1. **`DealDetail` only loads in `onMount`.** `dealId` is a `$props()` field. Hash change `#/deals/A` → `#/deals/B` without unmounting reused the Acme expansion heading while `location.hash` already pointed at the other deal; no second `GET`. The board-click path remounts (board branch vs `dealId` branch), so the happy path works. `$effect(() => { dealId; load(); })` (or a `{#key dealId}` block in `App.svelte`) is still required for a hash router.

2. **New-deal owner defaults to `users[0]`, not the session user.** `GET /api/users` returns Manager then Sales. Logged-in sales therefore creates manager-owned deals unless they change the select. First live drag of the seeded manager card was 403 `only the deal owner or a manager may change deal stage` — correct server rule, bad default. `localStorage.userId` is stored at login and never used.

3. **Company/contact `load()` and company `rename()` have no `try/catch`.** Create paths do. A 5xx/network error on list or rename is an unhandled rejection and an empty/`stale` table; `p.error` stays blank. 401 still redirects via `api.ts`.

4. **Company rename is `window.prompt`.** It works (live 204). It is not a form, not accessible, and ignores API errors (see 3). Fine as a stub; not the contact-edit substitute.

5. **Login failure copy is `user`.** That is `NotFoundException("user")` mapped to 404 (Stage 5 residual). The UI does not ignore the error; it prints the raw message. Demo-acceptable; do not document it as 401.

6. **Activity timestamps are raw ISO** (`2026-08-24T18:46:58.525236Z`). Timeline is ordered as returned, oldest first. Readable enough for a demo.

7. **`vite preview` has no `/api` proxy.** `vite.config.ts` only sets `server.proxy`. Relative `fetch('/api/...')` works on `:5173` (verified) and would 404 on `:4173` unless something else serves the API. CORS on 4173 is unused while the client stays same-origin. Stage 8 composition root can own this; do not hard-code `localhost:8080` in Svelte to “fix” it.

8. **All six stage buttons are always enabled**, including the current stage, backwards `LEAD`, and `CLOSED_WON`. Allowed by constitution (“may disable a button for UX; server is authority”). Keep it that way unless you add a *purely decorative* disable that still cannot be the real guard.

9. **No frontend tests.** Constitution parks UI QA in Stage 7 `docs/qa/`. Not a Stage 6 gate.

---

## What is already in good shape (do not rip out)

- Stage changes, win-guards, owner-or-manager, and forecast totals stay on the server. The UI POSTs and prints `ErrorBody.message`. That is the Stage 6 architecture, not a missing feature.
- HTML5 DnD is real (`text/plain` deal id, `preventDefault` on `dragover`, reload after 204). Do not replace it with a client-side state machine “for UX”.
- `api.ts` 401 path clears the token and sends the user to login (forged JWT live-tested). 204 handling is correct for PUT/stage.
- Forecast is a pair of tables over `/api/forecast`, not a `reduce` over the board’s deals.
- `GET /api/users` as consumed by the board has no password hash field; the UI never dumps the raw user JSON.
- `svelte-check` is clean. Keep it that way when adding contact edit.

Stage 6 is rejected on the contact-edit hole. Do not start Stage 7 until this report contains `STAGE 6 APPROVED`. The residuals above are not a waiver of contact update in the UI.
