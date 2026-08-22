package com.pipelinecrm.adapter.persistence.repository;

import com.pipelinecrm.adapter.persistence.row.UserRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data's view of the users table. Not a port: a detail one adapter uses. */
public interface UserRows extends JpaRepository<UserRow, UUID> {

    Optional<UserRow> findByEmail(String email);
}
