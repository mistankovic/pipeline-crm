package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.company.Company;
import java.util.List;

public interface ListCompaniesUseCase {

    List<Company> execute();
}
