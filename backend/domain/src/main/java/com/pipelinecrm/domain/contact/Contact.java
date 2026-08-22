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
}
