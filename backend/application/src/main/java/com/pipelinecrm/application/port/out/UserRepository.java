package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;

import java.util.List;
import java.util.Optional;

/** How the use cases reach users. */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(EmailAddress email);

    List<User> findAll();
}
