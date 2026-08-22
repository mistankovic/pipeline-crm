package com.pipelinecrm.adapter.persistence.row;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** A row of the companies table. */
@Entity
@Table(name = "companies")
public class CompanyRow {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    protected CompanyRow() {
    }

    public CompanyRow(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
