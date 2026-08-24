package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.Guards;

public record DealTitle(String value) {

    public static final int MAX_LENGTH = 180;

    public DealTitle {
        value = Guards.maxLength(Guards.notBlank(value, "title"), MAX_LENGTH, "title");
    }

    public static DealTitle of(String raw) {
        return new DealTitle(raw);
    }
}
