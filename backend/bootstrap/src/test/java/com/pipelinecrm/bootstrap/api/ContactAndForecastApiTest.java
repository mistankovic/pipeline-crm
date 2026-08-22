package com.pipelinecrm.bootstrap.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactAndForecastApiTest extends ApiTest {

    private String acme;

    @BeforeEach
    void createACompany() throws Exception {
        MvcResult created = http.perform(as(post("/api/companies"), sam())
                .content("""
                        {"name": "Acme"}""")).andReturn();
        acme = read(created).get("id").asText();
    }

    private String createContact(String name, String email) throws Exception {
        MvcResult created = http.perform(as(post("/api/contacts"), sam())
                        .content("""
                                {"companyId": "%s", "name": "%s", "email": "%s"}"""
                                .formatted(acme, name, email)))
                .andExpect(status().isCreated()).andReturn();
        return read(created).get("id").asText();
    }

    @Test
    void a_company_is_created_and_listed() throws Exception {
        http.perform(as(get("/api/companies"), sam()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Acme")));
    }

    @Test
    void a_company_with_no_name_is_400() throws Exception {
        http.perform(as(post("/api/companies"), sam()).content("""
                        {"name": "  "}""")).andExpect(status().isBadRequest());
    }

    @Test
    void a_company_is_renamed() throws Exception {
        http.perform(as(patch("/api/companies/" + acme), sam()).content("""
                        {"name": "Acme Holdings"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(acme))
                .andExpect(jsonPath("$.name").value("Acme Holdings"));

        http.perform(as(get("/api/companies"), sam()))
                .andExpect(jsonPath("$[*].name", hasItem("Acme Holdings")));
    }

    @Test
    void renaming_a_company_to_blank_is_400() throws Exception {
        http.perform(as(patch("/api/companies/" + acme), sam()).content("""
                        {"name": "  "}""")).andExpect(status().isBadRequest());
    }

    @Test
    void renaming_a_company_that_does_not_exist_is_404() throws Exception {
        http.perform(as(patch("/api/companies/" + UUID.randomUUID()), sam()).content("""
                        {"name": "Anything"}""")).andExpect(status().isNotFound());
    }

    @Test
    void a_contact_is_corrected() throws Exception {
        String cara = createContact("Cara Client", "cara@acme.test");

        http.perform(as(patch("/api/contacts/" + cara), sam()).content("""
                        {"name": "Cara Nguyen", "email": "cara.nguyen@acme.test"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cara))
                .andExpect(jsonPath("$.name").value("Cara Nguyen"))
                .andExpect(jsonPath("$.email").value("cara.nguyen@acme.test"));

        http.perform(as(get("/api/contacts?companyId=" + acme), sam()))
                .andExpect(jsonPath("$[*].name", hasItem("Cara Nguyen")));
    }

    @Test
    void correcting_a_contact_to_an_invalid_email_is_400() throws Exception {
        String cara = createContact("Cara Client", "cara@acme.test");

        http.perform(as(patch("/api/contacts/" + cara), sam()).content("""
                        {"name": "Cara Nguyen", "email": "not-an-email"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void correcting_a_contact_that_does_not_exist_is_404() throws Exception {
        http.perform(as(patch("/api/contacts/" + UUID.randomUUID()), sam()).content("""
                        {"name": "Nobody", "email": "nobody@acme.test"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void a_contact_cannot_be_moved_to_another_company_through_a_correction() throws Exception {
        String cara = createContact("Cara Client", "cara@acme.test");
        MvcResult other = http.perform(as(post("/api/companies"), sam()).content("""
                {"name": "Globex"}""")).andReturn();
        String globex = read(other).get("id").asText();

        // companyId is not part of the request body, so sending one must change nothing.
        http.perform(as(patch("/api/contacts/" + cara), sam()).content("""
                        {"name": "Cara Nguyen", "email": "cara@acme.test", "companyId": "%s"}"""
                        .formatted(globex)))
                .andExpect(status().isOk());

        http.perform(as(get("/api/contacts?companyId=" + globex), sam()))
                .andExpect(jsonPath("$.length()").value(0));
        http.perform(as(get("/api/contacts?companyId=" + acme), sam()))
                .andExpect(jsonPath("$[*].name", hasItem("Cara Nguyen")));
    }

    @Test
    void a_contact_is_created_at_a_company() throws Exception {
        createContact("Cara Client", "cara@acme.test");

        http.perform(as(get("/api/contacts").param("companyId", acme), sam()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Cara Client")));
    }

    @Test
    void a_contact_at_a_company_that_does_not_exist_is_404() throws Exception {
        http.perform(as(post("/api/contacts"), sam()).content("""
                        {"companyId": "00000000-0000-4000-8000-000000000000",
                         "name": "Ghost", "email": "ghost@acme.test"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void a_contact_with_an_address_that_is_not_one_is_refused_in_the_domains_words() throws Exception {
        http.perform(as(post("/api/contacts"), sam()).content("""
                        {"companyId": "%s", "name": "Cara", "email": "not-an-address"}""".formatted(acme)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvariantViolation"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("not an email address")));
    }

    @Test
    void an_activity_is_recorded_against_a_contact_and_appears_on_its_timeline() throws Exception {
        String cara = createContact("Cara", "cara2@acme.test");

        http.perform(as(post("/api/activities"), sam()).content("""
                        {"contactId": "%s", "type": "NOTE", "summary": "prefers email"}""".formatted(cara)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactId").value(cara))
                .andExpect(jsonPath("$.dealId").doesNotExist())
                .andExpect(jsonPath("$.author.email").value(SAM));

        http.perform(as(get("/api/contacts/" + cara + "/activities"), sam()))
                .andExpect(jsonPath("$[0].summary").value("prefers email"));
    }

    @Test
    void an_activity_about_both_a_deal_and_a_contact_is_400() throws Exception {
        String cara = createContact("Cara", "cara3@acme.test");

        http.perform(as(post("/api/activities"), sam()).content("""
                        {"contactId": "%s", "dealId": "00000000-0000-4000-8000-000000000000",
                         "type": "NOTE", "summary": "x"}""".formatted(cara)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MalformedRequest"));
    }

    @Test
    void an_activity_about_nothing_is_400() throws Exception {
        http.perform(as(post("/api/activities"), sam()).content("""
                        {"type": "NOTE", "summary": "x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MalformedRequest"));
    }

    @Test
    void an_activity_of_an_unknown_type_is_400() throws Exception {
        String cara = createContact("Cara", "cara4@acme.test");

        http.perform(as(post("/api/activities"), sam()).content("""
                        {"contactId": "%s", "type": "SMOKE_SIGNAL", "summary": "x"}""".formatted(cara)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvariantViolation"));
    }

    @Test
    void the_forecast_by_owner_names_the_owner() throws Exception {
        http.perform(as(post("/api/deals"), sam()).content("""
                {"title": "Forecastable", "companyId": "%s", "value": 2000,
                 "currency": "EUR", "probability": 25}""".formatted(acme)));

        http.perform(as(get("/api/forecast").param("by", "OWNER"), sam()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimension").value("OWNER"))
                .andExpect(jsonPath("$.lines[*].label", hasItem("Sam Sales")));
    }

    @Test
    void the_forecast_by_stage_groups_by_stage() throws Exception {
        http.perform(as(get("/api/forecast").param("by", "STAGE"), sam()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimension").value("STAGE"));
    }

    @Test
    void a_dimension_that_does_not_exist_is_400() throws Exception {
        http.perform(as(get("/api/forecast").param("by", "PHASE_OF_THE_MOON"), sam()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MalformedRequest"));
    }

    @Test
    void the_users_can_be_listed_for_the_owner_picker() throws Exception {
        http.perform(as(get("/api/users"), sam()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(MO)));
    }
}
