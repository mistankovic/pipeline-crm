package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.shared.Guard;

/** An activity recorded against a contact. */
public record ContactSubject(ContactId contact) implements ActivitySubject {

    public ContactSubject {
        Guard.present(contact, "contact an activity is about");
    }
}
