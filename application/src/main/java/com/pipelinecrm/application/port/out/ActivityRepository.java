package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import java.util.List;

public interface ActivityRepository {

    List<Activity> findByDeal(DealId dealId);

    List<Activity> findByContact(ContactId contactId);

    void save(Activity activity);
}
