# Stage 0 — Builder hand-off

## What was delivered

- Repository constitution: `CONSTITUTION.md` (numbered, mapped to automated gates)
- Maven multi-module skeleton: `domain`, `application`, `adapter-persistence`, `adapter-web`, `bootstrap`, `tools/crap-check`, plus `inner-parent` for shared inner-layer gates
- Svelte 5 + TypeScript + Vite scaffold in `frontend/`
- Quality gates wired into `./mvnw verify` and CI:
  - JDK 21 enforcer (`[21,22)`)
  - Banned Spring/JPA/Jackson/Lombok artifacts on inner layers
  - ArchUnit tests (JUnit Jupiter) per module + layered architecture in `bootstrap`
  - JaCoCo LINE/BRANCH ≥ 95% on `domain`, `application`, and `crap-check`
  - CRAP ≤ 6 via `tools/crap-check` (formula documented in the constitution)
  - PIT 100% mutation threshold on inner layers via `-Pmutation`
- `.editorconfig`, `.gitattributes`, Maven wrapper, GitHub Actions `quality-gates`

## How to verify

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # macOS
./mvnw verify
./mvnw -Pmutation -pl domain,application -am verify
cd frontend && npm ci && npm run build && npm run check
```

## Known Stage 0 limits (intentional)

- No domain entities yet (Stage 1)
- Spring Boot, PostgreSQL, JWT, and Testcontainers are version-locked in the parent BOM but not wired (Stage 2)
- Cucumber is version-locked in dependencyManagement, not on the test classpath (Stage 1/3)
- PIT `failWhenNoMutations` is `false` until inner modules contain executable production methods (Stage 3 must flip it)
- Frontend is a toolchain scaffold only (Stage 6)

## Reviewer focus

The constitution must be precise and the gates must actually fail when a rule is broken. Architecture tests now run as JUnit methods (not silent `@ArchTest` fields) and assert that production packages are non-empty so rules cannot pass vacuously.
