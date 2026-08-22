package com.pipelinecrm.adapter.persistence.testing;

import org.springframework.test.context.DynamicPropertyRegistry;

/** A PostgreSQL the environment already runs and told us about. */
public record ProvidedPostgres(String url, String username, String password) implements PostgresDatabase {

    @Override
    public void describeTo(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
    }
}
