package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.application.port.in.CreateContact;
import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.application.testing.Seed;
import com.pipelinecrm.application.testing.UseCases;
import com.pipelinecrm.application.view.ActivityView;
import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogActivityInteractorTest {

    private final UseCases application = new UseCases();
    private final Seed seed = new Seed(application);

    private User sam;
    private UUID deal;
    private UUID cara;

    @BeforeEach
    void seedTheWorld() {
        sam = seed.salesperson("Sam");
        UUID acme = seed.company("Acme");
        deal = seed.deal("Acme renewal", acme, sam.id().value(), "10000").id();
        cara = application.createContact.handle(
                new CreateContact.NewContact(acme, "Cara", "cara@example.com")).id();
    }

    private ActivityView logAgainstDeal(String type, String summary) {
        return application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutDeal(deal), type, summary, sam.id().value()));
    }

    @Test
    void records_an_activity_against_a_deal() {
        ActivityView logged = logAgainstDeal("CALL", "talked pricing");

        assertThat(logged.dealId()).isEqualTo(deal);
        assertThat(logged.contactId()).isNull();
        assertThat(logged.type()).isEqualTo("CALL");
        assertThat(logged.summary()).isEqualTo("talked pricing");
    }

    @Test
    void records_an_activity_against_a_contact() {
        ActivityView logged = application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutContact(cara), "NOTE", "prefers email", sam.id().value()));

        assertThat(logged.contactId()).isEqualTo(cara);
        assertThat(logged.dealId()).isNull();
    }

    @Test
    void stamps_the_activity_with_the_applications_clock_not_the_wall_clock() {
        assertThat(logAgainstDeal("NOTE", "when").occurredAt()).isEqualTo(UseCases.NOW);
    }

    @Test
    void records_who_logged_it() {
        assertThat(logAgainstDeal("NOTE", "who").author().id()).isEqualTo(sam.id().value());
    }

    @Test
    void gives_the_activity_a_fresh_identity() {
        assertThat(logAgainstDeal("NOTE", "one").id()).isNotEqualTo(logAgainstDeal("NOTE", "two").id());
    }

    @Test
    void runs_in_one_unit_of_work() {
        int before = application.transactions.started();

        logAgainstDeal("NOTE", "counted");

        assertThat(application.transactions.started()).isEqualTo(before + 1);
    }

    @Test
    void refuses_an_activity_type_that_does_not_exist() {
        assertThatThrownBy(() -> logAgainstDeal("SMOKE_SIGNAL", "nope"))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("unknown activity type: SMOKE_SIGNAL");
    }

    @Test
    void refuses_a_missing_activity_type() {
        assertThatThrownBy(() -> logAgainstDeal(null, "nope")).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_an_author_who_does_not_exist() {
        LogActivity.NewActivity request = new LogActivity.NewActivity(
                new LogActivity.AboutDeal(deal), "NOTE", "by nobody", UUID.randomUUID());

        assertThatThrownBy(() -> application.logActivity.handle(request))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no user with id");
    }

    @Test
    void refuses_a_deal_that_does_not_exist() {
        LogActivity.NewActivity request = new LogActivity.NewActivity(
                new LogActivity.AboutDeal(UUID.randomUUID()), "NOTE", "about nothing", sam.id().value());

        assertThatThrownBy(() -> application.logActivity.handle(request))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no deal with id");
    }

    @Test
    void refuses_a_contact_that_does_not_exist() {
        LogActivity.NewActivity request = new LogActivity.NewActivity(
                new LogActivity.AboutContact(UUID.randomUUID()), "NOTE", "about nothing", sam.id().value());

        assertThatThrownBy(() -> application.logActivity.handle(request))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no contact with id");
    }

    @Test
    void an_activity_about_a_contact_does_not_appear_on_a_deal_timeline() {
        application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutContact(cara), "MEETING", "introductions", sam.id().value()));

        assertThat(application.viewDeal.handle(deal).timeline()).isEmpty();
    }
}
