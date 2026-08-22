package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ContactView;

import java.util.UUID;

/** Add a person at a company. */
public interface CreateContact {

    ContactView handle(NewContact contact);

    record NewContact(UUID companyId, String name, String email) {
    }
}
