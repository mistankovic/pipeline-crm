# Stage 0 — Adversarial Review

STAGE 0 REJECTED

Reviewed on branch `local-grok-pipeline-crm` at `3e57110` (working tree clean). Scope: `CONSTITUTION.md`, `README.md`, `docs/reviews/stage-00-builder-handoff.md`, root / `inner-parent` / every module POM, ArchUnit tests, `tools/crap-check` (source + tests + verify binding), `.editorconfig`, `.gitignore`, `.github/workflows/quality-gates.yml`, `frontend/` scaffold, and git log on this branch only.

Commands run (JDK 21 via `/usr/libexec/java_home -v 21`):

- `./mvnw -B clean verify` — BUILD SUCCESS
- `./mvnw -B -Pmutation -pl domain,application -am verify` — BUILD SUCCESS
- `cd frontend && npm ci && npm run build && npm run check` — 0 errors

The skeleton compiles, architecture tests execute (not 0 tests), inner-layer enforcer/JaCoCo/CRAP/PIT are bound, and the Svelte 5 + TypeScript + Vite toolchain builds. That is not enough. Stage 0 promised a constitution that is precise **and** a green `./mvnw verify` that **is** the architecture review. Findings 1–3 are holes in that claim.

---

## Important

### 1. Inner-layer “JDK only” gates still admit `javax.*` Java EE / JPA

**Severity:** Important
**Confidence:** 94

**Evidence:**

- `domain/src/test/java/com/pipelinecrm/domain/architecture/DomainIndependenceTest.java:44-48` allowlist:

```java
.resideInAnyPackage("com.pipelinecrm.domain..", "java..", "javax..")
```

- `application/src/test/java/com/pipelinecrm/application/architecture/ApplicationIndependenceTest.java:56-61` same `javax..` allowlist.
- Forbidden-package denylist in those tests lists `jakarta.persistence..` / `jakarta.servlet..` / `jakarta.ws.rs..`, **not** the `javax.*` equivalents.
- `inner-parent/pom.xml:44-60` `bannedDependencies` excludes `jakarta.persistence:*`, `jakarta.servlet:*`, `jakarta.ws.rs:*` — **not** `javax.persistence`, `javax.servlet`, `javax.ws.rs`.
- Enforcer denylist is not a JDK-only allowlist. Unused `com.google.guava:guava` (or `javax.persistence:javax.persistence-api`) is not banned. CONSTITUTION.md §2.1 says domain production deps are “JDK only”.
- CONSTITUTION.md §2.4: “A green `./mvnw verify` **is** the architecture review for dependency direction.”
- CONSTITUTION.md §9.2: “Inner modules have zero framework dependencies.”

`javax..` in ArchUnit is every `javax.*` package, including `javax.persistence`, `javax.servlet`, `javax.ws.rs`, `javax.ejb`, `javax.xml.bind`. Adding `javax.persistence-api` and annotating a domain type with `@javax.persistence.Entity` would compile, satisfy `onlyDependOnClassesThat().resideInAnyPackage(..., "javax..")`, miss the Jakarta-only denylist, and miss enforcer. The build stays green. That is JPA in the domain.

`org.springframework.boot:spring-boot-starter` on domain **would** fail (`ban-frameworks-on-inner-layers` ran on domain/application during `./mvnw verify`). Finding 1 is the complementary hole: the denylist is incomplete and the bytecode allowlist is wider than the JDK.

**Required fix:**

1. Tighten ArchUnit allowlists to `java..` plus an explicit JDK `javax` list if needed (`javax.crypto..`, `javax.net..`, `javax.security..`, `javax.sql..`, `javax.naming..`, `javax.management..`, `javax.xml..`, `javax.annotation.processing..`, `javax.transaction.xa..`, …). Do **not** allow all of `javax..`.
2. Change inner-layer enforcer from a framework denylist to “ban `*:*` except the documented test tools” (`junit-jupiter`, `assertj-core`, `archunit-junit5`, `crap-check`, and later Mockito/Cucumber on `application` with `:test` scope). At minimum add `javax.persistence:*`, `javax.servlet:*`, `javax.ws.rs:*`.
3. Keep the existing Spring / Jakarta / Hibernate / Jackson / Lombok bans as belt-and-suspenders.

