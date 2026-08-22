package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.view.ActivityView;
import com.pipelinecrm.application.view.ActivityViews;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityAuthorship;
import com.pipelinecrm.domain.activity.ActivitySubject;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.user.User;

import java.time.Clock;

/** Records a note, call or meeting against a deal or a contact that exists. */
public final class LogActivityInteractor implements LogActivity {

    private final ActivityRepository activities;
    private final ActivitySubjects subjects;
    private final Parties parties;
    private final Recording recording;

    public LogActivityInteractor(ActivityRepository activities, ActivitySubjects subjects,
                                 Parties parties, Recording recording) {
        this.activities = activities;
        this.subjects = subjects;
        this.parties = parties;
        this.recording = recording;
    }

    @Override
    public ActivityView handle(NewActivity request) {
        return recording.writing().transactions().execute(() -> record(request));
    }

    private ActivityView record(NewActivity request) {
        User author = parties.user(request.authorId());
        ActivitySubject subject = subjects.resolve(request.about());
        Activity activity = new Activity(
                ActivityId.of(recording.writing().identifiers().newIdentifier()),
                subject, typeOf(request.type()), request.summary(),
                new ActivityAuthorship(author.id(), recording.clock().instant()));
        activities.save(activity);
        return ActivityViews.of(activity, author);
    }

    private ActivityType typeOf(String name) {
        try {
            return ActivityType.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new InvariantViolation("unknown activity type: " + name);
        }
    }

    /** What recording an activity needs beyond the things it looks up: identities, a unit of work, and the time. */
    public record Recording(WritingPorts writing, Clock clock) {
    }
}
