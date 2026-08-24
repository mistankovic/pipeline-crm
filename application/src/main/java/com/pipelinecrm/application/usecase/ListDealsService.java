package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ListDealsUseCase;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.deal.Deal;
import java.util.ArrayList;
import java.util.List;

public final class ListDealsService implements ListDealsUseCase {

    private final DealRepository deals;

    public ListDealsService(DealRepository deals) {
        this.deals = deals;
    }

    @Override
    public List<Deal> execute(Filter filter) {
        List<Deal> result = new ArrayList<>();
        for (Deal deal : deals.findAll()) {
            if (matches(deal, filter)) {
                result.add(deal);
            }
        }
        return List.copyOf(result);
    }

    private static boolean matches(Deal deal, Filter filter) {
        if (filter.stage() != null && deal.stage() != filter.stage()) {
            return false;
        }
        return filter.ownerId() == null || deal.ownerId().equals(filter.ownerId());
    }
}