---

### 2. Lombok is banned in every module; only inner layers enforce it

**Severity:** Important
**Confidence:** 96

**Evidence:**

- CONSTITUTION.md §2.2: “Lombok is banned in **every** module. Generate nothing that hides structure.”
- `inner-parent/pom.xml:59` bans `org.projectlombok:lombok` only for `domain` and `application`.
- ArchUnit lombok package rules exist only in `DomainIndependenceTest` and `ApplicationIndependenceTest`.
- `adapter-web/pom.xml`, `adapter-persistence/pom.xml`, `bootstrap/pom.xml`, `tools/crap-check/pom.xml` have no lombok ban.
- `./mvnw -B clean verify` ran `ban-frameworks-on-inner-layers` on domain and application only. Adapters/bootstrap ran `enforce-java-21` alone.

Adding Lombok to `adapter-web` or `bootstrap` is a green build. Generated getters/setters in adapters are exactly the structure-hiding the constitution forbids.

**Required fix:**

- Add `org.projectlombok:lombok` to the **root** enforcer execution so every module inherits the ban (compile and test).
- Add an ArchUnit `noClasses().should().dependOnClassesThat().resideInAnyPackage("org.projectlombok..")` in adapter and bootstrap production imports (or a shared test that imports each module’s `target/classes`).

---

### 3. Dependency-rule / CRAP / PIT gates key off package names, not module output

**Severity:** Important
**Confidence:** 88

**Evidence:**

- ArchUnit importers use `importPackages("com.pipelinecrm.domain")` (and analogously for other layers), not `importPath(target/classes)` / classpath of **that module’s** production output.
- Bootstrap `LayerDependencyRulesTest` uses `importPackages("com.pipelinecrm")` plus `consideringOnlyDependenciesInLayers()`. Classes outside a named layer are ignored as sources.
- CRAP Maven binding passes `--package ${crap.package.prefix}` (`inner-parent/pom.xml:121-123`). `CrapEvaluator` skips methods whose package does not match. A high-CRAP method in `com.acme` inside the `domain` module is not checked. `CrapEvaluatorTest.doesNotTreatSiblingPackageAsMatch` documents this filter.
- PIT `targetClasses` is `com.pipelinecrm.domain.*` / `com.pipelinecrm.application.*`. Off-package production types are not mutated.
- JaCoCo `BUNDLE` **does** cover the whole module — coverage is not the bypass. Architecture and CRAP are.

CONSTITUTION.md §2.1 binds layers to Maven **modules** and Java package roots. A class in `domain/src/main/java/com/acme/Cheat.java` that depends on `application` (add the Maven dep; enforcer does not ban sibling modules) is invisible to DomainIndependenceTest, the layered test, CRAP, and PIT. `./mvnw verify` stays green. That contradicts §2.4.

`production_package_is_present` only asserts the imported set is non-empty. `package-info` satisfies it. It does not assert every production class in the module lives under the layer package.

**Required fix:**

- Import production bytecode from the module output (`target/classes` / `importClasspath` filtered to this artifact), not a package prefix.
- Assert every production class (except `package-info` / `module-info`) `resideInAPackage("com.pipelinecrm.<layer>..")`.
- Run CRAP against **all** methods in that module’s `jacoco.xml` (drop the package filter for per-module reports, or fail if the report contains any other package).
- Fail PIT (or an extra ArchUnit test) if `target/classes` contains types outside the layer package.

---

## Minor

### 4. `failWhenNoMutations=false` is a documented empty-module escape, not a Stage 3 gate

**Severity:** Minor
**Confidence:** 90

`inner-parent/pom.xml:161` and CONSTITUTION.md §5.3 match. `./mvnw -Pmutation -pl domain,application -am verify` logged `No mutations found` / `Skipping coverage and analysis` and still **BUILD SUCCESS**.

Once executable production methods exist, PIT **will** create mutants and `mutationThreshold=100` applies even while this flag is false. The remaining risk is mis-targeted globs that find zero mutants after code exists. Stage 3 must flip the flag; nothing in verify will remind anyone. Acceptable for Stage 0 if finding 3’s package targeting is fixed and Stage 3 actually flips the flag.

