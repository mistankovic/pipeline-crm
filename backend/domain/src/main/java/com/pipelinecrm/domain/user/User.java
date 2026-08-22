package com.pipelinecrm.domain.user;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.shared.Guard;

/**
 * A person using the system. Modelled as an immutable record: nothing in this demo
 * changes a user in place, so structural equality is also identity equality.
 */
public record User(UserId id, EmailAddress email, String name, UserRole role) {

    public User {
        Guard.present(id, "user id");
        Guard.present(email, "user email");
        name = Guard.filled(name, "user name");
        Guard.present(role, "user role");
    }

    public boolean isManager() {
        return role.grantsAuthorityOverAnyDeal();
    }

    public boolean is(UserId other) {
        return id.equals(other);
    }
}
