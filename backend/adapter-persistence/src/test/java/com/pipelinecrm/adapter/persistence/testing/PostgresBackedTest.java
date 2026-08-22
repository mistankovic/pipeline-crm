package com.pipelinecrm.adapter.persistence.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for every test that needs a real database. One instance is resolved for the
 * whole JVM, so a build starts at most one container however many test classes there are.
 */
public abstract class PostgresBackedTest {

    private static final PostgresDatabase DATABASE = PostgresDatabase.resolve();

    @DynamicPropertySource
    static void useTheResolvedDatabase(DynamicPropertyRegistry registry) {
        DATABASE.describeTo(registry);
    }
}