### 5. CONSTITUTION.md §6 “No wildcard imports” has no tool

**Severity:** Minor
**Confidence:** 95

`.editorconfig` covers indent/charset. There is no Checkstyle, PMD, Error Prone, or compiler `-Werror` on `import ….*`. The constitution’s own preface says a rule without a check is a wish. Add Checkstyle `AvoidStarImport` (or equivalent) bound to `verify`, or delete the sentence.

### 6. §2.3 port / use-case package rules are unenforced

**Severity:** Minor
**Confidence:** 90

No ArchUnit that input ports live in `com.pipelinecrm.application.port.in`, output ports in `port.out`, use cases in `usecase`, or that adapters must not skip ports. Fine until those types exist; add the rules in the same commit as the first ports so they cannot rot.

### 7. `inner-parent` is not a reactor module

**Severity:** Minor
**Confidence:** 85

Root `pom.xml` `<modules>` omits `inner-parent`. RelativePath resolution works today (domain inherited `ban-frameworks-on-inner-layers`, JaCoCo check, CRAP). Version bumps, `versions:set`, and “is this project in the build?” become footguns. Add `<module>inner-parent</module>`.

### 8. README local frontend install is not CI

**Severity:** Minor
**Confidence:** 90

README: `npm install`. CI: `npm ci` with `frontend/package-lock.json` (tracked, lockfileVersion 3). Point the README at `npm ci`.

### 9. `tools/crap-check` “JDK only” is a wish

**Severity:** Minor
**Confidence:** 88

CONSTITUTION.md §2.1 table: crap-check compile deps are JDK only. No bannedDependencies / ArchUnit on that module. Spring or Jackson on the gate tool would still verify. Reuse the inner-layer allowlist enforcer there.

### 10. CRAP subprocess uses PATH `java`, not Maven’s JVM

**Severity:** Minor
**Confidence:** 82

`inner-parent/pom.xml` / `tools/crap-check/pom.xml`: `<executable>java</executable>`. Enforcer checks the Maven JVM (`[21,22)`). The forked gate can run a different `java` from `PATH`. Use `${java.home}/bin/java`.

### 11. Mockito is in the constitution forbidden list, not in the ArchUnit denylist

**Severity:** Minor
**Confidence:** 85

CONSTITUTION.md §2.2 lists `org.mockito..` in production. Domain/application `noClasses()` denylists do not. The `onlyDependOn` allowlist currently blocks used Mockito. If that test is deleted, mockito slips through. Add it to the denylist. `application` still has no Mockito test dependency (Stage 1/3).

---

## Attack-question notes (not extra findings)

