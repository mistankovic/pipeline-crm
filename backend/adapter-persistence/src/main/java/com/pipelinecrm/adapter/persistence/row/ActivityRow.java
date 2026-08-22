package com.pipelinecrm.adapter.persistence.row;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A row of the activities table. Exactly one of {@code dealId} and {@code contactId} is set;
 * the database enforces it with a check constraint, and the domain makes the other case
 * unrepresentable.
 */
@Entity
@Table(name = "activities")
public class ActivityRow {

    @Id
    private UUID id;

    @Column(name = "deal_id")
    private UUID dealId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String summary;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ActivityRow() {
    }

    public ActivityRow(UUID id, ActivitySubjectColumns subject, ActivityContent content, ActivityRecord record) {
        this.id = id;
        this.dealId = subject.dealId();
        this.contactId = subject.contactId();
        this.type = content.type();
        this.summary = content.summary();
        this.authorId = record.authorId();
        this.occurredAt = record.occurredAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDealId() {
        return dealId;
    }

    public UUID getContactId() {
        return contactId;
    }

    public String getType() {
        return type;
    }

    public String getSummary() {
        return summary;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /** The two nullable subject columns, exactly one of which is set. */
    public record ActivitySubjectColumns(UUID dealId, UUID contactId) {
    }

    /** What the activity was. */
    public record ActivityContent(String type, String summary) {
    }

    /** Who recorded it and when. */
    public record ActivityRecord(UUID authorId, Instant occurredAt) {
    }
}
