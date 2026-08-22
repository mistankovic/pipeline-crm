package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.CompanyMapping;
import com.pipelinecrm.adapter.persistence.repository.CompanyRows;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** The company port, over JPA. */
@Repository
public class JpaCompanyRepository implements CompanyRepository {

    private final CompanyRows rows;

    public JpaCompanyRepository(CompanyRows rows) {
        this.rows = rows;
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        return rows.findById(id.value()).map(CompanyMapping::toDomain);
    }

    @Override
    public List<Company> findAll() {
        return rows.findAll().stream().map(CompanyMapping::toDomain).toList();
    }

    @Override
    public List<Company> findAllByIds(Collection<CompanyId> ids) {
        return rows.findAllById(ids.stream().map(CompanyId::value).toList()).stream()
                .map(CompanyMapping::toDomain).toList();
    }

    @Override
    public void save(Company company) {
        rows.save(CompanyMapping.toRow(company));
    }
}
