package com.pipelinecrm.domain.activity;

public enum ActivityType {
    NOTE,
    CALL,
    MEETING;

    public boolean qualifiesCloseWon() {
        return this == CALL || this == MEETING;
    }
}
