package com.pipelinecrm.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pipelinecrm.application.port.in.ChangeDealStageUseCase;
import com.pipelinecrm.application.port.in.CreateCompanyUseCase;
import com.pipelinecrm.application.port.in.CreateContactUseCase;
import com.pipelinecrm.application.port.in.CreateDealUseCase;
import com.pipelinecrm.application.port.in.ListDealsUseCase;
import com.pipelinecrm.application.port.in.LoginUseCase;
import com.pipelinecrm.application.port.in.RecordActivityUseCase;
import com.pipelinecrm.application.port.in.UpdateCompanyUseCase;
import com.pipelinecrm.application.port.in.UpdateContactUseCase;
import com.pipelinecrm.application.port.in.UpdateDealUseCase;
import com.pipelinecrm.application.port.out.PasswordHasher;
import com.pipelinecrm.application.port.out.TokenIssuer;
import com.pipelinecrm.application.support.InMemoryActivityRepository;
import com.pipelinecrm.application.support.InMemoryCompanyRepository;
import com.pipelinecrm.application.support.InMemoryContactRepository;
import com.pipelinecrm.application.support.InMemoryDealRepository;
import com.pipelinecrm.application.support.InMemoryUserRepository;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.identity.UserRole;
import com.pipelinecrm.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UseCaseTest {

    private final InMemoryDealRepository deals = new InMemoryDealRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    private final InMemoryContactRepository contacts = new InMemoryContactRepository();
    private final InMemoryActivityRepository activities = new InMemoryActivityRepository();
    private final CompanyServices companyServices = new CompanyServices(companies);
    private final ContactServices contactServices = new ContactServices(contacts, companies);
    private final CreateDealService createDeal = new CreateDealService(deals, companies, users);
    private final UpdateDealService updateDeal = new UpdateDealService(deals);
    private final ChangeDealStageService changeStage = new ChangeDealStageService(deals, users);
    private final RecordActivityService recordActivity = new RecordActivityService(
            activities, deals, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    private final ListDealsService listDeals = new ListDealsService(deals);
    private final ViewDealService viewDeal = new ViewDealService(deals);
    private final ForecastService forecast = new ForecastService(deals);
    private UserId ownerId;
    private CompanyId companyId;

    @BeforeEach
    void seed() {
        ownerId = UserId.generate();
        users.save(User.register(ownerId, Email.of("ada@example.com"), PersonName.of("Ada"), UserRole.SALES, "hashed"));
        companyId = companyServices.execute(new CreateCompanyUseCase.Command("Acme"));
    }

    @Test
    void createUpdateListCompaniesAndContacts() {
        int companySaves = companies.saveCount;
        companyServices.execute(new UpdateCompanyUseCase.Command(companyId, "Acme Inc"));
        assertThat(companies.saveCount).isGreaterThan(companySaves);
        assertThat(companyServices.execute().getFirst().name().value()).isEqualTo("Acme Inc");
        var contactId = contactServices.execute(new CreateContactUseCase.Command(companyId, "Pat", "p@a.com"));
        int contactSaves = contacts.saveCount;
        contactServices.execute(new UpdateContactUseCase.Command(contactId, "Pat Lee", null));
        assertThat(contacts.saveCount).isEqualTo(contactSaves + 1);
        assertThat(contactServices.execute()).hasSize(1);
        assertThat(contactServices.execute().getFirst().name().value()).isEqualTo("Pat Lee");
        assertThat(contactServices.execute().getFirst().email()).isNull();
    }

    @Test
    void dealLifecycleAndForecast() {
        var dealId = createDeal.execute(new CreateDealUseCase.Command(
                ownerId, companyId, ownerId, "Deal", Money.of("100.00", "USD"), Probability.of(50)));
        int activitySaves = deals.saveCount;
        var recorded = recordActivity.execute(
                new RecordActivityUseCase.Command(ownerId, ActivityType.MEETING, "hi", dealId, null));
        assertThat(recorded).isNotNull();
        assertThat(deals.saveCount).isGreaterThan(activitySaves);
        int stageSaves = deals.saveCount;
        changeStage.execute(new ChangeDealStageUseCase.Command(ownerId, dealId, DealStage.CLOSED_WON));
        assertThat(deals.saveCount).isGreaterThan(stageSaves);
        assertThat(viewDeal.execute(dealId).deal().stage()).isEqualTo(DealStage.CLOSED_WON);
        assertThat(listDeals.execute(new ListDealsUseCase.Filter(DealStage.CLOSED_WON, ownerId))).hasSize(1);
        assertThat(forecast.byOwner(Currency.getInstance("USD"))).isEmpty();
        assertThat(forecast.byStage(Currency.getInstance("USD"))).hasSize(4);
    }

    @Test
    void updateOpenDeal() {
        var dealId = createDeal.execute(new CreateDealUseCase.Command(
                ownerId, companyId, ownerId, "Deal", Money.of("100.00", "USD"), Probability.of(50)));
        int updateSaves = deals.saveCount;
        updateDeal.execute(new UpdateDealUseCase.Command(
                ownerId, dealId, "Renamed", Money.of("80.00", "USD"), Probability.of(20)));
        assertThat(deals.saveCount).isGreaterThan(updateSaves);
        assertThat(viewDeal.execute(dealId).deal().title().value()).isEqualTo("Renamed");
        assertThat(viewDeal.execute(dealId).deal().value().amount()).isEqualByComparingTo("80.00");
        assertThat(viewDeal.execute(dealId).deal().probability().percent()).isEqualTo(20);
    }

    @Test
    void loginIssuesToken() {
        LoginService login = new LoginService(users, new PasswordHasher() {
            @Override
            public String hash(String rawPassword) {
                return rawPassword;
            }

            @Override
            public boolean matches(String rawPassword, String passwordHash) {
                return rawPassword.equals("secret") && passwordHash.equals("hashed");
            }
        }, user -> "jwt-" + user.id());
        assertThat(login.execute(new LoginUseCase.Command("ada@example.com", "secret")).token()).startsWith("jwt-");
        assertThatThrownBy(() -> login.execute(new LoginUseCase.Command("ada@example.com", "nope")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingAggregatesThrow() {
        assertThatThrownBy(() -> viewDeal.execute(com.pipelinecrm.domain.identity.DealId.generate()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> createDeal.execute(new CreateDealUseCase.Command(
                        ownerId,
                        com.pipelinecrm.domain.identity.CompanyId.generate(),
                        ownerId,
                        "X",
                        Money.of("1.00", "USD"),
                        Probability.of(1))))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> changeStage.execute(new ChangeDealStageUseCase.Command(
                        UserId.generate(),
                        com.pipelinecrm.domain.identity.DealId.generate(),
                        DealStage.QUALIFIED)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> loginMissingUser()).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listDealsFiltersAndContactActivity() {
        var dealId = createDeal.execute(new CreateDealUseCase.Command(
                ownerId, companyId, ownerId, "Deal", Money.of("100.00", "USD"), Probability.of(50)));
        var contactId = contactServices.execute(new CreateContactUseCase.Command(companyId, "Pat", null));
        recordActivity.execute(new RecordActivityUseCase.Command(ownerId, ActivityType.NOTE, "n", null, contactId));
        assertThat(listDeals.execute(new ListDealsUseCase.Filter(null, null))).hasSize(1);
        assertThat(listDeals.execute(new ListDealsUseCase.Filter(DealStage.QUALIFIED, null))).isEmpty();
        assertThat(listDeals.execute(new ListDealsUseCase.Filter(null, UserId.generate()))).isEmpty();
        assertThat(listDeals.execute(new ListDealsUseCase.Filter(DealStage.LEAD, ownerId))).hasSize(1);
        assertThat(activities.findByDeal(dealId)).isEmpty();
        assertThat(activities.findByContact(contactId)).hasSize(1);
        assertThatThrownBy(() -> contactServices.execute(new CreateContactUseCase.Command(
                        com.pipelinecrm.domain.identity.CompanyId.generate(), "X", "x@y.com")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> contactServices.execute(new UpdateContactUseCase.Command(
                        com.pipelinecrm.domain.identity.ContactId.generate(), "X", "x@y.com")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> companyServices.execute(new UpdateCompanyUseCase.Command(
                        com.pipelinecrm.domain.identity.CompanyId.generate(), "X")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> updateDeal.execute(new UpdateDealUseCase.Command(
                        ownerId,
                        com.pipelinecrm.domain.identity.DealId.generate(),
                        "X",
                        Money.of("1.00", "USD"),
                        Probability.of(1))))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> recordActivity.execute(new RecordActivityUseCase.Command(
                        ownerId, ActivityType.CALL, "c", com.pipelinecrm.domain.identity.DealId.generate(), null)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> createDeal.execute(new CreateDealUseCase.Command(
                        ownerId,
                        companyId,
                        UserId.generate(),
                        "No owner",
                        Money.of("1.00", "USD"),
                        Probability.of(1))))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> changeStage.execute(new ChangeDealStageUseCase.Command(
                        ownerId, com.pipelinecrm.domain.identity.DealId.generate(), DealStage.QUALIFIED)))
                .isInstanceOf(NotFoundException.class);
    }

    private void loginMissingUser() {
        new LoginService(
                        users,
                        new PasswordHasher() {
                            @Override
                            public String hash(String rawPassword) {
                                return rawPassword;
                            }

                            @Override
                            public boolean matches(String rawPassword, String passwordHash) {
                                return false;
                            }
                        },
                        user -> "t")
                .execute(new LoginUseCase.Command("missing@example.com", "x"));
    }
}
