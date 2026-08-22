package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class InMemoryDeals implements DealRepository {

    /**
     * Deals are stored and returned as copies.
     *
     * <p>The first version handed the caller the very object it had stored, so a use case
     * that mutated a deal and forgot to save it still appeared to work — and mutation
     * testing proved it, by deleting the save and watching every test still pass. A test
     * double that cannot tell you about a missing write is worse than no double.
     */
    private final Map<DealId, Deal> stored = new LinkedHashMap<>();

    @Override
    public Optional<Deal> findById(DealId id) {
        return Optional.ofNullable(stored.get(id)).map(InMemoryDeals::copyOf);
    }

    @Override
    public List<Deal> findAll() {
        return stored.values().stream().map(InMemoryDeals::copyOf).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Deal> findOwnedBy(UserId owner) {
        return stored.values().stream()
                .filter(deal -> deal.parties().ownedBy(owner))
                .map(InMemoryDeals::copyOf)
                .toList();
    }

    @Override
    public void save(Deal deal) {
        stored.put(deal.id(), copyOf(deal));
    }

    private static Deal copyOf(Deal deal) {
        return Deal.from(deal.snapshot());
    }
}
