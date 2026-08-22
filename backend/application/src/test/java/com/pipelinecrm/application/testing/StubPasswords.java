package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.PasswordChecker;
import com.pipelinecrm.domain.identity.UserId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Remembers a plain password per user. Adequate for a test double; never for anything else. */
public final class StubPasswords implements PasswordChecker {

    private final Map<UserId, String> passwords = new LinkedHashMap<>();
    private int comparisons;

    public void set(UserId user, String password) {
        passwords.put(user, password);
    }

    @Override
    public boolean matches(UserId user, String presentedPassword) {
        comparisons++;
        return Objects.equals(passwords.get(user), presentedPassword);
    }

    /** How many comparisons were asked for. A failed sign-in must cost the same as a successful one. */
    public int comparisons() {
        return comparisons;
    }
}
