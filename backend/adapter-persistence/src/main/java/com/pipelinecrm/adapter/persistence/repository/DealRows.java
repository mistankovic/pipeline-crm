package com.pipelinecrm.adapter.persistence.repository;

import com.pipelinecrm.adapter.persistence.row.DealRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data's view of the deals table. */
public interface DealRows extends JpaRepository<DealRow, UUID> {

    List<DealRow> findByOwnerId(UUID ownerId);
}
