package com.pipelinecrm.adapter.persistence.spring;

import com.pipelinecrm.adapter.persistence.entity.DealEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDealRepository extends JpaRepository<DealEntity, UUID> {}
