# Stage 6 — Adversarial Review (round 1)

I ran the real thing, as the Builder did, but I signed in as **Robin** — the salesperson who
does not own the deal.

## Verified

* **No business rule in the browser.** I grepped for every piece of domain vocabulary. Outside
  `COLUMNS` and the tests, the only occurrences are `types.ts` declaring the shape of the data
  and one `<option value="MEETING">`. No stage machine, no ownership check, no win condition.
* **No component calls `fetch`.** All HTTP is behind `Api`.
* **`never consults the stage machine itself`** is the right test and I want it kept forever.
* **The "Illegal invocation" find is excellent** and the write-up is honest about why eighteen
  green unit tests and a clean type-check all missed it.

Then I dragged Sam's deal as Robin.

## F-6.1 — BLOCKER. Every refusal in the application is silently swallowed.

```
robin sees a deal owned by: Acme Industries · Sam Sales
error banner shown to Robin: 0
banner text: NONE
stage after Robin dragged: QUALIFIED
moves offered to Robin: Proposal, Closed Lost
after Robin clicks a move, error: NONE
```

Robin drags a card he may not move. The server refuses with **403**, correctly. The card snaps
back. **Nothing is displayed.** He clicks the "Proposal" button on the detail screen: same
silence. As far as the user can tell, the application is broken and ignoring them.

The cause is two lines apart:

```js
try { await session.api.moveDeal(moving.id, stage); }
catch (refused) { failure = refused; }
await load();                       // and load() begins:  failure = null;
```

The error is caught, assigned, and then wiped by the reload on the very next line. The same
shape is in `DealDetail.attempt`, which is every write on that screen. `ErrorBanner` is
effectively dead code everywhere except the login form, which is the one screen that does not
reload afterwards.

This is worse than a missing feature. The whole argument of this project is that the domain
refuses things and the refusal is *carried outward faithfully* — 403s and 409s were designed
carefully, `ApiError` preserves the server's own words, and `ErrorBanner` exists to show them.
All of that machinery works, and then the last four inches throw the message away.

**Required:** a refusal survives the reload that follows it. And a test that would have caught
this — the eighteen unit tests cover pure functions, and no test drives a component through a
failing call.

## F-6.2 — MAJOR. The board invites moves it knows will be refused.

`Deal.allowedTransitions()` answers *"where could this deal go"*. It does not take an actor, so
it cannot answer *"where may **you** move it"*. The browser treats the first as if it were the
second: Robin's board highlights Proposal as a welcoming drop target, and the detail screen
offers him two buttons, all four of which the server will refuse with 403.

So the affordance is wrong for exactly the user who most needs it to be right. This is not a
frontend bug — the frontend is faithfully showing what it was told. The **view** is missing
information the domain already has: `Deal.requireAuthority` knows the answer.

Options, and I do not mind which:
* `DealView.allowedTransitions` becomes actor-aware — the use case knows who is asking, and the
  domain can be asked `transitionsAllowedFor(user)`.
* Or the view gains a plain `youMayChangeThis` flag and the UI stops offering any move on a deal
  the caller cannot touch.

What is not acceptable is the browser deciding it, by comparing `deal.owner.id` to the session
user. That would be rule 2 reimplemented in TypeScript, and it would be wrong the moment a
manager signs in.

## F-6.3 — MAJOR. An expired token leaves the user apparently signed in, forever.

`session.svelte.ts` stores the token and never reconsiders it. `Api` turns a 401 into an
`ApiError` like any other. Nothing anywhere calls `signOut()`.

After eight hours the token expires. The board then shows an error — or, given F-6.1, shows
nothing at all — the user is still "signed in as Sam Sales", and no amount of clicking helps.
The only recovery is to know that Sign out exists and press it.

A 401 from any call means the session is over. Ending it is one line, and it is the difference
between "please sign in again" and "this application is broken".

## F-6.4 — MINOR. The activity types are written down twice.

`types.ts` declares `'NOTE' | 'CALL' | 'MEETING'`, and `DealDetail.svelte` hard-codes the same
three as `<option>` elements. Add a fourth type to the domain and the dropdown silently keeps
offering three. The list should be derived from one place — and ideally from the server, the
way `allowedTransitions` already is.

## F-6.5 — MINOR. A slow or failing load looks identical to an empty pipeline.

There is no loading state. Between mount and the first response the board renders six empty
columns and "nothing here" six times. Combined with F-6.1, a failed load renders as a
confidently empty pipeline. A user would believe it.

---

## Verdict

The architecture of this frontend is right: no rules, no `fetch` in components, pure logic
tested directly, and a test that actively defends the boundary. The Builder also found a real
defect by running the real thing, which is the right instinct.

But F-6.1 means the application never tells the user why anything was refused — in a system
whose entire point is careful, faithful refusals. And F-6.2 means it actively invites the
refusals it then hides.

**STAGE 6 NOT APPROVED.** Fix F-6.1, F-6.2 and F-6.3. F-6.4 and F-6.5 may be fixed or refused
in writing.
