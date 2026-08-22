package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ActivityView;

import java.util.UUID;

/**
 * Record a note, call or meeting. Exactly one of dealId and contactId must be given;
 * the use case rejects both and neither.
 */
public interface LogActivity {

    ActivityView handle(NewActivity activity);

    record NewActivity(About about, String type, String summary, UUID authorId) {

        /** The one thing an activity is about. */
        public record About(UUID dealId, UUID contactId) {
        }
    }
}
