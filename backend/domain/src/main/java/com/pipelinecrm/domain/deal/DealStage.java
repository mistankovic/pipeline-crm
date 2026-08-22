package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.Probability;

import java.util.Map;
import java.util.Set;
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

    /**
     * The whole state machine, as a table you can read in one glance.
     *
     * <p>It was three conditionals — closed, lost-from-anywhere, next-in-line — which is how a
     * rule ends up spread across a method body. As data it is complexity 2 instead of 5, and
     * the pipeline is visible rather than inferred. Constitution §3.1 sets the CRAP *target*
     * at 4; this was the only method above it.
     */
    private static final Map<DealStage, Set<DealStage>> ALLOWED_NEXT = Map.of(
            LEAD, Set.of(QUALIFIED, CLOSED_LOST),
            QUALIFIED, Set.of(PROPOSAL, CLOSED_LOST),
            PROPOSAL, Set.of(NEGOTIATION, CLOSED_LOST),
            NEGOTIATION, Set.of(CLOSED_WON, CLOSED_LOST),
            CLOSED_WON, Set.of(),
            CLOSED_LOST, Set.of());

    public boolean isClosed() {
        return this == CLOSED_WON || this == CLOSED_LOST;
    }

    public boolean isOpen() {
        return !isClosed();
    }

    public boolean allowsTransitionTo(DealStage target) {
        // Set.of(...) throws on a null argument rather than answering false, so ask first.
        return target != null && ALLOWED_NEXT.get(this).contains(target);
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
