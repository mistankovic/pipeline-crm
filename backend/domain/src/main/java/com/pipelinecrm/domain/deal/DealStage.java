package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.Probability;

import java.util.Map;
import java.util.Optional;

/**
 * The pipeline state machine.
 *
 * <p>An open deal advances one step at a time along
 * LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON. A deal may be lost from any open
 * stage, because a customer can walk away at any point. A closed deal is terminal: it
 * cannot be reopened, advanced or re-lost. See docs/domain-decisions.md, decision D-1.
 */
public enum DealStage {

    LEAD,
    QUALIFIED,
    PROPOSAL,
    NEGOTIATION,
    CLOSED_WON,
    CLOSED_LOST;

    private static final Map<DealStage, DealStage> NEXT_STAGE = Map.of(
            LEAD, QUALIFIED,
            QUALIFIED, PROPOSAL,
            PROPOSAL, NEGOTIATION,
            NEGOTIATION, CLOSED_WON);

    public boolean isClosed() {
        return this == CLOSED_WON || this == CLOSED_LOST;
    }

    public boolean isOpen() {
        return !isClosed();
    }

    public boolean allowsTransitionTo(DealStage target) {
        if (isClosed() || target == null) {
            return false;
        }
        if (target == CLOSED_LOST) {
            return true;
        }
        return NEXT_STAGE.get(this) == target;
    }

    /** The probability a stage dictates, if it dictates one at all. */
    public Optional<Probability> forcedProbability() {
        return switch (this) {
            case CLOSED_WON -> Optional.of(Probability.certain());
            case CLOSED_LOST -> Optional.of(Probability.impossible());
            default -> Optional.empty();
        };
    }
}
