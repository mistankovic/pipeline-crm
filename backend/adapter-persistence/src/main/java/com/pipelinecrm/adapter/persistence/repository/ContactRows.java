package com.pipelinecrm.adapter.persistence.repository;

import com.pipelinecrm.adapter.persistence.row.ContactRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data's view of the contacts table. */
public interface ContactRows extends JpaRepository<ContactRow, UUID> {

    List<ContactRow> findByCompanyId(UUID companyId);
}
