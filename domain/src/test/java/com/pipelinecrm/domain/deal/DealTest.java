package com.pipelinecrm.domain.deal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityBody;
import com.pipelinecrm.domain.activity.ActivityTarget;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.identity.UserRole;
import com.pipelinecrm.domain.user.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DealTest {

    private final UserId ownerId = UserId.generate();
    private final User owner = user(ownerId, UserRole.SALES);
    private final User otherSales = user(UserId.generate(), UserRole.SALES);
    private final User manager = user(UserId.generate(), UserRole.MANAGER);

    @Test
    void newDealStartsAsLead() {
        Deal deal = openDeal();
        assertThat(deal.stage()).isEqualTo(DealStage.LEAD);
        assertThat(deal.isOpen()).isTrue();
    }

    @Test
    void ownerCanWalkTheHappyPath() {
        Deal deal = openDeal();
        deal.changeStage(owner, DealStage.QUALIFIED, List.of());
        deal.changeStage(owner, DealStage.PROPOSAL, List.of());
        deal.changeStage(owner, DealStage.NEGOTIATION, List.of());
        assertThat(deal.stage()).isEqualTo(DealStage.NEGOTIATION);
    }

    @Test
    void skippingAHappyPathStageIsRejected() {
        Deal deal = openDeal();
        assertThatThrownBy(() -> deal.changeStage(owner, DealStage.PROPOSAL, List.of()))
                .isInstanceOf(IllegalDealStageException.class);
    }

    @Test
    void closedWonRequiresPositiveValueAndQualifyingActivity() {
        Deal deal = openDeal();
        deal.changeStage(owner, DealStage.CLOSED_WON, List.of(meetingOn(deal.id())));
        assertThat(deal.stage()).isEqualTo(DealStage.CLOSED_WON);
        assertThat(deal.probability().percent()).isEqualTo(100);
        assertThat(deal.isOpen()).isFalse();
    }

    @Test
    void callAlsoQualifiesClosedWon() {
        Deal deal = openDeal();
        deal.changeStage(owner, DealStage.CLOSED_WON, List.of(callOn(deal.id())));
        assertThat(deal.stage()).isEqualTo(DealStage.CLOSED_WON);
    }

    @Test
    void noteDoesNotQualifyClosedWon() {
        Deal deal = openDeal();
        assertThatThrownBy(() -> deal.changeStage(owner, DealStage.CLOSED_WON, List.of(noteOn(deal.id()))))
                .isInstanceOf(DealNotWinnableException.class)
                .hasMessageContaining("call or meeting");
    }

    @Test
    void activityOnAnotherDealDoesNotQualify() {
        Deal deal = openDeal();
        assertThatThrownBy(() -> deal.changeStage(owner, DealStage.CLOSED_WON, List.of(meetingOn(DealId.generate()))))
                .isInstanceOf(DealNotWinnableException.class);
    }

    @Test
    void contactActivityDoesNotQualify() {
        Deal deal = openDeal();
        Activity contactMeeting = Activity.record(
                ActivityId.generate(),
                ActivityType.MEETING,
                ActivityBody.of("kickoff"),
                ActivityTarget.contact(ContactId.generate()),
                ownerId,
                Instant.parse("2026-01-01T10:00:00Z"));
        assertThatThrownBy(() -> deal.changeStage(owner, DealStage.CLOSED_WON, List.of(contactMeeting)))
                .isInstanceOf(DealNotWinnableException.class);
    }

    @Test
    void zeroValueCannotBeWon() {
        Deal deal = Deal.open(
                DealId.generate(),
                CompanyId.generate(),
                ownerId,
                DealTitle.of("Zero"),
                Money.of("0.00", "USD"),
                Probability.of(10));
        assertThatThrownBy(() -> deal.changeStage(owner, DealStage.CLOSED_WON, List.of(meetingOn(deal.id()))))
                .isInstanceOf(DealNotWinnableException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void closedLostForcesProbabilityToZero() {
        Deal deal = openDeal();
        deal.changeStage(owner, DealStage.CLOSED_LOST, List.of());
        assertThat(deal.stage()).isEqualTo(DealStage.CLOSED_LOST);
        assertThat(deal.probability().percent()).isZero();
    }

    @Test
    void managerMayChangeSomeoneElsesStage() {
        Deal deal = openDeal();
        deal.changeStage(manager, DealStage.QUALIFIED, List.of());
        assertThat(deal.stage()).isEqualTo(DealStage.QUALIFIED);
    }

    @Test
    void otherSalesCannotChangeStage() {
        Deal deal = openDeal();
        assertThatThrownBy(() -> deal.changeStage(otherSales, DealStage.QUALIFIED, List.of()))
                .isInstanceOf(DealStageNotAuthorizedException.class);
    }

    @Test
    void terminalDealCannotMoveAgain() {
        Deal deal = openDeal();
        deal.changeStage(owner, DealStage.CLOSED_LOST, List.of());
        assertThatThrownBy(() -> deal.changeStage(owner, DealStage.LEAD, List.of()))
                .isInstanceOf(IllegalDealStageException.class);
        assertThatThrownBy(() -> deal.changeProbability(Probability.of(10)))
                .isInstanceOf(IllegalDealStageException.class);
    }

    @Test
    void renameAndRevalueUpdateFields() {
        Deal deal = openDeal();
        deal.rename(DealTitle.of("Renamed"));
        deal.revalue(Money.of("99.50", "EUR"));
        deal.changeProbability(Probability.of(40));
        assertThat(deal.title().value()).isEqualTo("Renamed");
        assertThat(deal.value().amount()).hasToString("99.50");
        assertThat(deal.probability().percent()).isEqualTo(40);
    }

    @Test
    void restoreRebuildsExistingState() {
        DealId id = DealId.generate();
        CompanyId companyId = CompanyId.generate();
        Deal deal = Deal.restore(
                id,
                companyId,
                ownerId,
                DealTitle.of("Restored"),
                Money.of("5.00", "USD"),
                Probability.of(20),
                DealStage.PROPOSAL);
        assertThat(deal.id()).isEqualTo(id);
        assertThat(deal.companyId()).isEqualTo(companyId);
        assertThat(deal.stage()).isEqualTo(DealStage.PROPOSAL);
    }

    @Test
    void rejectsNullCollaborators() {
        Deal deal = openDeal();
        assertThatThrownBy(() -> deal.changeStage(null, DealStage.QUALIFIED, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> deal.changeStage(owner, null, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> deal.changeStage(owner, DealStage.QUALIFIED, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Deal openDeal() {
        return Deal.open(
                DealId.generate(),
                CompanyId.generate(),
                ownerId,
                DealTitle.of("Acme expansion"),
                Money.of("1000.00", "USD"),
                Probability.of(25));
    }

    private User user(UserId id, UserRole role) {
        return User.register(id, Email.of("user-" + id + "@example.com"), PersonName.of("Pat Sales"), role, "hash");
    }

    private Activity meetingOn(DealId dealId) {
        return Activity.record(
                ActivityId.generate(),
                ActivityType.MEETING,
                ActivityBody.of("discovery"),
                ActivityTarget.deal(dealId),
                ownerId,
                Instant.parse("2026-01-02T09:00:00Z"));
    }

    private Activity callOn(DealId dealId) {
        return Activity.record(
                ActivityId.generate(),
                ActivityType.CALL,
                ActivityBody.of("follow-up"),
                ActivityTarget.deal(dealId),
                ownerId,
                Instant.parse("2026-01-02T09:00:00Z"));
    }

    private Activity noteOn(DealId dealId) {
        return Activity.record(
                ActivityId.generate(),
                ActivityType.NOTE,
                ActivityBody.of("internal"),
                ActivityTarget.deal(dealId),
                ownerId,
                Instant.parse("2026-01-02T09:00:00Z"));
    }
}
