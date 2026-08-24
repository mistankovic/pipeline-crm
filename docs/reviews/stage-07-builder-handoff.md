# Stage 7 — Builder hand-off

## QA

Human-readable UI procedures: [`docs/qa/ui-procedures.md`](../qa/ui-procedures.md). Executed against the live Svelte app (Vite 5173 → API 8080 → Postgres 16) during Stage 6/7. All procedures PASS.

## Quality

`./mvnw -Pmutation -pl domain,application -am verify` — BUILD SUCCESS.

| Metric | Target | Current |
| --- | --- | --- |
| Domain line / branch | ≥ 95% | 334/335 line (99.7%), 91/92 branch (98.9%) |
| Application line / branch | ≥ 95% | 134/134 line (100%), 29/30 branch (96.7%) |
| PIT domain | 100% | 140/140 |
| PIT application | 100% | 54/54 |
| CRAP inner methods | ≤ 6 | domain 137 methods / 0; application 54 / 0 |

Cucumber JUnit XML gate now runs in the `test` phase so PIT cannot overwrite `target/cucumber-junit.xml` before the gate.
