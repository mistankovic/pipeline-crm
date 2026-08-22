# Stage 0 — Adversarial Review (round 1)

Reviewer posture: the Builder wrote a document full of promises and a build that
enforces some of them. My job is to find the promises with no enforcement behind them,
and the enforcement that does not actually work. I verified claims by running things,
not by reading the hand-off.

## What I verified as genuinely working

* `mvn -f backend/pom.xml verify` is green from clean. Confirmed.
* The domain dependency ban is real, not decorative. I added
  `org.springframework:spring-context` to `domain/pom.xml` and the build failed at
  `validate` with *"The domain module must have zero compile/runtime/provided
  dependencies … spring-context <--- banned via the exclude/include list"*. Reverted.
* Checkstyle is not cosmetic: it rejected the Builder's own first commit of
  `PipelineCrmApplication`.
* `tools/crap.py` computes CRAP correctly on the cases its tests cover.

Everything below is a defect.

---

## F-0.1 — BLOCKER. The build only works from one directory.

`checkstyle.config` and `checkstyle.suppressions` are resolved from
`${maven.multiModuleProjectDirectory}`. There is no `.mvn` directory, so Maven sets that
property to the **current working directory**, not the reactor root. Proof:

```
$ cd backend/domain && mvn validate
[ERROR] Unable to find suppressions file at location:
        .../backend/domain/config/checkstyle/suppressions.xml
$ mvn help:evaluate -Dexpression=maven.multiModuleProjectDirectory -DforceStdout
/home/user/uncle-bob-agentic-principles/backend/domain
```

So `mvn -pl domain test` from inside a module — the single most common developer
loop — breaks the quality gates. Worse, it breaks them by *erroring*, and the obvious
"fix" a hurried developer reaches for is to delete the `suppressionsLocation` line.
A gate that is annoying from the wrong directory is a gate that gets removed.

**Required:** make the location resolution independent of the working directory.

## F-0.2 — BLOCKER. The CRAP gate passes when there is nothing to measure.

```
$ printf '<report name="domain"></report>' > empty.xml
$ python3 tools/crap.py empty.xml --threshold 6 --report r.md
CRAP gate passed for domain: 0 methods, worst n/a   (exit 0)
```

An empty or truncated JaCoCo report — the exact thing you get when the test run was
skipped, when `-DskipTests` was passed, or when the agent failed to attach — is reported
as a **pass**. This is the worst possible failure mode for a quality gate: it is silent
and it says "green". The same class of hole exists inside `_counter`, which returns
`(0, 0)` for an absent counter, so a method with no `COMPLEXITY` counter scores CRAP 0
and a method with no `LINE` counter is treated as 100 % covered.

**Required:** zero analysed methods must be a failure unless explicitly declared empty;
a method carrying a `COMPLEXITY` counter but no `LINE` counter must be a failure.

## F-0.3 — MAJOR. Coverage and CRAP gates are opt-**in**, so a new module has none.

`backend/pom.xml` sets `coverage.skip=true` and `crap.skip=true`, and `domain` and
`application` switch them off. This is backwards. Add a sixth module tomorrow — say
`application-forecasting` — and it inherits *no* coverage check and *no* CRAP check,
silently. Defaults must be safe; opting out must be loud.

**Required:** default both to `false` in the parent and opt out explicitly, per module,
with a comment naming the reason.

## F-0.4 — MAJOR. `CONSTITUTION.md` §4 promises a CPD duplication gate that does not exist.

> "Duplicated blocks | 0 above 30 tokens in `domain`/`application` | PMD CPD, build fails"

There is no PMD plugin in any POM. The Builder listed this under "deliberately deferred",
but the Constitution is written in the present indicative and Stage 0's entire purpose is
the gates. A constitution that describes gates which do not exist trains everyone to
read it as aspirational. Either wire it now or strike the row.

**Required:** wire `maven-pmd-plugin` `cpd-check` for `domain` and `application`, failing
the build.

## F-0.5 — MAJOR. The Constitution and `.gitignore` contradict each other.

§5.2: *"with the report committed under `reports/`"*. `.gitignore` line: `reports/`.
It is impossible to comply with both. One of them is wrong and nobody noticed, which
tells me §5 was written without being executed.

**Required:** decide. My recommendation: build reports are generated artefacts and do not
belong in git; the *numbers* belong in the hand-off documents. Fix the Constitution, keep
the ignore.

## F-0.6 — MAJOR. Coverage is checked per bundle, so one dead class hides behind ten good ones.

The JaCoCo rule uses `<element>BUNDLE</element>` only. With twenty classes at 99 % and one
entirely untested class, `domain` still passes at 95 %. Since the untested class is
exactly the one likely to contain the bug, the gate is weakest precisely where it matters.

