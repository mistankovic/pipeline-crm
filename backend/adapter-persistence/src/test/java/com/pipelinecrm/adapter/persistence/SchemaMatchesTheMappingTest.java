package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hibernate runs with {@code ddl-auto: validate}, so this context starting at all is the
 * assertion that every column the mapping expects exists in the schema Flyway built, with a
 * compatible type. The rest of the class checks the constraints that back up the domain's own
 * rules.
 *
 * <p>Each rejection is asserted **by the name of the constraint that fired**. An earlier
 * version only checked that something threw, and passed when the row was rejected for an
 * entirely different reason — see docs/reviews/stage-4-review.md, finding F-4.1.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
class SchemaMatchesTheMappingTest extends PostgresBackedTest {

    private static final String SAM = "11111111-1111-4111-8111-111111111111";
    private static final String ONE_SUBJECT_ONLY = "activities_have_exactly_one_subject";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void the_migrations_created_every_table_the_mapping_needs() {
        assertThat(tableNames()).contains("users", "companies", "contacts", "deals", "activities");
    }

    @Test
    void the_demo_users_were_seeded() {
        assertThat(jdbc.queryForList("select email from users", String.class))
                .contains("sam@pipelinecrm.demo", "robin@pipelinecrm.demo", "mo@pipelinecrm.demo");
    }

    @Test
    void an_activity_about_exactly_one_deal_is_accepted() {
        assertThatCode(() -> insertActivity(aDeal(), null, "NOTE")).doesNotThrowAnyException();
    }

    @Test
    void an_activity_about_nothing_is_refused_by_the_constraint_that_says_so() {
        assertThatThrownBy(() -> insertActivity(null, null, "NOTE"))
                .hasMessageContaining(ONE_SUBJECT_ONLY);
    }

    @Test
    void an_activity_about_both_a_deal_and_a_contact_is_refused_by_the_same_constraint() {
        assertThatThrownBy(() -> insertActivity(aDeal(), aContact(), "NOTE"))
                .hasMessageContaining(ONE_SUBJECT_ONLY);
    }

    @Test
    void an_activity_of_an_unknown_type_is_refused_by_the_type_constraint() {
        assertThatThrownBy(() -> insertActivity(aDeal(), null, "SMOKE_SIGNAL"))
                .hasMessageContaining("activities_type_is_known");
    }

    @Test
    void a_deal_worth_less_than_nothing_is_refused() {
        assertThatThrownBy(() -> insertDeal("-1", 50, "LEAD"))
                .hasMessageContaining("deals_value_is_not_negative");
    }

    @Test
    void a_deal_with_an_impossible_probability_is_refused() {
        assertThatThrownBy(() -> insertDeal("1", 101, "LEAD"))
                .hasMessageContaining("deals_probability_is_a_percentage");
    }

    @Test
    void a_deal_in_a_stage_that_does_not_exist_is_refused() {
        assertThatThrownBy(() -> insertDeal("1", 50, "DAYDREAMING"))
                .hasMessageContaining("deals_stage_is_known");
    }

    private void insertActivity(String deal, String contact, String type) {
        jdbc.update("""
                insert into activities (id, deal_id, contact_id, type, summary, author_id, occurred_at)
                values (?::uuid, ?::uuid, ?::uuid, ?, 'recorded', ?::uuid, now())
                """, UUID.randomUUID().toString(), deal, contact, type, SAM);
    }

    private void insertDeal(String amount, int probability, String stage) {
        jdbc.update("""
                insert into deals (id, title, company_id, owner_id, value_amount, value_currency,
                                   probability, stage)
                values (?::uuid, 'probe', ?::uuid, ?::uuid, ?::numeric, 'EUR', ?, ?)
                """, UUID.randomUUID().toString(), aCompany(), SAM, amount, probability, stage);
    }

    private String aCompany() {
        String id = UUID.randomUUID().toString();
        jdbc.update("insert into companies (id, name) values (?::uuid, 'Probe')", id);
        return id;
    }

    private String aDeal() {
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                insert into deals (id, title, company_id, owner_id, value_amount, value_currency,
                                   probability, stage)
                values (?::uuid, 'probe', ?::uuid, ?::uuid, 1, 'EUR', 50, 'LEAD')
                """, id, aCompany(), SAM);
        return id;
    }

    private String aContact() {
        String id = UUID.randomUUID().toString();
        jdbc.update("insert into contacts (id, company_id, name, email) values (?::uuid, ?::uuid, 'Probe', 'p@x.test')",
                id, aCompany());
        return id;
    }

    private List<String> tableNames() {
        return jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
    }
}
