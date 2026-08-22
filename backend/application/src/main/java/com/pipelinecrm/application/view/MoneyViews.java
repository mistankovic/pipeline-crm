package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.shared.Money;

/**
 * Money as the outside world sees it.
 *
 * <p>The assemblers in this package are static because they are pure functions of their
 * arguments: they hold no state, reach no port, and there is nothing a test would want to
 * substitute. Injecting them would add a constructor parameter to every use case in order
 * to make a function replaceable by a different function that computes the same thing.
 */
public final class MoneyViews {

    private MoneyViews() {
    }

    public static MoneyView of(Money money) {
        return new MoneyView(money.amount(), money.currency().getCurrencyCode());
    }
}
