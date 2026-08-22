package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.CreateDeal;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.domain.deal.DealStage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Creating deals, moving them, and reading where they got to. */
public class DealSteps {

    private final World world;
    private final Fixtures fixtures;

    public DealSteps(World world) {
        this.world = world;
        this.fixtures = new Fixtures(world);
    }

    @Given("{word} owns a deal {string} in stage {stage} worth {money}")
    public void ownsADeal(String owner, String title, DealStage stage, Amount amount) {
        fixtures.dealAt(owner, title, stage, amount);
    }

    @Given("{word} owns a deal {string} in stage {stage} worth {amount}")
    public void ownsADealAtProbability(String owner, String title, DealStage stage, Amount amount) {
        fixtures.dealAt(owner, title, stage, amount);
    }

    @Given("{word} owns a won deal {string} worth {money}")
    public void ownsAWonDeal(String owner, String title, Amount amount) {
        fixtures.dealAt(owner, title, DealStage.CLOSED_WON, amount);
    }

    @When("{word} creates a deal {string} for {string} worth {amount}")
    public void createsADeal(String owner, String title, String company, Amount amount) {
        world.rememberDeal(title, create(owner, title, world.company(company), amount).id());
    }

    @When("{word} tries to create a deal {string} for {string} worth {amount}")
    public void triesToCreateADeal(String owner, String title, String company, Amount amount) {
        world.attempt(() -> create(owner, title, world.company(company), amount));
    }

    @When("{word} tries to create a deal {string} for an unknown company worth {amount}")
    public void triesToCreateADealForAnUnknownCompany(String owner, String title, Amount amount) {
        world.attempt(() -> create(owner, title, UUID.randomUUID(), amount));
    }

    @When("{word} tries to create a deal {string} for {string} with an unknown owner")
    public void triesToCreateADealWithAnUnknownOwner(String actor, String title, String company) {
        world.attempt(() -> world.application.createDeal.handle(new CreateDeal.NewDeal(
                title, world.company(company), UUID.randomUUID(), world.person(actor),
                BigDecimal.valueOf(1000), "EUR", 50)));
    }

    private DealView create(String owner, String title, UUID company, Amount amount) {
        UUID person = world.person(owner);
        return world.application.createDeal.handle(new CreateDeal.NewDeal(
                title, company, person, person, amount.value(), amount.currency(), amount.probability()));
    }

    @When("{word} moves {string} to {stage}")
    public void moves(String actor, String title, DealStage stage) {
        world.application.changeDealStage.handle(new ChangeDealStage.StageChange(
                world.deal(title), stage, world.person(actor)));
    }

    @When("{word} tries to move {string} to {stage}")
    public void triesToMove(String actor, String title, DealStage stage) {
        world.attempt(() -> moves(actor, title, stage));
    }

    @When("{word} tries to move an unknown deal to {stage}")
    public void triesToMoveAnUnknownDeal(String actor, DealStage stage) {
        world.attempt(() -> world.application.changeDealStage.handle(new ChangeDealStage.StageChange(
                UUID.randomUUID(), stage, world.person(actor))));
    }

    @When("{word} looks at {string}")
    public void looksAt(String viewer, String title) {
        world.rememberViewOf(title,
                world.application.viewDeal.handle(world.deal(title), world.person(viewer)).deal());
    }

    @Then("{word} is offered no way to move it")
    public void isOfferedNoWayToMoveIt(String viewer) {
        assertThat(world.lastViewed().allowedTransitions()).isEmpty();
        assertThat(world.lastViewed().youMayChangeThis()).isFalse();
    }

    @Then("{word} is offered the moves {word}, {word}")
    public void isOfferedTheMoves(String viewer, String first, String second) {
        assertThat(world.lastViewed().allowedTransitions()).containsExactlyInAnyOrder(first, second);
        assertThat(world.lastViewed().youMayChangeThis()).isTrue();
    }

    @Then("the deal {string} is in stage {stage}")
    public void theDealIsInStage(String title, DealStage stage) {
        assertThat(world.currentStateOf(title).stage()).isEqualTo(stage.name());
    }

    @Then("the deal {string} has probability {int}")
    public void theDealHasProbability(String title, int probability) {
        assertThat(world.currentStateOf(title).probability()).isEqualTo(probability);
    }

    @Then("the deal {string} is worth {money}")
    public void theDealIsWorth(String title, Amount amount) {
        assertThat(world.currentStateOf(title).value().amount()).isEqualByComparingTo(amount.value());
        assertThat(world.currentStateOf(title).value().currency()).isEqualTo(amount.currency());
    }
}
