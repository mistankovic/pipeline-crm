package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.application.port.out.Transactions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * The {@link Transactions} port, implemented with Spring's transaction template. This class
 * is the only place in the codebase that knows what "atomically" is made of.
 */
@Component
public class SpringTransactions implements Transactions {

    private final TransactionTemplate template;

    public SpringTransactions(TransactionTemplate template) {
        this.template = template;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
