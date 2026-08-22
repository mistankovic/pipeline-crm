package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.RepriceDeal;
import com.pipelinecrm.application.port.in.ReweightDeal;
import org.springframework.stereotype.Component;

/**
 * The three ways to change a deal, bundled so that {@link DealController}'s constructor stays
 * inside the four-parameter limit. Grouping them is honest: they are the write half of one
 * resource, and a controller that gained a fourth way to change a deal would add it here.
 */
@Component
public record DealChanges(ChangeDealStage stage, RepriceDeal price, ReweightDeal weight) {
}
