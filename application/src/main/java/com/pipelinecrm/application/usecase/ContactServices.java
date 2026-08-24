package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.CreateContactUseCase;
import com.pipelinecrm.application.port.in.ListContactsUseCase;
import com.pipelinecrm.application.port.in.UpdateContactUseCase;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import java.util.List;

public final class ContactServices implements CreateContactUseCase, UpdateContactUseCase, ListContactsUseCase {

    private final ContactRepository contacts;
    private final CompanyRepository companies;

    public ContactServices(ContactRepository contacts, CompanyRepository companies) {
        this.contacts = contacts;
        this.companies = companies;
    }

    @Override
    public ContactId execute(CreateContactUseCase.Command command) {
        companies.findById(command.companyId()).orElseThrow(() -> new NotFoundException("company"));
        ContactId id = ContactId.generate();
        Email email = command.email() == null ? null : Email.of(command.email());
        contacts.save(Contact.create(id, command.companyId(), PersonName.of(command.name()), email));
        return id;
    }

    @Override
    public void execute(UpdateContactUseCase.Command command) {
        Contact contact = contacts.findById(command.contactId()).orElseThrow(() -> new NotFoundException("contact"));
        contact.rename(PersonName.of(command.name()));
        contact.changeEmail(command.email() == null ? null : Email.of(command.email()));
        contacts.save(contact);
    }

    @Override
    public List<Contact> execute() {
        return contacts.findAll();
    }
}
