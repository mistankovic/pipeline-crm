package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.RenameCompany;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.view.CompanyView;
import com.pipelinecrm.application.view.CompanyViews;
import com.pipelinecrm.domain.company.Company;

import java.util.UUID;

/** Renames a company that must already exist. The name's rules are the domain's, not ours. */
public final class RenameCompanyInteractor implements RenameCompany {

    private final CompanyRepository companies;
    private final Parties parties;
    private final WritingPorts writing;

    public RenameCompanyInteractor(CompanyRepository companies, Parties parties, WritingPorts writing) {
        this.companies = companies;
        this.parties = parties;
        this.writing = writing;
    }

    @Override
    public CompanyView handle(UUID companyId, String newName) {
        return writing.transactions().execute(() -> rename(companyId, newName));
    }

    private CompanyView rename(UUID companyId, String newName) {
        Company renamed = parties.company(companyId).renamedTo(newName);
        companies.save(renamed);
        return CompanyViews.of(renamed);
    }
}
