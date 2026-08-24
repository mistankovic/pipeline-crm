package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guards;
import com.pipelinecrm.domain.user.User;
import java.util.Collection;

public final class Deal {

    private final DealId id;
    private final CompanyId companyId;
    private final UserId ownerId;
    private DealTitle title;
    private Money value;
    private Probability probability;
    private DealStage stage;

    private Deal(
            DealId id,
            CompanyId companyId,
            UserId ownerId,
            DealTitle title,
            Money value,
            Probability probability,
            DealStage stage) {
        this.id = Guards.notNull(id, "id");
        this.companyId = Guards.notNull(companyId, "companyId");
        this.ownerId = Guards.notNull(ownerId, "ownerId");
        this.title = Guards.notNull(title, "title");
        this.value = Guards.notNull(value, "value");
        this.probability = Guards.notNull(probability, "probability");
        this.stage = Guards.notNull(stage, "stage");
    }

    public static Deal open(
            DealId id, CompanyId companyId, UserId ownerId, DealTitle title, Money value, Probability probability) {
        return new Deal(id, companyId, ownerId, title, value, probability, DealStage.LEAD);
    }

    public static Deal restore(
            DealId id,
            CompanyId companyId,
            UserId ownerId,
            DealTitle title,
            Money value,
            Probability probability,
            DealStage stage) {
        return new Deal(id, companyId, ownerId, title, value, probability, stage);
    }

    public DealId id() {
        return id;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public UserId ownerId() {
        return ownerId;
    }

    public DealTitle title() {
        return title;
    }

    public Money value() {
        return value;
    }

    public Probability probability() {
        return probability;
    }

    public DealStage stage() {
        return stage;
    }

    public boolean isOpen() {
        return stage.isOpen();
    }

    public void rename(DealTitle newTitle) {
        this.title = Guards.notNull(newTitle, "newTitle");
    }

    public void revalue(Money newValue) {
        this.value = Guards.notNull(newValue, "newValue");
    }

    public void changeProbability(Probability newProbability) {
        Guards.notNull(newProbability, "newProbability");
        if (stage.isTerminal()) {
            throw new IllegalDealStageException("probability is locked on a closed deal");
        }
        this.probability = newProbability;
    }

    public void changeStage(User actor, DealStage target, Collection<Activity> dealActivities) {
        Guards.notNull(actor, "actor");
        Guards.notNull(target, "target");
        Guards.notNull(dealActivities, "dealActivities");
        assertAuthorized(actor);
        assertTransitionAllowed(target);
        if (target == DealStage.CLOSED_WON) {
            assertWinnable(dealActivities);
            this.probability = Probability.closedWon();
        } else if (target == DealStage.CLOSED_LOST) {
            this.probability = Probability.closedLost();
        }
        this.stage = target;
    }

    private void assertAuthorized(User actor) {
        if (actor.id().equals(ownerId) || actor.isManager()) {
            return;
        }
        throw new DealStageNotAuthorizedException();
    }

    private void assertTransitionAllowed(DealStage target) {
        if (!stage.canTransitionTo(target)) {
            throw new IllegalDealStageException("cannot move from " + stage + " to " + target);
        }
    }

    private void assertWinnable(Collection<Activity> dealActivities) {
        if (!value.isPositive()) {
            throw new DealNotWinnableException("closed-won requires a positive deal value");
        }
        if (!hasQualifyingActivity(dealActivities)) {
            throw new DealNotWinnableException("closed-won requires a call or meeting on the deal");
        }
    }

    private boolean hasQualifyingActivity(Collection<Activity> dealActivities) {
        for (Activity activity : dealActivities) {
            if (activity.qualifiesDealWin(id)) {
                return true;
            }
        }
        return false;
    }
}
