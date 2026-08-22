package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityAuthorship;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.activity.ContactSubject;
import com.pipelinecrm.domain.activity.DealSubject;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealParties;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.DealTerms;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What goes into the database is what comes out.
 *
 * <p>Round trips are asserted on the whole domain object, not on a field or two, because the
 * defect a mapper produces is almost always the field nobody thought to check.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
class RepositoryRoundTripTest extends PostgresBackedTest {

    private static final UserId SAM = UserId.of(UUID.fromString("11111111-1111-4111-8111-111111111111"));
    private static final UserId MO = UserId.of(UUID.fromString("33333333-3333-4333-8333-333333333333"));

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

    private Company aCompany() {
        Company company = new Company(CompanyId.of(UUID.randomUUID()), "Acme " + UUID.randomUUID());
        companies.save(company);
        return company;
    }

    private Deal aDealAt(DealStage stage, Company company) {
        Deal deal = Deal.open(DealId.of(UUID.randomUUID()), "Renewal",
                new DealParties(company.id(), SAM),
                new DealTerms(Money.of("12345.67", "EUR"), Probability.of(35)));
        walkTo(stage, deal);
        deals.save(deal);
        return deal;
    }

    private void walkTo(DealStage stage, Deal deal) {
        User sam = users.findById(SAM).orElseThrow();
        List<DealStage> forward = List.of(DealStage.QUALIFIED, DealStage.PROPOSAL, DealStage.NEGOTIATION);
        forward.subList(0, forward.indexOf(stage) + 1)
                .forEach(step -> deal.changeStageTo(step, sam, activities.findByDeal(deal.id())));
    }

    @Test
    void a_seeded_user_is_read_back_as_a_domain_user() {
        User sam = users.findById(SAM).orElseThrow();

        assertThat(sam.email()).isEqualTo(EmailAddress.of("sam@pipelinecrm.demo"));
        assertThat(sam.name()).isEqualTo("Sam Sales");
        assertThat(sam.role()).isEqualTo(UserRole.SALES);
    }

    @Test
    void a_seeded_manager_keeps_their_role() {
        assertThat(users.findById(MO).orElseThrow().isManager()).isTrue();
    }

    @Test
    void a_user_is_found_by_email() {
        assertThat(users.findByEmail(EmailAddress.of("robin@pipelinecrm.demo"))).isPresent();
    }

    @Test
    void an_unknown_email_finds_nobody() {
        assertThat(users.findByEmail(EmailAddress.of("nobody@pipelinecrm.demo"))).isEmpty();
    }

    @Test
    void only_the_requested_users_are_returned() {
        assertThat(users.findAllByIds(List.of(SAM))).extracting(User::id).containsExactly(SAM);
    }

    @Test
    void a_company_round_trips() {
        Company saved = aCompany();

        assertThat(companies.findById(saved.id())).contains(saved);
    }

    @Test
    void a_contact_round_trips() {
        Company company = aCompany();
        Contact contact = new Contact(ContactId.of(UUID.randomUUID()), company.id(),
                "Cara Client", EmailAddress.of("cara@acme.test"));
        contacts.save(contact);

        assertThat(contacts.findById(contact.id())).contains(contact);
    }

    @Test
    void contacts_are_found_by_company() {
        Company company = aCompany();
        Contact contact = new Contact(ContactId.of(UUID.randomUUID()), company.id(),
                "Cara", EmailAddress.of("cara2@acme.test"));
        contacts.save(contact);

        assertThat(contacts.findByCompany(company.id())).containsExactly(contact);
    }

    @Test
    void a_deal_round_trips_with_every_field_intact() {
        Deal saved = aDealAt(DealStage.PROPOSAL, aCompany());

        assertThat(deals.findById(saved.id()).orElseThrow().snapshot()).isEqualTo(saved.snapshot());
    }

    @Test
    void a_deals_money_keeps_its_scale_and_currency() {
        Deal saved = aDealAt(DealStage.LEAD, aCompany());

        Deal reloaded = deals.findById(saved.id()).orElseThrow();

        assertThat(reloaded.value()).isEqualTo(Money.of("12345.67", "EUR"));
    }

    @Test
    void a_deal_is_found_by_its_owner() {
        Deal saved = aDealAt(DealStage.LEAD, aCompany());

        assertThat(deals.findOwnedBy(SAM)).extracting(Deal::id).contains(saved.id());
    }

    @Test
    void a_deal_owned_by_somebody_else_is_not() {
        aDealAt(DealStage.LEAD, aCompany());

        assertThat(deals.findOwnedBy(MO)).extracting(Deal::id).isEmpty();
    }

    @Test
    void saving_a_changed_deal_overwrites_the_stored_one() {
        Deal deal = aDealAt(DealStage.LEAD, aCompany());
        deal.changeStageTo(DealStage.QUALIFIED, users.findById(SAM).orElseThrow(),
                activities.findByDeal(deal.id()));

        deals.save(deal);

        assertThat(deals.findById(deal.id()).orElseThrow().stage()).isEqualTo(DealStage.QUALIFIED);
    }

    @Test
    void an_activity_about_a_deal_round_trips() {
        Deal deal = aDealAt(DealStage.LEAD, aCompany());
        Activity activity = new Activity(ActivityId.of(UUID.randomUUID()), new DealSubject(deal.id()),
                ActivityType.CALL, "talked pricing", authorship());
        activities.save(activity);

        assertThat(activities.findByDeal(deal.id()).activities()).containsExactly(activity);
    }

    @Test
    void an_activity_about_a_contact_round_trips() {
        Company company = aCompany();
        Contact contact = new Contact(ContactId.of(UUID.randomUUID()), company.id(),
                "Cara", EmailAddress.of("cara3@acme.test"));
        contacts.save(contact);
        Activity activity = new Activity(ActivityId.of(UUID.randomUUID()), new ContactSubject(contact.id()),
                ActivityType.NOTE, "prefers email", authorship());
        activities.save(activity);

        assertThat(activities.findByContact(contact.id())).containsExactly(activity);
    }

    @Test
    void a_deals_history_is_returned_carrying_that_deals_identity() {
        Deal deal = aDealAt(DealStage.LEAD, aCompany());

        assertThat(activities.findByDeal(deal.id()).belongTo(deal.id())).isTrue();
    }

    @Test
    void a_deal_with_no_history_gets_an_empty_history_rather_than_nothing() {
        Deal deal = aDealAt(DealStage.LEAD, aCompany());

        assertThat(activities.findByDeal(deal.id()).activities()).isEmpty();
    }

    @Test
    void a_timeline_comes_back_oldest_first() {
        Deal deal = aDealAt(DealStage.LEAD, aCompany());
        Instant early = Instant.parse("2026-01-01T09:00:00Z");
        activities.save(activityAt(deal, "second", early.plus(1, ChronoUnit.HOURS)));
        activities.save(activityAt(deal, "first", early));

        assertThat(activities.findByDeal(deal.id()).activities())
                .extracting(Activity::summary).containsExactly("first", "second");
    }

    private Activity activityAt(Deal deal, String summary, Instant when) {
        return new Activity(ActivityId.of(UUID.randomUUID()), new DealSubject(deal.id()),
                ActivityType.NOTE, summary, new ActivityAuthorship(SAM, when));
    }

    private ActivityAuthorship authorship() {
        return new ActivityAuthorship(SAM, Instant.parse("2026-03-01T09:00:00Z"));
    }
}
