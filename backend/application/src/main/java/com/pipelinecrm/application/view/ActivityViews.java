package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivitySubject;
import com.pipelinecrm.domain.activity.ContactSubject;
import com.pipelinecrm.domain.activity.DealSubject;
import com.pipelinecrm.domain.user.User;

import java.util.UUID;

/** One entry on a timeline. The sealed subject flattens into two fields, exactly one set. */
public final class ActivityViews {

    private ActivityViews() {
    }

    public static ActivityView of(Activity activity, User author) {
        return new ActivityView(
                activity.id().value(),
                activity.type().name(),
                activity.summary(),
                dealIn(activity.subject()),
                contactIn(activity.subject()),
                UserViews.of(author),
                activity.authorship().occurredAt());
    }

    private static UUID dealIn(ActivitySubject subject) {
        return subject instanceof DealSubject about ? about.deal().value() : null;
    }

    private static UUID contactIn(ActivitySubject subject) {
        return subject instanceof ContactSubject about ? about.contact().value() : null;
    }
}
