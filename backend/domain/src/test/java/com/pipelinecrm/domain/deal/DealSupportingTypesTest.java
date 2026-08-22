package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.InvariantViolation;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DealSupportingTypesTest {

    private static final CompanyId ACME = CompanyId.of(UUID.randomUUID());
    private static final UserId SAM = UserId.of(UUID.randomUUID());
    private static final DealParties PARTIES = new DealParties(ACME, SAM);
    private static final DealTerms TERMS = new DealTerms(Money.of("100", "EUR"), Probability.of(1));
    private static final DealId A_DEAL = DealId.of(UUID.randomUUID());

    @Test
    void parties_recognise_their_owner() {
        assertThat(PARTIES.ownedBy(SAM)).isTrue();
    }

    @Test
    void parties_do_not_recognise_somebody_else_as_owner() {
        assertThat(PARTIES.ownedBy(UserId.of(UUID.randomUUID()))).isFalse();
    }

    @Test
    void parties_refuse_to_exist_without_a_company() {
        assertThatThrownBy(() -> new DealParties(null, SAM)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void parties_refuse_to_exist_without_an_owner() {
        assertThatThrownBy(() -> new DealParties(ACME, null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void terms_weight_the_value_by_the_probability() {
        DealTerms terms = new DealTerms(Money.of("1000.00", "EUR"), Probability.of(20));

        assertThat(terms.weighted()).isEqualTo(Money.of("200.00", "EUR"));
    }

    @Test
    void repricing_terms_keeps_the_probability() {
        DealTerms repriced = TERMS.pricedAt(Money.of("500", "EUR"));

        assertThat(repriced).isEqualTo(new DealTerms(Money.of("500", "EUR"), Probability.of(1)));
    }

    @Test
    void reweighting_terms_keeps_the_value() {
        DealTerms reweighted = TERMS.weightedAt(Probability.of(60));

        assertThat(reweighted).isEqualTo(new DealTerms(Money.of("100", "EUR"), Probability.of(60)));
    }

    @Test
    void terms_refuse_to_exist_without_a_value() {
        Probability probability = Probability.of(1);

        assertThatThrownBy(() -> new DealTerms(null, probability)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void terms_refuse_to_exist_without_a_probability() {
        Money value = Money.of("1", "EUR");

        assertThatThrownBy(() -> new DealTerms(value, null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void terms_refuse_to_be_repriced_to_nothing() {
        assertThatThrownBy(() -> TERMS.pricedAt(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void terms_refuse_to_be_reweighted_to_nothing() {
        assertThatThrownBy(() -> TERMS.weightedAt(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_snapshot_refuses_to_exist_without_an_id() {
        assertThatThrownBy(() -> new DealSnapshot(null, "t", PARTIES, TERMS, DealStage.LEAD))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_snapshot_refuses_to_exist_without_a_title() {
        assertThatThrownBy(() -> new DealSnapshot(A_DEAL, " ", PARTIES, TERMS, DealStage.LEAD))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_snapshot_refuses_to_exist_without_parties() {
        assertThatThrownBy(() -> new DealSnapshot(A_DEAL, "t", null, TERMS, DealStage.LEAD))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_snapshot_refuses_to_exist_without_terms() {
        assertThatThrownBy(() -> new DealSnapshot(A_DEAL, "t", PARTIES, null, DealStage.LEAD))
                .isInstanceOf(InvariantViolation.class);
    }

    @Test
    void a_snapshot_refuses_to_exist_without_a_stage() {
        assertThatThrownBy(() -> new DealSnapshot(A_DEAL, "t", PARTIES, TERMS, null))
                .isInstanceOf(InvariantViolation.class);
    }
}
