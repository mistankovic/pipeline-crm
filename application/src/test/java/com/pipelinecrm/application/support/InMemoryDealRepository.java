package com.pipelinecrm.application.support;

import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryDealRepository implements DealRepository {

    private final Map<DealId, Deal> store = new LinkedHashMap<>();
    public int saveCount;

    @Override
    public Optional<Deal> findById(DealId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Deal> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void save(Deal deal) {
        saveCount++;
        store.put(deal.id(), deal);
    }
}
