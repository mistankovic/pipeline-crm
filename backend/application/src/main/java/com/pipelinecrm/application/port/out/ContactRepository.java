package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;

import java.util.List;
import java.util.Optional;

/** How the use cases reach contacts. */
public interface ContactRepository {

    Optional<Contact> findById(ContactId id);

    List<Contact> findAll();

    List<Contact> findByCompany(CompanyId company);

    void save(Contact contact);
}
