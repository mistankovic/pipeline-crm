package com.pipelinecrm.adapter.web.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtSettings.class)
public class WebSecurityConfiguration {}
