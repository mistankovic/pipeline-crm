package com.pipelinecrm.application.error;

/**
 * Sign-in was refused. The message is deliberately identical for an unknown address and a
 * wrong password, so that the response does not tell an attacker which accounts exist.
 */
public final class AuthenticationFailed extends ApplicationException {

    private static final long serialVersionUID = 1L;

    private static final String SAME_MESSAGE_FOR_EVERY_FAILURE = "email address or password is incorrect";

    public AuthenticationFailed() {
        super(SAME_MESSAGE_FOR_EVERY_FAILURE);
    }
}
