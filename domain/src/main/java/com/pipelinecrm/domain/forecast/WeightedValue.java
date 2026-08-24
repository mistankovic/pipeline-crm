package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.shared.Guards;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record WeightedValue(BigDecimal amount, Currency currency) {

    public WeightedValue {
        Guards.notNull(amount, "amount");
        Guards.notNull(currency, "currency");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static WeightedValue of(Deal deal) {
        Guards.notNull(deal, "deal");
        BigDecimal weighted = deal.value().amount().multiply(deal.probability().fraction());
        return new WeightedValue(weighted, deal.value().currency());
    }

    public static WeightedValue zero(Currency currency) {
        return new WeightedValue(BigDecimal.ZERO.setScale(2), currency);
    }

    public WeightedValue plus(WeightedValue other) {
        Guards.notNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("cannot add amounts in different currencies");
        }
        return new WeightedValue(amount.add(other.amount), currency);
    }
}
