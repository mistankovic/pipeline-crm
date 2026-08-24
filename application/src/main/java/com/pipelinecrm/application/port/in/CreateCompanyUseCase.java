package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.identity.CompanyId;

public interface CreateCompanyUseCase {

    CompanyId execute(Command command);

    record Command(String name) {}
}
