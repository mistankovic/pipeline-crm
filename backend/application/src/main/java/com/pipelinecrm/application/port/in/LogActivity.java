package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ActivityView;

import java.util.UUID;

/**
 * Record a note, call or meeting. What the activity is about is a sealed choice, mirroring
 * the domain's own {@code ActivitySubject}: "both" and "neither" cannot be expressed, so
 * the use case never has to reject them.
 */
public interface LogActivity {

    ActivityView handle(NewActivity activity);

    record NewActivity(About about, String type, String summary, UUID authorId) {
    }

    /** The one thing an activity is about. */
    sealed interface About permits AboutDeal, AboutContact {
    }

    record AboutDeal(UUID dealId) implements About {
    }

    record AboutContact(UUID contactId) implements About {
    }
}
