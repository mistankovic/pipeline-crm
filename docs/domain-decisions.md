# Domain decisions

Every reading of the requirements that could reasonably have gone another way is recorded
here, with the alternative, so that a reviewer argues with a decision rather than
discovering an accident.

## D-1 — A deal may be lost from any open stage, but only won from NEGOTIATION

**Requirement text:** `LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON / CLOSED_LOST`.

**Decision:** forward movement is one stage at a time. `CLOSED_WON` is reachable only from
`NEGOTIATION`. `CLOSED_LOST` is reachable from **any** open stage.

**Why:** the strict linear reading would mean a lead that never answers the phone has to be
walked through three fictitious stages before it can be marked lost, which would corrupt
every forecast in the process. Losing early is the single most common real event in a
pipeline. Winning early is not: a win is a commercial commitment and the requirement
attaches conditions to it, so it stays at the end.

**Alternative, if the supervisor disagrees:** delete the `target == CLOSED_LOST` branch in
`DealStage.allowsTransitionTo`. One line, and two scenarios in
`deal_stage_transitions.feature` change from passing to rejected.

## D-2 — Closed deals are terminal and immutable

Nothing reopens a closed deal, and `reprice`/`reweight` are refused once closed
(`ClosedDealIsImmutable`). The requirement is silent. A won deal whose value can still be
edited is a reporting hole, so the domain forbids it.

## D-3 — Probability is forced on close, not merely defaulted

Rule 3 says a close *forces* probability to 100 or 0. The domain therefore overwrites
whatever the caller asks for; the caller cannot pass a probability to `changeStageTo` at
all. A closed deal's probability can never disagree with its stage.

## D-4 — Engagement means a CALL or a MEETING **linked to that deal**

Rule 1 says "at least one Activity of type Meeting or Call". An activity linked to a
*contact* at the same company does not count: the rule is about this opportunity. The
domain enforces the link by refusing to judge a deal against another deal's history
(`InapplicableActivityHistory`) rather than trusting the caller to pass the right list.

## D-5 — No currency conversion; forecasts are per currency

A deal's value carries its currency. `Money.plus` across currencies throws
`CurrencyMismatch`. A forecast therefore produces one line per (group, currency) pair.
The alternative — a single total in a reporting currency — needs FX rates and a rate date,
which Constitution §6 lists as a non-goal.

## D-6 — `Money` cannot be negative

There is no such thing as a deal worth minus ten thousand. A negative amount is an
`InvariantViolation` at construction, so no downstream code has to consider the case.

## D-7 — Zero-valued deals are legal, unwinnable deals are not

A deal may be created at value 0 (nobody has quoted yet) and may move through the
pipeline. It cannot be **won** at 0, by rule 1. This is why the value check lives in the
win path and not in the constructor.

## D-8 — `User`, `Company`, `Contact` and `Activity` are immutable records; `Deal` is not

Only `Deal` changes state during its life, and its state changes are the business rules
under test. The rest have no mutating behaviour in this feature set, so they are records
and structural equality is correct for them. `Deal` implements identity equality on its id.

## D-9 — A deal has a title

Not in the requirement's field list. Added because a Kanban board of unnamed cards is
unusable and every acceptance scenario has to refer to a deal by something. The title is
required and trimmed; it carries no rules.

## D-10 — Value and probability travel together as `DealTerms`

Writing `Deal.open(id, title, parties, value, probability)` would have been five
parameters, over the Constitution's limit of four. Rather than raise the limit, the two
fields that always change together and are always read together became one value object
with `weighted()`, `pricedAt()` and `weightedAt()`. The limit found a missing concept,
which is what limits are for.

## D-11 — The domain computes which transitions are legal, and says so out loud

`Deal.allowedTransitions()` returns the stages this deal could move to right now, and it
reaches the browser through `DealView.allowedTransitions`. The alternative — the browser
knowing the state machine — would put a business rule in the frontend, which Constitution
§2.2 forbids. The server still rejects an illegal move if a client sends one anyway, so
the exported list is an affordance, never the enforcement.

## D-12 — Sign-in failures are indistinguishable

`AuthenticationFailed` carries one message for a wrong password and for an unknown
address. A different message for each would let anyone enumerate the user list.

## D-13 — Users are seeded, not created through the application

There is no `CreateUser` use case and `UserRepository` has no `save`. Users are provisioned
by a database migration (Stage 4). The requirement's UI list contains no user management
screen, and adding one would drag in password policy, invitation flow and role
administration — all of which are noise against the point of this demo. Recorded in
Constitution §6 as a non-goal so that a reviewer sees a decision rather than a gap.

## D-14 — Every change to a deal is subject to the same authority rule

Rule 2 in the requirements names only the *stage*. The domain applies the owner-or-manager
check to `reprice` and `reweight` as well, because setting a rival's deal to zero value or
zero probability removes it from the forecast exactly as effectively as marking it lost.
Protecting one path and leaving the other open would be an accident, not a design.

## D-15 — Repricing states its currency

