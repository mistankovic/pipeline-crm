package com.pipelinecrm.adapter.web;

import com.pipelinecrm.adapter.web.dto.ApiDtos;
import com.pipelinecrm.adapter.web.dto.Responses;
import com.pipelinecrm.application.port.in.CreateContactUseCase;
import com.pipelinecrm.application.port.in.ListContactsUseCase;
import com.pipelinecrm.application.port.in.UpdateContactUseCase;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final CreateContactUseCase createContact;
    private final UpdateContactUseCase updateContact;
    private final ListContactsUseCase listContacts;

    public ContactController(
            CreateContactUseCase createContact,
            UpdateContactUseCase updateContact,
            ListContactsUseCase listContacts) {
        this.createContact = createContact;
        this.updateContact = updateContact;
        this.listContacts = listContacts;
    }

    @GetMapping
    public List<ApiDtos.ContactResponse> list() {
        return Responses.contacts(listContacts.execute());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.IdResponse create(@Valid @RequestBody ApiDtos.ContactRequest request) {
        return new ApiDtos.IdResponse(createContact
                .execute(new CreateContactUseCase.Command(
                        CompanyId.parse(request.companyId()), request.name(), request.email()))
                .toString());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody ApiDtos.ContactRequest request) {
        updateContact.execute(new UpdateContactUseCase.Command(ContactId.parse(id), request.name(), request.email()));
    }
}
