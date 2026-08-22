package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ActivityView;
import com.pipelinecrm.application.view.DealView;

import java.util.List;
import java.util.UUID;

/** One deal and its timeline. */
public interface ViewDeal {

    DealDetail handle(UUID dealId);

    record DealDetail(DealView deal, List<ActivityView> timeline) {
    }
}
