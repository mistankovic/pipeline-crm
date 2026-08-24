package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.UserMapper;
import com.pipelinecrm.adapter.persistence.spring.SpringUserRepository;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JpaUserRepository implements UserRepository {

    private final SpringUserRepository users;

    public JpaUserRepository(SpringUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return users.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return users.findByEmail(email.value()).map(UserMapper::toDomain);
    }

    @Override
    public void save(User user) {
        users.save(UserMapper.toEntity(user));
    }
}
