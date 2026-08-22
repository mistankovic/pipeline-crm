package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryUsers implements UserRepository {

    private final Map<UserId, User> stored = new LinkedHashMap<>();

    public User add(User user) {
        stored.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return stored.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(stored.values());
    }
}
