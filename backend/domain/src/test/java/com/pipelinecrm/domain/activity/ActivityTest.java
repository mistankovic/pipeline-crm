package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityTest {

    private static final ActivityId AN_ID = ActivityId.of(UUID.randomUUID());
    private static final DealId A_DEAL = DealId.of(UUID.randomUUID());
    private static final ContactId A_CONTACT = ContactId.of(UUID.randomUUID());
    private static final ActivityAuthorship BY_SOMEBODY =
            new ActivityAuthorship(UserId.of(UUID.randomUUID()), Instant.parse("2026-03-01T09:00:00Z"));

    private static Activity about(ActivitySubject subject, ActivityType type) {
        return new Activity(AN_ID, subject, type, "spoke about pricing", BY_SOMEBODY);
    }

    @ParameterizedTest
    @CsvSource({"CALL,true", "MEETING,true", "NOTE,false"})
    void only_a_call_or_a_meeting_counts_as_engagement(ActivityType type, boolean engagement) {
        assertThat(about(new DealSubject(A_DEAL), type).isEngagement()).isEqualTo(engagement);
    }

    @Test
    void an_activity_about_a_deal_is_about_that_deal() {
        assertThat(about(new DealSubject(A_DEAL), ActivityType.CALL).isAbout(A_DEAL)).isTrue();
    }

    @Test
    void an_activity_about_a_deal_is_not_about_another_deal() {
        Activity activity = about(new DealSubject(A_DEAL), ActivityType.CALL);

        assertThat(activity.isAbout(DealId.of(UUID.randomUUID()))).isFalse();
    }

    @Test
    void an_activity_about_a_contact_is_not_about_any_deal() {
        Activity activity = about(new ContactSubject(A_CONTACT), ActivityType.CALL);

        assertThat(activity.isAbout(A_DEAL)).isFalse();
    }

    @Test
    void an_activity_about_a_contact_is_about_that_contact() {
        assertThat(about(new ContactSubject(A_CONTACT), ActivityType.NOTE).isAbout(A_CONTACT)).isTrue();
    }

    @Test
    void an_activity_about_a_contact_is_not_about_another_contact() {
        Activity activity = about(new ContactSubject(A_CONTACT), ActivityType.NOTE);

        assertThat(activity.isAbout(ContactId.of(UUID.randomUUID()))).isFalse();
    }

    @Test
    void an_activity_about_a_deal_is_not_about_any_contact() {
        Activity activity = about(new DealSubject(A_DEAL), ActivityType.NOTE);

        assertThat(activity.isAbout(A_CONTACT)).isFalse();
    }

    @Test
    void trims_its_summary() {
        Activity activity = new Activity(AN_ID, new DealSubject(A_DEAL), ActivityType.NOTE, "  hi  ", BY_SOMEBODY);

        assertThat(activity.summary()).isEqualTo("hi");
    }

    @Test
    void refuses_to_exist_without_an_id() {
        assertThatThrownBy(() -> new Activity(null, new DealSubject(A_DEAL), ActivityType.NOTE, "s", BY_SOMEBODY))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_subject() {
        assertThatThrownBy(() -> new Activity(AN_ID, null, ActivityType.NOTE, "s", BY_SOMEBODY))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_type() {
        assertThatThrownBy(() -> new Activity(AN_ID, new DealSubject(A_DEAL), null, "s", BY_SOMEBODY))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_summary() {
        assertThatThrownBy(() -> new Activity(AN_ID, new DealSubject(A_DEAL), ActivityType.NOTE, " ", BY_SOMEBODY))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_authorship() {
        assertThatThrownBy(() -> new Activity(AN_ID, new DealSubject(A_DEAL), ActivityType.NOTE, "s", null))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void authorship_refuses_to_exist_without_an_author() {
        assertThatThrownBy(() -> new ActivityAuthorship(null, Instant.EPOCH))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void authorship_refuses_to_exist_without_a_time() {
        assertThatThrownBy(() -> new ActivityAuthorship(UserId.of(UUID.randomUUID()), null))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_deal_subject_refuses_to_exist_without_a_deal() {
        assertThatThrownBy(() -> new DealSubject(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_contact_subject_refuses_to_exist_without_a_contact() {
        assertThatThrownBy(() -> new ContactSubject(null)).isInstanceOf(InvariantViolation.class);
    }
}
