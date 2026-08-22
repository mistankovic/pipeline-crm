# QA procedures

These are written to be followed **by a person**, with a browser, without reading any code.
Each one is also scripted, in `frontend/qa/`, one spec file per section and one test per
numbered step. Where the two disagree, **this document is the specification** and the script
is wrong.

Every procedure states what to do and what you must see. "You must see" means exactly that: if
the screen does something else — including doing nothing — the procedure has failed, even when
no error appears. Two of this project's worst defects were the application doing nothing
quietly, so silence is a failure condition here, not a neutral outcome.

---

## Before you start

1. A PostgreSQL is running and empty of PipelineCRM tables, or the demo compose stack is up
   (`docker compose up`, see the README).
2. The backend is running and `GET /actuator/health` returns `{"status":"UP"}`.
3. The frontend is running and reachable at http://localhost:5173.
4. You know the three seeded users. They are in the README and in
   `V2__demo_users.sql`; this is a demo and the passwords are deliberately public:

   | Who | Email | Password | Role |
   |-----|-------|----------|------|
   | Sam Sales | `sam@pipelinecrm.demo` | `sam-password` | SALES |
   | Robin Reid | `robin@pipelinecrm.demo` | `robin-password` | SALES |
   | Mo Mancini | `mo@pipelinecrm.demo` | `mo-password` | MANAGER |

5. When a procedure says "create a company called X", use a name nothing else has used. The
   demo database is shared and nothing cleans it between runs.

---

## QA-1 — Signing in

| # | Do this | You must see |
|---|---------|--------------|
| 1.1 | Sign in as Sam with the right password. | The pipeline board, six columns (Lead, Qualified, Proposal, Negotiation, Closed Won, Closed Lost), and "Sam Sales (SALES)" in the top right. |
| 1.2 | Sign out, then sign in as Sam with the password `not-sams-password`. | A red banner reading exactly **"email address or password is incorrect"**. You are still on the sign-in screen. |
| 1.3 | Sign in as `nobody@pipelinecrm.demo` with Sam's password. | **The identical banner, word for word.** If this message differs from 1.2 in any way, the system is telling an attacker which accounts exist — that is a defect, report it. |
| 1.4 | Sign in correctly, then reload the page. | Still signed in. |
| 1.5 | Press **Sign out**. | The sign-in screen. |
| 1.6 | Sign in, then in the browser console run `JSON.parse(localStorage['pipelinecrm.session'])`, change the last six characters of `token`, put it back, and reload. | The sign-in screen. You must **not** be left apparently signed in with a broken session. |

## QA-2 — Companies and contacts

| # | Do this | You must see |
|---|---------|--------------|
| 2.1 | As Sam, open **Companies** and add a company. | It appears in the company list. |
| 2.2 | Add a company whose name is only spaces. | A red banner. The company is not added. |
| 2.3 | Add a contact at that company, then set **Filter by company** to it. | The contact is listed, and only contacts at that company are listed. |
| 2.4 | Add a contact whose email is `not-an-address`. | A red banner containing **"not an email address"** — the domain's own words, not a framework's. |
| 2.5 | Click a contact's name, type a note, press **Log note**. | The note appears on the contact's timeline, attributed to Sam Sales with a timestamp. |

## QA-3 — The pipeline board

| # | Do this | You must see |
|---|---------|--------------|
| 3.1 | As Sam, create a deal worth 12000 EUR at 50%. | A card in **Lead** showing €12,000 and a weighted €6,000. |
| 3.2 | Drag the card from Lead to Qualified. | The card moves and stays there after a reload. |
| 3.3 | Drag a Lead card onto **Proposal**. | The Proposal column does **not** light up and the card does not move. A deal advances one stage at a time. |
| 3.4 | Open the deal (click the card). | Under **Move**, exactly two buttons: *Qualified* and *Closed Lost*. No *Closed Won*, no *Proposal*. |
| 3.5 | On a Lead deal at 80%, press **Closed Lost**. | Stage becomes Closed Lost and the probability becomes **0%**, whatever it was before. |
| 3.6 | On the board, set the **Owner** filter to Robin Reid. | Only Robin's deals. |

