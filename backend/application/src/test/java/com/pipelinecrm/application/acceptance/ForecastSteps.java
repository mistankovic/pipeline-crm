package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.view.ForecastLineView;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.forecast.ForecastDimension;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Producing the forecast and reading its lines. */
public class ForecastSteps {

    private final World world;

    public ForecastSteps(World world) {
        this.world = world;
    }

    @When("the forecast is produced by {dimension}")
    public void theForecastIsProducedBy(ForecastDimension dimension) {
        world.rememberForecast(world.application.produceForecast.handle(dimension));
    }

    @Then("the forecast is empty")
    public void theForecastIsEmpty() {
        assertThat(world.forecast().lines()).isEmpty();
    }

    @Then("the forecast has {int} lines")
    public void theForecastHasLines(int lines) {
        assertThat(world.forecast().lines()).hasSize(lines);
    }

    @Then("the forecast for {word} in {word} is {bigdecimal}")
    public void theForecastForSomebodyIs(String person, String currency, BigDecimal expected) {
        assertLine(world.person(person).toString(), currency, expected);
    }

    @Then("the forecast for stage {stage} in {word} is {bigdecimal}")
    public void theForecastForStageIs(DealStage stage, String currency, BigDecimal expected) {
        assertLine(stage.name(), currency, expected);
    }

    private void assertLine(String group, String currency, BigDecimal expected) {
        Optional<ForecastLineView> line = world.forecast().lines().stream()
                .filter(candidate -> candidate.group().equals(group))
                .filter(candidate -> candidate.weightedValue().currency().equals(currency))
                .findFirst();

        assertThat(line).describedAs("a forecast line for %s in %s", group, currency).isPresent();
        assertThat(line.orElseThrow().weightedValue().amount()).isEqualByComparingTo(expected);
    }
}
