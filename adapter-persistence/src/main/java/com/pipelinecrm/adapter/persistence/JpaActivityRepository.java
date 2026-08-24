package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.ActivityMapper;
import com.pipelinecrm.adapter.persistence.spring.SpringActivityRepository;
import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JpaActivityRepository implements ActivityRepository {

    private final SpringActivityRepository activities;

    public JpaActivityRepository(SpringActivityRepository activities) {
        this.activities = activities;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> findByDeal(DealId dealId) {
        return activities.findByDealId(dealId.value()).stream()
                .map(ActivityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> findByContact(ContactId contactId) {
        return activities.findByContactId(contactId.value()).stream()
                .map(ActivityMapper::toDomain)
                .toList();
    }

    @Override
    public void save(Activity activity) {
        activities.save(ActivityMapper.toEntity(activity));
    }
}
