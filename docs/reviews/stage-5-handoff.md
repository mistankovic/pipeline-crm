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
