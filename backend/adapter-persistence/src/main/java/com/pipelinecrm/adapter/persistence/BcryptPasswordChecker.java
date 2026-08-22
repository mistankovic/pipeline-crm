package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.repository.UserRows;
import com.pipelinecrm.adapter.persistence.row.UserRow;
import com.pipelinecrm.application.port.out.PasswordChecker;
import com.pipelinecrm.domain.identity.UserId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Checks a presented password against the stored hash.
 *
 * <p>It lives beside the store because the hash is a column, and the application layer never
 * sees a hash, a salt or an algorithm — only a yes or a no.
 *
 * <p><strong>Every answer costs one bcrypt comparison</strong>, including "no such user" and
 * "no password given". Decision D-12 says a failed sign-in must not reveal whether an account
 * exists, and returning early for an unknown user reveals it perfectly well: bcrypt at cost 10
 * takes on the order of a hundred milliseconds, so an attacker reads the answer off a
 * stopwatch rather than off the message. See docs/reviews/stage-4-review.md, finding F-4.2.
 */
@Component
public class BcryptPasswordChecker implements PasswordChecker {

    /**
     * A real bcrypt hash of a value nobody knows, compared against when there is no user or no
     * password, purely so that the work done is the same either way.
     */
    private static final String HASH_OF_NOTHING_IN_PARTICULAR =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOa8Rq7SPtLQeD6oPzC8fZ4nCVKQ3vJ6y";

    private final UserRows rows;
    private final PasswordEncoder encoder;

    public BcryptPasswordChecker(UserRows rows, PasswordEncoder encoder) {
        this.rows = rows;
        this.encoder = encoder;
    }

    @Override
    public boolean matches(UserId user, String presentedPassword) {
        Optional<String> stored = rows.findById(user.value()).map(UserRow::getPasswordHash);
        boolean hashesAgree = encoder.matches(candidate(presentedPassword),
                stored.orElse(HASH_OF_NOTHING_IN_PARTICULAR));
        return stored.isPresent() && presentedPassword != null && hashesAgree;
    }

    private String candidate(String presentedPassword) {
        return Optional.ofNullable(presentedPassword).orElse("");
    }
}
