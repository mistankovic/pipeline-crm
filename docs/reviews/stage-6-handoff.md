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

---

# Stage 6 hand-off — round 2 (response to review)

| Finding | Disposition |
|---------|-------------|
| F-6.1 every refusal silently swallowed | **Fixed, and the fix is a class, not a patch.** `Failures.attempt(action, reload)` runs both as one unit: only `attempt` may clear the error, and only *before* the action runs, so a reload cannot wipe what the action just recorded. Every screen and the new-deal form use it; not one of them keeps its own `failure` variable any more. Eight new tests in `failures.test.ts`. **I proved the test catches the original defect** by reintroducing the clear-on-reload line: `× KEEPS the refusal through the reload that follows it`. |
| F-6.2 the board invites moves it knows will be refused | **Fixed in the domain, as the review demanded.** `Deal.transitionsAllowedFor(User)` and `Deal.mayBeChangedBy(User)` answer the question the view was pretending to answer. `DealView` gained `youMayChangeThis` and its `allowedTransitions` is now the caller's list. Crucially there is **no** overload of `DealViews` that omits the caller — the first version defaulted it to the owner, which was the bug wearing a hat — so every read had to start naming who is asking: `ViewPipeline.everything(callerId)`, `ViewDeal.handle(dealId, callerId)`. Nine domain tests, five use-case tests and three Gherkin scenarios pin it; three API tests pin the JSON. |
| F-6.3 an expired token leaves the user stuck | **Fixed.** Any 401, from any call, ends the session. Two tests. |
| F-6.4 activity types written down twice | **Fixed.** `ACTIVITY_TYPES` is declared once in `types.ts`; the dropdown is built from it and the `ActivityType` union is derived from it. |
| F-6.5 a slow load looks like an empty pipeline | **Fixed.** The board shows "Loading the pipeline…" until the first response arrives, so an empty board only ever means an empty board. |

## Verified in a browser, not in an argument

Two users, side by side, against the real stack:

```
SAM   card draggable: true
ROBIN card draggable: false
ROBIN stage after drag: LEAD                 ← the card would not move
ROBIN moves offered: 0
ROBIN told why: Sam Sales owns this deal. Only they or a manager may change it.
ROBIN revise controls: 0
```

Robin is no longer *invited* to do the thing that gets refused, and is told why in words.

And a refusal Sam can still legitimately trigger — winning a deal with no meeting logged:

```
stage now: Negotiation
F-6.1 banner shown: 1
F-6.1 banner text : this deal cannot be won: no call or meeting has been logged against it
stage unchanged   : Negotiation
```

That sentence came from `WinRequiresValueAndEngagement` in the domain, through the 409, through
`ApiError`, onto the screen — the whole chain this project exists to demonstrate, working, with
the user finally on the end of it.

Finally, a tampered token:

```
F-6.3 back at sign-in: true
```

## What the browser found that nothing else could

Both of this stage's real defects were invisible to the type checker and to every unit test:

1. `fetch` held in a field throws "Illegal invocation" — types fine, tests fine, application
   completely broken.
2. Every refusal wiped by the following reload — types fine, tests fine, and the one user who
   most needed an explanation got silence.

The lesson is the same one as F-3.1 in a different medium: **the tests covered the code that
had bugs, and proved nothing about it.** There is now a unit test for the second, written after
the fact, and Stage 7's QA procedures exist for the first.

## Metrics (round 2)

* `svelte-check`: 313 files, 0 errors, 0 warnings.
* **26 frontend unit tests** (was 18).
* `domain` 128 methods, worst CRAP 5.00, 100 % coverage, 100 % mutation.
* `application` 151 tests including **72 Gherkin scenarios**, 88/88 mutants, worst CRAP 2.00.
