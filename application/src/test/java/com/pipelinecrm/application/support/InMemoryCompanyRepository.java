package com.pipelinecrm.application.support;

import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCompanyRepository implements CompanyRepository {

    private final Map<CompanyId, Company> store = new LinkedHashMap<>();
    public int saveCount;

    @Override
    public Optional<Company> findById(CompanyId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Company> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void save(Company company) {
        saveCount++;
        store.put(company.id(), company);
    }
}
