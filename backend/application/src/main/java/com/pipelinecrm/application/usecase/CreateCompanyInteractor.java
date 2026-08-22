package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.CreateCompany;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.view.CompanyView;
import com.pipelinecrm.application.view.CompanyViews;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;

/** Adds a company. The name is validated by the entity, not here. */
public final class CreateCompanyInteractor implements CreateCompany {

    private final CompanyRepository companies;
    private final WritingPorts writing;

    public CreateCompanyInteractor(CompanyRepository companies, WritingPorts writing) {
        this.companies = companies;
        this.writing = writing;
    }

    @Override
    public CompanyView handle(NewCompany request) {
        return writing.transactions().execute(() -> store(request));
    }

    private CompanyView store(NewCompany request) {
        Company company = new Company(CompanyId.of(writing.identifiers().newIdentifier()), request.name());
        companies.save(company);
        return CompanyViews.of(company);
    }
}
