package com.pipelinecrm.application.view;

import java.util.UUID;

/** A contact as a caller sees it. */
public record ContactView(UUID id, UUID companyId, String name, String email) {
}
