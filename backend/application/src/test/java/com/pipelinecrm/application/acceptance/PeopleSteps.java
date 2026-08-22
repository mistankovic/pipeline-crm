package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.port.in.CreateCompany;
import com.pipelinecrm.application.port.in.CreateContact;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;
import com.pipelinecrm.domain.user.UserRole;
import io.cucumber.java.en.Given;

import java.util.Locale;
import java.util.UUID;

/**
 * Who and what exists before a scenario starts. Users are seeded rather than created
 * through a use case, because there is no user-management use case: see decision D-13.
 */
public class PeopleSteps {

    private final World world;

    public PeopleSteps(World world) {
        this.world = world;
    }

    @Given("a salesperson {string}")
    public void aSalesperson(String name) {
        seed(name, UserRole.SALES);
    }

    @Given("a manager {string}")
    public void aManager(String name) {
        seed(name, UserRole.MANAGER);
    }

    @Given("a salesperson {string} with password {string}")
    public void aSalespersonWithPassword(String name, String password) {
        User user = seed(name, UserRole.SALES);
        world.application.passwords.set(user.id(), password);
    }

    @Given("a company {string}")
    public void aCompany(String name) {
        world.rememberCompany(name, world.application.createCompany.handle(
                new CreateCompany.NewCompany(name)).id());
    }

    @Given("a contact {string} at {string}")
    public void aContactAt(String name, String company) {
        world.rememberContact(name, world.application.createContact.handle(
                new CreateContact.NewContact(world.company(company), name, emailFor(name))).id());
    }

    private User seed(String name, UserRole role) {
        User user = new User(UserId.of(UUID.randomUUID()), EmailAddress.of(emailFor(name)), name, role);
        world.application.users.add(user);
        world.rememberPerson(name, user.id().value());
        return user;
    }

    private String emailFor(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com";
    }
}
