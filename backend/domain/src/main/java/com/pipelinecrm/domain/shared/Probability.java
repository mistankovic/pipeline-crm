package com.pipelinecrm.domain.shared;

import java.math.BigDecimal;

/** A whole-percentage likelihood between 0 and 100 inclusive. */
public record Probability(int percentage) {

    private static final int LOWEST = 0;
    private static final int HIGHEST = 100;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(HIGHEST);

    public Probability {
        if (percentage < LOWEST || percentage > HIGHEST) {
            throw new InvariantViolation("probability must be between 0 and 100, was " + percentage);
        }
    }

    public static Probability of(int percentage) {
        return new Probability(percentage);
    }

    public static Probability certain() {
        return new Probability(HIGHEST);
    }

    public static Probability impossible() {
        return new Probability(LOWEST);
    }

    public BigDecimal asFraction() {
        return BigDecimal.valueOf(percentage).divide(HUNDRED);
    }
}
