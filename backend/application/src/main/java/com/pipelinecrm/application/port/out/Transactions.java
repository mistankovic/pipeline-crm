package com.pipelinecrm.application.port.out;

import java.util.function.Supplier;

/**
 * Runs a unit of work atomically. A use case that reads and then writes says so by wrapping
 * itself in this port; the meaning of "atomically" belongs to the infrastructure that
 * implements it, and the application layer never mentions a framework to say it.
 */
public interface Transactions {

    <T> T execute(Supplier<T> work);
}
