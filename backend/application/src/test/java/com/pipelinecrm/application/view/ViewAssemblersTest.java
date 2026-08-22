package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.forecast.Forecast;
import com.pipelinecrm.domain.forecast.ForecastDimension;
import com.pipelinecrm.domain.forecast.ForecastLine;
import com.pipelinecrm.domain.forecast.OwnerGroup;
import com.pipelinecrm.domain.forecast.StageGroup;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.user.User;
import com.pipelinecrm.domain.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The pure functions that turn domain objects into what a caller sees. */
class ViewAssemblersTest {

    private static final UserId SAM_ID = UserId.of(UUID.randomUUID());
    private static final User SAM =
            new User(SAM_ID, EmailAddress.of("sam@example.com"), "Sam", UserRole.SALES);

    @Test
    void money_becomes_an_amount_and_a_currency_code() {
        MoneyView view = MoneyViews.of(Money.of("12.50", "EUR"));

        assertThat(view.amount()).isEqualByComparingTo("12.50");
        assertThat(view.currency()).isEqualTo("EUR");
    }

    @Test
    void a_user_view_never_carries_anything_secret() {
        UserView view = UserViews.of(SAM);

        assertThat(view.id()).isEqualTo(SAM_ID.value());
        assertThat(view.email()).isEqualTo("sam@example.com");
        assertThat(view.name()).isEqualTo("Sam");
        assertThat(view.role()).isEqualTo("SALES");
    }

    @Test
    void a_forecast_line_for_a_stage_uses_the_stage_name_as_both_key_and_label() {
        Forecast forecast = new Forecast(List.of(
                new ForecastLine(new StageGroup(DealStage.PROPOSAL), Money.of("100.00", "EUR"))));

        ForecastView view = ForecastViews.of(ForecastDimension.STAGE, forecast, Map.of());

        assertThat(view.lines()).singleElement().satisfies(line -> {
            assertThat(line.group()).isEqualTo("PROPOSAL");
            assertThat(line.label()).isEqualTo("PROPOSAL");
        });
    }

    @Test
    void a_forecast_line_for_an_owner_uses_their_name_as_the_label() {
        Forecast forecast = new Forecast(List.of(
                new ForecastLine(new OwnerGroup(SAM_ID), Money.of("100.00", "EUR"))));

        ForecastView view = ForecastViews.of(ForecastDimension.OWNER, forecast, Map.of(SAM_ID, SAM));

        assertThat(view.lines()).singleElement().satisfies(line -> {
            assertThat(line.group()).isEqualTo(SAM_ID.value().toString());
            assertThat(line.label()).isEqualTo("Sam");
        });
    }

    @Test
    void a_forecast_falls_back_to_the_identity_when_the_owner_is_not_known() {
        Forecast forecast = new Forecast(List.of(
                new ForecastLine(new OwnerGroup(SAM_ID), Money.of("100.00", "EUR"))));

        ForecastView view = ForecastViews.of(ForecastDimension.OWNER, forecast, Map.of());

        assertThat(view.lines()).singleElement()
                .satisfies(line -> assertThat(line.label()).isEqualTo(SAM_ID.value().toString()));
    }

    @Test
    void an_empty_forecast_produces_no_lines() {
        assertThat(ForecastViews.of(ForecastDimension.OWNER, Forecast.empty(), Map.of()).lines()).isEmpty();
    }
}
