package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.util.List;
import java.util.UUID;

/**
 * The deals on the board. Two questions, two methods, rather than one nullable filter.
 *
 * <p>Both take the caller, because a {@code DealView} describes what <em>that</em> caller may
 * do with the deal, not what its owner may do.
 */
public interface ViewPipeline {

    List<DealView> everything(UUID callerId);

    List<DealView> ownedBy(UUID ownerId, UUID callerId);
}
