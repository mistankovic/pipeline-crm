# Stage 3 — Builder hand-off

## Delivered

- Use-case implementations in `application` (no Spring)
- In-memory adapters for tests
- Cucumber glue that runs Gherkin against the use-case layer
- Unit tests; JaCoCo and CRAP gates green; PIT 100% on domain + application
- `failWhenNoMutations=true` on inner layers
