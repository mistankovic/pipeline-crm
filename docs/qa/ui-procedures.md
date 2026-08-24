# PipelineCRM QA procedures (UI)

These steps exercise the **real Svelte UI** against the public REST API. They do not re-implement domain rules; they check that the server still enforces them.

**Demo users** (seeded on empty database):

| Email | Password | Role |
| --- | --- | --- |
| sales@pipelinecrm.demo | password | SALES |
| manager@pipelinecrm.demo | password | MANAGER |

**Start (local):** Postgres on `5432` (`pipelinecrm`/`pipelinecrm`/`pipelinecrm`), backend on `8080`, frontend `npm run dev` on `5173` (Vite proxies `/api`).

Expected: each step’s **Pass** column holds after a human (or the browser session recorded in Stage 6) follows it.

## P1 — Login

1. Open `/#/login`.
2. Submit a wrong password → error text from the API, stay on login.
3. Submit `sales@pipelinecrm.demo` / `password` → `#/board`.
4. Open `/#/board` in a private window with no token → redirected to login.

## P2 — Companies and contacts

1. Companies → create `Acme` → row appears.
2. Rename it (Rename) → new name in the table.
3. Contacts → create `Pat Lee` / `pat@acme.com` on Acme → row appears.
4. Edit contact name/email → table shows the new values.

## P3 — Pipeline board

1. Add deal titled `Sales pipeline`, owner **Sales**, amount `1000.00`, probability `25`.
2. Card appears in **LEAD**.
3. Owner filter → Sales shows only that owner’s cards.

## P4 — Stage change (server is the authority)

1. Logged in as Sales, open a **Manager**-owned deal and click `CLOSED_WON` (or drag) → error: only owner or manager may change stage; card stays put.
2. On your own LEAD deal, drag/click to **NEGOTIATION** (skip) → conflict; still LEAD.
3. Click **QUALIFIED** → card/detail shows QUALIFIED.
4. Closed-won **without** Call/Meeting → conflict `closed-won requires a call or meeting on the deal`.
5. Record a **MEETING** on the deal, then **CLOSED_WON** → stage `CLOSED_WON`, probability **100**.

## P5 — Deal detail and activity

1. Open a deal from the board.
2. Edit title and Save → title updates.
3. Record NOTE then MEETING; timeline lists both with timestamps.

## P6 — Forecast

1. Open Forecast.
2. Open deals contribute `value × probability / 100` **as returned by the API**.
3. Closed-won deals disappear from owner/stage buckets (empty open stages still listed at 0.00).

## Pass log (this branch)

Followed on 2026-08-24 against Vite `127.0.0.1:5173` + Spring Boot `:8080` + Postgres 16. All procedures **PASS**. Contact edit added after Stage 6 first review and re-verified (Edit button + `PUT /api/contacts/{id}`).
