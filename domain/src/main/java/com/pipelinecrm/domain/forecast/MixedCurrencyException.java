package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.shared.DomainException;

public final class MixedCurrencyException extends DomainException {

    public MixedCurrencyException() {
        super("forecast requires every open deal to use the requested currency");
    }
}
