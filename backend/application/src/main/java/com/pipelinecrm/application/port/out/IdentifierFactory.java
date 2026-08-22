package com.pipelinecrm.application.port.out;

import java.util.UUID;

/**
 * Where new identities come from. A port rather than a call to UUID.randomUUID, so that a
 * test can make identities predictable and an assertion can name them.
 */
public interface IdentifierFactory {

    UUID newIdentifier();
}
