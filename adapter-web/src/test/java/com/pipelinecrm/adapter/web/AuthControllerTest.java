package com.pipelinecrm.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pipelinecrm.application.port.in.LoginUseCase;
import com.pipelinecrm.domain.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private LoginUseCase login;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(login)).build();
    }

    @Test
    void loginReturnsTokenFromUseCase() throws Exception {
        UserId userId = UserId.generate();
        when(login.execute(new LoginUseCase.Command("sales@pipelinecrm.demo", "password")))
                .thenReturn(new LoginUseCase.Result(userId, "jwt-token"));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sales@pipelinecrm.demo\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }
}
