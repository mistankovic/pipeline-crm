package com.pipelinecrm.domain.testing;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityAuthorship;
import com.pipelinecrm.domain.activity.ActivitySubject;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.activity.ContactSubject;
import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.activity.DealSubject;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealParties;
import com.pipelinecrm.domain.deal.DealTerms;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;
import com.pipelinecrm.domain.user.User;
import com.pipelinecrm.domain.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Named example objects for tests. Everything a test does not care about gets a value
 * here, so that each test shows only the values that matter to the behaviour it pins down.
 */
public final class Examples {

    public static final Instant SOME_TIME = Instant.parse("2026-03-01T09:00:00Z");

    private Examples() {
    }

    public static UserId aUserId() {
        return UserId.of(UUID.randomUUID());
    }

    public static User salesperson(UserId id) {
        return new User(id, EmailAddress.of("sam@example.com"), "Sam Sales", UserRole.SALES);
    }

    public static User manager() {
        return new User(aUserId(), EmailAddress.of("mo@example.com"), "Mo Manager", UserRole.MANAGER);
    }

    public static DealParties partiesOwnedBy(UserId owner) {
        return new DealParties(CompanyId.of(UUID.randomUUID()), owner);
    }

    public static Deal dealWorth(String amount, UserId owner) {
        return Deal.open(DealId.of(UUID.randomUUID()), "Acme renewal", partiesOwnedBy(owner),
                new DealTerms(Money.of(amount, "EUR"), Probability.of(50)));
    }

    public static Deal dealAt(DealStage stage, String amount, UserId owner) {
        Deal deal = dealWorth(amount, owner);
        User mover = salesperson(owner);
        DealActivities history = engagementFor(deal.id(), owner);
        while (deal.stage() != stage) {
            deal.changeStageTo(nextTowards(stage, deal.stage()), mover, history);
        }
        return deal;
    }

    private static DealStage nextTowards(DealStage target, DealStage current) {
        return target == DealStage.CLOSED_LOST ? DealStage.CLOSED_LOST : forwardFrom(current);
    }

    private static DealStage forwardFrom(DealStage current) {
        return DealStage.values()[current.ordinal() + 1];
    }

    public static Activity activity(ActivityType type, ActivitySubject subject, UserId author) {
        return new Activity(ActivityId.of(UUID.randomUUID()), subject, type, "summary",
                new ActivityAuthorship(author, SOME_TIME));
    }

    public static DealActivities engagementFor(DealId deal, UserId author) {
        return new DealActivities(deal,
                List.of(activity(ActivityType.MEETING, new DealSubject(deal), author)));
    }

    public static DealActivities notesOnlyFor(DealId deal, UserId author) {
        return new DealActivities(deal,
                List.of(activity(ActivityType.NOTE, new DealSubject(deal), author)));
    }

    public static ContactSubject aContactSubject() {
        return new ContactSubject(ContactId.of(UUID.randomUUID()));
    }
}
