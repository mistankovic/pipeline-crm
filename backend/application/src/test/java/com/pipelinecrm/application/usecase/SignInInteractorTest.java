package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.AuthenticationFailed;
import com.pipelinecrm.application.port.in.SignIn;
import com.pipelinecrm.application.testing.Seed;
import com.pipelinecrm.application.testing.StubTokens;
import com.pipelinecrm.application.testing.UseCases;
import com.pipelinecrm.application.view.AuthenticatedUser;
import com.pipelinecrm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignInInteractorTest {

    private static final String RIGHT_PASSWORD = "correct-horse";

    private final UseCases application = new UseCases();
    private final Seed seed = new Seed(application);
    private User sam;

    @BeforeEach
    void seedSam() {
        sam = seed.salesperson("Sam");
        application.passwords.set(sam.id(), RIGHT_PASSWORD);
    }

    private AuthenticatedUser signIn(String email, String password) {
        return application.signIn.handle(new SignIn.Credentials(email, password));
    }

    @Test
    void returns_a_token_for_the_right_password() {
        AuthenticatedUser signedIn = signIn("sam@example.com", RIGHT_PASSWORD);

        assertThat(signedIn.token()).isEqualTo("token-for-" + sam.id().value());
        assertThat(signedIn.expiresAt()).isEqualTo(StubTokens.EXPIRY);
    }

    @Test
    void describes_the_user_who_signed_in() {
        AuthenticatedUser signedIn = signIn("sam@example.com", RIGHT_PASSWORD);

        assertThat(signedIn.user().id()).isEqualTo(sam.id().value());
        assertThat(signedIn.user().name()).isEqualTo("Sam");
        assertThat(signedIn.user().role()).isEqualTo("SALES");
    }

    @Test
    void matches_the_address_whatever_case_it_was_typed_in() {
        assertThat(signIn("SAM@Example.COM", RIGHT_PASSWORD).user().id()).isEqualTo(sam.id().value());
    }

    @Test
    void refuses_a_wrong_password() {
        assertThatThrownBy(() -> signIn("sam@example.com", "guess"))
                .isInstanceOf(AuthenticationFailed.class)
                .hasMessage("email address or password is incorrect");
    }

    @Test
    void refuses_an_unknown_address_with_exactly_the_same_message() {
        assertThatThrownBy(() -> signIn("nobody@example.com", RIGHT_PASSWORD))
                .isInstanceOf(AuthenticationFailed.class)
                .hasMessage("email address or password is incorrect");
    }

    @Test
    void refuses_an_address_that_is_not_an_address_without_saying_so() {
        assertThatThrownBy(() -> signIn("not-an-address", RIGHT_PASSWORD))
                .isInstanceOf(AuthenticationFailed.class)
                .hasMessage("email address or password is incorrect");
    }

    @Test
    void refuses_a_blank_address_the_same_way() {
        assertThatThrownBy(() -> signIn("   ", RIGHT_PASSWORD)).isInstanceOf(AuthenticationFailed.class);
    }

    @Test
    void refuses_a_missing_address_the_same_way() {
        assertThatThrownBy(() -> signIn(null, RIGHT_PASSWORD)).isInstanceOf(AuthenticationFailed.class);
    }

    @Test
    void an_unknown_address_still_costs_a_password_check_so_that_it_cannot_be_timed() {
        int before = application.passwords.comparisons();

        assertThatThrownBy(() -> signIn("nobody@example.com", RIGHT_PASSWORD))
                .isInstanceOf(AuthenticationFailed.class);

        assertThat(application.passwords.comparisons())
                .describedAs("an unknown address must do the same work as a known one, "
                        + "or the difference is measurable with a stopwatch")
                .isEqualTo(before + 1);
    }

    @Test
    void a_malformed_address_costs_the_same_check_as_well() {
        int before = application.passwords.comparisons();

        assertThatThrownBy(() -> signIn("not-an-address", RIGHT_PASSWORD))
                .isInstanceOf(AuthenticationFailed.class);

        assertThat(application.passwords.comparisons()).isEqualTo(before + 1);
    }

    @Test
    void a_wrong_password_costs_exactly_one_check_too() {
        int before = application.passwords.comparisons();

        assertThatThrownBy(() -> signIn("sam@example.com", "guess"))
                .isInstanceOf(AuthenticationFailed.class);

        assertThat(application.passwords.comparisons()).isEqualTo(before + 1);
    }

    @Test
    void never_reaches_the_token_issuer_for_a_failed_sign_in() {
        assertThatThrownBy(() -> signIn("sam@example.com", "guess")).isInstanceOf(AuthenticationFailed.class);
        assertThatThrownBy(() -> signIn("nobody@example.com", "x")).isInstanceOf(AuthenticationFailed.class);
    }
}
