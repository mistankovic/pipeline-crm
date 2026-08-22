package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Change what an open deal is worth. Separate from {@link ReweightDeal} so that neither
 * port needs a nullable field meaning "leave this one alone", and so that the currency is
 * always stated rather than inherited from whatever the deal happened to hold.
 */
public interface RepriceDeal {

    DealView handle(Repricing repricing);

    record Repricing(UUID dealId, UUID actorId, BigDecimal amount, String currency) {
    }
}
