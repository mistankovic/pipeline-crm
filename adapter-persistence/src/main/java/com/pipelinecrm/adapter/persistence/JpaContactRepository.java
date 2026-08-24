package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.ContactMapper;
import com.pipelinecrm.adapter.persistence.spring.SpringContactRepository;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.ContactId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JpaContactRepository implements ContactRepository {

    private final SpringContactRepository contacts;

    public JpaContactRepository(SpringContactRepository contacts) {
        this.contacts = contacts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contact> findById(ContactId id) {
        return contacts.findById(id.value()).map(ContactMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contact> findAll() {
        return contacts.findAll().stream().map(ContactMapper::toDomain).toList();
    }

    @Override
    public void save(Contact contact) {
        contacts.save(ContactMapper.toEntity(contact));
    }
}
