package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.identity.ContactId;

public interface UpdateContactUseCase {

    void execute(Command command);

    record Command(ContactId contactId, String name, String email) {}
}
