package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.CreateContact;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.view.ContactView;
import com.pipelinecrm.application.view.ContactViews;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.shared.EmailAddress;

/** Adds a person at a company that must already exist. */
public final class CreateContactInteractor implements CreateContact {

    private final ContactRepository contacts;
    private final Parties parties;
    private final WritingPorts writing;

    public CreateContactInteractor(ContactRepository contacts, Parties parties, WritingPorts writing) {
        this.contacts = contacts;
        this.parties = parties;
        this.writing = writing;
    }

    @Override
    public ContactView handle(NewContact request) {
        return writing.transactions().execute(() -> store(request));
    }

    private ContactView store(NewContact request) {
        Company company = parties.company(request.companyId());
        Contact contact = new Contact(ContactId.of(writing.identifiers().newIdentifier()),
                company.id(), request.name(), EmailAddress.of(request.email()));
        contacts.save(contact);
        return ContactViews.of(contact);
    }
}
