package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;
import java.util.List;
import java.util.Optional;

public interface DealRepository {

    Optional<Deal> findById(DealId id);

    List<Deal> findAll();

    void save(Deal deal);
}
