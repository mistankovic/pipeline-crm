package com.pipelinecrm.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class CompanyEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String name;

    protected CompanyEntity() {}

    public CompanyEntity(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }
}
