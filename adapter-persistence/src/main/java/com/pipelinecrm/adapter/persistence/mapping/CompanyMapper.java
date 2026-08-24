package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.entity.CompanyEntity;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.company.CompanyName;
import com.pipelinecrm.domain.identity.CompanyId;

public final class CompanyMapper {

    private CompanyMapper() {}

    public static Company toDomain(CompanyEntity entity) {
        return Company.create(new CompanyId(entity.id()), CompanyName.of(entity.name()));
    }

    public static CompanyEntity toEntity(Company company) {
        return new CompanyEntity(company.id().value(), company.name().value());
    }
}
