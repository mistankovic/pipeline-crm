package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ListCompanies;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.view.CompanyView;
import com.pipelinecrm.application.view.CompanyViews;

import java.util.List;

/** Every company. */
public final class ListCompaniesInteractor implements ListCompanies {

    private final CompanyRepository companies;

    public ListCompaniesInteractor(CompanyRepository companies) {
        this.companies = companies;
    }

    @Override
    public List<CompanyView> handle() {
        return companies.findAll().stream().map(CompanyViews::of).toList();
    }
}
