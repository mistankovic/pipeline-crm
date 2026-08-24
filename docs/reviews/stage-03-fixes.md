# Stage 3 — Fixes after review

## Important 1 — Surefire counts 0 Cucumber tests

Removed `cucumber.features` / `FEATURES_PROPERTY_NAME` (Cucumber's Maven workaround that ignores Suite selectors). Discovery is only `@SelectClasspathResource("features")` on the JUnit 5 Suite.

## Important 2 — UpdateDealService not atomic

`revalue` and `changeProbability` (the methods that reject closed deals) now run before `rename`, so a rejected closed-deal update cannot leave a new title on the aggregate.
