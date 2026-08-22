package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.CompanyView;

import java.util.List;

/** Every company, for pickers and list views. */
public interface ListCompanies {

    List<CompanyView> handle();
}
