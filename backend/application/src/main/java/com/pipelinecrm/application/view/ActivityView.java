package com.pipelinecrm.application.view;

import java.time.Instant;
import java.util.UUID;

/** One entry on a timeline. Exactly one of dealId and contactId is set. */
public record ActivityView(
        UUID id,
        String type,
        String summary,
        UUID dealId,
        UUID contactId,
        UserView author,
        Instant occurredAt) {
}
