package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.CreateContact;
import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.application.testing.Seed;
import com.pipelinecrm.application.testing.UseCases;
import com.pipelinecrm.application.view.ContactView;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.forecast.ForecastDimension;
import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadingUseCasesTest {

    private final UseCases application = new UseCases();
    private final Seed seed = new Seed(application);

    private User sam;
    private User robin;
    private UUID acme;
    private UUID globex;

    @BeforeEach
    void seedTheWorld() {
        sam = seed.salesperson("Sam");
        robin = seed.salesperson("Robin");
        acme = seed.company("Acme");
        globex = seed.company("Globex");
    }

    @Test
    void lists_every_company() {
        assertThat(application.listCompanies.handle()).extracting("name")
                .containsExactly("Acme", "Globex");
    }

    @Test
    void lists_every_contact() {
        contact("Cara", acme);
        contact("Dev", globex);

        assertThat(application.listContacts.everything()).extracting(ContactView::name)
                .containsExactly("Cara", "Dev");
    }

    @Test
    void lists_the_contacts_at_one_company() {
        contact("Cara", acme);
        contact("Dev", globex);

        assertThat(application.listContacts.atCompany(acme)).extracting(ContactView::name)
                .containsExactly("Cara");
    }

    @Test
    void a_contact_cannot_be_created_at_a_company_that_does_not_exist() {
        CreateContact.NewContact request =
                new CreateContact.NewContact(UUID.randomUUID(), "Ghost", "ghost@example.com");

        assertThatThrownBy(() -> application.createContact.handle(request))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no company with id");
    }

    @Test
    void a_contact_cannot_be_created_with_an_address_that_is_not_one() {
        CreateContact.NewContact request = new CreateContact.NewContact(acme, "Cara", "nope");

        assertThatThrownBy(() -> application.createContact.handle(request))
                .isInstanceOf(InvariantViolation.class).hasMessageContaining("not an email address");
    }

    @Test
    void shows_every_deal_on_the_board() {
        seed.deal("One", acme, sam.id().value(), "1000");
        seed.deal("Two", globex, robin.id().value(), "2000");

        assertThat(application.viewPipeline.everything()).extracting(DealView::title)
                .containsExactly("One", "Two");
    }

    @Test
    void shows_only_one_owners_deals_when_asked() {
        seed.deal("One", acme, sam.id().value(), "1000");
        seed.deal("Two", globex, robin.id().value(), "2000");

        assertThat(application.viewPipeline.ownedBy(robin.id().value()))
                .extracting(DealView::title).containsExactly("Two");
    }

    @Test
    void names_the_company_and_owner_of_every_deal_on_the_board() {
        seed.deal("One", acme, sam.id().value(), "1000");

        assertThat(application.viewPipeline.everything()).singleElement()
                .satisfies(deal -> {
                    assertThat(deal.company().name()).isEqualTo("Acme");
                    assertThat(deal.owner().name()).isEqualTo("Sam");
                });
    }

    @Test
    void an_unknown_deal_has_no_detail_view() {
        UUID nothing = UUID.randomUUID();

        assertThatThrownBy(() -> application.viewDeal.handle(nothing))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no deal with id");
    }

    @Test
    void a_deal_detail_view_carries_the_timeline_in_the_order_it_was_recorded() {
        UUID deal = seed.deal("One", acme, sam.id().value(), "1000").id();
        log(deal, "NOTE", "first");
        log(deal, "CALL", "second");

        assertThat(application.viewDeal.handle(deal).timeline())
                .extracting("summary").containsExactly("first", "second");
    }

    @Test
    void an_unknown_contact_has_no_timeline() {
        UUID nothing = UUID.randomUUID();

        assertThatThrownBy(() -> application.viewContactTimeline.handle(nothing))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no contact with id");
    }

    @Test
    void a_contact_timeline_holds_only_that_contacts_activities() {
        UUID cara = contact("Cara", acme);
        UUID dev = contact("Dev", globex);
        application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutContact(cara), "NOTE", "about Cara", sam.id().value()));
        application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutContact(dev), "NOTE", "about Dev", sam.id().value()));

        assertThat(application.viewContactTimeline.handle(cara))
                .extracting("summary").containsExactly("about Cara");
    }

    @Test
    void forecasts_by_owner_with_a_readable_label() {
        seed.deal("One", acme, sam.id().value(), "1000");

        assertThat(application.produceForecast.handle(ForecastDimension.OWNER).lines())
                .singleElement()
                .satisfies(line -> {
                    assertThat(line.group()).isEqualTo(sam.id().value().toString());
                    assertThat(line.label()).isEqualTo("Sam");
                    assertThat(line.weightedValue().amount()).isEqualByComparingTo("500.00");
                });
    }

    @Test
    void forecasts_by_stage() {
        seed.deal("One", acme, sam.id().value(), "1000");

        assertThat(application.produceForecast.handle(ForecastDimension.STAGE).lines())
                .singleElement()
                .satisfies(line -> {
                    assertThat(line.group()).isEqualTo("LEAD");
                    assertThat(line.label()).isEqualTo("LEAD");
                });
    }

    @Test
    void names_the_dimension_it_was_asked_for() {
        assertThat(application.produceForecast.handle(ForecastDimension.STAGE).dimension()).isEqualTo("STAGE");
    }

    @Test
    void a_closed_deal_leaves_the_forecast() {
        UUID deal = seed.deal("One", acme, sam.id().value(), "1000").id();
        application.changeDealStage.handle(
                new ChangeDealStage.StageChange(deal, DealStage.CLOSED_LOST, sam.id().value()));

        assertThat(application.produceForecast.handle(ForecastDimension.OWNER).lines()).isEmpty();
    }

    private UUID contact(String name, UUID company) {
        return application.createContact.handle(new CreateContact.NewContact(
                company, name, name.toLowerCase(java.util.Locale.ROOT) + "@example.com")).id();
    }

    private void log(UUID deal, String type, String summary) {
        application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutDeal(deal), type, summary, sam.id().value()));
    }
}
