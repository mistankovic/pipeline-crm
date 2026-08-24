package com.pipelinecrm.domain.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.DealTitle;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForecastTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void sumsValueTimesProbabilityForOpenDealsByOwner() {
        UserId alice = UserId.generate();
        UserId bob = UserId.generate();
        Deal aliceLead = deal(alice, "1000.00", 50, DealStage.LEAD);
        Deal aliceClosed = deal(alice, "9000.00", 100, DealStage.CLOSED_WON);
        Deal bobProposal = deal(bob, "200.00", 25, DealStage.PROPOSAL);

        List<ForecastBucket> buckets = Forecast.byOwner(List.of(aliceLead, aliceClosed, bobProposal), USD);

        assertThat(buckets).hasSize(2);
        assertThat(bucket(buckets, alice).total().amount()).hasToString("500.00");
        assertThat(bucket(buckets, bob).total().amount()).hasToString("50.00");
    }

    @Test
    void groupsOpenDealsByStageIncludingEmptyOpenStages() {
        UserId owner = UserId.generate();
        Deal lead = deal(owner, "100.00", 10, DealStage.LEAD);
        Deal lost = deal(owner, "500.00", 0, DealStage.CLOSED_LOST);

        List<ForecastBucket> buckets = Forecast.byStage(List.of(lead, lost), USD);

        assertThat(buckets).hasSize(4);
        assertThat(stage(buckets, DealStage.LEAD).total().amount()).hasToString("10.00");
        assertThat(stage(buckets, DealStage.QUALIFIED).total().amount()).hasToString("0.00");
        assertThat(stage(buckets, DealStage.PROPOSAL).total().amount()).hasToString("0.00");
        assertThat(stage(buckets, DealStage.NEGOTIATION).total().amount()).hasToString("0.00");
    }

    @Test
    void emptyPipelineHasNoOwnerBuckets() {
        assertThat(Forecast.byOwner(List.of(), USD)).isEmpty();
    }

    @Test
    void rejectsNullDeals() {
        assertThatThrownBy(() -> Forecast.byOwner(null, USD)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Forecast.byStage(null, USD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotAddDifferentCurrencies() {
        WeightedValue usd = WeightedValue.zero(USD);
        WeightedValue eur = WeightedValue.zero(Currency.getInstance("EUR"));
        assertThatThrownBy(() -> usd.plus(eur)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Deal deal(UserId owner, String amount, int probability, DealStage stage) {
        return Deal.restore(
                DealId.generate(),
                CompanyId.generate(),
                owner,
                DealTitle.of("D"),
                Money.of(amount, "USD"),
                Probability.of(probability),
                stage);
    }

    private static ForecastBucket bucket(List<ForecastBucket> buckets, UserId owner) {
        return buckets.stream().filter(b -> b.ownerId().orElseThrow().equals(owner)).findFirst().orElseThrow();
    }

    private static ForecastBucket stage(List<ForecastBucket> buckets, DealStage stage) {
        return buckets.stream().filter(b -> b.stage().orElseThrow() == stage).findFirst().orElseThrow();
    }
}
