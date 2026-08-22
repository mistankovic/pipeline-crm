package com.pipelinecrm.application.usecase;

import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;

import java.util.Map;
import java.util.Optional;

/**
 * The companies and users a set of deals refers to, read once.
 *
 * <p>Lookups go through {@link Required} rather than through {@code Map.get}. An earlier
 * version read straight from the map and handed the result on, so a deal whose owner was
 * missing produced a {@link NullPointerException} — reported as a server fault — while the
 * single-deal path produced a clean "no user with id". Two answers to the same question.
 * See docs/reviews/stage-3-review.md, finding F-3.2.
 */
public record PartyIndex(Map<CompanyId, Company> companies, Map<UserId, User> users) {

    public Company companyOf(Deal deal) {
        CompanyId id = deal.parties().company();
        return Required.found(Optional.ofNullable(companies.get(id)), id);
    }

    public User ownerOf(Deal deal) {
        return user(deal.parties().owner());
    }

    public User user(UserId id) {
        return Required.found(Optional.ofNullable(users.get(id)), id);
    }
}
