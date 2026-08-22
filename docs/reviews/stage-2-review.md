# Stage 2 — Adversarial Review (round 1)

My job here is to decide whether the dependency rule is *enforced* or merely *described*.
So I tried to break it.

## Sabotage results

| Injected violation | Caught by | Verdict |
|--------------------|-----------|---------|
| `@Component` on a class in `domain` | **javac** — `package org.springframework.stereotype does not exist` | The domain cannot even compile a framework import. Strongest possible enforcement. |
| `application` class referencing `adapter.web.security.SignedInUser` | **javac** — `package com.pipelinecrm.adapter.web does not exist` | Same. |
| `private DealStage stage` → `public` in `Deal` | Checkstyle `VisibilityModifier`, **and** ArchUnit `domain_state_is_private_and_mostly_final` (`Field <…Deal.stage> does not have modifier PRIVATE`) | Two independent gates. |

I also confirmed ArchUnit is not decorative theatre: it failed the build in a full reactor
run with the public field in place, and passed the moment I reverted.

Credit where due: rewriting the two vacuous rules instead of switching on
`allowEmptyShould(true)` was the right call, and the reasoning is in a comment in the code
where the next person will find it.

Now the problems.

---

## F-2.1 — MAJOR. The architecture tests can pass against code that no longer exists.

Running `mvn -pl bootstrap test` — the obvious way to "just check the architecture" — puts
the **last installed jars** of `domain` and `application` on the classpath, not the working
tree. I proved it: with `public DealStage stage` in `Deal.java` on disk and an install that
had failed before publishing, `mvn -pl bootstrap test` reported

```
InnerLayerPurityTest  Tests run: 4, Failures: 0
```

while the same violation in a full reactor run produced

```
Rule 'no domain object exposes its state directly' was violated (1 times)
Field <com.pipelinecrm.domain.deal.Deal.stage> does not have modifier PRIVATE
```

A green architecture report against stale bytecode is worse than no report: it is a gate
that says "safe" while looking at last week's code. Documentation will not fix this; nobody
reads a README before running a test.

**Required:** make the test refuse to run against a jar. ArchUnit exposes each class's
source URI; a rule that fails when a `com.pipelinecrm` class was loaded from a `.jar`
rather than from a `target/classes` directory is a few lines and closes the hole
permanently.

## F-2.2 — MAJOR. The default database path is the one that was never executed.

The Builder disclosed this honestly, and that matters. But look at what the disclosure
says: `ContainerisedPostgres` is the **default** branch of `PostgresDatabase.resolve()`,
and it has never run. Every developer who clones this repo takes that branch first.

The container start genuinely cannot be exercised here — that is the environment, not the
Builder. But `resolve()` itself is pure branch-selection logic over two environment
variables, and *that* can be tested anywhere:

* variable absent → containerised
* variable blank → containerised
* variable set → provided, with the given URL
* user/password variables absent → documented defaults

Right now zero of those are pinned. If someone inverts the condition, every CI run silently
switches to whatever `PIPELINECRM_TEST_DB_URL` happens to be, or to no database at all.

**Required:** unit tests for the selection logic. The container start stays unproven here
and the hand-off keeps saying so.

## F-2.3 — MAJOR. The security skeleton has no tests at all.

`JwtAuthenticationFilter` and `JwtAccessTokenIssuer` are the two classes that decide who a
caller is. Between them they have **zero** unit tests. None of these is pinned anywhere:

* an expired token,
* a token signed with a different secret,
* a token from a different issuer,
* a missing `Authorization` header,
* a header that is not `Bearer …`,
* a subject that is not a UUID,
* a round trip — issue a token, parse it, get the same user id back.

`adapter-web` opted out of the coverage gate on the grounds that "this module is held to
account by integration tests against a real HTTP surface". There are no such tests for these
classes. The opt-out was granted for a promise that has not been kept. This is precisely how
an opt-out granted in good faith turns into an untested module.

**Required:** unit tests for both classes covering at least the list above.

## F-2.4 — MAJOR. The two adapters *do* know each other, at runtime, and ArchUnit cannot see it.

`PersistenceConfiguration` declares:

```java
@Bean
public Clock systemClock() { return Clock.systemUTC(); }
```

and `JwtAccessTokenIssuer` — in **`adapter-web`** — takes a `Clock` in its constructor. So
the web adapter's behaviour depends on a bean the persistence adapter happens to publish.
Delete the persistence module and the web adapter stops working, for reasons no compiler and
no ArchUnit rule will explain, because the coupling exists only in the Spring context.

This is exactly the dependency `the_two_adapters_do_not_know_each_other` was written to
forbid, arriving through the one door that rule cannot watch. It is also plainly misplaced:
a clock is not a persistence concern.

**Required:** the `Clock` bean moves to `bootstrap`, which is the composition root and the
only layer entitled to decide what the other layers are wired to.

## F-2.5 — MINOR. `JwtAuthenticationFilter.token()` returns the string `"n/a"`.

```java
private String token() { return "n/a"; }
```

A private method whose entire job is to return a constant, used as the credentials of an
`Authentication`. It reads as if something was left half-done. Either pass the real token or
pass `null`, which is what Spring Security expects for an already-verified principal.

## F-2.6 — MINOR. `JwtSettings` mixes validation and defaulting in one constructor.

`secret` is mandatory and throws; `validity` and `issuer` silently default. Two different
policies in four lines, and the reader has to notice which is which. Not wrong, but the
`IllegalStateException` also escapes as a raw Spring bean-creation failure with a stack
trace rather than a clear startup message.

## F-2.7 — NIT. `SecurityConfiguration` permits `/api/sessions`, which does not exist yet.

The route is invented here and defined in Stage 5. If Stage 5 names it anything else, the
sign-in endpoint will be silently protected by the token it is supposed to issue, and the
symptom will be a 401 on login that looks like a credentials bug. Leave a marker so Stage 5
cannot forget.

## F-2.8 — NIT. No CORS configuration.

The Svelte dev server proxies `/api`, so development works. Anything that calls the API from
a browser on another origin will not. Fine for now; it needs a decision in Stage 6 rather
than a discovery.

---

## Verdict

The dependency rule is genuinely enforced — three independent mechanisms, and I could not get
a violation past them in a full build. But F-2.1 means the check can report green against
code that is not there, F-2.4 is a real inter-adapter coupling that slipped through the exact
rule meant to catch it, and F-2.3 is a module cashing in a coverage opt-out it has not
earned.

**STAGE 2 NOT APPROVED.** Fix F-2.1 through F-2.4. F-2.5 through F-2.8 may be fixed or
refused in writing.
