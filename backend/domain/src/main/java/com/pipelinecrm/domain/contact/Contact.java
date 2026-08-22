package com.pipelinecrm.domain.contact;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.shared.Guard;

/** A person at a company. A contact always belongs to exactly one company. */
public record Contact(ContactId id, CompanyId company, String name, EmailAddress email) {

    public Contact {
        Guard.present(id, "contact id");
        Guard.present(company, "company of a contact");
        name = Guard.filled(name, "contact name");
        Guard.present(email, "contact email");
    }

    public boolean worksFor(CompanyId candidate) {
        return company.equals(candidate);
    }

    /**
     * The same person with corrected details. Who they work for is deliberately not among
     * them: moving a contact between companies changes which deals they are relevant to, and
     * that is a different decision from fixing a misspelt name. See decision D-21.
     */
    public Contact correctedTo(String newName, EmailAddress newEmail) {
        return new Contact(id, company, newName, newEmail);
    }
}
