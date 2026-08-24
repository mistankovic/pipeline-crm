package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guards;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Forecast {

    private Forecast() {}

    public static List<ForecastBucket> byOwner(Collection<Deal> deals, Currency currency) {
        List<Deal> open = openDealsIn(deals, currency);
        Map<UserId, WeightedValue> totals = emptyOwnerTotals(open);
        return toOwnerBuckets(totals);
    }

    public static List<ForecastBucket> byStage(Collection<Deal> deals, Currency currency) {
        List<Deal> open = openDealsIn(deals, currency);
        Map<DealStage, WeightedValue> totals = emptyStageTotals(currency);
        for (Deal deal : open) {
            addStage(totals, deal);
        }
        return toStageBuckets(totals);
    }

    private static List<Deal> openDealsIn(Collection<Deal> deals, Currency currency) {
        Guards.notNull(currency, "currency");
        Guards.notNull(deals, "deals");
        List<Deal> open = new ArrayList<>();
        for (Deal deal : deals) {
            if (deal.isOpen()) {
                requireCurrency(deal, currency);
                open.add(deal);
            }
        }
        return open;
    }

    private static void requireCurrency(Deal deal, Currency currency) {
        if (!deal.value().currency().equals(currency)) {
            throw new MixedCurrencyException();
        }
    }

    private static Map<UserId, WeightedValue> emptyOwnerTotals(List<Deal> open) {
        Map<UserId, WeightedValue> totals = new LinkedHashMap<>();
        for (Deal deal : open) {
            totals.merge(deal.ownerId(), WeightedValue.of(deal), WeightedValue::plus);
        }
        return totals;
    }

    private static Map<DealStage, WeightedValue> emptyStageTotals(Currency currency) {
        Map<DealStage, WeightedValue> totals = new LinkedHashMap<>();
        for (DealStage stage : DealStage.values()) {
            if (stage.isOpen()) {
                totals.put(stage, WeightedValue.zero(currency));
            }
        }
        return totals;
    }

    private static void addStage(Map<DealStage, WeightedValue> totals, Deal deal) {
        totals.merge(deal.stage(), WeightedValue.of(deal), WeightedValue::plus);
    }

    private static List<ForecastBucket> toOwnerBuckets(Map<UserId, WeightedValue> totals) {
        List<ForecastBucket> buckets = new ArrayList<>();
        for (Map.Entry<UserId, WeightedValue> entry : totals.entrySet()) {
            buckets.add(ForecastBucket.owner(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(buckets);
    }

    private static List<ForecastBucket> toStageBuckets(Map<DealStage, WeightedValue> totals) {
        List<ForecastBucket> buckets = new ArrayList<>();
        for (Map.Entry<DealStage, WeightedValue> entry : totals.entrySet()) {
            buckets.add(ForecastBucket.stage(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(buckets);
    }
}
