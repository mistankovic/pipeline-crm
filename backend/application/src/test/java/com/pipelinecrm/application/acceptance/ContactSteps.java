package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.CreateContact;
import io.cucumber.java.en.When;

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


}
