package com.pipelinecrm.domain.deal;

import java.util.Map;
import java.util.Set;

public enum DealStage {
    LEAD,
    QUALIFIED,
    PROPOSAL,
    NEGOTIATION,
    CLOSED_WON,
    CLOSED_LOST;

    private static final Map<DealStage, Set<DealStage>> ALLOWED = Map.of(
            LEAD, Set.of(QUALIFIED, CLOSED_WON, CLOSED_LOST),
            QUALIFIED, Set.of(PROPOSAL, CLOSED_WON, CLOSED_LOST),
            PROPOSAL, Set.of(NEGOTIATION, CLOSED_WON, CLOSED_LOST),
            NEGOTIATION, Set.of(CLOSED_WON, CLOSED_LOST),
            CLOSED_WON, Set.of(),
            CLOSED_LOST, Set.of());

    public boolean isTerminal() {
        return this == CLOSED_WON || this == CLOSED_LOST;
    }

    public boolean isOpen() {
        return !isTerminal();
    }

    public boolean canTransitionTo(DealStage target) {
        if (target == null) {
            return false;
        }
        return ALLOWED.get(this).contains(target);
    }
}
