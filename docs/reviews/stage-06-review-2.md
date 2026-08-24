# Stage 6 — Adversarial Re-Review

STAGE 6 APPROVED

Reviewed on branch `local-grok-pipeline-crm` at `37aff63` (`fix: add contact edit in the Svelte UI`; working tree clean aside from this report). Scope: prior REJECTED review `docs/reviews/stage-06-review.md` at `7f8fbc2`, builder claims `docs/reviews/stage-06-fixes.md`, and remaining Stage 6 UI contract at HEAD (`frontend/src/**`, Vite proxy, public `PUT /api/contacts/{id}`). Did not inspect other git branches.

Commands / probes:

- `cd frontend && npm run check` — 0 errors, 0 warnings
- `POST /api/auth/login` as `sales@pipelinecrm.demo` / `password` — 200 JWT
- Unauthenticated `GET /api/users` — 401
- Authenticated `GET /api/users` — JSON keys `id`, `email`, `name`, `role` only (no `passwordHash`)
- Direct `PUT /api/contacts/{id}` with existing `companyId` + new name — 204; name persisted on subsequent `GET /api/contacts`; reverted
- Isolated Chrome context against `http://127.0.0.1:5173` with Vite proxy → `:8080`

Live UI (sales user): login; Contacts table shows Ada Globex / Pat Lee with an **Edit** button on each row; stubbed `prompt()` for name then email issued `PUT /api/contacts/1424464c-5338-412a-8f95-0179bec182a2` via the Vite proxy (204), table updated to `Pat Lee UI`, then reverted the same way. Board create-deal owner select defaulted to **Sales**, not Manager (`users[0]`).

---

## Previous Important findings

### 1. Contact edit is missing; the public API already has it

**Verdict: ADDRESSED**
**Confidence:** 95

A user can change a contact’s name and email from the Svelte UI without curling the API. That was the reject criterion.

**Required-fix items from the first review:**

| # | Required | Claimed | Actual |
| --- | --- | --- | --- |
| 1 | `client.updateContact` → `PUT /api/contacts/{id}`; send existing `companyId` because `ContactRequest.companyId` is `@NotBlank`; do not invent a company-change rule in Svelte | Yes | **Done.** `frontend/src/lib/api.ts:55-59`. Body is `{ companyId, name, email }`. Use case still ignores `companyId` (`ContactController.java:56` → `UpdateContactUseCase.Command(id, name, email)`). |
| 2 | Surface edit in the contacts list for **name and email** | Yes | **Done.** `Contacts.svelte` `edit()` uses two `prompt()`s (name, then email; blank email → `null`) and an Edit button per row. Live click (stubbed prompts) fired the PUT above and reloaded the table. |
| 3 | Show company on the row | Not claimed | **Not done.** Table is still Name / Email / Edit. Two companies exist live (Acme, Globex Ltd) and two contacts with distinct `companyId`s. See residuals. |

`npm run check` stayed clean after the addition.

Item 3 is **not** re-raised as Important. The first review already marked “simple list/filter views” **MET, thin** with the company column omitted; the reject line was “until a user can change a contact without curling the API.” That hole is closed. Re-rejecting on the display column would be moving the goalpost.

---

## Contract checklist (delta vs first review)

| Requirement | First review | This pass |
| --- | --- | --- |
| Create/edit Contact | **NOT MET** | **MET (crude)** — create form + `prompt()` edit, same grade as company rename. Live PUT 204. |
| Create/edit Company | MET (crude) | Unchanged; `rename()` now `try/catch`es. |
| Create/edit Deal | MET | Unchanged. |
| Create/edit Activity | MET for the public API (append-only) | Unchanged. Do not invent a PUT. |
| Login / board / DnD / deal detail / forecast / public API only / no §7 in UI | MET | Re-grepped; not re-litigated. `STAGES` is still a column list; `changeStage` still POSTs and displays the server error. |

---

## Claimed extra residuals (not the reject)

| Claim in `stage-06-fixes.md` | Verdict | Evidence |
| --- | --- | --- |
| Company/contact `load()` and company `rename()` surface API errors | **Done** | `Companies.svelte` `load`/`rename` and `Contacts.svelte` `load`/`edit` wrap in `try/catch` and write `error`. |
| New deals default to the logged-in user when that user exists | **Done** | `Board.svelte`: `ownerId` from `localStorage.userId` if present in `GET /api/users`, else `users[0]`. Live sales session: create-deal owner combobox value **Sales**. |

---

## Leakage attack (re-check)

- Grep of `frontend/src` for `canTransitionTo`, `qualifiesCloseWon`, `passwordHash`: none. `CLOSED_WON` remains a column label in `types.ts` only.
- Forecast caption still *describes* `value × probability / 100` and still does not compute it.
- Single `fetch` helper; all paths `/api/…`. `updateContact` is the same helper as `updateCompany`.
- No domain/application Java types imported. `svelte-check` clean under `strict`.
- Live `GET /api/users` still `{id,email,name,role}`.

Do not “fix” 409/403 by encoding the stage machine in the browser. That architecture is still correct.

---

## Attack results (this pass)

| Attack | Result |
| --- | --- |
| Missing contact edit | **Miss now.** Source has `updateContact` + Edit. Live UI PUT 204 through `:5173`. Direct API PUT 204 independently. |
| Invent company-change in Svelte | **Miss.** Edit sends `contact.companyId` only; no company picker on the row. |
| Domain rules in the UI | **Miss.** Same as first review. |
| Password hashes in `/api/users` | **Miss.** |
| Ignoring API errors on contact update | **Miss.** `edit()` catches and renders `p.error`. |

---

## Critical

None.

---

## Important

None. Previous Important 1 is closed. No new Important issues in the fix diff.

---

## Residual minors (do not by themselves block)

Carried from the first review unless noted.

1. **Contacts table still omits company.** First-review required-fix item 3. Live: Pat Lee vs Ada Globex, two companies, no company column. List/filter was already MET. Not a Stage 6 reject.

2. **`DealDetail` only loads in `onMount`.** Hash change `#/deals/A` → `#/deals/B` without unmount can reuse the first deal. Board-click remounts. `$effect` / `{#key dealId}` still missing.

3. **Company rename and contact edit are `window.prompt`.** Live contact edit worked. Not a form, not accessible, two sequential dialogs. Fine as a stub.

4. **`if (!nextName) return` treats Cancel and empty name the same.** Email Cancel is `=== null` (correct). Name Cancel is indistinguishable from clearing the field. Server still `@NotBlank`s name if a blank ever got through.

5. **Login failure copy is `user`.** Stage 5 residual (`NotFoundException("user")` → 404). UI prints the raw message.

6. **Activity timestamps are raw ISO.** Timeline ordered as returned.

7. **`vite preview` has no `/api` proxy.** `server.proxy` only. Stage 8 composition root can own this.

8. **All six stage buttons stay enabled.** Allowed by constitution. Keep it.

9. **No frontend tests.** Parked in Stage 7 `docs/qa/`. Not a Stage 6 gate.

---

## What is already in good shape (do not rip out)

- Contact update reuses `ContactRequest` and the existing company id. Do not add a client-side “move contact to another company” rule the use case does not implement.
- Stage guards, win-guards, owner-or-manager, and forecast totals stay on the server.
- HTML5 DnD, 401 → `#/login`, 204 handling, forecast as `/api/forecast` tables, `svelte-check` clean.

Stage 7 may start. Residuals above are not a waiver of the company column if a later UI pass cares, and they are not a reason to reopen Stage 6.
