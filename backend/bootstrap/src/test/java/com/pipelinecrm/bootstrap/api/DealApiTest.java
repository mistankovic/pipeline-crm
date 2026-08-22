package com.pipelinecrm.bootstrap.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The deal endpoints, driven over HTTP with real tokens.
 *
 * <p>The refusals matter more than the successes here: the point of this layer is that a
 * domain rule reaches the caller as the right status code, and that no rule can be bypassed
 * by talking to the API instead of to the use case.
 */
class DealApiTest extends ApiTest {

    private String acme;

    @BeforeEach
    void createACompany() throws Exception {
        MvcResult created = http.perform(as(post("/api/companies"), sam())
                .content("""
                        {"name": "Acme"}""")).andReturn();
        acme = read(created).get("id").asText();
    }

    private JsonNode createDeal(String title, String amount, int probability) throws Exception {
        MvcResult created = http.perform(as(post("/api/deals"), sam())
                        .content("""
                                {"title": "%s", "companyId": "%s", "value": %s,
                                 "currency": "EUR", "probability": %d}"""
                                .formatted(title, acme, amount, probability)))
                .andExpect(status().isCreated())
                .andReturn();
        return read(created);
    }

    private void move(String deal, String stage, String token) throws Exception {
        http.perform(as(patch("/api/deals/" + deal + "/stage"), token)
                .content("""
                        {"stage": "%s"}""".formatted(stage))).andExpect(status().isOk());
    }

    private void log(String deal, String type, String token) throws Exception {
        http.perform(as(post("/api/activities"), token)
                .content("""
                        {"dealId": "%s", "type": "%s", "summary": "spoke"}""".formatted(deal, type)))
                .andExpect(status().isCreated());
    }

    @Test
    void a_new_deal_is_created_as_a_lead_owned_by_the_caller() throws Exception {
        JsonNode deal = createDeal("Acme renewal", "10000", 50);

        assert deal.get("stage").asText().equals("LEAD");
        http.perform(as(get("/api/deals/" + deal.get("id").asText()), sam()))
                .andExpect(jsonPath("$.deal.owner.email").value(SAM))
                .andExpect(jsonPath("$.deal.company.name").value("Acme"))
                .andExpect(jsonPath("$.deal.weightedValue.amount").value(5000.00));
    }

    @Test
    void a_new_deal_advertises_where_it_may_go_next() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(get("/api/deals/" + deal), sam()))
                .andExpect(jsonPath("$.deal.allowedTransitions", hasItems("QUALIFIED", "CLOSED_LOST")));
    }

    @Test
    void the_owner_may_advance_their_deal() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/stage"), sam())
                        .content("""
                                {"stage": "QUALIFIED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("QUALIFIED"));
    }

    @Test
    void skipping_a_stage_is_409() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/stage"), sam())
                        .content("""
                                {"stage": "PROPOSAL"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IllegalStageTransition"));
    }

    @Test
    void another_salesperson_moving_a_deal_is_403() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/stage"), tokenFor(ROBIN, ROBIN_PASSWORD))
                        .content("""
                                {"stage": "QUALIFIED"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("StageChangeForbidden"));
    }

    @Test
    void a_manager_moving_somebody_elses_deal_is_allowed() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/stage"), tokenFor(MO, MO_PASSWORD))
                        .content("""
                                {"stage": "QUALIFIED"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void winning_without_evidence_is_409() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();
        move(deal, "QUALIFIED", sam());
        move(deal, "PROPOSAL", sam());
        move(deal, "NEGOTIATION", sam());

        http.perform(as(patch("/api/deals/" + deal + "/stage"), sam())
                        .content("""
                                {"stage": "CLOSED_WON"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("WinRequiresValueAndEngagement"));
    }

    @Test
    void winning_with_a_meeting_forces_the_probability_to_one_hundred() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();
        move(deal, "QUALIFIED", sam());
        move(deal, "PROPOSAL", sam());
        move(deal, "NEGOTIATION", sam());
        log(deal, "MEETING", sam());

        http.perform(as(patch("/api/deals/" + deal + "/stage"), sam())
                        .content("""
                                {"stage": "CLOSED_WON"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probability").value(100));
    }

    @Test
    void losing_forces_the_probability_to_zero() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/stage"), sam())
                        .content("""
                                {"stage": "CLOSED_LOST"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probability").value(0));
    }

    @Test
    void a_stage_that_does_not_exist_is_400_and_says_which_ones_do() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/stage"), sam())
                        .content("""
                                {"stage": "DAYDREAMING"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MalformedRequest"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NEGOTIATION")));
    }

    @Test
    void a_deal_that_does_not_exist_is_404() throws Exception {
        http.perform(as(get("/api/deals/00000000-0000-4000-8000-000000000000"), sam()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UnknownEntity"));
    }

    @Test
    void a_company_that_does_not_exist_is_404() throws Exception {
        http.perform(as(post("/api/deals"), sam())
                        .content("""
                                {"title": "Ghost", "companyId": "00000000-0000-4000-8000-000000000000",
                                 "value": 1, "currency": "EUR", "probability": 50}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void an_unknown_currency_is_400() throws Exception {
        http.perform(as(post("/api/deals"), sam())
                        .content("""
                                {"title": "Ghost", "companyId": "%s", "value": 1,
                                 "currency": "XYZ", "probability": 50}""".formatted(acme)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvariantViolation"));
    }

    @Test
    void a_probability_over_one_hundred_is_rejected_before_the_use_case_is_reached() throws Exception {
        http.perform(as(post("/api/deals"), sam())
                        .content("""
                                {"title": "Ghost", "companyId": "%s", "value": 1,
                                 "currency": "EUR", "probability": 150}""".formatted(acme)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidRequest"));
    }

    @Test
    void the_owner_may_reprice_an_open_deal() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/value"), sam())
                        .content("""
                                {"amount": 25000, "currency": "USD"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value.amount").value(25000.00))
                .andExpect(jsonPath("$.value.currency").value("USD"));
    }

    @Test
    void a_stranger_repricing_a_deal_is_403() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/value"), tokenFor(ROBIN, ROBIN_PASSWORD))
                        .content("""
                                {"amount": 0, "currency": "EUR"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void repricing_a_closed_deal_is_409() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();
        move(deal, "CLOSED_LOST", sam());

        http.perform(as(patch("/api/deals/" + deal + "/value"), sam())
                        .content("""
                                {"amount": 1, "currency": "EUR"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ClosedDealIsImmutable"));
    }

    @Test
    void the_owner_may_reweight_an_open_deal() throws Exception {
        String deal = createDeal("Acme renewal", "10000", 50).get("id").asText();

        http.perform(as(patch("/api/deals/" + deal + "/probability"), sam())
                        .content("""
                                {"probability": 80}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probability").value(80))
                .andExpect(jsonPath("$.weightedValue.amount").value(8000.00));
    }

    @Test
    void the_board_can_be_narrowed_to_one_owner() throws Exception {
        createDeal("Sams deal", "1000", 50);

        http.perform(as(get("/api/deals").param("ownerId", "11111111-1111-4111-8111-111111111111"), sam()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Sams deal")));
    }
}
