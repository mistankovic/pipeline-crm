package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.row.ContactRow;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.shared.EmailAddress;

/** Contact rows in, contacts out, and back again. */
public final class ContactMapping {

    private ContactMapping() {
    }

    public static Contact toDomain(ContactRow row) {
        return new Contact(ContactId.of(row.getId()), CompanyId.of(row.getCompanyId()),
                row.getName(), EmailAddress.of(row.getEmail()));
    }

    public static ContactRow toRow(Contact contact) {
        return new ContactRow(contact.id().value(), contact.company().value(),
                contact.name(), contact.email().value());
    }
}
