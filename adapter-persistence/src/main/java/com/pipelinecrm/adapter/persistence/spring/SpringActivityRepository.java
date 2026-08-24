package com.pipelinecrm.adapter.persistence.spring;

import com.pipelinecrm.adapter.persistence.entity.ActivityEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringActivityRepository extends JpaRepository<ActivityEntity, UUID> {

    List<ActivityEntity> findByDealId(UUID dealId);

    List<ActivityEntity> findByContactId(UUID contactId);
}
