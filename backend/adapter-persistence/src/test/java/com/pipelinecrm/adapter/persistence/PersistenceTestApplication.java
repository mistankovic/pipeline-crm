package com.pipelinecrm.adapter.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A Spring Boot application that exists only so these tests can start a context holding this
 * adapter and nothing else. Testing the persistence adapter through the real composition root
 * would drag in the web layer and stop telling us anything specific.
 *
 * <p>It has to declare the password encoder, because the adapter does not: that bean belongs
 * to whoever composes the system. This class is playing that role for one slice of it.
 */
@SpringBootApplication(scanBasePackages = "com.pipelinecrm.adapter.persistence")
public class PersistenceTestApplication {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
