package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.CompanyMapper;
import com.pipelinecrm.adapter.persistence.spring.SpringCompanyRepository;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JpaCompanyRepository implements CompanyRepository {

    private final SpringCompanyRepository companies;

    public JpaCompanyRepository(SpringCompanyRepository companies) {
        this.companies = companies;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findById(CompanyId id) {
        return companies.findById(id.value()).map(CompanyMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> findAll() {
        return companies.findAll().stream().map(CompanyMapper::toDomain).toList();
    }

    @Override
    public void save(Company company) {
        companies.save(CompanyMapper.toEntity(company));
    }
}
