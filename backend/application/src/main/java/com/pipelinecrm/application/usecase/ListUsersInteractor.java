package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ListUsers;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.application.view.UserView;
import com.pipelinecrm.application.view.UserViews;

import java.util.List;

/** Everyone. */
public final class ListUsersInteractor implements ListUsers {

    private final UserRepository users;

    public ListUsersInteractor(UserRepository users) {
        this.users = users;
    }

    @Override
    public List<UserView> handle() {
        return users.findAll().stream().map(UserViews::of).toList();
    }
}
