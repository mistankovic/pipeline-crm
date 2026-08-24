# Stage 1 — Fixes after review

## Important 1 — mixed currencies

`Forecast.byOwner` and `byStage` now reject any open deal whose currency is not the requested one (`MixedCurrencyException`). Gherkin covers mixed-currency and wrong-request-currency cases.

## Important 2 — restore / revalue invariants

`Deal` constructor/`restore` rejects `CLOSED_WON` unless value is positive, probability is 100, and a qualifying activity is recorded. `CLOSED_LOST` must have probability 0. `revalue` is locked on terminal deals, same as probability.

## Important 3 — win evidence on the Deal

`Deal` owns its activities. `recordActivity` accepts only deal-targeted activities. `changeStage(actor, target)` uses that list; callers cannot pass a fabricated collection.

## Minors 4–5

Terminal and same-stage Gherkin outlines expanded. `RecordActivityUseCase.Command` requires exactly one of dealId or contactId.
