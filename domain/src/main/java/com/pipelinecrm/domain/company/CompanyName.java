package com.pipelinecrm.domain.company;

import com.pipelinecrm.domain.shared.Guards;

public record CompanyName(String value) {

    public static final int MAX_LENGTH = 180;

    public CompanyName {
        value = Guards.maxLength(Guards.notBlank(value, "companyName"), MAX_LENGTH, "companyName");
    }

    public static CompanyName of(String raw) {
        return new CompanyName(raw);
    }
}
