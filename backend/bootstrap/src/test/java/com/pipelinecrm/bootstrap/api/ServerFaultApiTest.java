package com.pipelinecrm.bootstrap.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What a signed-in caller is told when the server genuinely breaks.
 *
 * <p>It used to be **401**. An unhandled failure left the dispatcher, the container
 * re-dispatched to {@code /error}, that dispatch was anonymous, and the security rules answered
 * it with "you are not authenticated" — so a numeric overflow logged people out, because the
 * browser correctly ends a session on a 401. See docs/reviews/stage-7-review.md, finding F-7.1.
 *
 * <p>This test provokes a genuinely unexpected failure through a controller that exists only
 * here, because provoking one through the real API means finding a bug — and the whole point is
 * that there should not be one to find.
 */
@Import(ServerFaultApiTest.ExplodingController.class)
class ServerFaultApiTest extends ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void an_unexpected_failure_is_500_and_not_401() throws Exception {
        mockMvc.perform(as(get("/api/explode"), sam()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("InternalError"));
    }

    @Test
    void an_unexpected_failure_tells_the_caller_nothing_about_itself() throws Exception {
        mockMvc.perform(as(get("/api/explode"), sam()).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("the request could not be completed"));
    }

    @Test
    void an_anonymous_caller_is_still_refused_with_401() throws Exception {
        mockMvc.perform(get("/api/explode")).andExpect(status().isUnauthorized());
    }

    /** Exists only to fail. Registered for this test class alone. */
    @TestConfiguration
    @RestController
    static class ExplodingController {

        @GetMapping("/api/explode")
        public String explode() {
            throw new IllegalStateException("a failure nobody planned for");
        }
    }
}
