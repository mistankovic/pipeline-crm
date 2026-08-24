package com.pipelinecrm.application.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.pipelinecrm.application.port.in.ChangeDealStageUseCase;
import com.pipelinecrm.application.port.in.RecordActivityUseCase;
import com.pipelinecrm.application.port.in.UpdateDealUseCase;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityBody;
import com.pipelinecrm.domain.activity.ActivityTarget;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.DealStageNotAuthorizedException;
import com.pipelinecrm.domain.deal.DealNotWinnableException;
import com.pipelinecrm.domain.deal.IllegalDealStageException;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.forecast.ForecastBucket;
import com.pipelinecrm.domain.forecast.MixedCurrencyException;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

public class DealSteps {

    private final AcceptanceWorld world = new AcceptanceWorld();

    @Given("a sales user {string} owns a deal {string} in stage LEAD with value {int} USD and probability {int}")
    public void ownedLeadDeal(String owner, String title, Integer value, Integer probability) {
        world.seedOwnedDeal(owner, title, DealStage.LEAD, value + ".00", "USD", probability);
    }

    @Given("a sales user {string} exists")
    public void salesExists(String name) {
        world.user(name);
    }

    @Given("a manager user {string} exists")
    public void managerExists(String name) {
        world.user(name);
    }

    @Given("the deal is in stage {word}")
    public void dealInStage(String stage) {
        world.moveTo(DealStage.valueOf(stage), world.user("owner"));
    }

    @Given("the deal has a qualifying meeting activity")
    @Given("the deal has a meeting activity")
    public void meetingOnDeal() {
        record(ActivityType.MEETING);
    }

    @Given("the deal has a call activity")
    public void callOnDeal() {
        record(ActivityType.CALL);
    }

    @Given("the deal has a note activity")
    public void noteOnDeal() {
        record(ActivityType.NOTE);
    }

