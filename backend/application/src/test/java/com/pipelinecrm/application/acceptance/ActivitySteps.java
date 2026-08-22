package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.application.view.ActivityView;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Recording calls, meetings and notes, and reading a timeline back. */
public class ActivitySteps {

    private final World world;

    public ActivitySteps(World world) {
        this.world = world;
    }

    @Given("a {word} has been logged against {string}")
    public void aTypeHasBeenLoggedAgainst(String type, String title) {
        // No actor is named, so the deal's own owner recorded it — which is what the
        // scenario means by "has been logged".
        logAgainstDeal(world.deal(title), type, "recorded by the scenario", ownerOf(title));
    }

    @When("{word} logs a {word} {string} against the deal {string}")
    public void logsAgainstTheDeal(String actor, String type, String summary, String title) {
        world.attempt(() -> logAgainstDeal(world.deal(title), type, summary, world.person(actor)));
    }

    @When("{word} logs a {word} {string} against the contact {string}")
    public void logsAgainstTheContact(String actor, String type, String summary, String contact) {
        world.attempt(() -> world.application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutContact(world.contact(contact)), type, summary, world.person(actor))));
    }

    @When("{word} tries to log a {word} against an unknown deal")
    public void triesToLogAgainstAnUnknownDeal(String actor, String type) {
        world.attempt(() -> logAgainstDeal(UUID.randomUUID(), type, "about nothing", world.person(actor)));
    }

    @When("{word} logs a {word} against an unknown contact")
    public void logsAgainstAnUnknownContact(String actor, String type) {
        world.attempt(() -> world.application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutContact(UUID.randomUUID()), type, "about nothing", world.person(actor))));
    }

    @When("an unknown user logs a {word} against {string}")
    public void anUnknownUserLogs(String type, String title) {
        world.attempt(() -> logAgainstDeal(world.deal(title), type, "by nobody", UUID.randomUUID()));
    }

    @When("the timeline of an unknown deal is requested")
    public void theTimelineOfAnUnknownDealIsRequested() {
        world.attempt(() -> world.application.viewDeal.handle(UUID.randomUUID()));
    }

    @Then("the timeline of {string} has {int} entry")
    public void theTimelineHasOneEntry(String title, int entries) {
        assertThat(timelineOf(title)).hasSize(entries);
    }

    @Then("the timeline of {string} has {int} entries")
    public void theTimelineHasEntries(String title, int entries) {
        assertThat(timelineOf(title)).hasSize(entries);
    }

    @Then("the timeline of {string} contains a {word}")
    public void theTimelineContains(String title, String type) {
        assertThat(timelineOf(title)).extracting(ActivityView::type).contains(type);
    }

    @Then("the timeline of the contact {string} has {int} entry")
    public void theContactTimelineHasOneEntry(String contact, int entries) {
        assertThat(world.application.viewContactTimeline.handle(world.contact(contact))).hasSize(entries);
    }

    @Then("the only entry on the timeline of {string} was recorded by {word}")
    public void theOnlyEntryWasRecordedBy(String title, String author) {
        assertThat(timelineOf(title)).singleElement()
                .extracting(entry -> entry.author().id()).isEqualTo(world.person(author));
    }

    private List<ActivityView> timelineOf(String title) {
        return world.application.viewDeal.handle(world.deal(title)).timeline();
    }

    private void logAgainstDeal(UUID deal, String type, String summary, UUID author) {
        world.application.logActivity.handle(new LogActivity.NewActivity(
                new LogActivity.AboutDeal(deal), type, summary, author));
    }

    private UUID ownerOf(String title) {
        return world.currentStateOf(title).owner().id();
    }
}
