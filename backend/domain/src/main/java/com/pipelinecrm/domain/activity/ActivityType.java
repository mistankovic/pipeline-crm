package com.pipelinecrm.domain.activity;

/**
 * What happened. Calls and meetings are two-way contact with the customer and count as
 * engagement; a note is something we wrote to ourselves and does not.
 */
public enum ActivityType {

    NOTE(false),
    CALL(true),
    MEETING(true);

    private final boolean engagement;

    ActivityType(boolean engagement) {
        this.engagement = engagement;
    }

    public boolean isEngagement() {
        return engagement;
    }
}
