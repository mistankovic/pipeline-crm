package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.LoginUseCase;
import com.pipelinecrm.application.port.out.PasswordHasher;
import com.pipelinecrm.application.port.out.TokenIssuer;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.user.User;

public final class LoginService implements LoginUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;

    public LoginService(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    @Override
    public Result execute(Command command) {
        User user = users.findByEmail(Email.of(command.email())).orElseThrow(() -> new NotFoundException("user"));
        if (!hasher.matches(command.password(), user.passwordHash())) {
            throw new NotFoundException("user");
        }
        return new Result(user.id(), tokens.issue(user));
    }
}
