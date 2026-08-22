package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.CompanyView;

/** Add a company to the book. */
public interface CreateCompany {

    CompanyView handle(NewCompany company);

    record NewCompany(String name) {
    }
}
