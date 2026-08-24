package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.entity.ContactEntity;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;

public final class ContactMapper {

    private ContactMapper() {}

    public static Contact toDomain(ContactEntity entity) {
        Email email = entity.email() == null ? null : Email.of(entity.email());
        return Contact.create(
                new ContactId(entity.id()),
                new CompanyId(entity.companyId()),
                PersonName.of(entity.name()),
                email);
    }

    public static ContactEntity toEntity(Contact contact) {
        String email = contact.email() == null ? null : contact.email().value();
        return new ContactEntity(
                contact.id().value(), contact.companyId().value(), contact.name().value(), email);
    }
}
