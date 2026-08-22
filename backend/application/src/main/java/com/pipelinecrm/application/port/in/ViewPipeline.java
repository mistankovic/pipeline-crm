package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Every deal on the board, optionally narrowed to one owner. */
public interface ViewPipeline {

    List<DealView> handle(Optional<UUID> ownerId);
}
