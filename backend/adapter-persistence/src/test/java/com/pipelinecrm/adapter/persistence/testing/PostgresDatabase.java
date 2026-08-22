package com.pipelinecrm.adapter.persistence.testing;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.util.function.UnaryOperator;

/**
 * The PostgreSQL instance the integration tests run against.
 *
 * <p>By default a container is started with Testcontainers. If the environment already
 * provides a PostgreSQL — a CI service container, or a developer's local server — then
 * {@code PIPELINECRM_TEST_DB_URL} points at it and no container is started.
 *
 * <p>What is never allowed is substituting a different database engine. Tests that claim to
 * exercise PostgreSQL run on PostgreSQL; see CONSTITUTION.md section 3.
 */
public sealed interface PostgresDatabase permits ProvidedPostgres, ContainerisedPostgres {

    String URL_VARIABLE = "PIPELINECRM_TEST_DB_URL";
    String USER_VARIABLE = "PIPELINECRM_TEST_DB_USER";
    String PASSWORD_VARIABLE = "PIPELINECRM_TEST_DB_PASSWORD";

    String DEFAULT_CREDENTIAL = "pipelinecrm";

    static PostgresDatabase resolve() {
        PostgresDatabase provided = chooseFrom(System::getenv);
        return provided == null ? ContainerisedPostgres.started() : provided;
    }

    /**
     * The selection itself, over a lookup rather than over the real environment, so that it
     * can be tested. Returns null when nothing was provided and a container is called for;
     * starting one is the caller's business, and is not something a unit test can do.
     */
    static PostgresDatabase chooseFrom(UnaryOperator<String> variables) {
        String url = variables.apply(URL_VARIABLE);
        if (url == null || url.isBlank()) {
            return null;
        }
        return new ProvidedPostgres(url,
                orDefault(variables.apply(USER_VARIABLE)),
                orDefault(variables.apply(PASSWORD_VARIABLE)));
    }

    private static String orDefault(String value) {
        return value == null || value.isBlank() ? DEFAULT_CREDENTIAL : value;
    }

    void describeTo(DynamicPropertyRegistry registry);
}
