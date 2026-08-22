package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.out.IdentifierFactory;
import com.pipelinecrm.application.port.out.Transactions;

/** What every use case that writes something needs: a source of identities and a unit of work. */
public record WritingPorts(IdentifierFactory identifiers, Transactions transactions) {
}
