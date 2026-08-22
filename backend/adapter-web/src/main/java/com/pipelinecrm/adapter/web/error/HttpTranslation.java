package com.pipelinecrm.adapter.web.error;

import com.pipelinecrm.application.error.AuthenticationFailed;
import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.domain.deal.ClosedDealIsImmutable;
import com.pipelinecrm.domain.deal.IllegalStageTransition;
import com.pipelinecrm.domain.deal.StageChangeForbidden;
import com.pipelinecrm.domain.deal.WinRequiresValueAndEngagement;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Which HTTP status each way of saying no becomes.
 *
 * <p>This table is the only place in the system that knows HTTP has status codes for these
 * situations. The domain raises a named failure; deciding that "you may not move this deal"
 * is 403 and "this deal cannot be won yet" is 409 is a translation, and translation is what
 * this layer is for.
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

    private HttpTranslation() {
    }

    /** The status for a failure, or empty when this layer has no opinion and it is a server fault. */
    public static HttpStatus statusFor(RuntimeException failure) {
        return STATUSES.get(failure.getClass());
    }

    /** The short machine-readable name a client can branch on. */
    public static String nameFor(RuntimeException failure) {
        return failure.getClass().getSimpleName();
    }
}
