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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns recorded activities into a timeline, naming who recorded each one.
 *
 * <p>The authors of *these* activities are read, in one query — not every user in the system.
 * An empty timeline reads nobody.
 */
public final class Timelines {

    private final Parties parties;

    public Timelines(Parties parties) {
        this.parties = parties;
    }

    public List<ActivityView> of(Collection<Activity> activities) {
        Map<UserId, User> authors = parties.usersById(authorsOf(activities));
        return activities.stream()
                .map(activity -> ActivityViews.of(activity, authorOf(activity, authors)))
                .toList();
    }

    private Set<UserId> authorsOf(Collection<Activity> activities) {
        return activities.stream().map(activity -> activity.authorship().author()).collect(Collectors.toSet());
    }

    private User authorOf(Activity activity, Map<UserId, User> authors) {
        UserId author = activity.authorship().author();
        return Required.found(Optional.ofNullable(authors.get(author)), author);
    }
}
