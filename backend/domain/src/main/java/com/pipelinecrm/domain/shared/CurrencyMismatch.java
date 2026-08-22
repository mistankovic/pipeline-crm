package com.pipelinecrm.domain.shared;

import java.util.Currency;

/** Raised when two amounts in different currencies are combined. This demo never converts. */
public final class CurrencyMismatch extends DomainException {

    private static final long serialVersionUID = 1L;

    public CurrencyMismatch(Currency expected, Currency actual) {
        super("cannot combine " + actual.getCurrencyCode() + " with " + expected.getCurrencyCode());
    }
}
