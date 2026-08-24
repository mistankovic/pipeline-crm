package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.CreateCompanyUseCase;
import com.pipelinecrm.application.port.in.ListCompaniesUseCase;
import com.pipelinecrm.application.port.in.UpdateCompanyUseCase;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.company.CompanyName;
import com.pipelinecrm.domain.identity.CompanyId;
import java.util.List;

public final class CompanyServices implements CreateCompanyUseCase, UpdateCompanyUseCase, ListCompaniesUseCase {

    private final CompanyRepository companies;

    public CompanyServices(CompanyRepository companies) {
        this.companies = companies;
    }

    @Override
    public CompanyId execute(CreateCompanyUseCase.Command command) {
        CompanyId id = CompanyId.generate();
        companies.save(Company.create(id, CompanyName.of(command.name())));
        return id;
    }

    @Override
    public void execute(UpdateCompanyUseCase.Command command) {
        Company company = companies.findById(command.companyId()).orElseThrow(() -> new NotFoundException("company"));
        company.rename(CompanyName.of(command.name()));
        companies.save(company);
    }

    @Override
    public List<Company> execute() {
        return companies.findAll();
    }
}
