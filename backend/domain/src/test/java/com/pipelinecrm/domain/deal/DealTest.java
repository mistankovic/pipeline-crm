package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;
import com.pipelinecrm.domain.testing.Examples;
import com.pipelinecrm.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DealTest {

    private final UserId ownerId = Examples.aUserId();
    private final User owner = Examples.salesperson(ownerId);
    private final User manager = Examples.manager();
    private final User stranger = Examples.salesperson(Examples.aUserId());

    private DealActivities engagement(Deal deal) {
        return Examples.engagementFor(deal.id(), ownerId);
    }

    @Test
    void a_new_deal_starts_as_a_lead() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        assertThat(deal.stage()).isEqualTo(DealStage.LEAD);
    }

    @Test
    void the_owner_may_advance_their_own_deal() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        deal.changeStageTo(DealStage.QUALIFIED, owner, engagement(deal));

        assertThat(deal.stage()).isEqualTo(DealStage.QUALIFIED);
    }

    @Test
    void a_manager_may_advance_somebody_elses_deal() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        deal.changeStageTo(DealStage.QUALIFIED, manager, engagement(deal));

        assertThat(deal.stage()).isEqualTo(DealStage.QUALIFIED);
    }

    @Test
    void another_salesperson_may_not_touch_the_stage() {
        Deal deal = Examples.dealWorth("1000", ownerId);
        DealActivities history = engagement(deal);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.QUALIFIED, stranger, history))
                .isInstanceOf(StageChangeForbidden.class)
                .hasMessageContaining("neither owns this deal nor is a manager");
    }

    @Test
    void a_rejected_stage_change_leaves_the_deal_alone() {
        Deal deal = Examples.dealWorth("1000", ownerId);
        DealActivities history = engagement(deal);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.QUALIFIED, stranger, history))
                .isInstanceOf(StageChangeForbidden.class);

        assertThat(deal.stage()).isEqualTo(DealStage.LEAD);
    }

    @Test
    void nobody_at_all_is_not_allowed_to_change_the_stage() {
        Deal deal = Examples.dealWorth("1000", ownerId);
        DealActivities history = engagement(deal);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.QUALIFIED, null, history))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_stage_may_not_be_skipped() {
        Deal deal = Examples.dealWorth("1000", ownerId);
        DealActivities history = engagement(deal);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.PROPOSAL, owner, history))
                .isInstanceOf(IllegalStageTransition.class)
                .hasMessageContaining("LEAD")
                .hasMessageContaining("PROPOSAL");
    }

    @Test
    void a_deal_may_be_lost_straight_from_lead() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        deal.changeStageTo(DealStage.CLOSED_LOST, owner, engagement(deal));

        assertThat(deal.stage()).isEqualTo(DealStage.CLOSED_LOST);
    }

    @Test
    void losing_a_deal_forces_its_probability_to_zero() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        deal.changeStageTo(DealStage.CLOSED_LOST, owner, engagement(deal));

        assertThat(deal.probability()).isEqualTo(Probability.impossible());
    }

    @Test
    void winning_a_deal_forces_its_probability_to_one_hundred() {
        Deal deal = Examples.dealAt(DealStage.NEGOTIATION, "1000", ownerId);

        deal.changeStageTo(DealStage.CLOSED_WON, owner, engagement(deal));

        assertThat(deal.probability()).isEqualTo(Probability.certain());
    }

    @Test
    void advancing_within_the_pipeline_leaves_the_probability_where_the_user_put_it() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        deal.changeStageTo(DealStage.QUALIFIED, owner, engagement(deal));

        assertThat(deal.probability()).isEqualTo(Probability.of(50));
    }

    @Test
    void a_deal_worth_nothing_cannot_be_won() {
        Deal deal = Examples.dealAt(DealStage.NEGOTIATION, "0", ownerId);
        DealActivities history = engagement(deal);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.CLOSED_WON, owner, history))
                .isInstanceOf(WinRequiresValueAndEngagement.class)
                .hasMessageContaining("its value is 0");
    }

    @Test
    void a_deal_nobody_has_spoken_about_cannot_be_won() {
        Deal deal = Examples.dealAt(DealStage.NEGOTIATION, "1000", ownerId);
        DealActivities notesOnly = Examples.notesOnlyFor(deal.id(), ownerId);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.CLOSED_WON, owner, notesOnly))
                .isInstanceOf(WinRequiresValueAndEngagement.class)
                .hasMessageContaining("no call or meeting");
    }

    @Test
    void a_deal_with_no_history_at_all_cannot_be_won() {
        Deal deal = Examples.dealAt(DealStage.NEGOTIATION, "1000", ownerId);
        DealActivities empty = DealActivities.none(deal.id());

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.CLOSED_WON, owner, empty))
                .isInstanceOf(WinRequiresValueAndEngagement.class);
    }

    @Test
    void a_worthless_deal_may_still_be_lost() {
        Deal deal = Examples.dealWorth("0", ownerId);

        deal.changeStageTo(DealStage.CLOSED_LOST, owner, DealActivities.none(deal.id()));

        assertThat(deal.stage()).isEqualTo(DealStage.CLOSED_LOST);
    }

    @Test
    void a_deal_refuses_to_be_judged_by_another_deals_history() {
        Deal deal = Examples.dealAt(DealStage.NEGOTIATION, "1000", ownerId);
        DealActivities somebodyElses = Examples.engagementFor(DealId.of(UUID.randomUUID()), ownerId);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.CLOSED_WON, owner, somebodyElses))
                .isInstanceOf(InapplicableActivityHistory.class)
                .hasMessageContaining("does not belong to deal");
    }

    @Test
    void a_deal_refuses_to_be_judged_with_no_history_object_at_all() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.QUALIFIED, owner, null))
                .isInstanceOf(InvariantViolation.class);
    }

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"CLOSED_WON", "CLOSED_LOST"})
    void a_closed_deal_cannot_be_moved_again(DealStage closedStage) {
        Deal deal = closedDealAt(closedStage);
        DealActivities history = engagement(deal);

        assertThatThrownBy(() -> deal.changeStageTo(DealStage.NEGOTIATION, manager, history))
                .isInstanceOf(IllegalStageTransition.class);
    }

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"CLOSED_WON", "CLOSED_LOST"})
    void a_closed_deal_cannot_be_repriced(DealStage closedStage) {
        Deal deal = closedDealAt(closedStage);
        Money newValue = Money.of("2000", "EUR");

        assertThatThrownBy(() -> deal.reprice(newValue))
                .isInstanceOf(ClosedDealIsImmutable.class)
                .hasMessageContaining(closedStage.name());
    }

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"CLOSED_WON", "CLOSED_LOST"})
    void a_closed_deal_cannot_be_reweighted(DealStage closedStage) {
        Deal deal = closedDealAt(closedStage);
        Probability newProbability = Probability.of(70);

        assertThatThrownBy(() -> deal.reweight(newProbability)).isInstanceOf(ClosedDealIsImmutable.class);
    }

    @Test
    void an_open_deal_may_be_repriced() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        deal.reprice(Money.of("2500", "EUR"));

        assertThat(deal.value()).isEqualTo(Money.of("2500", "EUR"));
    }

    @Test
    void an_open_deal_may_be_reweighted() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        deal.reweight(Probability.of(80));

        assertThat(deal.probability()).isEqualTo(Probability.of(80));
    }

    @Test
    void repricing_to_nothing_is_not_a_price() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        assertThatThrownBy(() -> deal.reprice(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void reweighting_to_nothing_is_not_a_probability() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        assertThatThrownBy(() -> deal.reweight(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void weights_its_value_by_its_probability() {
        Deal deal = Examples.dealWorth("1000.00", ownerId);

        assertThat(deal.weightedValue()).isEqualTo(Money.of("500.00", "EUR"));
    }

    @Test
    void an_open_deal_says_so() {
        assertThat(Examples.dealWorth("1000", ownerId).isOpen()).isTrue();
    }

    @Test
    void a_closed_deal_says_so() {
        assertThat(closedDealAt(DealStage.CLOSED_LOST).isOpen()).isFalse();
    }

    @Test
    void lists_the_stages_it_could_move_to_right_now() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        assertThat(deal.allowedTransitions())
                .containsExactlyInAnyOrder(DealStage.QUALIFIED, DealStage.CLOSED_LOST);
    }

    @Test
    void a_deal_in_negotiation_may_be_won_or_lost() {
        Deal deal = Examples.dealAt(DealStage.NEGOTIATION, "1000", ownerId);

        assertThat(deal.allowedTransitions())
                .containsExactlyInAnyOrder(DealStage.CLOSED_WON, DealStage.CLOSED_LOST);
    }

    @Test
    void a_closed_deal_may_move_nowhere() {
        assertThat(closedDealAt(DealStage.CLOSED_WON).allowedTransitions()).isEmpty();
    }

    @Test
    void keeps_the_title_it_was_opened_with() {
        assertThat(Examples.dealWorth("1000", ownerId).title()).isEqualTo("Acme renewal");
    }

    @Test
    void exposes_its_parties() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        assertThat(deal.parties().ownedBy(ownerId)).isTrue();
    }

    @Test
    void round_trips_through_a_snapshot_unchanged() {
        Deal original = Examples.dealAt(DealStage.PROPOSAL, "1234.50", ownerId);

        Deal restored = Deal.from(original.snapshot());

        assertThat(restored.snapshot()).isEqualTo(original.snapshot());
    }

    @Test
    void refuses_to_be_rebuilt_from_nothing() {
        assertThatThrownBy(() -> Deal.from(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void two_deals_with_the_same_id_are_the_same_deal() {
        Deal deal = Examples.dealWorth("1000", ownerId);
        Deal sameIdentity = Deal.from(deal.snapshot());
        sameIdentity.reprice(Money.of("9999", "EUR"));

        assertThat(deal).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
    }

    @Test
    void hashes_by_identity_so_it_can_be_kept_in_a_set_while_its_state_changes() {
        Deal deal = Examples.dealWorth("1000", ownerId);
        int before = deal.hashCode();

        deal.reprice(Money.of("9999", "EUR"));

        assertThat(deal.hashCode()).isEqualTo(before).isEqualTo(deal.id().hashCode());
    }

    @Test
    void two_deals_with_different_ids_are_different_deals() {
        assertThat(Examples.dealWorth("1000", ownerId)).isNotEqualTo(Examples.dealWorth("1000", ownerId));
    }

    @Test
    void a_deal_is_not_equal_to_something_that_is_not_a_deal() {
        assertThat(Examples.dealWorth("1000", ownerId)).isNotEqualTo("a deal");
    }

    @Test
    void describes_itself_by_identity_and_stage() {
        Deal deal = Examples.dealWorth("1000", ownerId);

        assertThat(deal.toString()).contains(deal.id().value().toString()).contains("LEAD");
    }

    private Deal closedDealAt(DealStage closedStage) {
        Deal deal = Examples.dealAt(DealStage.NEGOTIATION, "1000", ownerId);
        deal.changeStageTo(closedStage, owner, Examples.engagementFor(deal.id(), ownerId));
        return deal;
    }
}
