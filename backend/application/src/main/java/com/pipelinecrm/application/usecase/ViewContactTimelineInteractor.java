package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ViewContactTimeline;
import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.view.ActivityView;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.identity.ContactId;

import java.util.List;
import java.util.UUID;

/** Everything recorded against one contact. */
public final class ViewContactTimelineInteractor implements ViewContactTimeline {

    private final ContactRepository contacts;
    private final ActivityRepository activities;
    private final Timelines timelines;

    public ViewContactTimelineInteractor(ContactRepository contacts, ActivityRepository activities,
                                         Timelines timelines) {
        this.contacts = contacts;
        this.activities = activities;
        this.timelines = timelines;
    }

    @Override
    public List<ActivityView> handle(UUID contactId) {
        Contact contact = existing(contactId);
        return timelines.of(activities.findByContact(contact.id()));
    }

    private Contact existing(UUID contactId) {
        ContactId id = ContactId.of(contactId);
        return Required.found(contacts.findById(id), "contact", id);
    }
}
