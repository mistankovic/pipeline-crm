package com.pipelinecrm.adapter.persistence.row;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** A row of the deals table. */
@Entity
@Table(name = "deals")
public class DealRow {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "value_amount", nullable = false)
    private BigDecimal valueAmount;

    @Column(name = "value_currency", nullable = false, length = 3)
    private String valueCurrency;

    @Column(nullable = false)
    private int probability;

    @Column(nullable = false)
    private String stage;

    protected DealRow() {
    }

    public DealRow(UUID id, String title, DealParticipants participants, DealValuation valuation) {
        this.id = id;
        this.title = title;
        this.companyId = participants.companyId();
        this.ownerId = participants.ownerId();
        this.valueAmount = valuation.amount();
        this.valueCurrency = valuation.currency();
        this.probability = valuation.probability();
        this.stage = valuation.stage();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public String getValueCurrency() {
        return valueCurrency;
    }

    public int getProbability() {
        return probability;
    }

    public String getStage() {
        return stage;
    }

    /** Who a deal row belongs to. Groups two of the constructor's arguments so it stays at four. */
    public record DealParticipants(UUID companyId, UUID ownerId) {
    }

    /** What a deal row is worth and where it has got to. */
    public record DealValuation(BigDecimal amount, String currency, int probability, String stage) {
    }
}
