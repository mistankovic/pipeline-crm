package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.identity.UserId;
import java.util.List;

public interface ListUsersUseCase {

    List<PublicUser> execute();

    record PublicUser(UserId id, String email, String name, String role) {}
}
