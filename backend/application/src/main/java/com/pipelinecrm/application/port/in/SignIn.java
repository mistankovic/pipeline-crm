package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.AuthenticatedUser;

/** Exchange an email address and a password for a token. */
public interface SignIn {

    AuthenticatedUser handle(Credentials credentials);

    record Credentials(String email, String password) {
    }
}
