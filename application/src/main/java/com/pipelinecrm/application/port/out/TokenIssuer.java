package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.user.User;

public interface TokenIssuer {

    String issue(User user);
}
