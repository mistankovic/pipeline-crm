package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate runs with {@code ddl-auto: validate}, so this context starting at all is the
 * assertion: every column the mapping expects exists in the schema Flyway built, with a
 * compatible type. A mapping that drifts from a migration fails here rather than in
 * production.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
class SchemaMatchesTheMappingTest extends PostgresBackedTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void the_migrations_created_every_table_the_mapping_needs() {
        assertThat(tableNames()).contains("users", "companies", "contacts", "deals", "activities");
    }

    @Test
    void the_demo_users_were_seeded() {
        Integer users = jdbc.queryForObject("select count(*) from users", Integer.class);

        assertThat(users).isEqualTo(3);
    }

    @Test
    void an_activity_about_nothing_is_refused_by_the_database_as_well_as_by_the_domain() {
        assertThat(insertActivityWith(null, null)).isFalse();
    }

    private boolean insertActivityWith(String deal, String contact) {
        try {
            jdbc.update("""
                    insert into activities (id, deal_id, contact_id, type, summary, author_id, occurred_at)
                    values (gen_random_uuid(), ?::uuid, ?::uuid, 'NOTE', 'x',
                            '11111111-1111-4111-8111-111111111111', now())
                    """, deal, contact);
            return true;
        } catch (RuntimeException refused) {
            return false;
        }
    }

    private java.util.List<String> tableNames() {
        return jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
    }
}
