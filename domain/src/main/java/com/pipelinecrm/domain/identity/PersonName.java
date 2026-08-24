package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guards;

public record PersonName(String value) {

    public static final int MAX_LENGTH = 120;

    public PersonName {
        value = Guards.maxLength(Guards.notBlank(value, "name"), MAX_LENGTH, "name");
    }

    public static PersonName of(String raw) {
        return new PersonName(raw);
    }
}
