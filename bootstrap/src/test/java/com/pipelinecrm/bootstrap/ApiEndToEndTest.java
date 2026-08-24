package com.pipelinecrm.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        classes = PipelineCrmApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiEndToEndTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    @Test
    void unauthenticatedDealsAreRejected() {
        ResponseEntity<String> response = http.getForEntity(url("/api/deals"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginThenCreateCompany() {
        ResponseEntity<Map> login = http.postForEntity(
                url("/api/auth/login"),
                Map.of("email", DemoDataSeeder.SALES_EMAIL, "password", DemoDataSeeder.DEMO_PASSWORD),
                Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = String.valueOf(login.getBody().get("token"));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> created = http.exchange(
                url("/api/companies"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Acme"), headers),
                Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("id")).isNotNull();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
