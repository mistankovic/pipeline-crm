package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;

import java.util.List;
import java.util.Optional;

/** How the use cases reach deals. Implemented outside; named from the inside. */
public interface DealRepository {

    Optional<Deal> findById(DealId id);

    List<Deal> findAll();

    List<Deal> findOwnedBy(UserId owner);

    void save(Deal deal);
}
