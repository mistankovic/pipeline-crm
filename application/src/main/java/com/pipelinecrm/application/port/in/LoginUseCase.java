package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.identity.UserId;

public interface LoginUseCase {

    Result execute(Command command);

    record Command(String email, String password) {}

    record Result(UserId userId, String token) {}
}