## QA-4 — Winning a deal

| # | Do this | You must see |
|---|---------|--------------|
| 4.1 | Take a deal to Negotiation without logging anything, then press **Closed Won**. | A red banner containing **"no call or meeting has been logged against it"**, and the stage still Negotiation. |
| 4.2 | Log a **Meeting** on that deal, then press **Closed Won**. | Stage becomes Closed Won and the probability becomes **100%**. |
| 4.3 | On the won deal, look at **Move**, then try to change its value. | No move buttons at all. Changing the value produces a banner containing **"can no longer be changed"**, and the value is unchanged. |
| 4.4 | Log a Note then a Call on a deal. | Both on the timeline, **oldest first**, each showing who recorded it and when. |

## QA-5 — Who may move a deal

This needs **two browsers** (or one browser and one private window), signed in as two people.

| # | Do this | You must see |
|---|---------|--------------|
| 5.1 | As Sam, create a deal. In the other browser, sign in as Robin and find it. | Robin's copy of the card is visibly different (dashed, faded) and **cannot be dragged**. Dragging it does nothing. Opening it shows **"Sam Sales owns this deal. Only they or a manager may change it."** and no move buttons and no value/probability fields. |
| 5.2 | Back as Sam, drag the same card to Qualified. | It moves. |
| 5.3 | Sign in as Mo (a manager) and open Sam's deal. | The card is draggable and the move buttons are there. Moving it works. |
| 5.4 | As Sam, log an activity on the deal. As Robin, open it. | Robin can **read** the deal and its timeline. Not being allowed to change something is not the same as not being allowed to see it. |

## QA-6 — Revising and forecasting

| # | Do this | You must see |
|---|---------|--------------|
| 6.1 | On an open deal at 50%, change the value to 4000 and save. | Value €4,000 and weighted €2,000. |
| 6.2 | Change the probability to 25 and save. | 25% and the weighted value a quarter of the value. |
| 6.3 | Open **Forecast**, grouped by Owner. | A row per owner, labelled with the person's **name**, not an id. |
| 6.4 | Switch to grouped by Stage. | A row per stage that has open deals. |
| 6.5 | Note the Lead total. Create a Lead deal worth 777000 at 100%. Note the total again. Mark that deal Closed Lost. Note it a third time. | The total rises by exactly 777,000, then returns to exactly what it was. Won and lost deals are history and do not appear in a forecast. |
| 6.6 | Create a deal in **USD**. Open the forecast. | A separate line in dollars. Currencies are never added together. |

## QA-7 — When things go wrong

Added after hostile QA found a request a signed-in user could make and **be logged out by**.
The other six sections walk the happy paths and the business refusals; this one asks what the
application does when something actually breaks.

| # | Do this | You must see |
|---|---------|--------------|
| 7.1 | Create a deal with a value of `99999999999999999999`. | A red banner containing **"must not exceed"**, and you are **still signed in**. A data error must never end your session. |
| 7.2 | Create a company called `Bobby'); DROP TABLE deals;--`. | It appears in the list with that exact name, and the board still shows six columns. The name is data, not a command. |
| 7.3 | Create a company whose name contains `<img src=x onerror="...">`. | The name is displayed as text. Nothing executes. |

---

## Recording a run

Write the date, who ran it, the commit, and one line per procedure: pass, or what you saw
instead. A procedure that was skipped is not a pass. The most recent recorded run is in
`docs/qa/runs/`.

## Running the scripted form

```bash
cd frontend
npm run test:qa                 # needs the backend and a database running
```

The script starts the frontend itself and reuses a backend that is already up. It leaves an
HTML report in `frontend/playwright-report/`.

If Playwright cannot find a browser it will say so and every procedure will fail at launch,
which looks alarming but says nothing about the application. Point it at a browser you already
have rather than downloading one:

```bash
PLAYWRIGHT_CHROMIUM_PATH=/path/to/chrome npm run test:qa
```

This was needed throughout this project's own development, because the pinned browser build
could not be downloaded here. A launch failure is never a QA result: fix the browser path and
run again before recording anything.
