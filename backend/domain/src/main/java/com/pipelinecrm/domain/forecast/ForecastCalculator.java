package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.Money;

import java.util.Collection;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sums value x probability over the deals that are still open, grouped along one
 * dimension. Closed deals are history and never appear in a forecast.
 */
public final class ForecastCalculator {

    public Forecast forecast(Collection<Deal> deals, ForecastDimension dimension) {
        Guard.present(deals, "deals to forecast");
        Guard.present(dimension, "forecast dimension");
        Map<GroupKey, Money> totals = new LinkedHashMap<>();
        deals.stream()
                .filter(Deal::isOpen)
                .forEach(deal -> add(totals, keyOf(deal, dimension), deal.weightedValue()));
        return new Forecast(linesOf(totals));
    }

    private GroupKey keyOf(Deal deal, ForecastDimension dimension) {
        return new GroupKey(dimension.keyOf(deal), deal.value().currency());
    }

    private void add(Map<GroupKey, Money> totals, GroupKey key, Money weighted) {
        totals.merge(key, weighted, Money::plus);
    }

    private List<ForecastLine> linesOf(Map<GroupKey, Money> totals) {
        return totals.entrySet().stream()
                .map(entry -> new ForecastLine(entry.getKey().group(), entry.getValue()))
                .toList();
    }

    private record GroupKey(String group, Currency currency) {
    }
}
