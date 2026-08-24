package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.entity.ActivityEntity;
import com.pipelinecrm.adapter.persistence.entity.DealEntity;
import com.pipelinecrm.adapter.persistence.mapping.ActivityMapper;
import com.pipelinecrm.adapter.persistence.mapping.DealMapper;
import com.pipelinecrm.adapter.persistence.spring.SpringActivityRepository;
import com.pipelinecrm.adapter.persistence.spring.SpringDealRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JpaDealRepository implements DealRepository {

    private final SpringDealRepository deals;
    private final SpringActivityRepository activities;

    public JpaDealRepository(SpringDealRepository deals, SpringActivityRepository activities) {
        this.deals = deals;
        this.activities = activities;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Deal> findById(DealId id) {
        return deals.findById(id.value())
                .map(entity -> DealMapper.toDomain(entity, activities.findByDealId(id.value())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deal> findAll() {
        Map<UUID, List<ActivityEntity>> byDeal = groupDealActivities();
        List<Deal> result = new ArrayList<>();
        for (DealEntity entity : deals.findAll()) {
            result.add(DealMapper.toDomain(entity, byDeal.getOrDefault(entity.id(), List.of())));
        }
        return result;
    }

    @Override
    public void save(Deal deal) {
        deals.save(DealMapper.toEntity(deal));
        for (Activity activity : deal.activities()) {
            activities.save(ActivityMapper.toEntity(activity));
        }
    }

    private Map<UUID, List<ActivityEntity>> groupDealActivities() {
        Map<UUID, List<ActivityEntity>> byDeal = new HashMap<>();
        for (ActivityEntity activity : activities.findAll()) {
            if (activity.dealId() != null) {
                byDeal.computeIfAbsent(activity.dealId(), key -> new ArrayList<>()).add(activity);
            }
        }
        return byDeal;
    }
}
