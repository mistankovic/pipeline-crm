package com.pipelinecrm.application.support;

import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryActivityRepository implements ActivityRepository {

    private final List<Activity> store = new ArrayList<>();

    @Override
    public List<Activity> findByDeal(DealId dealId) {
        List<Activity> result = new ArrayList<>();
        for (Activity activity : store) {
            if (activity.target().isDeal(dealId)) {
                result.add(activity);
            }
        }
        return result;
    }

    @Override
    public List<Activity> findByContact(ContactId contactId) {
        List<Activity> result = new ArrayList<>();
        for (Activity activity : store) {
            if (activity.target().contactId().isPresent() && activity.target().contactId().get().equals(contactId)) {
                result.add(activity);
            }
        }
        return result;
    }

    @Override
    public void save(Activity activity) {
        store.add(activity);
    }
}
