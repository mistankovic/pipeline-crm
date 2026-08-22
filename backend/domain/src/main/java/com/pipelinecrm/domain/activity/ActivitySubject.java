package com.pipelinecrm.domain.activity;

/**
 * What an activity is about. Sealed with exactly two cases, which is how the domain
 * expresses "an activity is linked to a deal or to a contact, never both and never
 * neither" without a nullable field and a runtime check.
 */
public sealed interface ActivitySubject permits DealSubject, ContactSubject {
}
