package com.pipelinecrm.domain.identity;

import java.util.UUID;

/**
 * Every entity is identified by a UUID wrapped in its own type, so that a contact id can
 * never be passed where a deal id is expected. Identifiers live in their own package so
 * that entity packages can refer to one another's identities without forming a cycle.
 */
public sealed interface Identifier
        permits ActivityId, CompanyId, ContactId, DealId, UserId {

    UUID value();
}
