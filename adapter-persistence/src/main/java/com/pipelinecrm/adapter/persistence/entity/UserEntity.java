package com.pipelinecrm.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    protected UserEntity() {}

    public UserEntity(UUID id, String email, String name, String role, String passwordHash) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.passwordHash = passwordHash;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    public String role() {
        return role;
    }

    public String passwordHash() {
        return passwordHash;
    }
}
