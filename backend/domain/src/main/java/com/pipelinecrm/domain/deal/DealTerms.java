package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;

/** What a deal is worth and how likely we think it is. The two always travel together. */
public record DealTerms(Money value, Probability probability) {

    public DealTerms {
        Guard.present(value, "value of a deal");
        Guard.present(probability, "probability of a deal");
    }

    public Money weighted() {
        return value.weightedBy(probability);
    }

    public DealTerms pricedAt(Money newValue) {
        return new DealTerms(Guard.present(newValue, "new value"), probability);
    }

    public DealTerms weightedAt(Probability newProbability) {
        return new DealTerms(value, Guard.present(newProbability, "new probability"));
    }
}
