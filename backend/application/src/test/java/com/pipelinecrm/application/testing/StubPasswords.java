package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.PasswordChecker;
import com.pipelinecrm.domain.identity.UserId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Remembers a plain password per user. Adequate for a test double; never for anything else. */
public final class StubPasswords implements PasswordChecker {

    private final Map<UserId, String> passwords = new LinkedHashMap<>();

    public void set(UserId user, String password) {
        passwords.put(user, password);
    }

    @Override
    public boolean matches(UserId user, String presentedPassword) {
        return Objects.equals(passwords.get(user), presentedPassword);
    }
}