`RepriceDeal.Repricing` carries an explicit currency rather than inheriting the deal's
existing one. Inheriting would be a business decision ("a deal's currency can never
change") taken silently inside a mapper. If we later decide a deal's currency is fixed,
that rule belongs in `Deal`, enforced and tested — not implied by an absent field.

## D-16 — Ports speak the domain's types, not strings

`ChangeDealStage` takes a `DealStage` and `ProduceForecast` takes a `ForecastDimension`.
Parsing text into those enums is the web adapter's job. A use case handed a `String` would
have to validate it, which means an HTTP concern (a badly typed request) would be decided
one layer too deep.

## D-17 — Last write wins; there is no optimistic locking

`DealRepository.save` writes the whole row. If a manager closes a deal in the same moment its
owner reprices it, one of the two changes is lost with no warning.

**Decided: accepted for this demo, not fixed.** Adding a version column means a version on
`DealSnapshot`, a conflict exception in the domain, a 409 in the API and a retry in the
browser — a feature in its own right, and one that would spread across every layer this demo
exists to demonstrate. The pipeline board is single-team and low-contention, and the failure
is a lost edit rather than a corrupt state: every write goes through the domain's rules, so
the deal that survives is always internally consistent.

**What would change the decision:** more than one person routinely editing the same deal, or
any requirement to audit changes. Both make the lost write unacceptable rather than merely
untidy.

Raised by the Stage 4 review, finding F-4.6, which correctly refused to let "I would rather
the reviewer decide" stand in for a decision.

## D-18 — Anyone may open a deal for anyone; only the owner or a manager may change one

`POST /api/deals` accepts an `ownerId` and does not check who the caller is. A salesperson can
open a deal owned by a colleague — and then, by rule 2, be unable to touch it again.

**Decided: this asymmetry is deliberate.** Creating a deal for a colleague is a normal
handover ("this lead is really yours"), and the thing rule 2 protects is a deal that already
exists and already counts towards someone's forecast. A brand new deal at its creator's chosen
value is not yet anybody's number.

**What this is not:** it is not permission to edit somebody's deal by deleting and recreating
it. Deleting a deal is not implemented at all, so the loophole does not exist.

**What would change the decision:** deals becoming deletable, or targets being set per owner —
at which point creating a deal in someone else's name is a way to move a number they are
measured on, and the domain would need a rule about it.

Raised by the Stage 5 review, finding F-5.5. Recorded here rather than enforced by a validation
annotation in the web layer, because if the rule existed it would belong in the domain.

## D-19 — No CORS configuration; the browser and the API share an origin

Deferred from the Stage 2 review (finding F-2.8) to the stage that could answer it.

In development the Vite server proxies `/api` to the backend, so the browser only ever talks
to `localhost:5173`. In the packaged demo (Stage 8) nginx serves the built assets and proxies
`/api` to the backend, so the browser only ever talks to one origin there too.

**Decided: no CORS headers, deliberately.** Adding them would mean choosing an allowed origin
list that nothing needs, and `Access-Control-Allow-Origin` is the sort of setting that gets
widened to `*` by whoever hits the error next. A same-origin deployment has no CORS problem to
solve, and the absence of the configuration is what keeps it that way.

**What would change the decision:** the frontend being served from a different host to the API
— at which point the allowed origins are a real, named list, and they go in configuration
rather than in code.

## D-20 — The timeline is append-only; an activity is never edited or deleted

Raised by the Stage 8 review, finding F-8.8. There is no use case to change or remove an
activity, and that is deliberate rather than unfinished.

An activity records that something **happened**: a call was made, a meeting took place, a note
was written at a moment by a person. The deal's timeline is what later justifies closing it —
the rule that a deal cannot be won without a logged Call or Meeting is only worth anything if
the evidence cannot be edited afterwards to suit the conclusion. A timeline you can rewrite is
not evidence, it is a story.

**Decided: activities are immutable and permanent.** A mistake is corrected by logging another
activity that says so, which is also what an audit trail would require if this demo had one.

**What would change the decision:** a requirement to redact personal data on request, which is
a deletion with a legal shape and belongs nowhere near a general edit feature.

## D-21 — A contact can be corrected but not moved between companies

Raised by the Stage 8 review, finding F-8.6, when company and contact editing was added.

Correcting a contact changes their name and email address. It cannot change their employer,
and `CorrectContact.Corrections` has no field for one — sending `companyId` in the request body
does nothing, and there is an API test that proves it.

Two different things wear the same word "edit". Fixing a misspelt name is a correction: the
record was always meant to say this. Moving a person to another company is an event in the
world, and it raises questions this demo has not answered — do the activities logged against
them at the old company follow them? Are the old company's deals still relevant to them? A
single form field that silently reassigns a person is the wrong way to answer any of that.

Renaming a **company** is by contrast unambiguous: the identity is kept, so every deal and
contact that pointed at it still does. That is asserted in the acceptance scenarios and in a
persistence test that checks a rename leaves no second row behind.

**What would change the decision:** a stated requirement for people changing employer, at which
point it is its own use case with its own name, its own rules about what follows the person,
and its own scenarios.

## D-22 — Companies and contacts are shared reference data; any signed-in user may correct them

Raised by the Stage 8 round-2 review. D-14 says every change to a **deal** is subject to the
owner-or-manager rule, and a reader who meets `RenameCompany` next will reasonably ask why the
same rule does not apply there.

It does not apply because there is nothing for it to key on. A deal has an owner and counts
towards that person's forecast; protecting it protects a number somebody is measured on. A
company has no owner. It is reference data that everyone's deals point at, and the same is true
of a contact. There is no rival whose figure a rename moves.

**Decided: correcting a company or a contact requires a session and nothing more.** Both
endpoints reject an anonymous request with 401, verified in the API tests; beyond that, any
signed-in user may fix a misspelt name. Inventing an owner for a company purely so an authority
rule had something to check would be a rule protecting nothing.

**What would change the decision:** companies acquiring an owner or an account manager, or
per-company targets — at which point a rename is a change to somebody's territory and the
domain would need a rule about it. Deleting a company would change it immediately and for a
different reason, which is one of several reasons deletion is not implemented.
