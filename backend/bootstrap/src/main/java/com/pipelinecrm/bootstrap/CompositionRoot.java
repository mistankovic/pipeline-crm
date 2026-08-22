package com.pipelinecrm.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
