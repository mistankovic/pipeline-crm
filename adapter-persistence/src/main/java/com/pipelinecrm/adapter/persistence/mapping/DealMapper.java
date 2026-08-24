package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.entity.ActivityEntity;
import com.pipelinecrm.adapter.persistence.entity.DealEntity;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.DealTitle;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import java.util.Currency;
import java.util.List;

public final class DealMapper {

    private DealMapper() {}

    public static Deal toDomain(DealEntity entity, List<ActivityEntity> activities) {
        return Deal.restore(
                new DealId(entity.id()),
                new CompanyId(entity.companyId()),
                new UserId(entity.ownerId()),
                DealTitle.of(entity.title()),
                Money.of(entity.amount(), Currency.getInstance(entity.currency())),
                Probability.of(entity.probability()),
                DealStage.valueOf(entity.stage()),
                activities.stream().map(ActivityMapper::toDomain).toList());
    }

    public static DealEntity toEntity(Deal deal) {
        return new DealEntity(
                deal.id().value(),
                deal.companyId().value(),
                deal.ownerId().value(),
                deal.title().value(),
                deal.value().amount(),
                deal.value().currency().getCurrencyCode(),
                deal.probability().percent(),
                deal.stage().name());
    }
}
