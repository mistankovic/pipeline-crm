package com.pipelinecrm.adapter.web.error;

import com.pipelinecrm.application.error.ApplicationException;
import com.pipelinecrm.domain.shared.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Turns a refusal into a response.
 *
 * <p>A failure this layer has no translation for is a **server** fault: it is logged in full
 * and answered with 500 and nothing else, because a caller who was not meant to cause it
 * should not learn anything from it either.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException failure) {
        String detail = failure.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + " " + field.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiError("InvalidRequest", detail));
    }

    /**
     * The backstop.
     *
     * <p>Without it, anything the handlers above do not name escapes to the container, which
     * re-dispatches to {@code /error} — and that dispatch used to be answered with 401. A
     * numeric overflow was reported to a signed-in user as "you are not authenticated". Nothing
     * leaves this class unhandled now. F-7.1.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> anythingElse(Exception failure) {
        return unexpected(failure);
    }

    private ResponseEntity<ApiError> unexpected(Exception failure) {
        LOG.error("unhandled failure, answering 500", failure);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("InternalError", "the request could not be completed"));
    }
}
