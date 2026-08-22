package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guard;

/** A forecast line for one deal owner. */
public record OwnerGroup(UserId owner) implements ForecastGroup {

    public OwnerGroup {
        Guard.present(owner, "owner a forecast is grouped by");
    }
}
