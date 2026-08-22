package com.pipelinecrm.adapter.persistence.row;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A row of the users table.
 *
 * <p>Rows are not entities. They carry the shape the database needs — including the password
 * hash, which the domain has no concept of — and they never contain behaviour. Everything
 * that turns a row into a domain object lives in a mapper next door.
 */
@Entity
@Table(name = "users")
public class UserRow {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    protected UserRow() {
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
