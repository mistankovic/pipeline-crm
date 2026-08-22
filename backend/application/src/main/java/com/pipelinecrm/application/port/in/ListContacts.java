package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ContactView;

import java.util.List;
import java.util.UUID;

/** Contacts, either all of them or those at one company. */
public interface ListContacts {

    List<ContactView> everything();

    List<ContactView> atCompany(UUID companyId);
}
