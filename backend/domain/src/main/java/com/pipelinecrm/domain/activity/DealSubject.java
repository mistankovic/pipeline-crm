package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.Guard;

/** An activity recorded against a deal. */
public record DealSubject(DealId deal) implements ActivitySubject {

    public DealSubject {
        Guard.present(deal, "deal an activity is about");
    }
}
