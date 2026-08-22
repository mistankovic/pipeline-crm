package com.pipelinecrm.application.view;

import java.util.UUID;

/** A company as a caller sees it. */
public record CompanyView(UUID id, String name) {
}
