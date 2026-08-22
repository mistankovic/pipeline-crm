package com.pipelinecrm.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * A non-negative amount in a single currency. Amounts are never converted between
 * currencies; combining two currencies is a domain error rather than a silent guess.
 */
public record Money(BigDecimal amount, Currency currency) {

    /**
     * The largest amount this system will hold: 999,999,999,999.9999.
     *
     * <p>The limit existed before — as {@code NUMERIC(19,4)} in a migration — so the question
     * "how much can a deal be worth?" was answered by a column definition, three layers below
     * anything that could explain it, and the answer arrived as a raw JDBC overflow. The domain
     * now decides, refuses in its own words, and the schema agrees with it by construction.
     * See docs/reviews/stage-7-review.md, finding F-7.2.
     */
    public static final BigDecimal LARGEST = new BigDecimal("999999999999.99");

    public Money {
        Guard.present(amount, "amount");
        Guard.present(currency, "currency");
        if (amount.signum() < 0) {
            throw new InvariantViolation("amount must not be negative, was " + amount.toPlainString());
        }
        // Rounded first, then checked: an amount that rounds *up* past the limit is over it.
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        if (amount.compareTo(LARGEST) > 0) {
            throw new InvariantViolation("amount must not exceed " + LARGEST.toPlainString()
                    + ", was " + amount.toPlainString());
        }
    }

    public static Money of(String amount, String currencyCode) {
        return of(parseAmount(amount), currencyCode);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(Guard.present(amount, "amount"), parseCurrency(currencyCode));
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
