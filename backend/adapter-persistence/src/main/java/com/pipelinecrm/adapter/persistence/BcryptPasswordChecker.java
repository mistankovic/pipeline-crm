package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.repository.UserRows;
import com.pipelinecrm.application.port.out.PasswordChecker;
import com.pipelinecrm.domain.identity.UserId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Checks a presented password against the stored hash.
 *
 * <p>It lives beside the store because the hash is a column, and the application layer never
 * sees a hash, a salt or an algorithm — only a yes or a no. A user who does not exist gets
 * the same "no" as a wrong password; the reason is decided one layer in, by
 * {@code SignInInteractor}, which answers both identically on purpose.
 */
@Component
public class BcryptPasswordChecker implements PasswordChecker {

    private final UserRows rows;
    private final PasswordEncoder encoder;

    public BcryptPasswordChecker(UserRows rows, PasswordEncoder encoder) {
        this.rows = rows;
        this.encoder = encoder;
    }

    @Override
    public boolean matches(UserId user, String presentedPassword) {
        if (presentedPassword == null) {
            return false;
        }
        return rows.findById(user.value())
                .map(row -> encoder.matches(presentedPassword, row.getPasswordHash()))
                .orElse(false);
    }
}
