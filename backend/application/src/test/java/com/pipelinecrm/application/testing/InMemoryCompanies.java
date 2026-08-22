package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCompanies implements CompanyRepository {

    private final Map<CompanyId, Company> stored = new LinkedHashMap<>();

    @Override
    public Optional<Company> findById(CompanyId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public List<Company> findAll() {
        return new ArrayList<>(stored.values());
    }

    @Override
    public void save(Company company) {
        stored.put(company.id(), company);
    }
}
