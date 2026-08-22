# Stage 6 hand-off — The Svelte frontend

## What is there

| Screen | What it does |
|--------|--------------|
| Login | Email and password, the server's own refusal message on failure. |
| Board | Six Kanban columns, drag-and-drop between them, weighted total per column, filter by owner, inline new-deal form. |
| Deal detail | Value, probability, weighted value, stage; the moves the server says are legal; reprice and reweight; the activity timeline and a form to add to it. |
| Companies & contacts | Create both, filter contacts by company, open a contact's timeline and log a note against it. |
| Forecast | Grouped by owner or stage, one line per group **and currency**, weighted values only. |

Svelte 5 with runes throughout (`$state`, `$derived`, `$props`, `$effect`). No router library:
one `Screen` union in `App.svelte`, because five screens do not need routing and a URL scheme
is not what this demo is demonstrating.

## No business rules in the browser

This is the stage's rule, so here is the evidence rather than the claim.

**The frontend does not know the pipeline.** `board.ts` holds `COLUMNS` — the order columns
appear in, a presentation choice — and `mayDropOn`, which is one line:

```ts
return deal !== null && deal.allowedTransitions.includes(stage);
```

That list is computed by `Deal.allowedTransitions()` in the domain and travels in `DealView`.
There is no stage machine, no "a win needs a meeting", no "only the owner may move it" anywhere
in `frontend/`. Grep for `CLOSED_WON` outside `COLUMNS` and the tests: nothing decides anything
with it.

`board.test.ts` has a test named
`never consults the stage machine itself`: it hands the component a deal sitting in `LEAD` whose
server said it may go straight to `CLOSED_WON` — nonsense in the real domain — and asserts the
browser **obeys**. If that test ever fails, somebody has taught the frontend the pipeline and
there are now two copies of a rule.

The illegal column is greyed out and refuses the drop. That is a courtesy, not enforcement:
`DealApiTest` already proves the server answers 409 to a client that sends the move anyway.

## Structure

* `lib/types.ts` — the API's shapes. No methods, no derived values.
* `lib/api.ts` — the only file that knows about URLs, headers and status codes. A refusal
  becomes an `ApiError` carrying the server's `error` code and message.
* `lib/board.ts`, `lib/format.ts` — pure functions, unit tested directly.
* `lib/session.svelte.ts` — who is signed in and the client that speaks for them.
* `components/`, `routes/` — presentation only. No component calls `fetch`.

## Proven by running it, not by describing it

The stack was started for real — PostgreSQL, the packaged Spring Boot jar, the Vite dev server —
and driven with a headless Chromium:

```
signed in as: Sam Sales (SALES)
columns: 6
company created
deal cards: 1
card text: Acme renewal | Acme Industries · Sam Sales | €12,000 | 50% → €6,000
offered moves: Qualified, Closed Lost
stage: Lead
```

`50% → €6,000` on a €12,000 deal is the server's weighted value, and `Qualified, Closed Lost`
is the domain's answer for a lead — both arrived over HTTP.

Drag-and-drop, twice:

```
before drag, stage = LEAD
after legal drag, stage = QUALIFIED     ← LEAD → QUALIFIED accepted
after illegal drag, stage = QUALIFIED   ← QUALIFIED → CLOSED_WON never offered
```

## The defect this found

`Api` held `fetch` in a field defaulted to the global function. Invoked as `this.fetcher(...)`
a browser throws **"Illegal invocation"**, because `fetch` must be called with `window` as its
receiver. **Every request failed**, and the user saw "Something went wrong talking to the
server" on a correct password.

Eighteen unit tests did not catch it: they all pass their own fetcher, which is a plain
function and binds fine. `npm run check` did not catch it — the types are correct. Only
starting the real thing in a real browser did. The constructor now wraps the call, with a
comment saying why.

## Metrics

* `svelte-check`: 312 files, **0 errors, 0 warnings** — including `noUncheckedIndexedAccess`,
  which found two unsafe array reads in my own tests and was obeyed rather than relaxed.
* 18 unit tests over the pure logic: board layout, drop eligibility, formatting, and the API
  client's headers, URLs and refusal handling.
* Production build: 63 kB JS, 5 kB CSS.

## Deliberately not done

* No Playwright suite yet — Stage 7 is QA procedures, and that is where they belong. What is
  here is a manual browser run whose output is quoted above.
* No optimistic UI. Every change round-trips and the board reloads, so what the screen shows is
  what the server holds. Slower, and never wrong.
* CORS: decided as **D-19** — the browser and the API share an origin in development (Vite
  proxy) and in the packaged demo (nginx), so there is no CORS problem to configure. The
  decision names what would change it.
