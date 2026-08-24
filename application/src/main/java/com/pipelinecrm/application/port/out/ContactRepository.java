package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.ContactId;
import java.util.List;
import java.util.Optional;

public interface ContactRepository {

    Optional<Contact> findById(ContactId id);

    List<Contact> findAll();

    void save(Contact contact);
}
