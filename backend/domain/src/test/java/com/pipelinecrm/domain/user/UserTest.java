package com.pipelinecrm.domain.user;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final UserId AN_ID = UserId.of(UUID.randomUUID());
    private static final EmailAddress AN_EMAIL = EmailAddress.of("sam@example.com");

    @Test
    void a_manager_has_authority_over_any_deal() {
        User manager = new User(AN_ID, AN_EMAIL, "Mo", UserRole.MANAGER);

        assertThat(manager.isManager()).isTrue();
    }

    @Test
    void a_salesperson_does_not() {
        User salesperson = new User(AN_ID, AN_EMAIL, "Sam", UserRole.SALES);

        assertThat(salesperson.isManager()).isFalse();
    }

    @Test
    void recognises_its_own_identity() {
        User user = new User(AN_ID, AN_EMAIL, "Sam", UserRole.SALES);

        assertThat(user.is(AN_ID)).isTrue();
    }

    @Test
    void does_not_recognise_somebody_else() {
        User user = new User(AN_ID, AN_EMAIL, "Sam", UserRole.SALES);

        assertThat(user.is(UserId.of(UUID.randomUUID()))).isFalse();
    }

    @Test
    void trims_the_name() {
        assertThat(new User(AN_ID, AN_EMAIL, "  Sam  ", UserRole.SALES).name()).isEqualTo("Sam");
    }

    @Test
    void refuses_to_exist_without_an_id() {
        assertThatThrownBy(() -> new User(null, AN_EMAIL, "Sam", UserRole.SALES))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_an_email() {
        assertThatThrownBy(() -> new User(AN_ID, null, "Sam", UserRole.SALES))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_name() {
        assertThatThrownBy(() -> new User(AN_ID, AN_EMAIL, " ", UserRole.SALES))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_role() {
        assertThatThrownBy(() -> new User(AN_ID, AN_EMAIL, "Sam", null))
                .isInstanceOf(InvariantViolation.class);
    }
}
