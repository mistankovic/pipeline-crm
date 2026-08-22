package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.forecast.Forecast;
import com.pipelinecrm.domain.forecast.ForecastDimension;
import com.pipelinecrm.domain.forecast.ForecastGroup;
import com.pipelinecrm.domain.forecast.ForecastLine;
import com.pipelinecrm.domain.forecast.OwnerGroup;
import com.pipelinecrm.domain.forecast.StageGroup;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;

import java.util.Map;

/**
 * A forecast as a caller sees one. Each line carries a stable key to group on and a human
 * label to print: an owner's key is their id and their label is their name.
 */
public final class ForecastViews {

    private ForecastViews() {
    }

    public static ForecastView of(ForecastDimension dimension, Forecast forecast, Map<UserId, User> owners) {
        return new ForecastView(dimension.name(),
                forecast.lines().stream().map(line -> lineOf(line, owners)).toList());
    }

    private static ForecastLineView lineOf(ForecastLine line, Map<UserId, User> owners) {
        return new ForecastLineView(keyOf(line.group()), labelOf(line.group(), owners),
                MoneyViews.of(line.weightedValue()));
    }

    private static String keyOf(ForecastGroup group) {
        return switch (group) {
            case OwnerGroup owner -> owner.owner().value().toString();
            case StageGroup stage -> stage.stage().name();
        };
    }

    private static String labelOf(ForecastGroup group, Map<UserId, User> owners) {
        return switch (group) {
            case OwnerGroup owner -> nameOf(owner.owner(), owners);
            case StageGroup stage -> stage.stage().name();
        };
    }

    private static String nameOf(UserId owner, Map<UserId, User> owners) {
        User user = owners.get(owner);
        return user == null ? owner.value().toString() : user.name();
    }
}
