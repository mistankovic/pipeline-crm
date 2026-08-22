package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.mapping.DealMapping;
import com.pipelinecrm.adapter.persistence.repository.DealRows;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** The deal port, over JPA. */
@Repository
public class JpaDealRepository implements DealRepository {

    private final DealRows rows;

    public JpaDealRepository(DealRows rows) {
        this.rows = rows;
    }

    @Override
    public Optional<Deal> findById(DealId id) {
        return rows.findById(id.value()).map(DealMapping::toDomain);
    }

    @Override
    public List<Deal> findAll() {
        return rows.findAll().stream().map(DealMapping::toDomain).toList();
    }

    @Override
    public List<Deal> findOwnedBy(UserId owner) {
        return rows.findByOwnerId(owner.value()).stream().map(DealMapping::toDomain).toList();
    }

    @Override
    public void save(Deal deal) {
        rows.save(DealMapping.toRow(deal));
    }
}
