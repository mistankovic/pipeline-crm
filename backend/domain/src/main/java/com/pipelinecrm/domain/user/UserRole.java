package com.pipelinecrm.domain.user;

/** What a user is allowed to do. A manager may act on any deal; a salesperson only on their own. */
public enum UserRole {

    SALES,
    MANAGER;

    public boolean grantsAuthorityOverAnyDeal() {
        return this == MANAGER;
    }
}
