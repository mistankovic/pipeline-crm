package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guards;
import java.util.Optional;

public final class ForecastBucket {

    private final UserId ownerId;
    private final DealStage stage;
    private final WeightedValue total;

    private ForecastBucket(UserId ownerId, DealStage stage, WeightedValue total) {
        this.ownerId = ownerId;
        this.stage = stage;
        this.total = Guards.notNull(total, "total");
    }

    public static ForecastBucket owner(UserId ownerId, WeightedValue total) {
        return new ForecastBucket(Guards.notNull(ownerId, "ownerId"), null, total);
    }

    public static ForecastBucket stage(DealStage stage, WeightedValue total) {
        return new ForecastBucket(null, Guards.notNull(stage, "stage"), total);
    }

    public Optional<UserId> ownerId() {
        return Optional.ofNullable(ownerId);
    }

    public Optional<DealStage> stage() {
        return Optional.ofNullable(stage);
    }

    public WeightedValue total() {
        return total;
    }
}
