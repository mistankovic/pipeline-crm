package com.pipelinecrm.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityBody;
import com.pipelinecrm.domain.activity.ActivityTarget;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.company.CompanyName;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.deal.DealTitle;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.shared.Guards;
import com.pipelinecrm.domain.user.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    @Test
    void emailNormalizesAndRejectsInvalid() {
        assertThat(Email.of("  A@B.COM ").value()).isEqualTo("a@b.com");
        assertThatThrownBy(() -> Email.of("not-an-email")).isInstanceOf(InvalidEmailException.class);
        assertThatThrownBy(() -> Email.of(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void idsRoundTripThroughParse() {
        UserId userId = UserId.generate();
        assertThat(UserId.parse(userId.toString())).isEqualTo(userId);
        assertThat(CompanyId.parse(CompanyId.generate().toString())).isNotNull();
        assertThat(ContactId.parse(ContactId.generate().toString())).isNotNull();
        assertThat(DealId.parse(DealId.generate().toString())).isNotNull();
        assertThat(ActivityId.parse(ActivityId.generate().toString())).isNotNull();
    }

    @Test
    void namesAndTitlesRejectBlankAndTooLong() {
        assertThatThrownBy(() -> PersonName.of(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PersonName.of("x".repeat(PersonName.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CompanyName.of("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DealTitle.of("x".repeat(DealTitle.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActivityBody.of("x".repeat(ActivityBody.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PersonName.of("  Ada  ").value()).isEqualTo("Ada");
    }

    @Test
    void moneyRejectsNegativeAndHighScale() {
        assertThat(Money.of("10", "USD").isPositive()).isTrue();
        assertThat(Money.of(new BigDecimal("1.00"), java.util.Currency.getInstance("USD")).amount())
                .isEqualByComparingTo("1.00");
        assertThat(Money.of("0.00", "USD").isPositive()).isFalse();
        assertThatThrownBy(() -> Money.of("-1.00", "USD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(new BigDecimal("1.234"), java.util.Currency.getInstance("USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void probabilityBounds() {
        assertThat(Probability.of(0).percent()).isZero();
        assertThat(Probability.closedLost().percent()).isZero();
        assertThat(Probability.closedWon().percent()).isEqualTo(100);
        assertThat(Probability.of(100).fraction()).hasToString("1.0000");
        assertThatThrownBy(() -> Probability.of(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Probability.of(101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userCompanyContactMutators() {
        User user = User.register(
                UserId.generate(), Email.of("a@b.com"), PersonName.of("Ada"), UserRole.MANAGER, "hash");
        user.rename(PersonName.of("Ada Lovelace"));
        assertThat(user.isManager()).isTrue();
        assertThat(user.role()).isEqualTo(UserRole.MANAGER);
        assertThat(user.name().value()).isEqualTo("Ada Lovelace");
        assertThat(user.email().value()).isEqualTo("a@b.com");
        assertThat(user.passwordHash()).isEqualTo("hash");

        Company company = Company.create(CompanyId.generate(), CompanyName.of("Acme"));
        company.rename(CompanyName.of("Acme Inc"));
        assertThat(company.name().value()).isEqualTo("Acme Inc");

        Contact contact = Contact.create(ContactId.generate(), company.id(), PersonName.of("Pat"), Email.of("p@a.com"));
        assertThat(contact.id()).isNotNull();
        assertThat(contact.email().value()).isEqualTo("p@a.com");
        contact.rename(PersonName.of("Pat Lee"));
        contact.changeEmail(null);
        assertThat(contact.name().value()).isEqualTo("Pat Lee");
        assertThat(contact.email()).isNull();
        assertThat(contact.companyId()).isEqualTo(company.id());
    }

    @Test
    void activityRecordExposesFields() {
        DealId dealId = DealId.generate();
        UserId userId = UserId.generate();
        Activity activity = Activity.record(
                ActivityId.generate(),
                ActivityType.MEETING,
                ActivityBody.of("discovery"),
                ActivityTarget.deal(dealId),
                userId,
                java.time.Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(activity.type()).isEqualTo(ActivityType.MEETING);
        assertThat(activity.body().value()).isEqualTo("discovery");
        assertThat(activity.target().dealId()).contains(dealId);
        assertThat(activity.createdBy()).isEqualTo(userId);
        assertThat(activity.createdAt()).isEqualTo(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(activity.id()).isNotNull();
        assertThat(activity.qualifiesDealWin(dealId)).isTrue();
    }

    @Test
    void activityTargetDistinguishesDealAndContact() {
        DealId dealId = DealId.generate();
        assertThat(ActivityTarget.deal(dealId).isDeal(dealId)).isTrue();
        assertThat(ActivityTarget.deal(dealId).isDeal(DealId.generate())).isFalse();
        ContactId contactId = ContactId.generate();
        assertThat(ActivityTarget.contact(contactId).contactId()).contains(contactId);
        assertThat(ActivityTarget.contact(contactId).dealId()).isEmpty();
        assertThat(ActivityType.NOTE.qualifiesCloseWon()).isFalse();
        assertThat(ActivityType.CALL.qualifiesCloseWon()).isTrue();
    }

    @Test
    void guardsCoverNullBlankAndLength() {
        assertThatThrownBy(() -> Guards.notNull(null, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThat(Guards.notNull("a", "x")).isEqualTo("a");
        assertThatThrownBy(() -> Guards.notBlank("  ", "x")).isInstanceOf(IllegalArgumentException.class);
        assertThat(Guards.maxLength("ab", 2, "x")).isEqualTo("ab");
    }
}
