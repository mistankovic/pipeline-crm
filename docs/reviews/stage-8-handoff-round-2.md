# Stage 8 hand-off — round 2

**Builder → Adversarial Reviewer**

Answering F-8.6, F-8.7 and F-8.8. The review was right, and F-8.6 in particular is the finding
I should have raised myself seven stages ago.

---

## F-8.6 — Company and contact editing (implemented, not waived)

The review offered two ways out and said only one was honest. I implemented it.

**Domain.** Two methods, both of which keep identity rather than create:

```java
public Company renamedTo(String newName)                          // Company
public Contact correctedTo(String newName, EmailAddress newEmail) // Contact
```

Both are one line and both go back through the record's compact constructor, so a rename obeys
exactly the same name rule as a creation. Nothing new validates anything — the invariant was
already in one place and stayed there.

`Contact.correctedTo` deliberately takes no company. Moving a person between employers is a
different decision, recorded as **D-21**.

**Application.** `RenameCompany` and `CorrectContact` input ports, two interactors, both
transactional, both using the existing `Parties`/`Required` lookups so the "no company with id
…" wording is unchanged. No new output ports: `save` on an existing id was already the contract.

**Persistence.** No new code. Four tests were added because "save updates rather than inserts"
was an assumption about Spring Data's `isNew()`, and an assumption about the database is not a
fact until PostgreSQL says so. Two of them assert no second row is left behind.

**Web.** `PATCH /api/companies/{id}` and `PATCH /api/contacts/{id}`. The contact request record
has no `companyId` field, and there is an API test that sends one anyway and proves the contact
does not move.

**UI.** Inline **Rename** and **Edit** on each row of the directory, one row at a time.

**QA.** Six new procedures, 2.6–2.11.

### Gates after the change

| Module | Line | Branch | Mutation | Methods | Worst CRAP |
|--------|------|--------|----------|---------|-----------|
| `domain` | 100.00 % (305/305) | 100.00 % (69/69) | **126/126** | 131 | 4.00 |
| `application` | 100.00 % (297/297) | 100.00 % (16/16) | **96/96** | 136 | 2.00 |

Still 100 %, still zero survivors, CRAP unmoved. The bar did not drop for new code.

| Suite | Before | After |
|-------|--------|-------|
| `domain` unit | 267 | 273 |
| `application` unit + Gherkin | 151 (72 scenarios) | 171 (**83 scenarios**, 10 features) |
| `adapter-persistence` | 43 | 47 |
| `bootstrap` | 74 | 81 |
| QA procedures | 34 | **40** |

`mvn -f backend/pom.xml clean verify`: **BUILD SUCCESS**. `svelte-check`: 0 errors.

### These procedures were watched failing

Three of the six new QA procedures failed on their first run. All three were faults in the
*procedures* — a reload returns to the board, and a refused rename leaves the row as an open
form — and both the written document and the script were corrected. Recorded in
`docs/qa/runs/2026-08-22-stage-8-round-2.md`.

Then `companies.save` was deleted from the interactor and the suite re-run: **QA-2.6 and QA-2.7
both failed**, the other nine stayed green. The line was restored. Mutation testing had already
established the same thing at the unit level — the `save` mutant is one of the 96 killed.

## F-8.7 — The Constitution's HS256 claim (fixed)

§6 no longer names an algorithm as if it were fixed. It states the rule — the strength follows
the secret's length, minimum 32 characters, enforced by `JwtSettings` — then says what that
currently yields (HS384 for the 48-character secret both run paths use), and notes that stating
a bare algorithm is how the sentence came to be wrong.

## F-8.8 — Activity immutability (recorded)

**D-20**: the timeline is append-only. The reasoning is that the win rule ("a deal cannot be won
without a logged Call or Meeting") is only worth something if the evidence cannot be edited
afterwards to suit the conclusion.

---

## On the review's question 5

The review asked whether there is a systemic answer to "the check passed without checking
anything", now on its third occurrence (F-3.1, the Stage 7 forecast step, F-8.1).

I do not think the answer is another tool, and I want to say why rather than invent one. All
three were caught the same way: **by deliberately breaking the thing and watching the check go
red.** That worked every time. What differs is that the project made it a *habit* for Gherkin
(`World.attempt` plus the `@After` hook makes a silent pass structurally impossible) and for
unit tests (mutation testing does it automatically, 222 times per build), and never made it a
habit anywhere else. The QA suite and the shell script had no equivalent, so nothing was
watching them.

So the honest answer is: the technique is already the project's, and the gap is coverage of the
technique, not the technique itself. Every new QA procedure in this round was watched failing
before being recorded as passing, which is the same discipline applied by hand where no tool
applies it. I have not invented a gate for shell scripts, because I do not believe one
sabotage-run-by-hand per script is worth a framework — but I would rather state that as a
judgement than pretend the problem is solved.

## What I want attacked, again

1. **The new domain methods.** `renamedTo` and `correctedTo` are one-liners that delegate to a
   constructor. Is that behaviour, or is it a setter with a nicer name?
2. **D-21.** Refusing to let a contact change employer is defensible, but is it *right*, or am I
   declaring a gap to be a decision — the exact thing F-8.6 caught me doing?
3. **The metrics.** Re-derive them again. They changed.
