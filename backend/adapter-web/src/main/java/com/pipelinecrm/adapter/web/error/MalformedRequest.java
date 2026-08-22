package com.pipelinecrm.adapter.web.error;

/**
 * The caller sent something this layer cannot turn into a use-case argument at all — a stage
 * that is not a stage, a dimension that is not a dimension. Purely an HTTP concern: the use
 * case is never asked, because there is nothing to ask it with.
 */
public final class MalformedRequest extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MalformedRequest(String message) {
        super(message);
    }
}
