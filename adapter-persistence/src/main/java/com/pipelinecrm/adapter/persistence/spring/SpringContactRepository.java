package com.pipelinecrm.adapter.persistence.spring;

import com.pipelinecrm.adapter.persistence.entity.ContactEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringContactRepository extends JpaRepository<ContactEntity, UUID> {}
