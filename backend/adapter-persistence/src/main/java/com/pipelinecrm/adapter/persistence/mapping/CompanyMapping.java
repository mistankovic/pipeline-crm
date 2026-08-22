package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.row.CompanyRow;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;

/** Company rows in, companies out, and back again. */
public final class CompanyMapping {

    private CompanyMapping() {
    }

    public static Company toDomain(CompanyRow row) {
        return new Company(CompanyId.of(row.getId()), row.getName());
    }

    public static CompanyRow toRow(Company company) {
        return new CompanyRow(company.id().value(), company.name());
    }
}
