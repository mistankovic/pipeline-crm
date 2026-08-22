package com.pipelinecrm.adapter.persistence.repository;

import com.pipelinecrm.adapter.persistence.row.CompanyRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data's view of the companies table. */
public interface CompanyRows extends JpaRepository<CompanyRow, UUID> {
}
