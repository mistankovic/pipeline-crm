package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.contact.Contact;

/** A contact as a caller sees one. */
public final class ContactViews {

    private ContactViews() {
    }

    public static ContactView of(Contact contact) {
        return new ContactView(contact.id().value(), contact.company().value(),
                contact.name(), contact.email().value());
    }
}
