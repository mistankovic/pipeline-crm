package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import com.pipelinecrm.application.port.out.PasswordChecker;
import com.pipelinecrm.domain.identity.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Against the real hashes in the seed migration, not against a stub. */
@SpringBootTest(classes = PersistenceTestApplication.class)
class BcryptPasswordCheckerTest extends PostgresBackedTest {

    private static final UserId SAM = UserId.of(UUID.fromString("11111111-1111-4111-8111-111111111111"));

    @Autowired
    private PasswordChecker passwords;

    @Test
    void accepts_the_seeded_password() {
        assertThat(passwords.matches(SAM, "sam-password")).isTrue();
    }

    @Test
    void refuses_a_wrong_password() {
        assertThat(passwords.matches(SAM, "not-sams-password")).isFalse();
    }

    @Test
    void refuses_another_users_password() {
        assertThat(passwords.matches(SAM, "mo-password")).isFalse();
    }

    @Test
    void refuses_an_empty_password() {
        assertThat(passwords.matches(SAM, "")).isFalse();
    }

    @Test
    void refuses_a_missing_password_rather_than_failing() {
        assertThat(passwords.matches(SAM, null)).isFalse();
    }

    @Test
    void refuses_a_user_who_does_not_exist_rather_than_failing() {
        assertThat(passwords.matches(UserId.of(UUID.randomUUID()), "sam-password")).isFalse();
    }
}
