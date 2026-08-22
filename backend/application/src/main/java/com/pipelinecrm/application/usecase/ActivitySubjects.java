package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.application.port.out.ContactRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.activity.ActivitySubject;
import com.pipelinecrm.domain.activity.ContactSubject;
import com.pipelinecrm.domain.activity.DealSubject;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;

import java.util.UUID;

/**
 * Turns the port's sealed "what is this activity about" into the domain's sealed version,
 * checking on the way that the thing exists. Both sides are sealed, so this translation is
 * exhaustive by construction: a third kind of subject cannot be added on one side alone.
 */
public final class ActivitySubjects {

    private final DealRepository deals;
    private final ContactRepository contacts;

    public ActivitySubjects(DealRepository deals, ContactRepository contacts) {
        this.deals = deals;
        this.contacts = contacts;
    }

    public ActivitySubject resolve(LogActivity.About about) {
        return switch (about) {
            case LogActivity.AboutDeal onDeal -> existingDeal(onDeal.dealId());
            case LogActivity.AboutContact onContact -> existingContact(onContact.contactId());
        };
    }

    private ActivitySubject existingDeal(UUID id) {
        DealId dealId = DealId.of(id);
        Required.found(deals.findById(dealId), dealId);
        return new DealSubject(dealId);
    }

    private ActivitySubject existingContact(UUID id) {
        ContactId contactId = ContactId.of(id);
        Required.found(contacts.findById(contactId), contactId);
        return new ContactSubject(contactId);
    }
}
