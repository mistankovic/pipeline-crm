package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ActivityView;

import java.util.List;
import java.util.UUID;

/** Everything recorded against one contact. */
public interface ViewContactTimeline {

    List<ActivityView> handle(UUID contactId);
}
