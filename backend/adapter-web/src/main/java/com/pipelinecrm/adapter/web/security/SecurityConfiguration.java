package com.pipelinecrm.adapter.web.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Who may reach which URL. Authorisation *within* the domain — whether this user may move
 * this deal — is not decided here; it is a business rule and lives in the domain.
 */
@Configuration
@EnableConfigurationProperties(JwtSettings.class)
public class SecurityConfiguration {

    private static final String PUBLIC_SESSIONS = "/api/sessions";
    private static final String PUBLIC_HEALTH = "/actuator/health/**";

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers(PUBLIC_SESSIONS, PUBLIC_HEALTH).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                // Without this Spring Security answers an anonymous caller with 403, which
                // says "you may not" when the truth is "I do not know who you are".
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
