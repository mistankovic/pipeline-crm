package com.pipelinecrm.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * A non-negative amount in a single currency. Amounts are never converted between
 * currencies; combining two currencies is a domain error rather than a silent guess.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Guard.present(amount, "amount");
        Guard.present(currency, "currency");
        if (amount.signum() < 0) {
            throw new InvariantViolation("amount must not be negative, was " + amount.toPlainString());
        }
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(parseAmount(amount), parseCurrency(currencyCode));
    }

    private static BigDecimal parseAmount(String amount) {
        String text = Guard.filled(amount, "amount");
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException notANumber) {
            throw new InvariantViolation("amount is not a number: " + text);
        }
    }

    private static Currency parseCurrency(String currencyCode) {
        String text = Guard.filled(currencyCode, "currency code");
        try {
            return Currency.getInstance(text);
        } catch (IllegalArgumentException unknown) {
            throw new InvariantViolation("unknown currency code: " + text);
        }
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public Money plus(Money other) {
        Guard.present(other, "amount to add");
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatch(currency, other.currency);
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money weightedBy(Probability probability) {
        Guard.present(probability, "probability");
        return new Money(amount.multiply(probability.asFraction()), currency);
    }
}
