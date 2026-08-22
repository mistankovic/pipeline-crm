package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.view.ActivityView;
import com.pipelinecrm.application.view.ActivityViews;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns recorded activities into a timeline, naming who recorded each one. Authors are read
 * once for the whole timeline rather than once per entry.
 */
public final class Timelines {

    private final Parties parties;

    public Timelines(Parties parties) {
        this.parties = parties;
    }

    public List<ActivityView> of(Collection<Activity> activities) {
        Map<UserId, User> authors = parties.everyone();
        return activities.stream().map(activity -> ActivityViews.of(activity, authorOf(activity, authors))).toList();
    }

    private User authorOf(Activity activity, Map<UserId, User> authors) {
        UserId author = activity.authorship().author();
        return Required.found(Optional.ofNullable(authors.get(author)), "user", author);
    }
}
