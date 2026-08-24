package com.pipelinecrm.adapter.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.pipelinecrm.adapter.persistence")
@EnableJpaRepositories(basePackages = "com.pipelinecrm.adapter.persistence")
public class PersistenceJpaConfig {}
