package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.ChangeDealStageUseCase;
import com.pipelinecrm.application.port.in.ForecastUseCase;
import com.pipelinecrm.application.port.in.RecordActivityUseCase;
import com.pipelinecrm.application.port.in.UpdateDealUseCase;
import com.pipelinecrm.application.support.InMemoryActivityRepository;
import com.pipelinecrm.application.support.InMemoryCompanyRepository;
import com.pipelinecrm.application.support.InMemoryDealRepository;
import com.pipelinecrm.application.support.InMemoryUserRepository;
import com.pipelinecrm.application.usecase.ChangeDealStageService;
import com.pipelinecrm.application.usecase.ForecastService;
import com.pipelinecrm.application.usecase.RecordActivityService;
import com.pipelinecrm.application.usecase.UpdateDealService;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.company.CompanyName;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.DealTitle;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.forecast.ForecastBucket;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.identity.UserRole;
import com.pipelinecrm.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AcceptanceWorld {

    final InMemoryUserRepository users = new InMemoryUserRepository();
    final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    final InMemoryDealRepository deals = new InMemoryDealRepository();
    final InMemoryActivityRepository activities = new InMemoryActivityRepository();
    final ChangeDealStageUseCase changeStage = new ChangeDealStageService(deals, users);
    final RecordActivityUseCase recordActivity =
            new RecordActivityService(activities, deals, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    final ForecastUseCase forecast = new ForecastService(deals);
    final UpdateDealUseCase updateDeal = new UpdateDealService(deals);
    final Map<String, User> namedUsers = new HashMap<>();
    final CompanyId companyId = CompanyId.generate();
    DealId currentDealId;
    Exception lastError;
    List<ForecastBucket> lastForecast = List.of();

    AcceptanceWorld() {
        companies.save(Company.create(companyId, CompanyName.of("Acme")));
    }

    User user(String name) {
        return namedUsers.computeIfAbsent(name, this::createUser);
    }

    Deal deal() {
        return deals.findById(currentDealId).orElseThrow();
    }

    void catchError(Runnable action) {
        lastError = null;
        try {
            action.run();
        } catch (Exception ex) {
            lastError = ex;
        }
    }

    void seedOwnedDeal(String ownerName, String title, DealStage stage, String amount, String currency, int probability) {
        User owner = user(ownerName);
        Deal deal = Deal.open(
                DealId.generate(),
                companyId,
                owner.id(),
                DealTitle.of(title),
                Money.of(amount, currency),
                Probability.of(probability));
        deals.save(deal);
        currentDealId = deal.id();
        moveTo(stage, owner);
    }

    void moveTo(DealStage target, User actor) {
        Deal deal = deal();
        while (deal.stage() != target && deal.stage().canTransitionTo(nextToward(deal.stage(), target))) {
            DealStage next = nextToward(deal.stage(), target);
            if (next == DealStage.CLOSED_WON && deal.activities().isEmpty() && deal.value().isPositive()) {
                recordActivity.execute(new RecordActivityUseCase.Command(
                        actor.id(), ActivityType.MEETING, "qualifying", deal.id(), null));
            }
            changeStage.execute(new ChangeDealStageUseCase.Command(actor.id(), deal.id(), next));
            deal = deal();
        }
        if (deal.stage() != target) {
            changeStage.execute(new ChangeDealStageUseCase.Command(actor.id(), deal.id(), target));
        }
    }

    private DealStage nextToward(DealStage from, DealStage target) {
        if (target.isTerminal() || from.canTransitionTo(target)) {
            return target;
        }
        return switch (from) {
            case LEAD -> DealStage.QUALIFIED;
            case QUALIFIED -> DealStage.PROPOSAL;
            case PROPOSAL -> DealStage.NEGOTIATION;
            default -> target;
        };
    }

    private User createUser(String name) {
        UserRole role = "boss".equals(name) ? UserRole.MANAGER : UserRole.SALES;
        User user = User.register(
                UserId.generate(),
                Email.of(name + "@example.com"),
                PersonName.of(name),
                role,
                "hash");
        users.save(user);
        return user;
    }

    Contact extraContact() {
        Contact contact = Contact.create(ContactId.generate(), companyId, PersonName.of("Pat"), Email.of("pat@acme.com"));
        return contact;
    }
}
