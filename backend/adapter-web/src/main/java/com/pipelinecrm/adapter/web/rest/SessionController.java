package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.adapter.web.security.SecurityConfiguration;
import com.pipelinecrm.application.port.in.SignIn;
import com.pipelinecrm.application.view.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Signing in. The only route that does not require a token, by construction. */
@RestController
public class SessionController {

    private final SignIn signIn;

    public SessionController(SignIn signIn) {
        this.signIn = signIn;
    }

    /**
     * The path is the constant the security configuration permits, referenced rather than
     * repeated: if the two ever disagree, sign-in is protected by the token it exists to issue.
     */
    @PostMapping(SecurityConfiguration.SIGN_IN_ROUTE)
    public AuthenticatedUser open(@Valid @RequestBody SignInRequest request) {
        return signIn.handle(new SignIn.Credentials(request.email(), request.password()));
    }

    /** What a caller sends to sign in. */
    public record SignInRequest(@NotBlank String email, @NotBlank String password) {
    }
}
