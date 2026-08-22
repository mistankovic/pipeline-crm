package com.pipelinecrm.adapter.persistence.repository;

import com.pipelinecrm.adapter.persistence.row.ActivityRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data's view of the activities table. */
public interface ActivityRows extends JpaRepository<ActivityRow, UUID> {

    List<ActivityRow> findByDealIdOrderByOccurredAtAsc(UUID dealId);

    List<ActivityRow> findByContactIdOrderByOccurredAtAsc(UUID contactId);
}
