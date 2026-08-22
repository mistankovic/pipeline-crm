package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.InvariantViolation;

import java.util.UUID;

/**
 * Reads identities from text. Exists so that the five identifier types share one
 * implementation, and so that a malformed id is a domain failure rather than a
 * {@link IllegalArgumentException} escaping from the JDK into an outer layer that would
 * report it as a server fault.
 */
final class Identifiers {

    private Identifiers() {
    }

    static UUID parse(String value, String subject) {
        String text = Guard.filled(value, subject);
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException malformed) {
            throw new InvariantViolation(subject + " is not a valid identifier: " + text);
        }
    }
}
