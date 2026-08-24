package com.pipelinecrm.tools.crap;

final class Require {

    private Require() {}

    static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    static int notNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return value;
    }

    static double notNegative(double value, String name) {
        if (value < 0.0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return value;
    }
}
