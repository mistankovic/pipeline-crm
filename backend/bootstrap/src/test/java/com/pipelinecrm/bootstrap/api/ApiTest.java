package com.pipelinecrm.bootstrap.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base for the API tests: a full application over a real database, driven over HTTP with no
 * security shortcuts. Tokens are obtained by signing in, exactly as a browser would, so an
 * accidentally broken sign-in cannot hide behind a mocked principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTest extends PostgresBackedTest {

    static final String SAM = "sam@pipelinecrm.demo";
    static final String ROBIN = "robin@pipelinecrm.demo";
    static final String MO = "mo@pipelinecrm.demo";
    static final String SAM_PASSWORD = "sam-password";
    static final String ROBIN_PASSWORD = "robin-password";
    static final String MO_PASSWORD = "mo-password";

    @Autowired
    protected MockMvc http;

    @Autowired
    protected ObjectMapper json;

    private String samsToken;

    @BeforeEach
    void signInAsSam() throws Exception {
        samsToken = tokenFor(SAM, SAM_PASSWORD);
    }

    protected String sam() {
        return samsToken;
    }

    protected String tokenFor(String email, String password) throws Exception {
        MvcResult result = http.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}""".formatted(email, password)))
                .andReturn();
        return read(result).get("token").asText();
    }

    protected MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
