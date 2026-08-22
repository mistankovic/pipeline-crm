package com.pipelinecrm.adapter.web.error;

/** What a caller is told when something is refused. Never a stack trace, never an internal id. */
public record ApiError(String error, String message) {
}
