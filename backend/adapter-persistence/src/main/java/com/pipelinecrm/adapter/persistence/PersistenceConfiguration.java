package com.pipelinecrm.adapter.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Wires the persistence adapter. Nothing here is a business rule. */
@Configuration
@EnableJpaRepositories(basePackages = "com.pipelinecrm.adapter.persistence")
@EntityScan(basePackages = "com.pipelinecrm.adapter.persistence")
public class PersistenceConfiguration {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }
}
