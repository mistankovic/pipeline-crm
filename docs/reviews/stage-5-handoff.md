# Stage 5 hand-off — The API layer

## Controllers

Seven, in `adapter-web/rest/`. Every method reads the same way: take what HTTP delivered,
name who is asking, call one port, return what it gave back.

| Route | Port |
|-------|------|
| `POST /api/sessions` | `SignIn` — the only public route, mapped from the constant the security config permits |
| `GET/POST /api/companies` | `ListCompanies`, `CreateCompany` |
| `GET/POST /api/contacts`, `GET /api/contacts/{id}/activities` | `ListContacts`, `CreateContact`, `ViewContactTimeline` |
| `GET /api/deals`, `GET /api/deals/{id}`, `POST /api/deals` | `ViewPipeline`, `ViewDeal`, `CreateDeal` |
| `PATCH /api/deals/{id}/stage` `/value` `/probability` | `ChangeDealStage`, `RepriceDeal`, `ReweightDeal` |
| `POST /api/activities` | `LogActivity` |
| `GET /api/forecast?by=` | `ProduceForecast` |
| `GET /api/users`, `GET /api/users/me` | `ListUsers` |

There is **no `if` about a deal** anywhere in `DealController`. The two conditionals in the
whole controller package are `ownerOr(caller)` — defaulting the owner to whoever is asking —
and `about()`, which collapses two nullable JSON fields into the port's sealed choice. Both
are about the shape of an HTTP request, not about the business.

## A fitness function for this stage's actual rule

`no_controller_ever_touches_an_entity`: nothing in `com.pipelinecrm.adapter.web.rest..` may
depend on `Deal`, `Activity`, `Company`, `Contact`, `User`, `Money`, `Probability`, `Forecast`
or the deal's supporting types. If a controller cannot reach an entity, it cannot ask one a
question and act on the answer — which is how a rule ends up in a controller.

The first version of this rule covered the whole web adapter and **failed**, on
`JwtAccessTokenIssuer.issueFor(User)`. That was the rule being wrong, not the code: the class
implements an *output* port whose signature the inner layer chose, which is exactly the traffic
the dependency rule permits. Scoped to the controllers, it passes.

## HTTP translation

`HttpTranslation` is a table, and the only place in the system that knows these situations
have status codes:

| Failure | Status |
|---------|--------|
| `AuthenticationFailed` | 401 |
| `StageChangeForbidden` | 403 |
| `UnknownEntity` | 404 |
| `IllegalStageTransition`, `WinRequiresValueAndEngagement`, `ClosedDealIsImmutable` | 409 |
| `InvariantViolation`, `MalformedRequest`, bean-validation failure | 400 |
| anything else | 500, logged in full, and the caller is told nothing |

`MalformedRequest` is a web-layer failure for input this layer cannot turn into a use-case
argument at all — a stage that is not a stage. `RequestedEnum` reads the legal values **from
the enum**, so a new deal stage is accepted by the API the moment the domain accepts one, and
the error message lists them without repeating them.

## Wiring

`UseCaseConfiguration` in `bootstrap` news up all twenty-two collaborators and interactors by
hand, because not one class in `application` is annotated and nothing can scan them into
existence. That is the price of the dependency rule and also the proof of it: the acceptance
tests build the same graph with no Spring at all, and the use cases cannot tell the difference.

Its failure mode is a port somebody forgot, which otherwise surfaces only if a controller
happens to ask for it. `EveryPortIsWiredTest` scans `port.in` and `port.out` off the classpath
and asserts each has exactly one bean — and it refuses to pass if it found no ports, so it
cannot succeed by looking at nothing.

## Tests — 41 new, over real HTTP against a real database

`ApiTest` obtains tokens by **signing in**, exactly as a browser would. No mocked principal,
no `@WithMockUser`: a broken sign-in cannot hide.

* `SessionApiTest` (9): correct sign-in, no hash anywhere in the response, wrong password,
  unknown address with byte-identical body, missing field is 400 not 401, no token / forged
  token / real token, and who-am-I.
* `DealApiTest` (19): create as LEAD owned by the caller, advertised transitions, advance,
  skip a stage → 409, stranger → 403, manager allowed, win without evidence → 409, win with a
  meeting forces 100, lose forces 0, nonsense stage → 400 listing the real ones, unknown deal
  → 404, unknown company → 404, unknown currency → 400, probability 150 rejected before the
  use case, reprice in another currency, stranger repricing → 403, repricing a closed deal →
  409, reweight recomputes the weighted value, board filtered by owner.
