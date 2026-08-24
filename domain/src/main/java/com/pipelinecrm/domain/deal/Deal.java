package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guards;
import com.pipelinecrm.domain.user.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Deal {

    private final DealId id;
    private final CompanyId companyId;
    private final UserId ownerId;
    private DealTitle title;
    private Money value;
    private Probability probability;
    private DealStage stage;
    private final List<Activity> activities = new ArrayList<>();

    private Deal(
            DealId id,
            CompanyId companyId,
            UserId ownerId,
            DealTitle title,
            Money value,
            Probability probability,
            DealStage stage,
            Collection<Activity> activities) {
        this.id = Guards.notNull(id, "id");
        this.companyId = Guards.notNull(companyId, "companyId");
        this.ownerId = Guards.notNull(ownerId, "ownerId");
        this.title = Guards.notNull(title, "title");
        this.value = Guards.notNull(value, "value");
        this.probability = Guards.notNull(probability, "probability");
        this.stage = Guards.notNull(stage, "stage");
        recordAll(Guards.notNull(activities, "activities"));
        assertConsistent();
    }

    public static Deal open(
            DealId id, CompanyId companyId, UserId ownerId, DealTitle title, Money value, Probability probability) {
        return new Deal(id, companyId, ownerId, title, value, probability, DealStage.LEAD, List.of());
    }

    public static Deal restore(
            DealId id,
            CompanyId companyId,
            UserId ownerId,
            DealTitle title,
            Money value,
            Probability probability,
            DealStage stage,
            Collection<Activity> activities) {
        return new Deal(id, companyId, ownerId, title, value, probability, stage, activities);
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

    public List<Activity> activities() {
        return List.copyOf(activities);
    }

    public boolean isOpen() {
        return stage.isOpen();
    }

    public void rename(DealTitle newTitle) {
        this.title = Guards.notNull(newTitle, "newTitle");
    }

    public void revalue(Money newValue) {
        Guards.notNull(newValue, "newValue");
        assertNotTerminal("value");
        this.value = newValue;
    }

    public void changeProbability(Probability newProbability) {
        Guards.notNull(newProbability, "newProbability");
        assertNotTerminal("probability");
        this.probability = newProbability;
    }

    public void recordActivity(Activity activity) {
        Guards.notNull(activity, "activity");
        if (!activity.target().isDeal(id)) {
            throw new IllegalArgumentException("activity does not belong to this deal");
        }
        activities.add(activity);
    }

    public void changeStage(User actor, DealStage target) {
        Guards.notNull(actor, "actor");
        Guards.notNull(target, "target");
        assertAuthorized(actor);
        assertTransitionAllowed(target);
        applyCloseEffects(target);
        this.stage = target;
    }

    private void applyCloseEffects(DealStage target) {
        if (target == DealStage.CLOSED_WON) {
            assertWinnable();
            this.probability = Probability.closedWon();
        } else if (target == DealStage.CLOSED_LOST) {
            this.probability = Probability.closedLost();
        }
    }

    private void recordAll(Collection<Activity> incoming) {
        for (Activity activity : incoming) {
            recordActivity(activity);
        }
    }

    private void assertNotTerminal(String field) {
        if (stage.isTerminal()) {
            throw new IllegalDealStageException(field + " is locked on a closed deal");
        }
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

    private void assertConsistent() {
        if (stage == DealStage.CLOSED_WON) {
            assertWinnable();
            if (probability.percent() != 100) {
                throw new IllegalDealStageException("closed-won probability must be 100");
            }
        }
        if (stage == DealStage.CLOSED_LOST && probability.percent() != 0) {
            throw new IllegalDealStageException("closed-lost probability must be 0");
        }
    }

    private void assertWinnable() {
        if (!value.isPositive()) {
            throw new DealNotWinnableException("closed-won requires a positive deal value");
        }
        if (!hasQualifyingActivity()) {
            throw new DealNotWinnableException("closed-won requires a call or meeting on the deal");
        }
    }

    private boolean hasQualifyingActivity() {
        for (Activity activity : activities) {
            if (activity.qualifiesDealWin(id)) {
                return true;
            }
        }
        return false;
    }
}
