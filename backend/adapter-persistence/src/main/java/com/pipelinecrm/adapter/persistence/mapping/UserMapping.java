package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.row.UserRow;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;
import com.pipelinecrm.domain.user.UserRole;

/**
 * Rows in, entities out.
 *
 * <p>All the mapping lives on this side of the boundary. The domain has no annotations, no
 * getters it would not otherwise have, and no idea that a row exists. Users are read only:
 * they are provisioned by a migration, so there is no entity-to-row direction here.
 */
public final class UserMapping {

    private UserMapping() {
    }

    public static User toDomain(UserRow row) {
        return new User(UserId.of(row.getId()), EmailAddress.of(row.getEmail()),
                row.getName(), UserRole.valueOf(row.getRole()));
    }
}
