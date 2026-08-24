package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.shared.Guards;

public record ActivityBody(String value) {

    public static final int MAX_LENGTH = 4000;

    public ActivityBody {
        value = Guards.maxLength(Guards.notBlank(value, "body"), MAX_LENGTH, "body");
    }

    public static ActivityBody of(String raw) {
        return new ActivityBody(raw);
    }
}