* `ContactAndForecastApiTest` (13): companies, contacts, blank name → 400, unknown company →
  404, bad email → 400, activity on a contact with its timeline, activity about **both** → 400,
  about **neither** → 400, unknown type → 400, forecast by owner with a readable label,
  forecast by stage, nonsense dimension → 400, user list.

## Metrics

| Metric | `domain` | `application` |
|--------|---------:|--------------:|
| Line / branch coverage | 100 % / 100 % | 100 % / 100 % |
| Mutation score | 125/125 | 88/88 |
| Worst CRAP | 5.00 | 2.00 |

Test totals: `domain` 254, `application` 143, `adapter-persistence` 43, `adapter-web` 23,
`bootstrap` 46 (13 architecture + 5 wiring + 2 port-wiring + 41 API… of which 41 are the new
API tests).

## Deliberately not done

* No CORS configuration yet — the Stage 2 review deferred the decision to Stage 6, where the
  frontend can state what it actually needs.
* No pagination. Constitution §6 lists it as a non-goal.
* `POST /api/deals` accepts an optional `ownerId` so a manager can open a deal for somebody
  else; it defaults to the caller. Nothing stops a salesperson naming another owner — the
  domain has no rule about *creating* a deal for someone else, only about changing one. Worth
  a reviewer's opinion on whether that rule should exist.

---

# Stage 5 hand-off — round 2 (response to review)

| Finding | Disposition |
|---------|-------------|
| F-5.1 exact-class table with no completeness check | **Fixed, and it immediately paid for itself.** `statusFor` returns `Optional`, and `DELIBERATELY_A_SERVER_FAULT` names the failures that are our fault rather than the caller's. `EveryFailureIsTranslatedTest` scans the classpath for every concrete `DomainException` and `ApplicationException` and requires each to be in one list or the other — and refuses to pass if it found none. **On its first run it failed on `CurrencyMismatch`**, which nothing had mapped: it would have been a silent 500. It is now a declared server fault, with the reasoning written down. `InapplicableActivityHistory` likewise. |
| F-5.2 `LOWEST_PROBABILITY` used as the floor on money | **Fixed.** `NOT_NEGATIVE` for the two money floors, `LOWEST_PROBABILITY` for the probability floor, and `@Min(0)` replaced by the named constant so the magic number is gone too. |
| F-5.3 request DTO reaching into the security layer | **Fixed.** `ownerOr(SignedInUser)` is gone; `DealController` reads `request.ownerId()` and falls back to the caller. The body no longer knows authentication exists. |
| F-5.4 `/api/users/me` read every user to find one | **Fixed by deleting the endpoint.** The reviewer's second suggestion was the right one: the sign-in response already hands the browser its own `UserView`. The endpoint existed because it sounded like one should. A `SessionApiTest` case now pins that the sign-in response carries id, name, email and role, so the frontend has what the endpoint used to provide. |
| F-5.5 anyone may create a deal for anyone | **Decided, as D-18.** Deliberate: creating a deal for a colleague is a handover, and rule 2 protects a deal that already counts towards somebody's forecast. The decision names what would reverse it — deletable deals, or per-owner targets. Recorded in `domain-decisions.md` rather than enforced with a validation annotation, because if the rule existed it would belong in the domain. |
| F-5.6 the 500 path was untested | **Fixed.** `ApiExceptionHandlerTest`: a translated failure keeps its name and message; a server fault is 500; and a server fault's body **does not contain the deal id** that was in the exception's own message. |

## The finding the new test found

`CurrencyMismatch` is thrown by `Money.plus` when two currencies are added. It is almost
certainly unreachable — forecast lines are keyed by currency precisely so that it cannot
happen — but "almost certainly unreachable" was not why it was absent from the table. It was
absent because nobody thought about it. A completeness test does not care about the difference,
which is the point of having one.

## Metrics (round 2)

| Metric | `domain` | `application` |
|--------|---------:|--------------:|
| Line / branch coverage | 100 % / 100 % | 100 % / 100 % |
| Mutation score | 125/125 | 88/88 |
| Worst CRAP | 5.00 | 2.00 |

`adapter-web` 28 tests, `bootstrap` 46, `adapter-persistence` 43. Full `mvn clean install` green
across six modules.
