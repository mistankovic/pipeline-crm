package com.pipelinecrm.adapter.persistence.testing;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The choice between a container and a provided database decides what every integration test
 * in this build runs against. It is pure logic over two environment variables, so it is
 * pinned here even though the container itself can only be started where Docker can pull.
 *
 * <p>The resolution is exercised through a seam rather than by setting real environment
 * variables, which a JVM cannot do to itself.
 */
class PostgresDatabaseTest {

    private static final String A_URL = "jdbc:postgresql://db.example:5432/pipelinecrm";

    @Test
    void a_provided_database_describes_itself_with_the_given_url() {
        Map<String, String> recorded = describe(new ProvidedPostgres(A_URL, "sam", "secret"));

        assertThat(recorded)
                .containsEntry("spring.datasource.url", A_URL)
                .containsEntry("spring.datasource.username", "sam")
                .containsEntry("spring.datasource.password", "secret");
    }

    @Test
    void an_absent_url_variable_means_start_a_container() {
        assertThat(PostgresDatabase.chooseFrom(name -> null)).isNull();
    }

    @Test
    void a_blank_url_variable_means_start_a_container_too() {
        assertThat(PostgresDatabase.chooseFrom(name -> "   ")).isNull();
    }

    @Test
    void a_url_variable_selects_the_provided_database() {
        PostgresDatabase resolved = PostgresDatabase.chooseFrom(variables(Map.of(
                PostgresDatabase.URL_VARIABLE, A_URL,
                PostgresDatabase.USER_VARIABLE, "sam",
                PostgresDatabase.PASSWORD_VARIABLE, "secret")));

        assertThat(resolved).isEqualTo(new ProvidedPostgres(A_URL, "sam", "secret"));
    }

    @Test
    void the_credentials_fall_back_to_the_documented_defaults() {
        PostgresDatabase resolved =
                PostgresDatabase.chooseFrom(variables(Map.of(PostgresDatabase.URL_VARIABLE, A_URL)));

        assertThat(resolved).isEqualTo(new ProvidedPostgres(A_URL, "pipelinecrm", "pipelinecrm"));
    }

    @Test
    void a_blank_credential_falls_back_as_well() {
        PostgresDatabase resolved = PostgresDatabase.chooseFrom(variables(Map.of(
                PostgresDatabase.URL_VARIABLE, A_URL,
                PostgresDatabase.USER_VARIABLE, "  ")));

        assertThat(resolved).isEqualTo(new ProvidedPostgres(A_URL, "pipelinecrm", "pipelinecrm"));
    }

    private static java.util.function.UnaryOperator<String> variables(Map<String, String> values) {
        return values::get;
    }

    private static Map<String, String> describe(PostgresDatabase database) {
        Map<String, String> recorded = new LinkedHashMap<>();
        database.describeTo(new DynamicPropertyRegistry() {
            @Override
            public void add(String name, Supplier<Object> value) {
                recorded.put(name, String.valueOf(value.get()));
            }
        });
        return recorded;
    }
}
