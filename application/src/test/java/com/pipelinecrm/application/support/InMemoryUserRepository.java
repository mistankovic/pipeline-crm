package com.pipelinecrm.application.support;

import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> byId = new LinkedHashMap<>();
    private final Map<Email, User> byEmail = new LinkedHashMap<>();

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return Optional.ofNullable(byEmail.get(email));
    }

    @Override
    public void save(User user) {
        byId.put(user.id(), user);
        byEmail.put(user.email(), user);
    }
}
