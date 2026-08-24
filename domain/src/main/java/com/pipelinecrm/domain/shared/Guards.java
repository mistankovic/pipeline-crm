package com.pipelinecrm.domain.shared;

public final class Guards {

    private Guards() {}

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    public static String notBlank(String value, String name) {
        notNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public static String maxLength(String value, int max, String name) {
        if (value.length() > max) {
            throw new IllegalArgumentException(name + " must be at most " + max + " characters");
        }
        return value;
    }
}