**Required:** add a `CLASS`-element rule with a floor (I will accept 0.90 line / 0.85
branch per class alongside the 0.95 bundle rule), so no single class can be dark.

## F-0.7 — MINOR. Test sources are exempt from every Clean Code check.

`includeTestSourceDirectory` is `false`, yet §3.2 of the Constitution legislates test
style (one reason to fail, AAA, named builders). None of it is enforced and — given the
required coverage levels — test code will be the majority of the codebase. Unreviewed
test code is where duplication goes to breed.

**Required:** either enable Checkstyle on test sources with a relaxed ruleset, or amend
§3.2 to say plainly that test style is enforced by review only.

## F-0.8 — MINOR. `docs/mutation-survivors.md` is legislated but absent.

§3 requires every surviving mutant to be justified in that file. The file does not exist
and no gate reads it. Acceptable as a review-only obligation, but the Constitution must
say "review gate", the way §1.3 and §4 honestly do elsewhere.

## F-0.9 — MINOR. Nothing runs these gates except a human who remembers to.

There is no CI configuration. Every gate in this stage is a gate only for the developer
who types the full `mvn verify` rather than `mvn test -DskipTests` in a hurry. This is not
promised by the Constitution, which is itself the problem.

**Required:** a CI workflow running the full verify, or an explicit non-goal entry.

## F-0.10 — NIT. `.gitignore` bans `*.jar`, which would silently swallow a Maven wrapper.

If a `.mvn/wrapper/maven-wrapper.jar` is added while fixing F-0.1, it will be ignored and
the wrapper will be broken for everyone who clones. Pre-empt it.

## F-0.11 — NIT. `vite.config.ts` is not type-checked.

`tsconfig.json` includes only `src/**` and `tests/**`, so the config file — which now
contains a `test:` block that is not part of Vite's own `UserConfig` type — is checked by
nothing. `npm run check` reporting "0 errors" is therefore weaker than it looks.

---

## Verdict

**STAGE 0 NOT APPROVED.** Two blockers (F-0.1, F-0.2) mean the gates do not work in the
conditions they will actually meet: a developer in a subdirectory, and a test run that
did not happen. Fix F-0.1 through F-0.6 before resubmission; F-0.7 through F-0.11 may be
fixed or refused in writing.

---

# Stage 0 — Adversarial Review (round 2)

I re-ran the checks myself rather than reading the Builder's table.

| Finding | Independently verified? |
|---------|--------------------------|
| F-0.1 | Yes. `cd backend/application && mvn validate` → success. Previously errored. |
| F-0.2 | Yes. The empty report that passed in round 1 now returns exit 3 with *"That means the tests did not run, not that the code is clean."* That is the right message: it tells the reader what the failure means, not just that something failed. |
| F-0.3 | Yes. Parent defaults are `false`; the four opt-outs each carry a reason naming the Constitution clause that permits them. |
| F-0.4 | Yes. `domain/target/cpd.xml` is produced; `cpd-check` is bound to `verify`. |
| F-0.5, F-0.8 | Yes. The Constitution no longer claims enforcement it does not have. §3 marking the mutation-survivor file as review-only is the honest fix. |
| F-0.6 | Yes. `CLASS` rule present at 0.90/0.85. |
| F-0.7 | Yes, and better than I asked for — the structural rules apply to tests and only four judgement-based checks are lifted, each justified in the file rather than in a commit message nobody will read. |
| F-0.9, F-0.10, F-0.11 | Yes. |
| Full build | `mvn -f backend/pom.xml verify` with **no** `-D` overrides → BUILD SUCCESS. This matters: in round 1 the Builder's own green run required two gates to be switched off from the command line. |

## Remaining objection, overruled by me

The temporary `crap.skip`/`coverage.skip`/`pit.skip` in `domain` and `application` are a
gate switched off, which is exactly what I complained about in F-0.3. I accept the
Builder's argument: after the F-0.2 fix these gates now fail *loudly* on an empty module,
and the right response to "the gate correctly reports nothing to measure" is to declare
the module unmeasured, not to teach the gate to shrug. The declaration is three lines of
comment naming the stage that must delete it.

**This is now a tracked debt.** The Stage 1 review will fail if
`domain/pom.xml` still contains those three properties. The Stage 3 review will fail if
`application/pom.xml` does. I have written it here so it cannot be quietly forgotten.

## One new nit, not blocking

`docs/mutation-survivors.md` still does not exist. It should be created empty with a
header at the point mutation testing first runs (Stage 3), not later.

## Verdict

The scaffolding now enforces what the document promises, the two fail-open holes are
closed, and the build is green without command-line escape hatches.

**STAGE 0 APPROVED**
