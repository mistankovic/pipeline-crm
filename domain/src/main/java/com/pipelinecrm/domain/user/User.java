package com.pipelinecrm.domain.user;

import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.identity.UserRole;
import com.pipelinecrm.domain.shared.Guards;

public final class User {

    private final UserId id;
    private final Email email;
    private PersonName name;
    private final UserRole role;
    private final String passwordHash;

    private User(UserId id, Email email, PersonName name, UserRole role, String passwordHash) {
        this.id = Guards.notNull(id, "id");
        this.email = Guards.notNull(email, "email");
        this.name = Guards.notNull(name, "name");
        this.role = Guards.notNull(role, "role");
        this.passwordHash = Guards.notBlank(passwordHash, "passwordHash");
    }

    public static User register(
            UserId id, Email email, PersonName name, UserRole role, String passwordHash) {
        return new User(id, email, name, role, passwordHash);
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public PersonName name() {
        return name;
    }

    public UserRole role() {
        return role;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean isManager() {
        return role == UserRole.MANAGER;
    }

    public void rename(PersonName newName) {
        this.name = Guards.notNull(newName, "newName");
    }
}
