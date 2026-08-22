package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ForecastTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    private static final Forecast TWO_CURRENCIES = new Forecast(List.of(
            new ForecastLine("sam", Money.of("100.00", "EUR")),
            new ForecastLine("robin", Money.of("50.00", "EUR")),
            new ForecastLine("sam", Money.of("70.00", "USD"))));

    @Test
    void an_empty_forecast_has_no_lines() {
        assertThat(Forecast.empty().lines()).isEmpty();
    }

    @Test
    void an_empty_forecast_has_no_total() {
        assertThat(Forecast.empty().total(EUR)).isEmpty();
    }

    @Test
    void finds_the_value_of_a_group_in_a_currency() {
        assertThat(TWO_CURRENCIES.valueOf("sam", USD)).contains(Money.of("70.00", "USD"));
    }

    @Test
    void reports_nothing_for_a_group_it_does_not_hold() {
        assertThat(TWO_CURRENCIES.valueOf("nobody", EUR)).isEmpty();
    }

    @Test
    void reports_nothing_for_a_currency_a_group_does_not_use() {
        assertThat(TWO_CURRENCIES.valueOf("robin", USD)).isEmpty();
    }

    @Test
    void totals_only_the_lines_in_the_requested_currency() {
        assertThat(TWO_CURRENCIES.total(EUR)).contains(Money.of("150.00", "EUR"));
    }

    @Test
    void totals_the_other_currency_separately() {
        assertThat(TWO_CURRENCIES.total(USD)).contains(Money.of("70.00", "USD"));
    }

    @Test
    void copies_its_lines_so_a_caller_cannot_edit_the_result() {
        List<ForecastLine> mutable = new ArrayList<>();
        mutable.add(new ForecastLine("sam", Money.of("1.00", "EUR")));
        Forecast forecast = new Forecast(mutable);

        mutable.clear();

        assertThat(forecast.lines()).hasSize(1);
    }

    @Test
    void refuses_to_exist_without_lines() {
        assertThatThrownBy(() -> new Forecast(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_line_refuses_to_exist_without_a_group() {
        Money value = Money.of("1", "EUR");

        assertThatThrownBy(() -> new ForecastLine(" ", value)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_line_refuses_to_exist_without_a_value() {
        assertThatThrownBy(() -> new ForecastLine("sam", null)).isInstanceOf(InvariantViolation.class);
    }
}
