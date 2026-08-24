package com.pipelinecrm.domain.deal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DealStageTest {

    @ParameterizedTest
    @CsvSource({
        "LEAD,QUALIFIED,true",
        "LEAD,PROPOSAL,false",
        "LEAD,NEGOTIATION,false",
        "LEAD,CLOSED_WON,true",
        "LEAD,CLOSED_LOST,true",
        "LEAD,LEAD,false",
        "QUALIFIED,PROPOSAL,true",
        "QUALIFIED,LEAD,false",
        "QUALIFIED,NEGOTIATION,false",
        "QUALIFIED,CLOSED_WON,true",
        "PROPOSAL,NEGOTIATION,true",
        "PROPOSAL,QUALIFIED,false",
        "NEGOTIATION,CLOSED_WON,true",
        "NEGOTIATION,CLOSED_LOST,true",
        "NEGOTIATION,PROPOSAL,false",
        "CLOSED_WON,LEAD,false",
        "CLOSED_WON,CLOSED_LOST,false",
        "CLOSED_LOST,CLOSED_WON,false"
    })
    void transitionsMatchTheStateMachine(DealStage from, DealStage to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void nullTargetIsNotAllowed() {
        assertThat(DealStage.LEAD.canTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
            value = DealStage.class,
            names = {"LEAD", "QUALIFIED", "PROPOSAL", "NEGOTIATION"})
    void openStagesAreNotTerminal(DealStage stage) {
        assertThat(stage.isOpen()).isTrue();
        assertThat(stage.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
            value = DealStage.class,
            names = {"CLOSED_WON", "CLOSED_LOST"})
    void closedStagesAreTerminal(DealStage stage) {
        assertThat(stage.isOpen()).isFalse();
        assertThat(stage.isTerminal()).isTrue();
    }
}
