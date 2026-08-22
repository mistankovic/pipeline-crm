package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;
import com.pipelinecrm.domain.user.User;

import java.util.Arrays;
import java.util.List;

/**
 * A sales opportunity. This is where the pipeline rules live: who may move a deal, where
 * it may move to, what a win requires, and what a close does to the probability.
 */
public final class Deal {

    private final DealId id;
    private final String title;
    private final DealParties parties;
    private DealTerms terms;
    private DealStage stage;

    private Deal(DealSnapshot state) {
        this.id = state.id();
        this.title = state.title();
        this.parties = state.parties();
        this.terms = state.terms();
        this.stage = state.stage();
    }

    /** A brand new opportunity, which always starts as a lead. */
    public static Deal open(DealId id, String title, DealParties parties, DealTerms terms) {
        return new Deal(new DealSnapshot(id, title, parties, terms, DealStage.LEAD));
    }

    /** Rebuild a deal that already exists. Used only by persistence adapters. */
    public static Deal from(DealSnapshot state) {
        return new Deal(Guard.present(state, "deal state"));
    }

    public DealSnapshot snapshot() {
        return new DealSnapshot(id, title, parties, terms, stage);
    }

    public void changeStageTo(DealStage target, User actor, DealActivities history) {
        requireAuthority(actor);
        requireHistoryOfThisDeal(history);
        requireTransitionIsLegal(target);
        requireWinIsEarned(target, history);
        stage = target;
        terms = terms.weightedAt(target.forcedProbability().orElse(terms.probability()));
    }

    public void reprice(Money newValue, User actor) {
        requireAuthority(actor);
        requireStillOpen();
        terms = terms.pricedAt(newValue);
    }

    public void reweight(Probability newProbability, User actor) {
        requireAuthority(actor);
        requireStillOpen();
        terms = terms.weightedAt(newProbability);
    }

    public Money weightedValue() {
        return terms.weighted();
    }

    public boolean isOpen() {
        return stage.isOpen();
    }

    public DealId id() {
        return id;
    }

    public DealStage stage() {
        return stage;
    }

    public String title() {
        return title;
    }

    public DealParties parties() {
        return parties;
    }

    public Money value() {
        return terms.value();
    }

    public Probability probability() {
        return terms.probability();
    }

    /** The stages this deal could legally move to right now, for anybody entitled to move it. */
    public List<DealStage> allowedTransitions() {
        return Arrays.stream(DealStage.values()).filter(stage::allowsTransitionTo).toList();
    }

    /**
     * The stages <em>this user</em> may move it to right now — which is nothing at all if they
     * are neither the owner nor a manager.
     *
     * <p>The two questions are different, and confusing them showed on screen: a board built
     * from {@link #allowedTransitions()} invited a rival salesperson to drag a card the server
     * would then refuse. See docs/reviews/stage-6-review.md, finding F-6.2.
     */
    public List<DealStage> transitionsAllowedFor(User actor) {
        Guard.present(actor, "user asking what they may do");
        return mayBeChangedBy(actor) ? allowedTransitions() : List.of();
    }

    /** Whether this user may change anything about this deal at all. */
    public boolean mayBeChangedBy(User actor) {
        Guard.present(actor, "user asking what they may do");
        return actor.isManager() || parties.ownedBy(actor.id());
    }

    /**
     * A deal belongs to its owner. Nobody but the owner or a manager may change anything
     * about it: setting a rival's deal to zero value removes it from the forecast just as
     * effectively as marking it lost.
     */
    private void requireAuthority(User actor) {
        Guard.present(actor, "user changing the deal");
        if (!mayBeChangedBy(actor)) {
            throw new StageChangeForbidden(actor.id());
        }
    }

    private void requireHistoryOfThisDeal(DealActivities history) {
        Guard.present(history, "activity history");
        if (!history.belongTo(id)) {
            throw new InapplicableActivityHistory(id);
        }
    }

    private void requireTransitionIsLegal(DealStage target) {
        if (!stage.allowsTransitionTo(target)) {
            throw new IllegalStageTransition(stage, target);
        }
    }

    private void requireWinIsEarned(DealStage target, DealActivities history) {
        if (target != DealStage.CLOSED_WON) {
            return;
        }
        if (!terms.value().isPositive()) {
            throw new WinRequiresValueAndEngagement("its value is " + terms.value().amount().toPlainString());
        }
        if (!history.includeEngagement()) {
            throw new WinRequiresValueAndEngagement("no call or meeting has been logged against it");
        }
    }

    private void requireStillOpen() {
        if (stage.isClosed()) {
            throw new ClosedDealIsImmutable(stage);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Deal deal && id.equals(deal.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Deal[" + id.value() + " " + stage + "]";
    }
}
