package com.pipelinecrm.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void rounds_the_amount_to_the_currency_scale_on_construction() {
        Money money = Money.of("10.005", "EUR");

        assertThat(money.amount()).isEqualTo(new BigDecimal("10.01"));
    }

    @Test
    void rejects_a_negative_amount() {
        assertThatThrownBy(() -> Money.of("-0.01", "EUR"))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejects_a_missing_amount() {
        assertThatThrownBy(() -> new Money(null, EUR)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void rejects_a_missing_currency() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void rejects_a_blank_amount_string() {
        assertThatThrownBy(() -> Money.of("  ", "EUR")).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void rejects_a_blank_currency_code() {
        assertThatThrownBy(() -> Money.of("1", " ")).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void rejects_an_amount_that_is_not_a_number_as_a_domain_failure() {
        assertThatThrownBy(() -> Money.of("ten euros", "EUR"))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("amount is not a number");
    }

    @Test
    void rejects_an_unknown_currency_code_as_a_domain_failure() {
        assertThatThrownBy(() -> Money.of("10", "NOTACURRENCY"))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("unknown currency code");
    }

    @Test
    void accepts_an_amount_that_is_already_a_number() {
        assertThat(Money.of(new BigDecimal("12.34"), "EUR")).isEqualTo(Money.of("12.34", "EUR"));
    }

    @Test
    void rejects_a_missing_amount_given_as_a_number() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null, "EUR")).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void rejects_an_unknown_currency_for_an_amount_given_as_a_number() {
        BigDecimal amount = BigDecimal.ONE;

        assertThatThrownBy(() -> Money.of(amount, "ZZZ")).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void treats_zero_as_not_positive() {
        assertThat(Money.zero(EUR).isPositive()).isFalse();
    }

    @Test
    void treats_any_amount_above_zero_as_positive() {
        assertThat(Money.of("0.01", "EUR").isPositive()).isTrue();
    }

    @Test
    void adds_two_amounts_in_the_same_currency() {
        Money sum = Money.of("10.50", "EUR").plus(Money.of("4.50", "EUR"));

        assertThat(sum).isEqualTo(Money.of("15.00", "EUR"));
    }

    @Test
    void refuses_to_add_across_currencies_rather_than_guessing_a_rate() {
        Money euros = Money.of("10", "EUR");
        Money dollars = Money.of("10", "USD");

        assertThatThrownBy(() -> euros.plus(dollars))
                .isInstanceOf(CurrencyMismatch.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("EUR");
    }

    @Test
    void rejects_adding_nothing() {
        Money euros = Money.of("10", "EUR");

        assertThatThrownBy(() -> euros.plus(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void weights_an_amount_by_a_probability() {
        Money weighted = Money.of("1000.00", "EUR").weightedBy(Probability.of(30));

        assertThat(weighted).isEqualTo(Money.of("300.00", "EUR"));
    }

    @Test
    void weighting_by_certainty_leaves_the_amount_alone() {
        Money original = Money.of("1234.56", "EUR");

        assertThat(original.weightedBy(Probability.certain())).isEqualTo(original);
    }

    @Test
    void rejects_weighting_by_no_probability() {
        Money euros = Money.of("10", "EUR");

        assertThatThrownBy(() -> euros.weightedBy(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void keeps_the_currency_it_was_given() {
        assertThat(Money.of("1", "USD").currency()).isEqualTo(USD);
    }
}
