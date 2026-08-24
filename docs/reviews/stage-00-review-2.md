# Stage 0 — Re-review

STAGE 0 APPROVED

Scoped re-review of `02448f9` (`fix: enforce JDK allowlist and module-output architecture gates`) against `docs/reviews/stage-00-review.md` and `docs/reviews/stage-00-fixes.md`. Branch `local-grok-pipeline-crm`, working tree clean.

Command: `export JAVA_HOME="$(/usr/libexec/java_home -v 21)" && ./mvnw -B verify` — BUILD SUCCESS (8 reactor modules, including `inner-parent`).

---

## Previous Important findings

### 1. Inner-layer “JDK only” gates still admit `javax.*` Java EE / JPA

**Verdict: ADDRESSED**

- ArchUnit `onlyDependOn` no longer allows `javax..`. Explicit JDK namespaces only (`DomainIndependenceTest.java:59-78`, `ApplicationIndependenceTest.java:69-89`).
- Denylist now includes `javax.persistence..`, `javax.servlet..`, `javax.ws.rs..`, `javax.ejb..`, and `org.mockito..` (`DomainIndependenceTest.java:35-49`).
- Inner-layer enforcer is an allowlist: ban `*:*` except documented test tools / transitives / `crap-check` / `domain` (`inner-parent/pom.xml:38-64`), plus the framework denylist including `javax.persistence/servlet/ws.rs/ejb` and remaining Jackson modules (`inner-parent/pom.xml:69-97`).
- `./mvnw -B verify` ran `ban-non-jdk-artifacts-on-inner-layers` and `ban-frameworks-on-inner-layers` on `domain` and `application`; both passed.
- CONSTITUTION.md §2.2 records the tightened allowlist and `javax.*` EE prefixes.

Unused `javax.persistence-api` or Guava on an inner module now fails the `*:*` allowlist. Used `javax.persistence` also fails ArchUnit.

### 2. Lombok banned in every module; only inner layers enforced

**Verdict: ADDRESSED**

- Root enforcer `enforce-java-21` now includes `bannedDependencies` `org.projectlombok:lombok` (`pom.xml:183-189`). Verify logged `BannedDependencies passed` on every module, including adapters, bootstrap, and crap-check.
- ArchUnit lombok rules: `PersistenceIndependenceTest.java:41-48`, `WebIndependenceTest.java:41-48`, `LayerDependencyRulesTest.java:98-105` (bootstrap imports all module outputs), `CrapArchitectureTest.java:31-41`. Inner-layer tests already listed `org.projectlombok..`.

### 3. Architecture / CRAP / PIT keyed off package names, not module output

**Verdict: ADDRESSED**

- Architecture tests import `target/classes` (`DomainIndependenceTest.java:14` and siblings), not `importPackages`. Bootstrap imports each module output directory (`LayerDependencyRulesTest.java:16-22`).
- Every production class must `resideInAPackage` of its layer (`DomainIndependenceTest.java:22-28`, same pattern on application / adapters / crap-check / bootstrap).
- CRAP Maven binding no longer passes `--package` (`inner-parent/pom.xml:146-159`). Empty prefix list checks every executable method (`CrapEvaluator.java:32-35`; `CrapGateTest.withoutPackageFilterChecksEveryExecutableMethodInTheReport`; verify: `crap-gate` on domain/application after `jacoco:report`).
- Off-package types cannot survive ArchUnit, so PIT’s package glob is no longer an escape hatch for production code that exists in the module.

---

## Previous Minors the Builder claimed fixed

| # | Claim | Verdict | Evidence |
| --- | --- | --- | --- |
| 5 | Checkstyle `AvoidStarImport` on `verify` | **ADDRESSED** | `tools/checkstyle/checkstyle.xml`; root `maven-checkstyle-plugin` `avoid-star-imports` / `includeTestSourceDirectory=true` (`pom.xml:196-221`). Verify: “You have 0 Checkstyle violations” on every module. |
| 7 | `inner-parent` is a reactor module | **ADDRESSED** | `pom.xml:16`; reactor order includes Inner Layer Parent. Domain/application still run inner-layer enforcer, JaCoCo check, and CRAP (gates not hollowed). |
| 8 | README uses `npm ci` | **ADDRESSED** | `README.md:47`. |
| 9 | crap-check artifact allowlist | **ADDRESSED** | `tools/crap-check/pom.xml:24-52` `ban-non-jdk-artifacts-on-crap-check`; ran on verify. |
| 10 | CRAP uses `${java.home}/bin/java` | **ADDRESSED** | `inner-parent/pom.xml:147`, `tools/crap-check/pom.xml:110`. |
| 11 | Mockito on ArchUnit denylist | **ADDRESSED** | `DomainIndependenceTest.java:48`, `ApplicationIndependenceTest.java:48`. |

Minors 4 (`failWhenNoMutations=false`) and 6 (port package rules) were left for Stage 3 / Stage 1 as the first review allowed.

---

## New issues from the fix diff

No new Critical or Important findings.

`pluginManagement` on `inner-parent` did **not** drop inner-layer gates on `domain`/`application`. It also did **not** fully isolate the POM: inherited root `enforcer` / `jacoco` plugins caused `ban-non-jdk-artifacts-on-inner-layers`, `ban-frameworks-on-inner-layers`, and `jacoco:check` to run on `inner-parent` itself (JaCoCo skipped: no `jacoco.exec`). Verify still succeeded; domain/application gates still fired. Not a reject.

### Residual Minors (do not block Stage 0)

1. Inner-layer allowlist includes (`slf4j`, `asm`, `bytebuddy`, JUnit, …) are not scoped `:test`. An unused compile dependency on those artifacts would pass enforcer; using them in production still fails ArchUnit `onlyDependOn`.
2. ArchUnit still allows `javax.xml..`, which includes EE `javax.xml.bind`. Adding `javax.xml.bind:jaxb-api` is still banned by the `*:*` artifact allowlist.
3. CONSTITUTION.md §5.2 still describes CRAP as package-prefix scoped; the Maven binding now checks the whole module report. Equivalent while resideInAPackage holds; update the clause when convenient.
4. PIT `targetClasses` remains a package glob. Safe only because ArchUnit forbids off-package types in the module.

---

Stage 1 may start. Further stages still need `STAGE N APPROVED` in `docs/reviews/` before N+1.
