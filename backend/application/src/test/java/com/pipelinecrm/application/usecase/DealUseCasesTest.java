package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.CreateDeal;
import com.pipelinecrm.application.port.in.RepriceDeal;
import com.pipelinecrm.application.port.in.ReweightDeal;
import com.pipelinecrm.application.testing.Seed;
import com.pipelinecrm.application.testing.UseCases;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.domain.deal.ClosedDealIsImmutable;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.StageChangeForbidden;
import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DealUseCasesTest {

    private final UseCases application = new UseCases();
    private final Seed seed = new Seed(application);

    private User sam;
    private User robin;
    private UUID acme;

    @BeforeEach
    void seedTheWorld() {
        sam = seed.salesperson("Sam");
        robin = seed.salesperson("Robin");
        acme = seed.company("Acme");
    }

    private DealView aDeal() {
        return seed.deal("Acme renewal", acme, sam.id().value(), "10000");
    }

    private DealView move(UUID deal, DealStage stage, User actor) {
        return application.changeDealStage.handle(
                new ChangeDealStage.StageChange(deal, stage, actor.id().value()));
    }

    @Test
    void a_created_deal_starts_as_a_lead_whatever_the_caller_wants() {
        assertThat(aDeal().stage()).isEqualTo("LEAD");
    }

    @Test
    void a_created_deal_reports_its_weighted_value() {
        assertThat(aDeal().weightedValue().amount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void a_created_deal_reports_the_stages_it_may_move_to() {
        assertThat(aDeal().allowedTransitions()).containsExactlyInAnyOrder("QUALIFIED", "CLOSED_LOST");
    }

    @Test
    void a_created_deal_names_its_company_and_owner() {
        DealView deal = aDeal();

        assertThat(deal.company().name()).isEqualTo("Acme");
        assertThat(deal.owner().name()).isEqualTo("Sam");
    }

    @Test
    void creating_a_deal_runs_in_one_unit_of_work() {
        int before = application.transactions.started();

        aDeal();

        assertThat(application.transactions.started()).isEqualTo(before + 1);
    }

    @Test
    void a_deal_cannot_be_created_for_a_company_that_does_not_exist() {
        CreateDeal.NewDeal request = new CreateDeal.NewDeal(
                "Ghost", UUID.randomUUID(), sam.id().value(), sam.id().value(), BigDecimal.TEN, "EUR", 50);

        assertThatThrownBy(() -> application.createDeal.handle(request))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no company with id");
    }

    @Test
    void a_deal_cannot_be_created_for_an_owner_who_does_not_exist() {
        CreateDeal.NewDeal request = new CreateDeal.NewDeal(
                "Ghost", acme, UUID.randomUUID(), sam.id().value(), BigDecimal.TEN, "EUR", 50);

        assertThatThrownBy(() -> application.createDeal.handle(request))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no user with id");
    }

    @Test
    void a_deal_cannot_be_created_in_a_currency_that_does_not_exist() {
        CreateDeal.NewDeal request = new CreateDeal.NewDeal(
                "Ghost", acme, sam.id().value(), sam.id().value(), BigDecimal.TEN, "XYZ", 50);

        assertThatThrownBy(() -> application.createDeal.handle(request))
                .isInstanceOf(InvariantViolation.class).hasMessageContaining("unknown currency code");
    }

    @Test
    void a_deal_cannot_be_created_with_an_impossible_probability() {
        CreateDeal.NewDeal request = new CreateDeal.NewDeal(
                "Ghost", acme, sam.id().value(), sam.id().value(), BigDecimal.TEN, "EUR", 150);

        assertThatThrownBy(() -> application.createDeal.handle(request))
                .isInstanceOf(InvariantViolation.class).hasMessageContaining("between 0 and 100");
    }

    @Test
    void the_owner_may_advance_their_deal() {
        assertThat(move(aDeal().id(), DealStage.QUALIFIED, sam).stage()).isEqualTo("QUALIFIED");
    }

    @Test
    void a_stranger_may_not() {
        UUID deal = aDeal().id();

        assertThatThrownBy(() -> move(deal, DealStage.QUALIFIED, robin))
                .isInstanceOf(StageChangeForbidden.class);
    }

    @Test
    void an_unknown_deal_cannot_be_moved() {
        UUID nothing = UUID.randomUUID();

        assertThatThrownBy(() -> move(nothing, DealStage.QUALIFIED, sam))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no deal with id");
    }

    @Test
    void an_unknown_actor_cannot_move_a_deal() {
        UUID deal = aDeal().id();
        ChangeDealStage.StageChange change =
                new ChangeDealStage.StageChange(deal, DealStage.QUALIFIED, UUID.randomUUID());

        assertThatThrownBy(() -> application.changeDealStage.handle(change))
                .isInstanceOf(UnknownEntity.class).hasMessageContaining("no user with id");
    }

    @Test
    void a_move_that_succeeds_is_written_back() {
        UUID deal = aDeal().id();

        move(deal, DealStage.QUALIFIED, sam);

        assertThat(application.viewDeal.handle(deal, sam.id().value()).deal().stage()).isEqualTo("QUALIFIED");
    }

    @Test
    void the_owner_may_reprice_an_open_deal_in_another_currency() {
        UUID deal = aDeal().id();

        DealView repriced = application.repriceDeal.handle(new RepriceDeal.Repricing(
                deal, sam.id().value(), new BigDecimal("1500"), "USD"));

        assertThat(repriced.value().amount()).isEqualByComparingTo("1500.00");
        assertThat(repriced.value().currency()).isEqualTo("USD");
    }

    @Test
    void a_stranger_may_not_reprice_a_deal() {
        UUID deal = aDeal().id();
        RepriceDeal.Repricing repricing =
                new RepriceDeal.Repricing(deal, robin.id().value(), BigDecimal.ZERO, "EUR");

        assertThatThrownBy(() -> application.repriceDeal.handle(repricing))
                .isInstanceOf(StageChangeForbidden.class);
    }

    @Test
    void the_owner_may_reweight_an_open_deal() {
        UUID deal = aDeal().id();

        DealView reweighted = application.reweightDeal.handle(
                new ReweightDeal.Reweighting(deal, sam.id().value(), 80));

        assertThat(reweighted.probability()).isEqualTo(80);
        assertThat(reweighted.weightedValue().amount()).isEqualByComparingTo("8000.00");
    }

    @Test
    void a_stranger_may_not_reweight_a_deal() {
        UUID deal = aDeal().id();
        ReweightDeal.Reweighting reweighting =
                new ReweightDeal.Reweighting(deal, robin.id().value(), 0);

        assertThatThrownBy(() -> application.reweightDeal.handle(reweighting))
                .isInstanceOf(StageChangeForbidden.class);
    }

    @Test
    void a_closed_deal_can_no_longer_be_repriced() {
        UUID deal = aDeal().id();
        move(deal, DealStage.CLOSED_LOST, sam);
        RepriceDeal.Repricing repricing =
                new RepriceDeal.Repricing(deal, sam.id().value(), BigDecimal.TEN, "EUR");

        assertThatThrownBy(() -> application.repriceDeal.handle(repricing))
                .isInstanceOf(ClosedDealIsImmutable.class);
    }

    @Test
    void an_unknown_deal_cannot_be_repriced() {
        RepriceDeal.Repricing repricing =
                new RepriceDeal.Repricing(UUID.randomUUID(), sam.id().value(), BigDecimal.TEN, "EUR");

        assertThatThrownBy(() -> application.repriceDeal.handle(repricing)).isInstanceOf(UnknownEntity.class);
    }

    @Test
    void an_unknown_deal_cannot_be_reweighted() {
        ReweightDeal.Reweighting reweighting =
                new ReweightDeal.Reweighting(UUID.randomUUID(), sam.id().value(), 10);

        assertThatThrownBy(() -> application.reweightDeal.handle(reweighting)).isInstanceOf(UnknownEntity.class);
    }
}
