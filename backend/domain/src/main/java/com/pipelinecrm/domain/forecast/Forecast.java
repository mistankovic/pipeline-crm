package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.Money;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * The weighted value of the open pipeline, split into groups. Lines are per currency:
 * this demo never converts between currencies, so two currencies produce two lines.
 */
public record Forecast(List<ForecastLine> lines) {

    public Forecast {
        Guard.present(lines, "forecast lines");
        lines = List.copyOf(lines);
    }

    public static Forecast empty() {
        return new Forecast(List.of());
    }

    public Optional<Money> valueOf(String group, Currency currency) {
        return lines.stream()
                .filter(line -> line.group().equals(group))
                .map(ForecastLine::weightedValue)
                .filter(value -> value.currency().equals(currency))
                .findFirst();
    }

    public Optional<Money> total(Currency currency) {
        return lines.stream()
                .map(ForecastLine::weightedValue)
                .filter(value -> value.currency().equals(currency))
                .reduce(Money::plus);
    }
}
