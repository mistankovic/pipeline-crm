package com.pipelinecrm.adapter.persistence.spring;

import com.pipelinecrm.adapter.persistence.entity.CompanyEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringCompanyRepository extends JpaRepository<CompanyEntity, UUID> {}
