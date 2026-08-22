package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.application.port.in.CreateContact;
import com.pipelinecrm.application.port.in.ListContacts;
import com.pipelinecrm.application.port.in.ViewContactTimeline;
import com.pipelinecrm.application.view.ActivityView;
import com.pipelinecrm.application.view.ContactView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contacts and their timelines. */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private static final int LONGEST_NAME = 200;

    private final ListContacts listing;
    private final CreateContact creation;
    private final ViewContactTimeline timeline;

    public ContactController(ListContacts listing, CreateContact creation, ViewContactTimeline timeline) {
        this.listing = listing;
        this.creation = creation;
        this.timeline = timeline;
    }

    @GetMapping
    public List<ContactView> all(@RequestParam(name = "companyId", required = false) UUID companyId) {
        return Optional.ofNullable(companyId).map(listing::atCompany).orElseGet(listing::everything);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactView create(@Valid @RequestBody NewContactRequest request) {
        return creation.handle(new CreateContact.NewContact(
                request.companyId(), request.name(), request.email()));
    }

    @GetMapping("/{id}/activities")
    public List<ActivityView> activitiesOf(@PathVariable("id") UUID contactId) {
        return timeline.handle(contactId);
    }

    /** What a caller sends to add a contact. */
    public record NewContactRequest(
            @NotNull UUID companyId,
            @NotBlank @Size(max = LONGEST_NAME) String name,
            @NotBlank @Email String email) {
    }
}
