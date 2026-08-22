package com.pipelinecrm.adapter.web.error;

import com.pipelinecrm.application.error.ApplicationException;
import com.pipelinecrm.domain.shared.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Turns a refusal into a response.
 *
 * <p>It extends {@link ResponseEntityExceptionHandler} so that the failures Spring already
 * classifies keep the statuses they deserve: a malformed body is 400, an unknown route 404, a
 * wrong method 405, an unsupported content type 415. The first version of the catch-all below
 * caught those too and answered 500 to all of them — the API blaming itself for the caller's
 * typo. See docs/reviews/stage-7-review.md, finding F-7.6.
 *
 * <p>The catch-all remains, for what is genuinely unexpected. Without it an unhandled failure
 * escapes to the container, which re-dispatches to {@code /error} — and that used to be
 * answered with 401, so a numeric overflow told a signed-in user they were not signed in.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({DomainException.class, ApplicationException.class})
    public ResponseEntity<ApiError> refused(RuntimeException failure) {
        return HttpTranslation.statusFor(failure)
                .map(status -> ResponseEntity.status(status)
                        .body(new ApiError(HttpTranslation.nameFor(failure), failure.getMessage())))
                .orElseGet(() -> unexpected(failure));
    }

    @ExceptionHandler(MalformedRequest.class)
    public ResponseEntity<ApiError> malformed(MalformedRequest failure) {
        return ResponseEntity.badRequest().body(new ApiError("MalformedRequest", failure.getMessage()));
    }

    /** Anything Spring did not classify and no handler above named. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> anythingElse(Exception failure) {
        return unexpected(failure);
    }

    /**
     * Bean-validation failures, in this project's error shape rather than Spring's default body.
     * Overriding the base class keeps one response format across the whole API.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException failure, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String detail = failure.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + " " + field.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiError("InvalidRequest", detail));
    }

    /**
     * The statuses Spring chose, kept, but re-bodied into this project's error shape so a client
     * can parse every failure the same way.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiError(HttpStatus.valueOf(status.value()).name(), reasonFor(status)));
    }

    private static String reasonFor(HttpStatusCode status) {
        return HttpStatus.valueOf(status.value()).getReasonPhrase();
    }

    private ResponseEntity<ApiError> unexpected(Exception failure) {
        LOG.error("unhandled failure, answering 500", failure);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("InternalError", "the request could not be completed"));
    }
}
