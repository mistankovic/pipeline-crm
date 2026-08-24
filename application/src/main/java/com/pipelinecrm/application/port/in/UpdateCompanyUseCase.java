package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.identity.CompanyId;

public interface UpdateCompanyUseCase {

    void execute(Command command);

    record Command(CompanyId companyId, String name) {}
}
