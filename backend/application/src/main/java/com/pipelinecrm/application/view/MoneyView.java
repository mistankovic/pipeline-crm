package com.pipelinecrm.application.view;

import java.math.BigDecimal;

/** An amount as the outside world sees it: a number and a currency code, never a Money. */
public record MoneyView(BigDecimal amount, String currency) {
}
