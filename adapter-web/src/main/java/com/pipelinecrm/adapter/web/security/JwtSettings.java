package com.pipelinecrm.adapter.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pipelinecrm.jwt")
public class JwtSettings {

    private String secret = "demo-only-change-me-please-use-32-bytes!";
    private long ttlSeconds = 3600;

    public String secret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
