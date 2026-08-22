package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ContactView;

import java.util.UUID;

/** Correct a contact's name and email address. Their employer is not changed here. */
public interface CorrectContact {

    ContactView handle(Corrections corrections);

    record Corrections(UUID contactId, String name, String email) {
    }
}
