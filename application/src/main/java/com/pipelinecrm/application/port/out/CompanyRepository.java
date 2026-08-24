package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {

    Optional<Company> findById(CompanyId id);

    List<Company> findAll();

    void save(Company company);
}
