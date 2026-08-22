package com.pipelinecrm.bootstrap.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionApiTest extends ApiTest {

    private org.springframework.test.web.servlet.ResultActions signIn(String email, String password)
            throws Exception {
        return http.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}""".formatted(email, password)));
    }

    @Test
    void a_correct_sign_in_returns_a_token_and_the_user() throws Exception {
        signIn(SAM, SAM_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(SAM))
                .andExpect(jsonPath("$.user.role").value("SALES"));
    }

    @Test
    void a_sign_in_response_never_contains_a_hash() throws Exception {
        signIn(SAM, SAM_PASSWORD).andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void a_wrong_password_is_401() throws Exception {
        signIn(SAM, "guess")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AuthenticationFailed"));
    }

    @Test
    void an_unknown_address_is_401_with_the_identical_body() throws Exception {
        signIn("nobody@pipelinecrm.demo", SAM_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("email address or password is incorrect"));
    }

    @Test
    void a_missing_password_is_400_not_401() throws Exception {
        http.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "sam@pipelinecrm.demo"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidRequest"));
    }

    @Test
    void the_board_cannot_be_read_without_a_token() throws Exception {
        http.perform(get("/api/deals")).andExpect(status().isUnauthorized());
    }

    @Test
    void the_board_cannot_be_read_with_a_forged_token() throws Exception {
        http.perform(get("/api/deals").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void the_board_can_be_read_with_a_real_token() throws Exception {
        http.perform(as(get("/api/deals"), sam())).andExpect(status().isOk());
    }

    @Test
    void the_sign_in_response_tells_the_browser_everything_it_needs_about_the_caller() throws Exception {
        signIn(SAM, SAM_PASSWORD)
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Sam Sales"))
                .andExpect(jsonPath("$.user.email").value(SAM))
                .andExpect(jsonPath("$.user.role").value("SALES"));
    }
}
