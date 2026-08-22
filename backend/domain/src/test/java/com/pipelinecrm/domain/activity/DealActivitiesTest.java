package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DealActivitiesTest {

    private static final DealId THIS_DEAL = DealId.of(UUID.randomUUID());
    private static final DealId ANOTHER_DEAL = DealId.of(UUID.randomUUID());
    private static final ActivityAuthorship BY_SOMEBODY =
            new ActivityAuthorship(UserId.of(UUID.randomUUID()), Instant.parse("2026-03-01T09:00:00Z"));

    private static Activity activity(ActivitySubject subject, ActivityType type) {
        return new Activity(ActivityId.of(UUID.randomUUID()), subject, type, "summary", BY_SOMEBODY);
    }

    @Test
    void an_empty_history_holds_no_engagement() {
        assertThat(DealActivities.none(THIS_DEAL).includeEngagement()).isFalse();
    }

    @Test
    void a_history_of_notes_holds_no_engagement() {
        DealActivities history = new DealActivities(THIS_DEAL,
                List.of(activity(new DealSubject(THIS_DEAL), ActivityType.NOTE)));

        assertThat(history.includeEngagement()).isFalse();
    }

    @Test
    void a_single_call_among_notes_is_engagement() {
        DealActivities history = new DealActivities(THIS_DEAL, List.of(
                activity(new DealSubject(THIS_DEAL), ActivityType.NOTE),
                activity(new DealSubject(THIS_DEAL), ActivityType.CALL)));

        assertThat(history.includeEngagement()).isTrue();
    }

    @Test
    void refuses_activities_belonging_to_another_deal() {
        List<Activity> foreign = List.of(activity(new DealSubject(ANOTHER_DEAL), ActivityType.CALL));

        assertThatThrownBy(() -> new DealActivities(THIS_DEAL, foreign))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("about something else");
    }

    @Test
    void refuses_activities_belonging_to_a_contact() {
        List<Activity> contactActivity =
                List.of(activity(new ContactSubject(ContactId.of(UUID.randomUUID())), ActivityType.MEETING));

        assertThatThrownBy(() -> new DealActivities(THIS_DEAL, contactActivity))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_deal() {
        assertThatThrownBy(() -> new DealActivities(null, List.of())).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_list() {
        assertThatThrownBy(() -> new DealActivities(THIS_DEAL, null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void copies_the_list_so_later_additions_cannot_change_the_answer() {
        List<Activity> mutable = new ArrayList<>();
        DealActivities history = new DealActivities(THIS_DEAL, mutable);

        mutable.add(activity(new DealSubject(THIS_DEAL), ActivityType.CALL));

        assertThat(history.includeEngagement()).isFalse();
    }

    @Test
    void knows_which_deal_it_describes() {
        assertThat(DealActivities.none(THIS_DEAL).belongTo(THIS_DEAL)).isTrue();
    }

    @Test
    void knows_which_deal_it_does_not_describe() {
        assertThat(DealActivities.none(THIS_DEAL).belongTo(ANOTHER_DEAL)).isFalse();
    }
}
