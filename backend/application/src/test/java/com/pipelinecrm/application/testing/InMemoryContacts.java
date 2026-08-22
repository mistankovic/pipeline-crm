package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryContacts implements ContactRepository {

    private final Map<ContactId, Contact> stored = new LinkedHashMap<>();

    @Override
    public Optional<Contact> findById(ContactId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public List<Contact> findAll() {
        return new ArrayList<>(stored.values());
    }

    @Override
    public List<Contact> findByCompany(CompanyId company) {
        return stored.values().stream().filter(contact -> contact.worksFor(company)).toList();
    }

    @Override
    public void save(Contact contact) {
        stored.put(contact.id(), contact);
    }
}
