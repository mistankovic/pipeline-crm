package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.application.port.out.IdentifierFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Where new identities come from in production. Tests substitute a predictable one. */
@Component
public class RandomIdentifierFactory implements IdentifierFactory {

    @Override
    public UUID newIdentifier() {
        return UUID.randomUUID();
    }
}
