# Stage 8 review — round 2

**Adversarial Reviewer → Builder**

---

## Verification I performed

I re-derived every claimed number from the reports rather than reading the hand-off's table.

| Claim | Verdict |
|-------|---------|
| domain 100 % line/branch, **126/126** mutants, 131 methods, worst CRAP 4.00 | **holds** |
| application 100 % line/branch, **96/96** mutants, 136 methods, worst CRAP 2.00 | **holds** |
| 273 / 171 / 47 / 28 / 81 backend tests, `clean verify` green | **holds** |
| 83 Gherkin scenarios across 10 features | **holds** — counted from the runner, not the files |
| 40/40 QA | **holds** — ran it |
| frontend 26 unit tests, svelte-check 0 errors | **holds** |
| A contact cannot be moved between companies via PATCH | **holds** — there is a test that sends `companyId` and proves it is ignored |
| A rename leaves no second row in PostgreSQL | **holds** |

I also checked two things the hand-off did not claim:

* **Both new endpoints reject an anonymous request.** `PATCH /api/companies/{id}` and
  `PATCH /api/contacts/{id}` return 401 without a token. Confirmed against the running stack.
* **CPD did not start ignoring the new code.** `RenameCompanyInteractor` and
  `CorrectContactInteractor` are structurally similar, and the inner-layer duplication
  threshold is 30 tokens. The build passes, so they are genuinely not duplicates — they differ
  in their lookup and in what they construct. Had the Builder written one generic
  "update entity" interactor to avoid the similarity, that would have been worse.

## F-8.9 — Why the authority rule does not apply here was not written down (**fixed during review**)

D-14 says *every* change to a deal goes through the owner-or-manager check. `RenameCompany` and
`CorrectContact` have no authority check at all. That is defensible — a company has no owner, so
there is no rival's number to protect — but the project's own standard is that a decision with
more than one defensible answer gets written down, and this one had not been.

This is the same shape as F-8.6, one layer down: not a defect, an undeclared decision. Recorded
as **D-22**, including that both endpoints do require a session, and what would change the
answer (companies acquiring an owner, or deletion).

## On the hand-off's question 1 — are `renamedTo`/`correctedTo` setters with nicer names?

No, and the distinction is worth stating because the Builder was right to ask.

A setter mutates and returns nothing; these return a new instance and leave the receiver
untouched, which there are explicit tests for. More importantly they route through the compact
constructor, so a rename cannot produce a state a creation could not — which is exactly the
property a setter destroys. `Company.renamedTo("  ")` throws for the same reason and with the
same message as `new Company(id, "  ")`. There is one definition of a valid company name and
both paths meet it.

The name carries the intent, too: `renamedTo` says the identity is kept. That is the whole
business rule, and it is asserted at three levels — a unit test on identity, an acceptance
scenario that a deal follows its company, and a persistence test that no second row appears.

## On question 2 — is D-21 a decision or a gap being dressed up?

A fair challenge, given F-8.6. I think it is a decision, on one specific ground: the brief says
"edit Company, Contact" and the natural reading of editing a contact is fixing their details.
Changing employer is not editing a record, it is recording an event in the world, and D-21 names
the questions it would raise rather than hand-waving them. It also states what would change the
decision, which a gap dressed as a decision never does.

The test that sends `companyId` and proves it is ignored is what makes me accept it. A gap
would have left the field quietly accepted or quietly dropped; this one is closed on purpose.

## On question 5 — the systemic answer

I accept the Builder's answer, including its refusal to invent a gate it does not believe in.
"The technique is already the project's; the gap is coverage of the technique" is right, and it
is a better answer than a new tool would have been. Every new QA procedure in this round was
watched failing before being recorded as passing, and the sabotage run is in the record.

I note that the discipline caught three faulty procedures in this round *before* they were
recorded as passing. That is the mechanism working.

---

## What is still not done, and is correctly not done

- The compose stack remains unexecuted, and the README says so where it tells you to run it.
- No deletion of anything. Declared, with reasons, in D-22 and the round-1 review.
- Coverage and mutation gates apply to two modules of five. The README says so in the sentence
  above the table, which was the round-1 question and remains adequately answered.

## Verdict

The brief's four nouns are now all create-and-edit except where immutability is a stated
design decision with reasoning. Every gate is green at values that did not move: 100 % line and
branch, zero surviving mutants out of 222, worst CRAP 4 against a limit of 6. The metrics in the
README match the reports. Sixty-seven findings across nine gates are recorded with their fixes,
including the ones that were embarrassing and the three in this stage that were caught only
because someone re-read the original brief at the end.

**STAGE 8 APPROVED.**
