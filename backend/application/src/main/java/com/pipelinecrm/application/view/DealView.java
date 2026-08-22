package com.pipelinecrm.application.view;

import java.util.List;
import java.util.UUID;

/**
 * A deal as a caller sees it.
 *
 * <p>{@code allowedTransitions} is computed by the domain and handed out deliberately: the
 * user interface needs to know which columns a card may be dropped into, and the rule that
 * decides it must not be duplicated in the browser. The server still rejects an illegal
 * move if the browser sends one anyway.
 */
public record DealView(
        UUID id,
        String title,
        CompanyView company,
        UserView owner,
        MoneyView value,
        int probability,
        String stage,
        MoneyView weightedValue,
        List<String> allowedTransitions) {
}
