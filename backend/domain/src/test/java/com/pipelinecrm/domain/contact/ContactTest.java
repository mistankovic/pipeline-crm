package com.pipelinecrm.domain.contact;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactTest {

    private static final ContactId AN_ID = ContactId.of(UUID.randomUUID());
    private static final CompanyId ACME = CompanyId.of(UUID.randomUUID());
    private static final EmailAddress AN_EMAIL = EmailAddress.of("cara@acme.test");

    @Test
    void knows_the_company_it_belongs_to() {
        Contact contact = new Contact(AN_ID, ACME, "Cara", AN_EMAIL);

        assertThat(contact.worksFor(ACME)).isTrue();
    }

    @Test
    void does_not_belong_to_a_different_company() {
        Contact contact = new Contact(AN_ID, ACME, "Cara", AN_EMAIL);

        assertThat(contact.worksFor(CompanyId.of(UUID.randomUUID()))).isFalse();
    }

    @Test
    void refuses_to_exist_without_an_id() {
        assertThatThrownBy(() -> new Contact(null, ACME, "Cara", AN_EMAIL))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_company() {
        assertThatThrownBy(() -> new Contact(AN_ID, null, "Cara", AN_EMAIL))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_name() {
        assertThatThrownBy(() -> new Contact(AN_ID, ACME, " ", AN_EMAIL))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_an_email() {
        assertThatThrownBy(() -> new Contact(AN_ID, ACME, "Cara", null))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void keeps_its_identity_and_employer_when_corrected() {
        Contact corrected = new Contact(AN_ID, ACME, "Cara", AN_EMAIL)
                .correctedTo("Cara Nguyen", EmailAddress.of("cara.nguyen@acme.test"));

        assertThat(corrected.id()).isEqualTo(AN_ID);
        assertThat(corrected.worksFor(ACME)).isTrue();
        assertThat(corrected.name()).isEqualTo("Cara Nguyen");
        assertThat(corrected.email()).isEqualTo(EmailAddress.of("cara.nguyen@acme.test"));
    }

    @Test
    void applies_the_same_rules_when_corrected() {
        Contact cara = new Contact(AN_ID, ACME, "Cara", AN_EMAIL);

        assertThat(cara.correctedTo("  Cara Nguyen  ", AN_EMAIL).name()).isEqualTo("Cara Nguyen");
        assertThatThrownBy(() -> cara.correctedTo(" ", AN_EMAIL))
                .isInstanceOf(InvariantViolation.class);
        assertThatThrownBy(() -> cara.correctedTo("Cara Nguyen", null))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void leaves_the_original_alone_when_corrected() {
        Contact cara = new Contact(AN_ID, ACME, "Cara", AN_EMAIL);

        cara.correctedTo("Cara Nguyen", EmailAddress.of("cara.nguyen@acme.test"));

        assertThat(cara.name()).isEqualTo("Cara");
        assertThat(cara.email()).isEqualTo(AN_EMAIL);
    }
}
