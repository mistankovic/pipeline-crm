package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.error.AuthenticationFailed;
import com.pipelinecrm.application.port.in.SignIn;
import com.pipelinecrm.application.view.AuthenticatedUser;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** Signing in, and the deliberately indistinguishable ways of failing to. */
public class SignInSteps {

    private static final String THE_ONLY_FAILURE_MESSAGE = "email address or password is incorrect";

    private final World world;
    private AuthenticatedUser signedIn;

    public SignInSteps(World world) {
        this.world = world;
    }

    @When("{string} signs in with password {string}")
    public void signsInWithPassword(String email, String password) {
        signedIn = null;
        world.attempt(() -> {
            signedIn = world.application.signIn.handle(new SignIn.Credentials(email, password));
        });
    }

    @Then("the sign-in succeeds and returns a token for {word}")
    public void theSignInSucceedsFor(String person) {
        assertThat(signedIn).describedAs("sign-in should have succeeded").isNotNull();
        assertThat(signedIn.user().id()).isEqualTo(world.person(person));
        assertThat(signedIn.token()).isNotBlank();
        assertThat(signedIn.expiresAt()).isNotNull();
    }

    @Then("the sign-in is refused with the same message as an unknown user")
    public void theSignInIsRefused() {
        world.expectRefusal(AuthenticationFailed.class, THE_ONLY_FAILURE_MESSAGE);
        assertThat(signedIn).isNull();
    }
}
