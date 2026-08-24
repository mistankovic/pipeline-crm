package com.pipelinecrm.application.support;

import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.ContactId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryContactRepository implements ContactRepository {

    private final Map<ContactId, Contact> store = new LinkedHashMap<>();
    public int saveCount;

    @Override
    public Optional<Contact> findById(ContactId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Contact> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void save(Contact contact) {
        saveCount++;
        store.put(contact.id(), contact);
    }
}
