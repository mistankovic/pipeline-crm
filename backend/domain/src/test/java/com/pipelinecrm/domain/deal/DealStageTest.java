package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.Probability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static com.pipelinecrm.domain.deal.DealStage.CLOSED_LOST;
import static com.pipelinecrm.domain.deal.DealStage.CLOSED_WON;
import static com.pipelinecrm.domain.deal.DealStage.LEAD;
import static com.pipelinecrm.domain.deal.DealStage.NEGOTIATION;
import static com.pipelinecrm.domain.deal.DealStage.PROPOSAL;
import static com.pipelinecrm.domain.deal.DealStage.QUALIFIED;
import static org.assertj.core.api.Assertions.assertThat;

class DealStageTest {

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"LEAD", "QUALIFIED", "PROPOSAL", "NEGOTIATION"})
    void an_open_stage_reports_itself_open(DealStage stage) {
        assertThat(stage.isOpen()).isTrue();
        assertThat(stage.isClosed()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"CLOSED_WON", "CLOSED_LOST"})
    void a_closed_stage_reports_itself_closed(DealStage stage) {
        assertThat(stage.isClosed()).isTrue();
        assertThat(stage.isOpen()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"LEAD,QUALIFIED", "QUALIFIED,PROPOSAL", "PROPOSAL,NEGOTIATION", "NEGOTIATION,CLOSED_WON"})
    void each_open_stage_advances_to_exactly_one_successor(DealStage from, DealStage to) {
        assertThat(from.allowsTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"LEAD", "QUALIFIED", "PROPOSAL", "NEGOTIATION"})
    void any_open_stage_may_be_lost(DealStage stage) {
        assertThat(stage.allowsTransitionTo(CLOSED_LOST)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"LEAD,PROPOSAL", "LEAD,NEGOTIATION", "LEAD,CLOSED_WON", "QUALIFIED,NEGOTIATION",
            "QUALIFIED,CLOSED_WON", "PROPOSAL,CLOSED_WON"})
    void a_stage_may_not_be_skipped(DealStage from, DealStage to) {
        assertThat(from.allowsTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"QUALIFIED,LEAD", "PROPOSAL,QUALIFIED", "NEGOTIATION,PROPOSAL", "NEGOTIATION,LEAD"})
    void a_deal_never_moves_backwards(DealStage from, DealStage to) {
        assertThat(from.allowsTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DealStage.class)
    void a_closed_stage_is_terminal(DealStage target) {
        assertThat(CLOSED_WON.allowsTransitionTo(target)).isFalse();
        assertThat(CLOSED_LOST.allowsTransitionTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DealStage.class)
    void no_stage_transitions_to_nowhere(DealStage from) {
        assertThat(from.allowsTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"LEAD", "QUALIFIED", "PROPOSAL", "NEGOTIATION"})
    void a_stage_may_not_transition_to_itself(DealStage stage) {
        assertThat(stage.allowsTransitionTo(stage)).isFalse();
    }

    @Test
    void winning_forces_certainty() {
        assertThat(CLOSED_WON.forcedProbability()).contains(Probability.certain());
    }

    @Test
    void losing_forces_impossibility() {
        assertThat(CLOSED_LOST.forcedProbability()).contains(Probability.impossible());
    }

    @ParameterizedTest
    @EnumSource(value = DealStage.class, names = {"LEAD", "QUALIFIED", "PROPOSAL", "NEGOTIATION"})
    void an_open_stage_forces_no_probability(DealStage stage) {
        assertThat(stage.forcedProbability()).isEmpty();
    }

    @Test
    void the_pipeline_runs_lead_qualified_proposal_negotiation() {
        assertThat(LEAD.allowsTransitionTo(QUALIFIED)).isTrue();
        assertThat(QUALIFIED.allowsTransitionTo(PROPOSAL)).isTrue();
        assertThat(PROPOSAL.allowsTransitionTo(NEGOTIATION)).isTrue();
    }
}
