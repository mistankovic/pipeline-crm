package com.pipelinecrm.tools.crap;

/**
 * Original CRAP: {@code CRAP(m) = comp(m)^2 * (1 - cov(m))^3 + comp(m)}.
 */
public final class CrapCalculator {

    private CrapCalculator() {}

    public static double crap(int cyclomaticComplexity, double lineCoverageRatio) {
        requireComplexity(cyclomaticComplexity);
        requireRatio(lineCoverageRatio);
        double uncovered = 1.0 - lineCoverageRatio;
        double complexity = cyclomaticComplexity;
        return complexity * complexity * uncovered * uncovered * uncovered + complexity;
    }

    public static boolean exceeds(double crap, double threshold) {
        Require.notNegative(threshold, "threshold");
        return crap > threshold;
    }

    private static void requireComplexity(int cyclomaticComplexity) {
        Require.notNegative(cyclomaticComplexity, "complexity");
    }

    private static void requireRatio(double lineCoverageRatio) {
        if (lineCoverageRatio < 0.0 || lineCoverageRatio > 1.0) {
            throw new IllegalArgumentException("coverage ratio must be in [0, 1]");
        }
    }
}
