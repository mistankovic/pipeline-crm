package com.pipelinecrm.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityBody;
import com.pipelinecrm.domain.activity.ActivityTarget;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.company.CompanyName;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.DealTitle;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
class JpaRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository users;

    @Autowired
    private CompanyRepository companies;

    @Autowired
    private ContactRepository contacts;

    @Autowired
    private DealRepository deals;

    @Autowired
    private ActivityRepository activities;

    @Test
    void roundTripsUsersCompaniesAndContacts() {
        UserId userId = UserId.generate();
        users.save(User.register(userId, Email.of("ada@example.com"), PersonName.of("Ada"), UserRole.SALES, "hash"));
        assertThat(users.findByEmail(Email.of("ada@example.com")).orElseThrow().name().value()).isEqualTo("Ada");
        assertThat(users.findById(userId).orElseThrow().role()).isEqualTo(UserRole.SALES);

        CompanyId companyId = CompanyId.generate();
        companies.save(Company.create(companyId, CompanyName.of("Acme")));
        companies.save(Company.create(companyId, CompanyName.of("Acme Inc")));
        assertThat(companies.findAll()).extracting(company -> company.name().value()).containsExactly("Acme Inc");

        ContactId contactId = ContactId.generate();
        contacts.save(Contact.create(contactId, companyId, PersonName.of("Pat"), Email.of("pat@acme.com")));
        Contact loaded = contacts.findById(contactId).orElseThrow();
        assertThat(loaded.email().value()).isEqualTo("pat@acme.com");
        contacts.save(Contact.create(contactId, companyId, PersonName.of("Pat Lee"), null));
        assertThat(contacts.findAll().getFirst().email()).isNull();
        assertThat(users.findById(UserId.generate())).isEmpty();
    }

    @Test
    void roundTripsDealWithQualifyingActivityAndContactNote() {
        UserId ownerId = UserId.generate();
        users.save(User.register(
                ownerId, Email.of("owner@example.com"), PersonName.of("Owner"), UserRole.MANAGER, "hash"));
        CompanyId companyId = CompanyId.generate();
        companies.save(Company.create(companyId, CompanyName.of("Globex")));
        ContactId contactId = ContactId.generate();
        contacts.save(Contact.create(contactId, companyId, PersonName.of("Kim"), null));

        DealId dealId = DealId.generate();
        Deal deal = Deal.open(
                dealId, companyId, ownerId, DealTitle.of("Expansion"), Money.of("1000.00", "USD"), Probability.of(40));
        Activity meeting = Activity.record(
                ActivityId.generate(),
                ActivityType.MEETING,
                ActivityBody.of("Kickoff"),
                ActivityTarget.deal(dealId),
                ownerId,
                Instant.parse("2026-01-01T10:00:00Z"));
        deal.recordActivity(meeting);
        deals.save(deal);

        Activity note = Activity.record(
                ActivityId.generate(),
                ActivityType.NOTE,
                ActivityBody.of("Follow up"),
                ActivityTarget.contact(contactId),
                ownerId,
                Instant.parse("2026-01-01T11:00:00Z"));
        activities.save(note);

        Deal reloaded = deals.findById(dealId).orElseThrow();
        assertThat(reloaded.title().value()).isEqualTo("Expansion");
        assertThat(reloaded.activities()).hasSize(1);
        assertThat(reloaded.activities().getFirst().type()).isEqualTo(ActivityType.MEETING);
        assertThat(activities.findByDeal(dealId)).hasSize(1);
        assertThat(activities.findByContact(contactId)).hasSize(1);
        assertThat(deals.findAll()).hasSize(1);

        reloaded.changeStage(users.findById(ownerId).orElseThrow(), DealStage.CLOSED_WON);
        deals.save(reloaded);
        Deal closed = deals.findById(dealId).orElseThrow();
        assertThat(closed.stage()).isEqualTo(DealStage.CLOSED_WON);
        assertThat(closed.probability().percent()).isEqualTo(100);
    }
}
