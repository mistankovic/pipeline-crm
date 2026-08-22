package com.pipelinecrm.adapter.persistence.row;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** A row of the contacts table. */
@Entity
@Table(name = "contacts")
public class ContactRow {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    protected ContactRow() {
    }

    public ContactRow(UUID id, UUID companyId, String name, String email) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
