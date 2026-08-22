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

    @When("{word} creates a contact {string} at an unknown company")
    public void createsAContactAtAnUnknownCompany(String actor, String name) {
        world.attempt(() -> world.application.createContact.handle(
                new CreateContact.NewContact(UUID.randomUUID(), name, "ghost@example.com")));
    }

    @When("{word} creates a contact {string} at {string} with email {string}")
    public void createsAContactWithEmail(String actor, String name, String company, String email) {
        world.attempt(() -> world.application.createContact.handle(
                new CreateContact.NewContact(world.company(company), name, email)));
    }

    @When("a deal {string} is created for {string} with an unknown owner")
    public void aDealIsCreatedWithAnUnknownOwner(String title, String company) {
        world.attempt(() -> world.application.createDeal.handle(
                new com.pipelinecrm.application.port.in.CreateDeal.NewDeal(title,
                        world.company(company), UUID.randomUUID(),
                        java.math.BigDecimal.valueOf(1000), "EUR", 50)));
    }

    @When("{word} creates a deal {string} for an unknown company worth {amount}")
    public void createsADealForAnUnknownCompany(String actor, String title, Amount amount) {
        world.attempt(() -> world.application.createDeal.handle(
                new com.pipelinecrm.application.port.in.CreateDeal.NewDeal(title,
                        UUID.randomUUID(), world.person(actor),
                        amount.value(), amount.currency(), amount.probability())));
    }
}
