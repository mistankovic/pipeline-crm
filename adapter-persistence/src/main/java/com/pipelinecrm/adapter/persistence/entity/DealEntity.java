package com.pipelinecrm.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "deals")
public class DealEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int probability;

    @Column(nullable = false, length = 32)
    private String stage;

    protected DealEntity() {}

    public DealEntity(
            UUID id,
            UUID companyId,
            UUID ownerId,
            String title,
            BigDecimal amount,
            String currency,
            int probability,
            String stage) {
        this.id = id;
        this.companyId = companyId;
        this.ownerId = ownerId;
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.probability = probability;
        this.stage = stage;
    }

    public UUID id() {
        return id;
    }

    public UUID companyId() {
        return companyId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String title() {
        return title;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public int probability() {
        return probability;
    }

    public String stage() {
        return stage;
    }
}
