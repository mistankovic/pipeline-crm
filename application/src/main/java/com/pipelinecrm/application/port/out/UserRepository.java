package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    List<User> findAll();

    void save(User user);
}
