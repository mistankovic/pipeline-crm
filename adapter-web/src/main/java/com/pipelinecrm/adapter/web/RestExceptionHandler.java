package com.pipelinecrm.adapter.web;

import com.pipelinecrm.application.usecase.NotFoundException;
import com.pipelinecrm.domain.deal.DealNotWinnableException;
import com.pipelinecrm.domain.deal.DealStageNotAuthorizedException;
import com.pipelinecrm.domain.deal.IllegalDealStageException;
import com.pipelinecrm.domain.forecast.MixedCurrencyException;
import com.pipelinecrm.domain.identity.InvalidEmailException;
import com.pipelinecrm.domain.shared.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    public record ErrorBody(String error, String message) {}

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorBody> notFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorBody("not_found", ex.getMessage()));
    }

    @ExceptionHandler(DealStageNotAuthorizedException.class)
    ResponseEntity<ErrorBody> forbidden(DealStageNotAuthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorBody("forbidden", ex.getMessage()));
    }

    @ExceptionHandler({
        IllegalDealStageException.class,
        DealNotWinnableException.class,
        MixedCurrencyException.class
    })
    ResponseEntity<ErrorBody> conflict(DomainException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorBody("conflict", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, InvalidEmailException.class})
    ResponseEntity<ErrorBody> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorBody("bad_request", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ErrorBody("bad_request", ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorBody> domain(DomainException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErrorBody("domain", ex.getMessage()));
    }
}
