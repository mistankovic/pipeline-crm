package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.CorrectContact;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.view.ContactView;
import com.pipelinecrm.application.view.ContactViews;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.shared.EmailAddress;

/** Corrects a contact that must already exist. */
public final class CorrectContactInteractor implements CorrectContact {

    private final ContactRepository contacts;
    private final WritingPorts writing;

    public CorrectContactInteractor(ContactRepository contacts, WritingPorts writing) {
        this.contacts = contacts;
        this.writing = writing;
    }

    @Override
    public ContactView handle(Corrections corrections) {
        return writing.transactions().execute(() -> correct(corrections));
    }

    private ContactView correct(Corrections corrections) {
        ContactId id = ContactId.of(corrections.contactId());
        Contact corrected = Required.found(contacts.findById(id), id)
                .correctedTo(corrections.name(), EmailAddress.of(corrections.email()));
        contacts.save(corrected);
        return ContactViews.of(corrected);
    }
}
