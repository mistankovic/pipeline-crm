package com.pipelinecrm.domain.shared;

/** The two checks every constructor in this module needs, so no constructor rewrites them. */
public final class Guard {

    private Guard() {
    }

    public static <T> T present(T value, String subject) {
        if (value == null) {
            throw new InvariantViolation(subject + " is required");
        }
        return value;
    }

    public static String filled(String value, String subject) {
        if (value == null || value.isBlank()) {
            throw new InvariantViolation(subject + " must not be blank");
        }
        return value.strip();
    }
}
