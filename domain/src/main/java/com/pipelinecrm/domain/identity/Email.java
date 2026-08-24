package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guards;
import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        String trimmed = Guards.notBlank(value, "email").toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidEmailException(trimmed);
        }
        value = trimmed;
    }

    public static Email of(String raw) {
        return new Email(raw);
    }
}
