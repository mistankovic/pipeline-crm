package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.InvariantViolation;

import java.util.List;

/**
 * Everything recorded against one deal. The type exists so that a rule about a deal's
 * history is asked of an object that knows it holds *that* deal's history, rather than of
 * a bare list somebody might have filled from the wrong query.
 */
public record DealActivities(DealId deal, List<Activity> activities) {

    public DealActivities {
        Guard.present(deal, "deal the activities belong to");
        Guard.present(activities, "activities");
        activities = List.copyOf(activities);
        if (!activities.stream().allMatch(activity -> activity.isAbout(deal))) {
            throw new InvariantViolation("activities of " + deal.value() + " contain an entry about something else");
        }
    }

    public static DealActivities none(DealId deal) {
        return new DealActivities(deal, List.of());
    }

    public boolean includeEngagement() {
        return activities.stream().anyMatch(Activity::isEngagement);
    }

    public boolean belongTo(DealId candidate) {
        return deal.equals(candidate);
    }
}
