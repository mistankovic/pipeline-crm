# Stage 1 — Builder hand-off

## Delivered

- Pure domain model under `com.pipelinecrm.domain` (no Spring/JPA)
- Gherkin features in `application/src/test/resources/features`
- Input/output ports under `com.pipelinecrm.application.port`
- Domain unit tests targeting ≥95% coverage, CRAP ≤ 6, PIT 100% on inner-layer production code

## How to verify

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./mvnw verify
./mvnw -Pmutation -pl domain,application -am verify
```

Gherkin is specified in Stage 1; the Cucumber runner and glue that execute it against use cases land in Stage 3.
