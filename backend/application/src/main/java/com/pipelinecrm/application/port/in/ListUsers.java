package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.UserView;

import java.util.List;

/**
 * Everyone, for the owner picker on a new deal and the owner filter on the board. There is
 * still no way to create a user (decision D-13); this only reads the ones the migration seeded.
 */
public interface ListUsers {

    List<UserView> handle();
}
