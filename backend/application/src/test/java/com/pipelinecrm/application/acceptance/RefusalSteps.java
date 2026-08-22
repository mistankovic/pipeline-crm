package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.domain.deal.ClosedDealIsImmutable;
import com.pipelinecrm.domain.deal.IllegalStageTransition;
import com.pipelinecrm.domain.deal.StageChangeForbidden;
import com.pipelinecrm.domain.deal.WinRequiresValueAndEngagement;
import com.pipelinecrm.domain.shared.InvariantViolation;
import io.cucumber.java.en.Then;

/**
 * Every way the system says no. Each phrase in the feature files maps to exactly one
 * failure type, so a scenario that says "rejected because the deal is closed" cannot be
 * satisfied by the system failing for some other reason.
 */
public class RefusalSteps {

    private final World world;

    public RefusalSteps(World world) {
        this.world = world;
    }

    @Then("the move is rejected because the transition is not allowed")
    public void rejectedAsIllegalTransition() {
        world.expectRefusal(IllegalStageTransition.class);
    }

    @Then("the move is rejected because the deal has not earned a win")
    public void rejectedAsUnearnedWin() {
        world.expectRefusal(WinRequiresValueAndEngagement.class);
    }

    @Then("the move is rejected because the user has no authority over the deal")
    public void moveRejectedAsUnauthorised() {
        world.expectRefusal(StageChangeForbidden.class);
    }

    @Then("the change is rejected because the user has no authority over the deal")
    public void changeRejectedAsUnauthorised() {
        world.expectRefusal(StageChangeForbidden.class);
    }

    @Then("the change is rejected because the deal is closed")
    public void rejectedAsClosed() {
        world.expectRefusal(ClosedDealIsImmutable.class);
    }

    @Then("the move is rejected because the deal does not exist")
    public void moveRejectedAsUnknownDeal() {
        world.expectRefusal(UnknownEntity.class, "no deal with id");
    }

    @Then("the change is rejected because the deal does not exist")
    public void changeRejectedAsUnknownDeal() {
        world.expectRefusal(UnknownEntity.class, "no deal with id");
    }

    @Then("the activity is rejected because the deal does not exist")
    public void activityRejectedAsUnknownDeal() {
        world.expectRefusal(UnknownEntity.class, "no deal with id");
    }

    @Then("the request is rejected because the deal does not exist")
    public void requestRejectedAsUnknownDeal() {
        world.expectRefusal(UnknownEntity.class, "no deal with id");
    }

    @Then("the request is rejected because the company does not exist")
    public void requestRejectedAsUnknownCompany() {
        world.expectRefusal(UnknownEntity.class, "no company with id");
    }

    @Then("the request is rejected because the user does not exist")
    public void requestRejectedAsUnknownUser() {
        world.expectRefusal(UnknownEntity.class, "no user with id");
    }

    @Then("the request is rejected because the contact does not exist")
    public void requestRejectedAsUnknownContact() {
        world.expectRefusal(UnknownEntity.class, "no contact with id");
    }

    @Then("the request is rejected because the value is not a valid amount")
    public void requestRejectedAsInvalidAmount() {
        world.expectRefusal(InvariantViolation.class, "currency");
    }

    @Then("the request is rejected because the probability is out of range")
    public void requestRejectedAsInvalidProbability() {
        world.expectRefusal(InvariantViolation.class, "between 0 and 100");
    }

    @Then("the request is rejected because the email address is not valid")
    public void requestRejectedAsInvalidEmail() {
        world.expectRefusal(InvariantViolation.class, "not an email address");
    }
}
