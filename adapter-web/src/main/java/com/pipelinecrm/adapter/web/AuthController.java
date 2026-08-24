package com.pipelinecrm.adapter.web;

import com.pipelinecrm.adapter.web.dto.ApiDtos;
import com.pipelinecrm.application.port.in.LoginUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase login;

    public AuthController(LoginUseCase login) {
        this.login = login;
    }

    @PostMapping("/login")
    public ApiDtos.LoginResponse login(@Valid @RequestBody ApiDtos.LoginRequest request) {
        LoginUseCase.Result result = login.execute(new LoginUseCase.Command(request.email(), request.password()));
        return new ApiDtos.LoginResponse(result.token(), result.userId().toString());
    }
}
