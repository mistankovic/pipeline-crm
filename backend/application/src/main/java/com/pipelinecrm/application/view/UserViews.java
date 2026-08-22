package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.user.User;

/** A user as a caller sees one. Never carries a password or a hash. */
public final class UserViews {

    private UserViews() {
    }

    public static UserView of(User user) {
        return new UserView(user.id().value(), user.email().value(), user.name(), user.role().name());
    }
}