    @Given("a meeting activity exists on a contact of the same company")
    public void meetingOnContact() {
        var contact = world.extraContact();
        world.activities.save(Activity.record(
                ActivityId.generate(),
                ActivityType.MEETING,
                ActivityBody.of("contact meeting"),
                ActivityTarget.contact(contact.id()),
                world.user("owner").id(),
                Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Given("a meeting activity exists on a different deal")
    public void meetingOnOtherDeal() {
        world.seedOwnedDeal("owner", "Other", DealStage.LEAD, "100.00", "USD", 10);
        record(ActivityType.MEETING);
        world.seedOwnedDeal("owner", "Acme expansion", DealStage.LEAD, "1000.00", "USD", 25);
    }

    @Given("the deal value is {double} USD")
    public void setValue(Double amount) {
        world.deals.findById(world.currentDealId).orElseThrow().revalue(Money.of(BigDecimal.valueOf(amount).setScale(2), Currency.getInstance("USD")));
    }

    @Given("the owner moved the deal to {word}")
    public void ownerMoved(String stage) {
        world.moveTo(DealStage.valueOf(stage), world.user("owner"));
    }

    @Given("{string} owns an open deal worth {int} {word} at probability {int}")
    public void ownsOpen(String name, Integer value, String currency, Integer probability) {
        world.seedOwnedDeal(name, name + "-open", DealStage.LEAD, value + ".00", currency, probability);
    }

    @Given("{string} owns a CLOSED_WON deal worth {int} {word} at probability {int}")
    public void ownsWon(String name, Integer value, String currency, Integer probability) {
        world.seedOwnedDeal(name, name + "-won", DealStage.CLOSED_WON, value + ".00", currency, probability);
    }

    @Given("{string} owns a CLOSED_LOST deal worth {int} {word} at probability {int}")
    public void ownsLost(String name, Integer value, String currency, Integer probability) {
        world.seedOwnedDeal(name, name + "-lost", DealStage.CLOSED_LOST, value + ".00", currency, probability);
    }

    @Given("{string} owns a LEAD deal worth {int} {word} at probability {int}")
    public void ownsLead(String name, Integer value, String currency, Integer probability) {
        world.seedOwnedDeal(name, name + "-lead", DealStage.LEAD, value + ".00", currency, probability);
    }

    @When("the owner moves the deal to {word}")
    public void ownerMoves(String stage) {
        move("owner", stage);
    }

    @When("{string} moves the deal to {word}")
    public void namedMoves(String name, String stage) {
        move(name, stage);
    }

    @When("the owner changes the deal probability to {int}")
    public void changeProbability(Integer percent) {
        world.catchError(() -> world.updateDeal.execute(new UpdateDealUseCase.Command(
                world.user("owner").id(),
                world.currentDealId,
                world.deal().title().value(),
                world.deal().value(),
                Probability.of(percent))));
    }

    @When("the owner changes the deal value to {double} USD")
    public void changeValue(Double amount) {
        world.catchError(() -> world.updateDeal.execute(new UpdateDealUseCase.Command(
                world.user("owner").id(),
                world.currentDealId,
                world.deal().title().value(),
                Money.of(BigDecimal.valueOf(amount).setScale(2), Currency.getInstance("USD")),
                world.deal().probability())));
    }

    @When("the forecast is grouped by owner in {word}")
    public void forecastOwner(String currency) {
        world.catchError(() -> world.lastForecast = world.forecast.byOwner(Currency.getInstance(currency)));
    }

    @When("the forecast is grouped by stage in {word}")
    public void forecastStage(String currency) {
        world.catchError(() -> world.lastForecast = world.forecast.byStage(Currency.getInstance(currency)));
    }

    @Then("the deal stage is {word}")
    public void stageIs(String stage) {
        assertThat(world.lastError).isNull();
        assertThat(world.deal().stage()).isEqualTo(DealStage.valueOf(stage));
    }

    @Then("the deal stage is still {word}")
    public void stageStill(String stage) {
        assertThat(world.deal().stage()).isEqualTo(DealStage.valueOf(stage));
    }

    @Then("the change is rejected as an illegal stage transition")
    public void illegalStage() {
        assertThat(world.lastError).isInstanceOf(IllegalDealStageException.class);
    }

    @Then("the change is rejected because the deal is not winnable")
    public void notWinnable() {
        assertThat(world.lastError).isInstanceOf(DealNotWinnableException.class);
    }

    @Then("the change is rejected as unauthorized")
    public void unauthorized() {
        assertThat(world.lastError).isInstanceOf(DealStageNotAuthorizedException.class);
    }

    @Then("the change is rejected because the deal is closed")
    public void closed() {
        assertThat(world.lastError).isInstanceOf(IllegalDealStageException.class);
    }

    @Then("the deal probability is {int}")
    public void probabilityIs(Integer percent) {
        assertThat(world.deal().probability().percent()).isEqualTo(percent);
    }

    @Then("the forecast for {string} is {double} USD")
    public void forecastOwnerIs(String name, Double amount) {
        UserId id = world.user(name).id();
        ForecastBucket bucket = world.lastForecast.stream()
                .filter(b -> b.ownerId().orElseThrow().equals(id))
                .findFirst()
                .orElseThrow();
        assertThat(bucket.total().amount()).isEqualByComparingTo(BigDecimal.valueOf(amount).setScale(2));
    }

    @Then("the forecast for stage {word} is {double} USD")
    public void forecastStageIs(String stage, Double amount) {
        ForecastBucket bucket = world.lastForecast.stream()
                .filter(b -> b.stage().orElseThrow() == DealStage.valueOf(stage))
                .findFirst()
                .orElseThrow();
        assertThat(bucket.total().amount()).isEqualByComparingTo(BigDecimal.valueOf(amount).setScale(2));
    }

    @Then("there are no owner forecast buckets")
    public void noOwnerBuckets() {
        assertThat(world.lastForecast).isEmpty();
    }

    @Then("the forecast is rejected as mixed currency")
    public void mixedCurrency() {
        assertThat(world.lastError).isInstanceOf(MixedCurrencyException.class);
    }

    private void move(String actor, String stage) {
        world.catchError(() -> world.changeStage.execute(
                new ChangeDealStageUseCase.Command(world.user(actor).id(), world.currentDealId, DealStage.valueOf(stage))));
    }

    private void record(ActivityType type) {
        world.recordActivity.execute(new RecordActivityUseCase.Command(
                world.user("owner").id(), type, type.name(), world.currentDealId, null));
    }
}
