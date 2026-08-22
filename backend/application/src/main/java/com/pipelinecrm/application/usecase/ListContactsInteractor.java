package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ListContacts;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.view.ContactView;
import com.pipelinecrm.application.view.ContactViews;
import com.pipelinecrm.domain.identity.CompanyId;

import java.util.List;
import java.util.UUID;

/** Contacts, either all of them or those at one company. */
public final class ListContactsInteractor implements ListContacts {

    private final ContactRepository contacts;

    public ListContactsInteractor(ContactRepository contacts) {
        this.contacts = contacts;
    }

    @Override
    public List<ContactView> everything() {
        return viewsOf(contacts.findAll());
    }

    @Override
    public List<ContactView> atCompany(UUID companyId) {
        return viewsOf(contacts.findByCompany(CompanyId.of(companyId)));
    }

    private List<ContactView> viewsOf(List<com.pipelinecrm.domain.contact.Contact> found) {
        return found.stream().map(ContactViews::of).toList();
    }
}
