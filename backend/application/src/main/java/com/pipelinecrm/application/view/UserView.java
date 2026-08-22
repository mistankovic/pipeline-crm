package com.pipelinecrm.application.view;

import java.util.UUID;

/** A user as a caller sees it. Never carries a password or a hash. */
public record UserView(UUID id, String email, String name, String role) {
}
