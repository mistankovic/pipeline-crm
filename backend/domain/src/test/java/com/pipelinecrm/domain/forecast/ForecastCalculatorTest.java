package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealParties;
import com.pipelinecrm.domain.deal.DealTerms;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;
import com.pipelinecrm.domain.testing.Examples;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ForecastCalculatorTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    private final ForecastCalculator calculator = new ForecastCalculator();
    private final UserId sam = Examples.aUserId();
    private final UserId robin = Examples.aUserId();

    private Deal deal(UserId owner, String amount, String currencyCode, int probability) {
        return Deal.open(DealId.of(UUID.randomUUID()), "a deal",
                new DealParties(CompanyId.of(UUID.randomUUID()), owner),
                new DealTerms(Money.of(amount, currencyCode), Probability.of(probability)));
    }

    @Test
    void an_empty_pipeline_forecasts_nothing() {
        Forecast forecast = calculator.forecast(List.of(), ForecastDimension.OWNER);

        assertThat(forecast.lines()).isEmpty();
    }

    @Test
    void weights_a_single_deal_by_its_probability() {
        Deal deal = deal(sam, "1000.00", "EUR", 40);

        Forecast forecast = calculator.forecast(List.of(deal), ForecastDimension.OWNER);

        assertThat(forecast.valueOf(new OwnerGroup(sam), EUR)).contains(Money.of("400.00", "EUR"));
    }

    @Test
    void adds_up_the_deals_of_one_owner() {
        Forecast forecast = calculator.forecast(
                List.of(deal(sam, "1000.00", "EUR", 50), deal(sam, "500.00", "EUR", 20)),
                ForecastDimension.OWNER);

        assertThat(forecast.valueOf(new OwnerGroup(sam), EUR)).contains(Money.of("600.00", "EUR"));
    }

    @Test
    void keeps_owners_apart() {
        Forecast forecast = calculator.forecast(
                List.of(deal(sam, "1000.00", "EUR", 50), deal(robin, "1000.00", "EUR", 10)),
                ForecastDimension.OWNER);

        assertThat(forecast.valueOf(new OwnerGroup(sam), EUR)).contains(Money.of("500.00", "EUR"));
        assertThat(forecast.valueOf(new OwnerGroup(robin), EUR)).contains(Money.of("100.00", "EUR"));
    }

    @Test
    void keeps_currencies_apart_rather_than_converting_them() {
        Forecast forecast = calculator.forecast(
                List.of(deal(sam, "1000.00", "EUR", 50), deal(sam, "1000.00", "USD", 50)),
                ForecastDimension.OWNER);

        assertThat(forecast.lines()).hasSize(2);
        assertThat(forecast.valueOf(new OwnerGroup(sam), EUR)).contains(Money.of("500.00", "EUR"));
        assertThat(forecast.valueOf(new OwnerGroup(sam), USD)).contains(Money.of("500.00", "USD"));
    }

    @Test
    void groups_by_stage_when_asked_to() {
        Deal advanced = deal(sam, "1000.00", "EUR", 50);
        advanced.changeStageTo(DealStage.QUALIFIED, Examples.salesperson(sam), DealActivities.none(advanced.id()));

        Forecast forecast = calculator.forecast(
                List.of(advanced, deal(robin, "200.00", "EUR", 50)), ForecastDimension.STAGE);

        assertThat(forecast.valueOf(new StageGroup(DealStage.QUALIFIED), EUR)).contains(Money.of("500.00", "EUR"));
        assertThat(forecast.valueOf(new StageGroup(DealStage.LEAD), EUR)).contains(Money.of("100.00", "EUR"));
    }

    @Test
    void a_won_deal_is_history_and_leaves_the_forecast() {
        Deal won = Examples.dealAt(DealStage.NEGOTIATION, "1000.00", sam);
        won.changeStageTo(DealStage.CLOSED_WON, Examples.salesperson(sam), Examples.engagementFor(won.id(), sam));

        Forecast forecast = calculator.forecast(List.of(won), ForecastDimension.OWNER);

        assertThat(forecast.lines()).isEmpty();
    }

    @Test
    void a_lost_deal_leaves_the_forecast_too() {
        Deal lost = Examples.dealWorth("1000.00", sam);
        lost.changeStageTo(DealStage.CLOSED_LOST, Examples.salesperson(sam), DealActivities.none(lost.id()));

        Forecast forecast = calculator.forecast(List.of(lost), ForecastDimension.OWNER);

        assertThat(forecast.lines()).isEmpty();
    }

    @Test
    void a_group_refuses_to_exist_without_an_owner() {
        assertThatThrownBy(() -> new OwnerGroup(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_group_refuses_to_exist_without_a_stage() {
        assertThatThrownBy(() -> new StageGroup(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_forecast_no_collection() {
        assertThatThrownBy(() -> calculator.forecast(null, ForecastDimension.OWNER))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_forecast_along_no_dimension() {
        List<Deal> deals = List.of(deal(sam, "1", "EUR", 1));

        assertThatThrownBy(() -> calculator.forecast(deals, null)).isInstanceOf(InvariantViolation.class);
    }
}
