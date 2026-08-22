package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.adapter.web.security.SignedInUser;
import com.pipelinecrm.application.port.in.ListUsers;
import com.pipelinecrm.application.view.UserView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Who the users are, and who the caller is. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ListUsers users;

    public UserController(ListUsers users) {
        this.users = users;
    }

    @GetMapping
    public List<UserView> all() {
        return users.handle();
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal SignedInUser caller) {
        return users.handle().stream()
                .filter(user -> user.id().equals(caller.id().value()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("the signed-in user no longer exists"));
    }
}
