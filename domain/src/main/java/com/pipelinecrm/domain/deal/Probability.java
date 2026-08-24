package com.pipelinecrm.domain.deal;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Probability(int percent) {

    public Probability {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("probability must be between 0 and 100");
        }
    }

    public static Probability of(int percent) {
        return new Probability(percent);
    }

    public static Probability closedWon() {
        return new Probability(100);
    }

    public static Probability closedLost() {
        return new Probability(0);
    }

    public BigDecimal fraction() {
        return BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 4, RoundingMode.UNNECESSARY);
    }
}
