package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.deal.Deal;

/** The two ways this demo slices the pipeline. */
public enum ForecastDimension {

    OWNER {
        @Override
        public String keyOf(Deal deal) {
            return deal.parties().owner().value().toString();
        }
    },
    STAGE {
        @Override
        public String keyOf(Deal deal) {
            return deal.stage().name();
        }
    };

    public abstract String keyOf(Deal deal);
}
