package com.pipelinecrm.application.error;

import com.pipelinecrm.domain.identity.Identifier;

import java.util.Locale;

/** The caller referred to something that is not there. */
public final class UnknownEntity extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public UnknownEntity(Identifier id) {
        super("no " + kindOf(id) + " with id " + id.value());
    }

    /**
     * A DealId is not ambiguous about what it identifies, so the caller does not repeat it as
     * a string literal at nine call sites. See docs/reviews/stage-3-review.md, finding F-3.5.
     */
    private static String kindOf(Identifier id) {
        String type = id.getClass().getSimpleName().replace("Id", "");
        return type.toLowerCase(Locale.ROOT);
    }
}
