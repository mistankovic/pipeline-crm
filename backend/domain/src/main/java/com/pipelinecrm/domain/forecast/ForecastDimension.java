package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.deal.Deal;

/** The two ways this demo slices the pipeline. */
public enum ForecastDimension {

    OWNER {
        @Override
        public ForecastGroup groupOf(Deal deal) {
            return new OwnerGroup(deal.parties().owner());
        }
    },
    STAGE {
        @Override
        public ForecastGroup groupOf(Deal deal) {
            return new StageGroup(deal.stage());
        }
    };

    public abstract ForecastGroup groupOf(Deal deal);
}