| # | Question | Result |
| --- | --- | --- |
| 1 | Every constitution rule mapped to a failing gate? | No. Findings 1–3, 5, 6, 9. Numeric gates (JaCoCo, CRAP formula, PIT threshold, Java 21) **are** mapped. |
| 2 | ArchUnit tests actually run? Vacuous? | They run: domain 3, application 4, persistence 2, web 2, bootstrap 4, all green. `package-info` keeps the imported set non-empty (ArchUnit 1.4 empty-should would fail). Not 0 tests. Vacuity remaining is findings 1 and 3, not empty packages. |
| 3 | CRAP formula / threshold / missing report | Formula `comp² * (1-cov)³ + comp` implemented (`CrapCalculator`). `comp` = JaCoCo COMPLEXITY missed+covered; `cov` = LINE ratio. Threshold 6, `crap > 6` fails. Missing `jacoco.xml` + real `.class` files fails; package-info-only passes. Bound to `verify` **after** `jacoco:report` (confirmed on `clean verify`). Package filter is finding 3. |
| 4 | JaCoCo 95% line **and** branch on domain + application | Wired: `inner-parent` `jacoco:check` BUNDLE LINE+BRANCH COVEREDRATIO 0.95, `verify`. `clean verify`: “Analyzed bundle 'domain' with 0 classes” / “All coverage checks have been met.” 0/0 is JaCoCo 1.0. Honest for Stage 0; will fail once uncovered executable code exists. crap-check itself is really covered (~98% lines on current CSV). |
| 5 | PIT 100% once code exists? | `mutationThreshold=100` is set. Empty modules: no mutants, success because `failWhenNoMutations=false` (Minor 4). Not a time bomb for **real** methods in the target packages. |
| 6 | Spring/JPA leak via parent compile classpath? | Domain test classpath (Surefire) had JUnit, AssertJ, ArchUnit, crap-check, SLF4J — **no Spring/JPA**. Root Spring Boot BOM is `dependencyManagement` only. Direct `spring-boot-starter` on domain would fail enforcer. `javax.persistence` is finding 1. |
| 7 | Root `<dependencies>` junit/assertj/archunit on all modules? | Yes, in `<dependencies>` not only `dependencyManagement`, **`test` scope**. Not packaged; not on main compile. Intentional for per-module ArchUnit. Not a production leak. |
| 8 | Cucumber / Testcontainers claimed vs present | Cucumber 7.22.2 in root `dependencyManagement` only — not on a test classpath (handoff: Stage 1/3). Testcontainers **is** version-locked via Spring Boot 3.5.16 BOM (`testcontainers.version` 1.21.4, `testcontainers-bom`). Mockito 5.17.0 via the same BOM. Matches the documented Stage 0 limit. |
| 9 | Frontend Svelte 5 + TS + Vite builds? | Yes. `svelte` 5.56.10, `vite` 6.4.3, `typescript` 5.9.3. `mount` from `svelte` is the Svelte 5 API. `npm ci && npm run build && npm run check` succeeded. `package-lock.json` is tracked. |
| 10 | Java 21 enforcer vs system Java 26 | `requireJavaVersion` `[21,22)` ran and passed on Temurin 21.0.11. This machine has no JDK 26. On 26 the build **fails by design** (README + constitution). `maven.compiler.release=21`. No toolchain; not a Stage 0 defect. |
| 11 | DRY / names / overbuilding | crap-check is split into small types with real tests (39 tests, CRAP checked 68 methods, 0 violations). Architecture forbidden-package lists are duplicated (acceptable). `OVERRIDE_ME` properties on inner-parent are ugly but overridden by children. |
| 12 | CI same gates? npm ci needs lockfile | `.github/workflows/quality-gates.yml`: `./mvnw -B verify` then `./mvnw -B -Pmutation -pl domain,application -am verify`; frontend `npm ci` + build + check, Node 22, lockfile cache path set. Same gates as local, plus mutation. |
| 13 | Banned Lombok/Jackson/Spring — enforcer scopes | Inner-layer bans have no `:compile` limiter, so test scope is banned too (stricter, OK). Lombok/Jackson/Spring are **not** banned on adapters (finding 2 for Lombok). Jackson `com.fasterxml.jackson.core` + `datatype` only; `dataformat` / `module` missing from enforcer (ArchUnit `com.fasterxml.jackson..` covers **used** Jackson on inner layers). |
| 14 | Add `spring-boot-starter` to domain, still green? | **No.** `bannedDependencies` `excludes` are the banned artifacts (Maven Enforcer docs). `org.springframework.boot:*` matches. Execution `ban-frameworks-on-inner-layers` ran on domain. Unused Spring fails Maven; used Spring also fails ArchUnit. The green-build bypass is finding 1 (`javax.*`), not Spring Boot. |

---

## What is already in good shape (do not rip out)

- Module graph: `crap-check` → `domain` → `application` → adapters → `bootstrap`; adapters do not depend on each other; bootstrap is the composition root.
- Java 21 enforcer + compiler release 21; Maven wrapper 3.9.11.
- ArchUnit tests are `@Test` methods, not silent `@ArchTest` fields; they ran.
- JaCoCo report → check → CRAP order on `clean verify` is correct; missing-report behavior is tested.
- CRAP formula, complexity source, line coverage, threshold 6, package-prefix matching (exact or child, not `domainx`) are tested.
- Frontend scaffold is Svelte 5 + Vite 6 + strict TS and is not where §7 rules live.
- `.editorconfig` / `.gitattributes` / `.gitignore` / constitution / README exist.

Fix findings 1–3 (re-bind inner-layer purity to a JDK allowlist, ban Lombok on every module, bind architecture/CRAP/PIT to **module output**). Then request re-review. Do not start Stage 1 until this file contains the line `STAGE 0 APPROVED`.
