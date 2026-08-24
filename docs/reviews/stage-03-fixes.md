# Stage 3 — Fixes after review

## Important 1 — Surefire counts 0 Cucumber tests

First pass: removed `cucumber.features` / `FEATURES_PROPERTY_NAME` (Cucumber's Maven workaround that ignores Suite selectors). Discovery is only `@SelectClasspathResource("features")` on the JUnit 5 Suite.

Re-review still rejected: Surefire 3.5.3 reported `CucumberTest` `Tests run: 0` even while 48 scenarios executed, and vanished `features/` stayed green.

Second pass:

- Maven Surefire/Failsafe **3.5.4** (SUREFIRE-2298 nested Cucumber XML).
- Application Surefire uses `JUnit5Xml30StatelessReporter` phrased names and `cucumber.junit-platform.naming-strategy=long`.
- `@Suite(failIfNoTests = true)`.
- Cucumber writes `target/cucumber-junit.xml`; verify runs `CucumberJunitXmlGate` (fails if &lt; 48 testcases or any failure/error).
- `CucumberDiscoveryGateTest` asserts classpath `features` yields ≥ 48 tests and a missing resource yields 0.

Evidence after this pass: `CucumberTest` Surefire `Tests run: 48`; application module `Tests run: 66`; cucumber XML gate on `verify`.

## Important 2 — UpdateDealService not atomic

`revalue` and `changeProbability` (the methods that reject closed deals) now run before `rename`, so a rejected closed-deal update cannot leave a new title on the aggregate. Closed in re-review 2.
