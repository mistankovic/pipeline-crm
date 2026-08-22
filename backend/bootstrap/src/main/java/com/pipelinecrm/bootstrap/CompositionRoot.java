package com.pipelinecrm.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

/**
 * Beans that belong to no single adapter.
 *
 * <p>The clock lives here rather than in an adapter. It was briefly declared by the
 * persistence adapter and consumed by the web adapter, which coupled the two through the
 * Spring context — a dependency no compiler and no ArchUnit rule can see, because it exists
 * only at wiring time. Deciding what the layers are wired to is this layer's whole job.
 * See docs/reviews/stage-2-review.md, finding F-2.4.
 */
@Configuration
public class CompositionRoot {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    /**
     * Password hashing is used by the persistence adapter, which owns the stored hash, and it
     * was briefly declared by the web adapter. That is the same invisible coupling as the
     * clock: the Stage 2 review predicted it would recur here, so the bean is declared where
     * wiring belongs. See docs/reviews/stage-2-review.md, round 2.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
