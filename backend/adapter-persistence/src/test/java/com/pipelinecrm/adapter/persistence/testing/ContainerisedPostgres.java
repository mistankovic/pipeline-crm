package com.pipelinecrm.adapter.persistence.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** A PostgreSQL this test run starts for itself. The default when nothing is provided. */
public final class ContainerisedPostgres implements PostgresDatabase {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16-alpine");

    private final PostgreSQLContainer<?> container;

    private ContainerisedPostgres(PostgreSQLContainer<?> container) {
        this.container = container;
    }

    static ContainerisedPostgres started() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName("pipelinecrm")
                .withUsername("pipelinecrm")
                .withPassword("pipelinecrm");
        container.start();
        return new ContainerisedPostgres(container);
    }

    @Override
    public void describeTo(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }
}
