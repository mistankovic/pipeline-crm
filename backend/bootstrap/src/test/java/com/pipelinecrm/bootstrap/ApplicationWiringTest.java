package com.pipelinecrm.bootstrap;

import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import com.pipelinecrm.application.port.out.AccessTokenIssuer;
import com.pipelinecrm.application.port.out.Transactions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 2's claim is that the technology is wired and the layers meet correctly. This test
 * makes the claim falsifiable: the context starts against a real PostgreSQL, the output
 * ports have exactly one implementation each, and an unauthenticated request to a protected
 * URL is refused.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationWiringTest extends PostgresBackedTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Transactions transactions;

    @Autowired
    private AccessTokenIssuer tokenIssuer;

    @Autowired
    private TestRestTemplate http;

    @Test
    void the_application_talks_to_a_real_postgresql() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select version()")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).startsWith("PostgreSQL");
        }
    }

    @Test
    void the_transactions_port_is_wired_to_a_real_transaction_manager() {
        assertThat(transactions.execute(() -> "committed")).isEqualTo("committed");
    }

    @Test
    void the_token_issuer_port_has_an_implementation() {
        assertThat(tokenIssuer).isNotNull();
    }

    @Test
    void health_is_public() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void an_unauthenticated_request_to_a_protected_url_is_refused() {
        ResponseEntity<String> response = http.getForEntity("/api/deals", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
