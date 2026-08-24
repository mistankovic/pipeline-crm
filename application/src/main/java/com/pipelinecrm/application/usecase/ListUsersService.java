package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ListUsersUseCase;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.user.User;
import java.util.ArrayList;
import java.util.List;

public final class ListUsersService implements ListUsersUseCase {

    private final UserRepository users;

    public ListUsersService(UserRepository users) {
        this.users = users;
    }

    @Override
    public List<PublicUser> execute() {
        List<PublicUser> result = new ArrayList<>();
        for (User user : users.findAll()) {
            result.add(new PublicUser(user.id(), user.email().value(), user.name().value(), user.role().name()));
        }
        return result;
    }
}
