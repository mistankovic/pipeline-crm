# Stage 8 review — round 1

**Adversarial Reviewer → Builder**

**NOT APPROVED.**

The packaging work is good and the Builder's self-reporting is the most honest of the eight
stages — five defects found and fixed in what was supposed to be a clerical stage, three wrong
numbers caught by re-derivation rather than by me. I verified the metrics independently and
they hold. That is not what blocks this.

What blocks it is that **Stage 8 is where I read the original brief again**, line by line,
against the finished product. One required capability is missing and nobody ever said it was
being left out.

---

## Verification I performed

Before the findings, what I checked and accept.

| Claim | How I checked it | Verdict |
|-------|------------------|---------|
| domain 100 % line/branch, 124/124 mutants | parsed `domain/target/site/jacoco/jacoco.xml` and `pit-reports/mutations.xml` myself | **holds** |
| application 100 % line/branch, 88/88 mutants | same | **holds** |
| Worst CRAP 4.00 / 2.00 | gate output on this commit | **holds** |
| 563 backend tests | 267 + 151 + 43 + 28 + 74 from the reactor log | **holds** |
| 72 Gherkin scenarios | ran the suite alone; runner reports 72 | **holds** |
| 13 ArchUnit rules | counted, and ran them: 13 tests | **holds** |
| 34/34 QA | ran it | **holds** |
| Every README command works | ran all four | **holds** — and `-pl bootstrap` without `-am` fails exactly as the README warns |
| `run-locally.sh` starts, exits, and stops cleanly | ran it from a genuinely empty state; 15 s, exit 0; after `--stop`, no `pipelinecrm.jar`, no `vite`, both ports closed | **holds** |
| Port guard | put a decoy on 8080: refused, exit 1. Removed it: started, exit 0 | **holds** |

The Builder's answers to its own challenges 1–4 are accepted. On challenge 1 specifically: an
unrun compose stack that is *labelled* unrun in the place a reader will meet it is acceptable
for a demo whose environment cannot pull images. Hiding it would not have been.

---

## F-8.6 — A required capability is missing, and was never declared missing (**blocking**)

The brief specifies the UI must support:

> create/edit Company, Contact, Deal, Activity

**Company and Contact cannot be edited.** There is no use case, no port, no endpoint and no
screen. The complete list of input ports is 15, and it contains `CreateCompany` and
`CreateContact` with nothing to change either afterwards. A company created with a typo in its
name is a company with a typo in its name for ever.

Deal editing exists and is good (`ChangeDealStage`, `RepriceDeal`, `ReweightDeal`). Activity
editing is *correctly* absent — an activity is a record of something that happened, and a
timeline you can rewrite is not a timeline. But that reasoning appears nowhere either.

What makes this blocking rather than a note:

1. It is an **explicit** requirement, not an inference. The brief lists four nouns; two of them
   are half-implemented.
2. `CONSTITUTION.md` §6 exists precisely so that "nobody reviews us for missing them". Nine
   things are listed there. Company and contact editing are not among them. The project has a
   mechanism for declaring a non-goal and did not use it.
3. Eight review gates passed over this. That is the more interesting failure: every stage
   reviewed its own hand-off thoroughly and no stage re-read the brief. Stage 1 designed the
   domain from the brief's *domain* paragraph and never revisited its *UI* paragraph.

Two ways out, and only one is honest. Either implement it, or amend §6 with a numbered entry
saying it is out of scope and why. Given that the brief asks for it in as many words and the
work is small — both entities are records with their invariants already in their compact
constructors — **implement it.** Amending the Constitution to delete a requirement at the final
gate would be the Constitution serving the Builder rather than the other way round.

Required, to the same standard as everything else: domain behaviour, use cases behind input
ports, REST endpoints, UI, unit tests, Gherkin scenarios, QA procedures, and coverage, mutation
and CRAP gates still green at their current values. **100 % and zero survivors is the bar this
project set; new code does not get a lower one.**

## F-8.7 — The Constitution states a fact that is false (**blocking**)

`CONSTITUTION.md` §6:

> **JWT in this demo is deliberately simple** (HS256, single shared secret, 8 h expiry, no
> refresh).

It is not HS256. `Keys.hmacShaKeyFor` derives the strength from the key length, and both
documented run paths now use a 48-character secret, which is HS384. The Builder found and fixed
this exact claim in the README (F-8.5) and did not check whether the same sentence appeared in
the binding document. It did.

This matters more than a typo because §6 is the document the other eight review gates were
measured against. A binding document that is wrong about the system is worse than no document.
Fix the sentence, and state the rule rather than the current answer, so it cannot rot again.

## F-8.8 — Activity immutability is a design decision presented as an absence (**minor**)

Not editing activities is right. But `docs/domain-decisions.md` records nineteen decisions far
less consequential than "the timeline is append-only", and this one is inferrable only from the
lack of a use case. Add it as D-20, with what would change it.

---

## What I am not asking for

- Not asking for company or contact **deletion**. The brief does not ask, and deletion with
  deals and activities hanging off these rows is a genuinely large design question
  (cascade? soft delete? refuse when referenced?) that a demo should not answer casually.
  Declare it in §6 instead.
- Not asking for the compose stack to be run. It cannot be, here.
- Not asking for changes to `run-locally.sh`. It is now better than it needed to be.

## To approve

F-8.6 and F-8.7 fixed, F-8.8 recorded, every gate still green at its present values, and the
README's metrics table updated to whatever the numbers become. Resubmit.
