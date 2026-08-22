package com.pipelinecrm.adapter.persistence.testing;

import org.springframework.test.context.DynamicPropertyRegistry;

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

    static PostgresDatabase resolve() {
        String url = System.getenv(URL_VARIABLE);
        if (url == null || url.isBlank()) {
            return ContainerisedPostgres.started();
        }
        return new ProvidedPostgres(url, environment(USER_VARIABLE, "pipelinecrm"),
                environment(PASSWORD_VARIABLE, "pipelinecrm"));
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    void describeTo(DynamicPropertyRegistry registry);
}
