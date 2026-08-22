package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.application.testing.UseCases;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.application.view.ForecastView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What one scenario knows: the wired use cases, the names the scenario gave things, and
 * whatever the last step produced.
 *
 * <p>Scenarios talk about "Sam" and "Acme renewal"; the system talks in UUIDs. This is the
 * only place that translates between the two, so the step definitions read like the feature
 * files rather than like plumbing.
 */
public final class World {

    public final UseCases application = new UseCases();

    public World() {
    }

    private final Map<String, UUID> people = new LinkedHashMap<>();
    private final Map<String, UUID> companies = new LinkedHashMap<>();
    private final Map<String, UUID> contacts = new LinkedHashMap<>();
    private final Map<String, UUID> deals = new LinkedHashMap<>();

    private RuntimeException failure;
    private ForecastView forecast;

    void rememberPerson(String name, UUID id) {
        people.put(name, id);
    }

    void rememberCompany(String name, UUID id) {
        companies.put(name, id);
    }

    void rememberContact(String name, UUID id) {
        contacts.put(name, id);
    }

    void rememberDeal(String title, UUID id) {
        deals.put(title, id);
    }

    UUID person(String name) {
        return required(people, name, "person");
    }

    UUID company(String name) {
        return required(companies, name, "company");
    }

    UUID contact(String name) {
        return required(contacts, name, "contact");
    }

    UUID deal(String title) {
        return required(deals, title, "deal");
    }

    DealView currentStateOf(String title) {
        return application.viewDeal.handle(deal(title)).deal();
    }

    /** Runs a step that the scenario expects to be refused, and keeps the refusal for the Then. */
    void attempt(Runnable action) {
        failure = null;
        try {
            action.run();
        } catch (RuntimeException refused) {
            failure = refused;
        }
    }

    void expectRefusal(Class<? extends RuntimeException> expected) {
        assertThat(failure)
                .describedAs("the attempted action should have been refused")
                .isInstanceOf(expected);
    }

    void expectRefusal(Class<? extends RuntimeException> expected, String messageFragment) {
        expectRefusal(expected);
        assertThat(failure).hasMessageContaining(messageFragment);
    }

    void rememberForecast(ForecastView produced) {
        forecast = produced;
    }

    ForecastView forecast() {
        assertThat(forecast).describedAs("no forecast has been produced").isNotNull();
        return forecast;
    }

    private UUID required(Map<String, UUID> known, String name, String kind) {
        UUID id = known.get(name);
        assertThat(id).describedAs("the scenario never introduced the %s \"%s\"", kind, name).isNotNull();
        return id;
    }
}
