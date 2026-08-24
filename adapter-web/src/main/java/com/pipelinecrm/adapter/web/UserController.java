package com.pipelinecrm.adapter.web;

import com.pipelinecrm.adapter.web.dto.ApiDtos;
import com.pipelinecrm.adapter.web.dto.Responses;
import com.pipelinecrm.application.port.in.ListUsersUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ListUsersUseCase listUsers;

    public UserController(ListUsersUseCase listUsers) {
        this.listUsers = listUsers;
    }

    @GetMapping
    public List<ApiDtos.UserResponse> list() {
        return listUsers.execute().stream().map(Responses::user).toList();
    }
}
