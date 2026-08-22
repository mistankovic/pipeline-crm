package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.DomainException;

/** Rule: only the deal's owner or a manager may move it through the pipeline. */
public final class StageChangeForbidden extends DomainException {

    private static final long serialVersionUID = 1L;

    public StageChangeForbidden(UserId actor) {
        super("user " + actor.value() + " neither owns this deal nor is a manager");
    }
}
