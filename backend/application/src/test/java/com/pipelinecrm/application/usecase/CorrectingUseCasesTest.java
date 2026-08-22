package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.application.port.in.CorrectContact;
import com.pipelinecrm.application.port.in.CreateContact;
import com.pipelinecrm.application.testing.Seed;
import com.pipelinecrm.application.testing.UseCases;
import com.pipelinecrm.application.view.CompanyView;
import com.pipelinecrm.application.view.ContactView;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Correcting a company or a contact after the fact.
 *
 * <p>Every test here reads the value back through a *different* use case than the one that
 * wrote it. Returning the right view while never storing anything is the failure these are
 * written to catch — it is exactly what a surviving mutant on the `save` call looks like.
 */
class CorrectingUseCasesTest {

    private final UseCases application = new UseCases();
    private final Seed seed = new Seed(application);

    private UUID acme;

    @BeforeEach
    void seedTheWorld() {
        acme = seed.company("Acme");
    }

    @Test
    void renames_a_company_and_stores_the_new_name() {
        CompanyView renamed = application.renameCompany.handle(acme, "Acme Holdings");

        assertThat(renamed.name()).isEqualTo("Acme Holdings");
        assertThat(application.listCompanies.handle()).extracting(CompanyView::name)
                .containsExactly("Acme Holdings");
    }

    @Test
    void keeps_a_renamed_company_the_same_company() {
        UUID contact = contact("Cara", acme).id();

        application.renameCompany.handle(acme, "Acme Holdings");

        assertThat(application.renameCompany.handle(acme, "Acme Holdings").id()).isEqualTo(acme);
        assertThat(application.listContacts.atCompany(acme)).extracting(ContactView::id)
                .containsExactly(contact);
    }

    @Test
    void refuses_to_rename_a_company_that_does_not_exist() {
        UUID nobody = UUID.randomUUID();

        assertThatThrownBy(() -> application.renameCompany.handle(nobody, "Anything"))
                .isInstanceOf(UnknownEntity.class)
                .hasMessage("no company with id " + nobody);
    }

    @Test
    void refuses_a_blank_company_name_and_changes_nothing() {
        assertThatThrownBy(() -> application.renameCompany.handle(acme, "  "))
                .isInstanceOf(InvariantViolation.class);

        assertThat(application.listCompanies.handle()).extracting(CompanyView::name)
                .containsExactly("Acme");
    }

    @Test
    void corrects_a_contact_and_stores_both_fields() {
        UUID cara = contact("Cara", acme).id();

        ContactView corrected = application.correctContact.handle(
                new CorrectContact.Corrections(cara, "Cara Nguyen", "cara.nguyen@acme.test"));

        assertThat(corrected.name()).isEqualTo("Cara Nguyen");
        assertThat(corrected.email()).isEqualTo("cara.nguyen@acme.test");
        assertThat(application.listContacts.everything()).singleElement()
                .satisfies(stored -> {
                    assertThat(stored.name()).isEqualTo("Cara Nguyen");
                    assertThat(stored.email()).isEqualTo("cara.nguyen@acme.test");
                });
    }

    @Test
    void keeps_a_corrected_contact_at_the_same_company() {
        UUID cara = contact("Cara", acme).id();

        application.correctContact.handle(
                new CorrectContact.Corrections(cara, "Cara Nguyen", "cara.nguyen@acme.test"));

        assertThat(application.listContacts.atCompany(acme)).extracting(ContactView::id)
                .containsExactly(cara);
    }

    @Test
    void refuses_to_correct_a_contact_that_does_not_exist() {
        UUID nobody = UUID.randomUUID();

        assertThatThrownBy(() -> application.correctContact.handle(
                new CorrectContact.Corrections(nobody, "Someone", "someone@acme.test")))
                .isInstanceOf(UnknownEntity.class)
                .hasMessage("no contact with id " + nobody);
    }

    @Test
    void refuses_a_malformed_email_and_changes_nothing() {
        UUID cara = contact("Cara", acme).id();

        assertThatThrownBy(() -> application.correctContact.handle(
                new CorrectContact.Corrections(cara, "Cara Nguyen", "not-an-email")))
                .isInstanceOf(InvariantViolation.class);

        assertThat(application.listContacts.everything()).singleElement()
                .satisfies(stored -> assertThat(stored.name()).isEqualTo("Cara"));
    }

    @Test
    void refuses_a_blank_contact_name_and_changes_nothing() {
        UUID cara = contact("Cara", acme).id();

        assertThatThrownBy(() -> application.correctContact.handle(
                new CorrectContact.Corrections(cara, " ", "cara@acme.test")))
                .isInstanceOf(InvariantViolation.class);

        assertThat(application.listContacts.everything()).singleElement()
                .satisfies(stored -> assertThat(stored.name()).isEqualTo("Cara"));
    }

    private ContactView contact(String name, UUID company) {
        return application.createContact.handle(new CreateContact.NewContact(
                company, name, name.toLowerCase(java.util.Locale.ROOT) + "@acme.test"));
    }
}
