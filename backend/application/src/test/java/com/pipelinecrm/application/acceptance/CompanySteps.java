package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.view.CompanyView;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Renaming a company after the fact, including the ways that fail. */
public class CompanySteps {

    private final World world;

    public CompanySteps(World world) {
        this.world = world;
    }

    @When("{word} renames the company {string} to {string}")
    public void renamesTheCompany(String actor, String name, String newName) {
        world.rememberCompany(newName,
                world.application.renameCompany.handle(world.company(name), newName).id());
    }

    @When("{word} tries to rename the company {string} to {string}")
    public void triesToRenameTheCompany(String actor, String name, String newName) {
        world.attempt(() -> world.application.renameCompany.handle(world.company(name), newName));
    }

    @When("{word} tries to rename a company that does not exist")
    public void triesToRenameAnUnknownCompany(String actor) {
        world.attempt(() -> world.application.renameCompany.handle(UUID.randomUUID(), "Anything"));
    }

    @Then("the companies are {string}")
    public void theCompaniesAre(String expected) {
        assertThat(world.application.listCompanies.handle()).extracting(CompanyView::name)
                .containsExactly(expected.split(", "));
    }

    @Then("the deal {string} still belongs to the company now called {string}")
    public void theDealStillBelongsTo(String title, String company) {
        assertThat(world.currentStateOf(title).company().name()).isEqualTo(company);
    }

    @Then("the company {string} keeps the id it had as {string}")
    public void theCompanyKeepsItsId(String newName, String oldName) {
        assertThat(world.company(newName)).isEqualTo(world.company(oldName));
    }
}
