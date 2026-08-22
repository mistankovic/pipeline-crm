package com.pipelinecrm.adapter.web.error;

import com.pipelinecrm.application.error.AuthenticationFailed;
import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.domain.deal.ClosedDealIsImmutable;
import com.pipelinecrm.domain.deal.IllegalStageTransition;
import com.pipelinecrm.domain.deal.InapplicableActivityHistory;
import com.pipelinecrm.domain.deal.StageChangeForbidden;
import com.pipelinecrm.domain.deal.WinRequiresValueAndEngagement;
import com.pipelinecrm.domain.shared.CurrencyMismatch;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which HTTP status each way of saying no becomes.
 *
 * <p>This table is the only place in the system that knows HTTP has status codes for these
 * situations. The domain raises a named failure; deciding that "you may not move this deal" is
 * 403 and "this deal cannot be won yet" is 409 is a translation, and translation is what this
 * layer is for.
 *
 * <p>The table is hand-maintained, so a failure nobody added would quietly become a 500 in
 * production. {@code EveryFailureIsTranslatedTest} enumerates every failure type on the
 * classpath and requires each to appear either here or in {@link #DELIBERATELY_A_SERVER_FAULT}.
 * Adding a domain exception and forgetting about it now breaks the build.
 * See docs/reviews/stage-5-review.md, finding F-5.1.
 */
public final class HttpTranslation {

    private static final Map<Class<? extends RuntimeException>, HttpStatus> STATUSES = Map.of(
            AuthenticationFailed.class, HttpStatus.UNAUTHORIZED,
            StageChangeForbidden.class, HttpStatus.FORBIDDEN,
            UnknownEntity.class, HttpStatus.NOT_FOUND,
            IllegalStageTransition.class, HttpStatus.CONFLICT,
            WinRequiresValueAndEngagement.class, HttpStatus.CONFLICT,
            ClosedDealIsImmutable.class, HttpStatus.CONFLICT,
            InvariantViolation.class, HttpStatus.BAD_REQUEST);

    /**
     * Failures that are our fault, not the caller's, and must stay 500.
     *
     * <p>{@link InapplicableActivityHistory} means an outer layer handed a deal another deal's
     * history. No request can ask for that; if it happens, this code is wrong, and the caller
     * should learn nothing beyond "it did not work".
     *
     * <p>{@link CurrencyMismatch} means something tried to add two currencies. Forecast lines
     * are keyed by currency precisely so that cannot happen, so reaching it means the grouping
     * is broken — again our fault, not the caller's. It was **unmapped by omission** until the
     * completeness test found it, which is the whole argument for having that test.
     */
    private static final Set<Class<? extends RuntimeException>> DELIBERATELY_A_SERVER_FAULT =
            Set.of(InapplicableActivityHistory.class, CurrencyMismatch.class);

    private HttpTranslation() {
    }

    /** The status for a failure, or empty when this layer has no opinion and it is a server fault. */
    public static Optional<HttpStatus> statusFor(RuntimeException failure) {
        return Optional.ofNullable(STATUSES.get(failure.getClass()));
    }

    /** Whether this layer has decided, rather than merely forgotten, that a failure is a 500. */
    public static boolean isKnownServerFault(Class<?> failure) {
        return DELIBERATELY_A_SERVER_FAULT.contains(failure);
    }

    /** Whether this layer knows what to do with a failure at all. Used by the completeness test. */
    public static boolean isTranslated(Class<?> failure) {
        return STATUSES.containsKey(failure) || isKnownServerFault(failure);
    }

    /** The short machine-readable name a client can branch on. */
    public static String nameFor(RuntimeException failure) {
        return failure.getClass().getSimpleName();
    }
}
