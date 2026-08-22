package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryActivities implements ActivityRepository {

    private final Map<ActivityId, Activity> stored = new LinkedHashMap<>();

    @Override
    public DealActivities findByDeal(DealId deal) {
        return new DealActivities(deal, stored.values().stream().filter(a -> a.isAbout(deal)).toList());
    }

    @Override
    public List<Activity> findByContact(ContactId contact) {
        return stored.values().stream().filter(activity -> activity.isAbout(contact)).toList();
    }

    @Override
    public void save(Activity activity) {
        stored.put(activity.id(), activity);
    }
}
