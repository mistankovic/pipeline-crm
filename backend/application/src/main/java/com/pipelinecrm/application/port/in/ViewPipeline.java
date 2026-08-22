package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.util.List;
import java.util.UUID;

/** The deals on the board. Two questions, two methods, rather than one nullable filter. */
public interface ViewPipeline {

    List<DealView> everything();

    List<DealView> ownedBy(UUID ownerId);
}
