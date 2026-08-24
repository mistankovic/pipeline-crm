package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.entity.UserEntity;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.identity.UserRole;
import com.pipelinecrm.domain.user.User;

public final class UserMapper {

    private UserMapper() {}

    public static User toDomain(UserEntity entity) {
        return User.register(
                new UserId(entity.id()),
                Email.of(entity.email()),
                PersonName.of(entity.name()),
                UserRole.valueOf(entity.role()),
                entity.passwordHash());
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.id().value(),
                user.email().value(),
                user.name().value(),
                user.role().name(),
                user.passwordHash());
    }
}
