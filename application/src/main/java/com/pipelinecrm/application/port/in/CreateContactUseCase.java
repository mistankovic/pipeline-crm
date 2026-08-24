package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;

public interface CreateContactUseCase {

    ContactId execute(Command command);

    record Command(CompanyId companyId, String name, String email) {}
}
