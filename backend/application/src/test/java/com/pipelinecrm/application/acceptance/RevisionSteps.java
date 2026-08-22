package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.RepriceDeal;
import com.pipelinecrm.application.port.in.ReweightDeal;
import io.cucumber.java.en.When;

import java.util.UUID;

/** Changing what an open deal is worth and how likely it is. */
public class RevisionSteps {

    private final World world;

    public RevisionSteps(World world) {
        this.world = world;
    }

    @When("{word} reprices {string} to {money}")
    public void reprices(String actor, String title, Amount amount) {
        reprice(world.deal(title), world.person(actor), amount);
    }

    @When("{word} tries to reprice {string} to {money}")
    public void triesToReprice(String actor, String title, Amount amount) {
        world.attempt(() -> reprice(world.deal(title), world.person(actor), amount));
    }

    @When("{word} tries to reprice an unknown deal to {money}")
    public void triesToRepriceAnUnknownDeal(String actor, Amount amount) {
        world.attempt(() -> reprice(UUID.randomUUID(), world.person(actor), amount));
    }

    @When("{word} reweights {string} to {int}%")
    public void reweights(String actor, String title, int probability) {
        reweight(world.deal(title), world.person(actor), probability);
    }

    @When("{word} tries to reweight {string} to {int}%")
    public void triesToReweight(String actor, String title, int probability) {
        world.attempt(() -> reweight(world.deal(title), world.person(actor), probability));
    }

    private void reprice(UUID deal, UUID actor, Amount amount) {
        world.application.repriceDeal.handle(
                new RepriceDeal.Repricing(deal, actor, amount.value(), amount.currency()));
    }

    private void reweight(UUID deal, UUID actor, int probability) {
        world.application.reweightDeal.handle(new ReweightDeal.Reweighting(deal, actor, probability));
    }
}
