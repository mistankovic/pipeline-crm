package com.pipelinecrm.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "contacts")
public class ContactEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 320)
    private String email;

    protected ContactEntity() {}

    public ContactEntity(UUID id, UUID companyId, String name, String email) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.email = email;
    }

    public UUID id() {
        return id;
    }

    public UUID companyId() {
        return companyId;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }
}
