package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.UserMapping;
import com.pipelinecrm.adapter.persistence.repository.UserRows;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** The user port, over JPA. Query, map, return: there is nothing else a repository may do. */
@Repository
public class JpaUserRepository implements UserRepository {

    private final UserRows rows;

    public JpaUserRepository(UserRows rows) {
        this.rows = rows;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return rows.findById(id.value()).map(UserMapping::toDomain);
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return rows.findByEmail(email.value()).map(UserMapping::toDomain);
    }

    @Override
    public List<User> findAll() {
        return rows.findAll().stream().map(UserMapping::toDomain).toList();
    }

    @Override
    public List<User> findAllByIds(Collection<UserId> ids) {
        return rows.findAllById(ids.stream().map(UserId::value).toList()).stream()
                .map(UserMapping::toDomain).toList();
    }
}
