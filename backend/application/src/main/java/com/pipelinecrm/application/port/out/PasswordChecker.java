package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.identity.UserId;

/**
 * Checks a presented password against whatever the infrastructure stores. The application
 * layer never sees a hash, a salt or an algorithm.
 */
public interface PasswordChecker {

    boolean matches(UserId user, String presentedPassword);
}
