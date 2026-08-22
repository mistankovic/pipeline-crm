package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ContactView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contacts, optionally narrowed to one company. */
public interface ListContacts {

    List<ContactView> handle(Optional<UUID> companyId);
}
