package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.ContactMapping;
import com.pipelinecrm.adapter.persistence.repository.ContactRows;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** The contact port, over JPA. */
@Repository
public class JpaContactRepository implements ContactRepository {

    private final ContactRows rows;

    public JpaContactRepository(ContactRows rows) {
        this.rows = rows;
    }

    @Override
    public Optional<Contact> findById(ContactId id) {
        return rows.findById(id.value()).map(ContactMapping::toDomain);
    }

    @Override
    public List<Contact> findAll() {
        return rows.findAll().stream().map(ContactMapping::toDomain).toList();
    }

    @Override
    public List<Contact> findByCompany(CompanyId company) {
        return rows.findByCompanyId(company.value()).stream().map(ContactMapping::toDomain).toList();
    }

    @Override
    public void save(Contact contact) {
        rows.save(ContactMapping.toRow(contact));
    }
}
