package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.company.Company;

/** A company as a caller sees one. */
public final class CompanyViews {

    private CompanyViews() {
    }

    public static CompanyView of(Company company) {
        return new CompanyView(company.id().value(), company.name());
    }
}
