package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.Transactions;

import java.util.function.Supplier;

/** Runs the work at once and counts the calls, so a test can assert a use case asked for one. */
public final class ImmediateTransactions implements Transactions {

    private int started;

    @Override
    public <T> T execute(Supplier<T> work) {
        started++;
        return work.get();
    }

    public int started() {
        return started;
    }
}
