package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.application.port.in.ListUsers;
import com.pipelinecrm.application.view.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Who the users are, for the owner picker and the board's owner filter.
 *
 * <p>There is no {@code /me}. There was, and it read every user in order to find one — while
 * the sign-in response already hands the browser its own {@code UserView}. The endpoint existed
 * because it sounded like an endpoint should exist. See docs/reviews/stage-5-review.md,
 * finding F-5.4.
 */
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
}
