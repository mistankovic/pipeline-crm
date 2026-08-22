package com.pipelinecrm.domain.shared;

import java.util.Locale;
import java.util.regex.Pattern;

/** An address that is at least shaped like an email address, stored lower case. */
public record EmailAddress(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    public EmailAddress {
        value = Guard.filled(value, "email address").toLowerCase(Locale.ROOT);
        if (!SHAPE.matcher(value).matches()) {
            throw new InvariantViolation("not an email address: " + value);
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }
}
