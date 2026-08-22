package com.pipelinecrm.application.acceptance;

import java.math.BigDecimal;

/**
 * What a deal is worth, as a scenario states it: "10000 EUR", optionally "at 50%
 * probability". Exists so that a step method takes one argument for one idea instead of
 * three, which keeps step signatures inside the Constitution's parameter limit without
 * weakening it for tests.
 */
record Amount(BigDecimal value, String currency, int probability) {

    private static final int UNSTATED_PROBABILITY = 50;

    static Amount of(BigDecimal value, String currency) {
        return new Amount(value, currency, UNSTATED_PROBABILITY);
    }
}
