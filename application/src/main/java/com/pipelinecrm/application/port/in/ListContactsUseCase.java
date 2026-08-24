package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.contact.Contact;
import java.util.List;

public interface ListContactsUseCase {

    List<Contact> execute();
}
