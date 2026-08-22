package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.CreateDeal;
import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.domain.deal.DealStage;

import java.util.List;
import java.util.UUID;

/**
 * Puts a deal into the state a Given describes, by driving the real use cases rather than by
 * writing to the repositories behind their backs. A scenario that says "a deal in
 * NEGOTIATION" gets one that arrived there legally, which is the only kind the system can
 * actually produce.
 */
final class Fixtures {

    /** Scenarios that do not name a company mean the one their Background created. */
    private static final String DEFAULT_COMPANY = "Acme";

    private static final List<DealStage> FORWARD = List.of(
            DealStage.QUALIFIED, DealStage.PROPOSAL, DealStage.NEGOTIATION, DealStage.CLOSED_WON);

    private final World world;

    Fixtures(World world) {
        this.world = world;
    }

    UUID dealAt(String owner, String title, DealStage stage, Amount amount) {
        UUID ownerId = world.person(owner);
        UUID deal = world.application.createDeal.handle(new CreateDeal.NewDeal(title,
                world.company(DEFAULT_COMPANY), ownerId, ownerId,
                amount.value(), amount.currency(), amount.probability())).id();
        world.rememberDeal(title, deal);
        walkTo(stage, deal, ownerId);
        return deal;
    }

    private void walkTo(DealStage stage, UUID deal, UUID owner) {
        if (stage == DealStage.LEAD) {
            return;
        }
        if (stage == DealStage.CLOSED_LOST) {
            move(deal, DealStage.CLOSED_LOST, owner);
            return;
        }
        if (stage == DealStage.CLOSED_WON) {
            logEngagement(deal, owner);
        }
        FORWARD.subList(0, FORWARD.indexOf(stage) + 1).forEach(step -> move(deal, step, owner));
    }

    private void logEngagement(UUID deal, UUID owner) {
        world.application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutDeal(deal), "MEETING", "closing call", owner));
    }

    private void move(UUID deal, DealStage stage, UUID actor) {
        world.application.changeDealStage.handle(new ChangeDealStage.StageChange(deal, stage, actor));
    }
}
