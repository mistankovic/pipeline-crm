package com.pipelinecrm.application.error;

import com.pipelinecrm.domain.identity.Identifier;

/** The caller referred to something that is not there. */
public final class UnknownEntity extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public UnknownEntity(String kind, Identifier id) {
        super("no " + kind + " with id " + id.value());
    }
}
