package com.pipelinecrm.domain.contact;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import com.pipelinecrm.domain.shared.Guards;

public final class Contact {

    private final ContactId id;
    private final CompanyId companyId;
    private PersonName name;
    private Email email;

    private Contact(ContactId id, CompanyId companyId, PersonName name, Email email) {
        this.id = Guards.notNull(id, "id");
        this.companyId = Guards.notNull(companyId, "companyId");
        this.name = Guards.notNull(name, "name");
        this.email = email;
    }

    public static Contact create(ContactId id, CompanyId companyId, PersonName name, Email email) {
        return new Contact(id, companyId, name, email);
    }

    public ContactId id() {
        return id;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public PersonName name() {
        return name;
    }

    public Email email() {
        return email;
    }

    public void rename(PersonName newName) {
        this.name = Guards.notNull(newName, "newName");
    }

    public void changeEmail(Email newEmail) {
        this.email = newEmail;
    }
}
