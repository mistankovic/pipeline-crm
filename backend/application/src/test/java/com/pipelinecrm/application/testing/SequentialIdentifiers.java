package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.IdentifierFactory;

import java.util.UUID;

/** Predictable identities, so an assertion can name the thing that was just created. */
public final class SequentialIdentifiers implements IdentifierFactory {

    private int issued;

    @Override
    public UUID newIdentifier() {
        issued++;
        return UUID.fromString("00000000-0000-4000-8000-%012d".formatted(issued));
    }

    public UUID lastIssued() {
        return UUID.fromString("00000000-0000-4000-8000-%012d".formatted(issued));
    }
}
