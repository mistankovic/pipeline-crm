package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.domain.identity.UserId;

/**
 * Who the current request is from, as the web layer knows it. Controllers pass the id to a
 * use case; they never decide what that user is allowed to do — the domain does.
 */
public record SignedInUser(UserId id, String role) {
}
