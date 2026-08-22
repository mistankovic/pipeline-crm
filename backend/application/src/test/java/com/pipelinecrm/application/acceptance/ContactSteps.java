package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.CorrectContact;
import com.pipelinecrm.application.port.in.CreateContact;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

/** Creating contacts through the use case, including the ways that fail. */
public class ContactSteps {

    private final World world;

    public ContactSteps(World world) {
        this.world = world;
    }

    @When("{word} tries to create a contact {string} at an unknown company")
    public void triesToCreateAContactAtAnUnknownCompany(String actor, String name) {
        world.attempt(() -> world.application.createContact.handle(
                new CreateContact.NewContact(UUID.randomUUID(), name, "ghost@example.com")));
    }

    @When("{word} tries to create a contact {string} at {string} with email {string}")
    public void triesToCreateAContactWithEmail(String actor, String name, String company, String email) {
        world.attempt(() -> world.application.createContact.handle(
                new CreateContact.NewContact(world.company(company), name, email)));
    }

    @When("{word} corrects the contact {string} to {string} with email {string}")
    public void correctsTheContact(String actor, String name, String newName, String email) {
        world.rememberContact(newName, world.application.correctContact.handle(
                new CorrectContact.Corrections(world.contact(name), newName, email)).id());
    }

    @When("{word} tries to correct the contact {string} to {string} with email {string}")
    public void triesToCorrectTheContact(String actor, String name, String newName, String email) {
        world.attempt(() -> world.application.correctContact.handle(
                new CorrectContact.Corrections(world.contact(name), newName, email)));
    }

    @When("{word} tries to correct a contact that does not exist")
    public void triesToCorrectAnUnknownContact(String actor) {
        world.attempt(() -> world.application.correctContact.handle(
                new CorrectContact.Corrections(UUID.randomUUID(), "Nobody", "nobody@example.com")));
    }

    @Then("the contact {string} has email {string}")
    public void theContactHasEmail(String name, String email) {
        assertThat(storedContact(name).email()).isEqualTo(email);
    }

    @Then("the contact {string} still works for {string}")
    public void theContactStillWorksFor(String name, String company) {
        assertThat(world.application.listContacts.atCompany(world.company(company)))
                .extracting("id").contains(world.contact(name));
    }

    @Then("there is no contact called {string}")
    public void thereIsNoContactCalled(String name) {
        assertThat(world.application.listContacts.everything()).extracting("name")
                .doesNotContain(name);
    }

    private com.pipelinecrm.application.view.ContactView storedContact(String name) {
        return world.application.listContacts.everything().stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stored contact called \"" + name + "\""));
    }
}
