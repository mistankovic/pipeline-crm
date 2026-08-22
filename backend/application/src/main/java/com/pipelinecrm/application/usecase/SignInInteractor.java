package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.AuthenticationFailed;
import com.pipelinecrm.application.port.in.SignIn;
import com.pipelinecrm.application.port.out.AccessTokenIssuer;
import com.pipelinecrm.application.port.out.PasswordChecker;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.application.view.AuthenticatedUser;
import com.pipelinecrm.application.view.UserViews;
import com.pipelinecrm.domain.shared.DomainException;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;

import java.util.Optional;

/**
 * Exchanges credentials for a token.
 *
 * <p>Every way of failing produces the same {@link AuthenticationFailed}: an unknown
 * address, a wrong password, and an address that is not even well formed. Distinguishing
 * them would let anyone discover which accounts exist.
 */
public final class SignInInteractor implements SignIn {

    private final UserRepository users;
    private final PasswordChecker passwords;
    private final AccessTokenIssuer tokens;

    public SignInInteractor(UserRepository users, PasswordChecker passwords, AccessTokenIssuer tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Override
    public AuthenticatedUser handle(Credentials credentials) {
        User user = userClaiming(credentials);
        if (!passwords.matches(user.id(), credentials.password())) {
            throw new AuthenticationFailed();
        }
        AccessTokenIssuer.IssuedToken token = tokens.issueFor(user);
        return new AuthenticatedUser(UserViews.of(user), token.value(), token.expiresAt());
    }

    private User userClaiming(Credentials credentials) {
        return addressIn(credentials)
                .flatMap(users::findByEmail)
                .orElseThrow(AuthenticationFailed::new);
    }

    private Optional<EmailAddress> addressIn(Credentials credentials) {
        try {
            return Optional.of(EmailAddress.of(credentials.email()));
        } catch (DomainException malformed) {
            return Optional.empty();
        }
    }
}
