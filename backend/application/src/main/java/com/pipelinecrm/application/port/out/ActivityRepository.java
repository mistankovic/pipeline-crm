package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;

import java.util.List;

/**
 * How the use cases reach activities. The deal query returns a {@link DealActivities}
 * rather than a bare list, so that a caller cannot accidentally hand one deal's history to
 * another deal.
 */
public interface ActivityRepository {

    DealActivities findByDeal(DealId deal);

    List<Activity> findByContact(ContactId contact);

    void save(Activity activity);
}
