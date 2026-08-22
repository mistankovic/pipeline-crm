package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.ActivityMapping;
import com.pipelinecrm.adapter.persistence.repository.ActivityRows;
import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The activity port, over JPA. The deal query returns a {@link DealActivities}, which carries
 * the deal it belongs to — so a caller cannot hand one deal's history to another deal, and
 * the guarantee is made here, at the only place that knows the query was for that deal.
 */
@Repository
public class JpaActivityRepository implements ActivityRepository {

    private final ActivityRows rows;

    public JpaActivityRepository(ActivityRows rows) {
        this.rows = rows;
    }

    @Override
    public DealActivities findByDeal(DealId deal) {
        return new DealActivities(deal, rows.findByDealIdOrderByOccurredAtAsc(deal.value()).stream()
                .map(ActivityMapping::toDomain).toList());
    }

    @Override
    public List<Activity> findByContact(ContactId contact) {
        return rows.findByContactIdOrderByOccurredAtAsc(contact.value()).stream()
                .map(ActivityMapping::toDomain).toList();
    }

    @Override
    public void save(Activity activity) {
        rows.save(ActivityMapping.toRow(activity));
    }
}
