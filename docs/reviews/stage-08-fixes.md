# Stage 8 — Fixes after review

## Critical — Docker backend image cannot build with -DskipTests

`CucumberJunitXmlGate` is skipped when `skipTests` is true, so `Dockerfile.backend` `package -DskipTests` no longer requires a leftover `cucumber-junit.xml`.

## Important — README advertised host Postgres

Compose Postgres is not published; README now says it is internal. QA start lists Compose `:8081` as well as Vite.
