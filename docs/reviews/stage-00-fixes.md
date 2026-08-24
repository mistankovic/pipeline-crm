# Stage 0 — Fixes after review

Responding to `docs/reviews/stage-00-review.md` (`STAGE 0 REJECTED`).

## Important 1 — `javax..` allowlist / incomplete enforcer

- Inner-layer ArchUnit `onlyDependOn` no longer allows all of `javax..`. It lists JDK `javax.*` namespaces only.
- Denylist now includes `javax.persistence..`, `javax.servlet..`, `javax.ws.rs..`, `javax.ejb..`, and `org.mockito..`.
- Inner-layer enforcer has an allowlist (`*:*` banned except documented test tools) plus the previous framework denylist, now including `javax.persistence/servlet/ws.rs/ejb` and remaining Jackson modules.

## Important 2 — Lombok only banned on inner layers

- Root enforcer `bannedDependencies` now bans `org.projectlombok:lombok` on every module.
- ArchUnit lombok rules added for persistence, web, bootstrap (all production bytecode), and crap-check.

## Important 3 — Gates keyed off package names

- Architecture tests import `target/classes` (bootstrap imports every module output directory), not `importPackages`.
- Every production class must `resideInAPackage` of its layer. Off-package types in a module fail verify.
- CRAP Maven binding no longer passes `--package`; it checks every executable method in that module’s JaCoCo report. `--package` remains an optional CLI filter.

## Minors

- 5: Checkstyle `AvoidStarImport` on `verify`.
- 7: `inner-parent` is a reactor module; extra plugin executions live in `pluginManagement` so they do not run on the POM itself.
- 8: README uses `npm ci`.
- 9: crap-check has the same artifact allowlist enforcer.
- 10: CRAP subprocess uses `${java.home}/bin/java`.
- 11: Mockito is on the ArchUnit denylist.
- 4 and 6 left for Stage 3 / Stage 1 as the review allowed.
