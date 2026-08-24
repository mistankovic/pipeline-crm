package com.pipelinecrm.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class ActivityEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(name = "deal_id")
    private UUID dealId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActivityEntity() {}

    public ActivityEntity(
            UUID id,
            String type,
            String body,
            UUID dealId,
            UUID contactId,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.type = type;
        this.body = body;
        this.dealId = dealId;
        this.contactId = contactId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public String type() {
        return type;
    }

    public String body() {
        return body;
    }

    public UUID dealId() {
        return dealId;
    }

    public UUID contactId() {
        return contactId;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
