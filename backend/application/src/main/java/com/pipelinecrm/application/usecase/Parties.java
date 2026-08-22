package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The people and organisations a deal refers to.
 *
 * <p>Almost every use case has to turn an id from the outside into a company or a user, and
 * say clearly which one was missing when it cannot. Collecting that here keeps the lookup in
 * one place and keeps use-case constructors down to the ports they actually orchestrate.
 */
public final class Parties {

    private final CompanyRepository companies;
    private final UserRepository users;

    public Parties(CompanyRepository companies, UserRepository users) {
        this.companies = companies;
        this.users = users;
    }

    public Company company(UUID id) {
        CompanyId companyId = CompanyId.of(id);
        return Required.found(companies.findById(companyId), "company", companyId);
    }

    public User user(UUID id) {
        UserId userId = UserId.of(id);
        return Required.found(users.findById(userId), "user", userId);
    }

    public User ownerOf(Deal deal) {
        UserId owner = deal.parties().owner();
        return Required.found(users.findById(owner), "user", owner);
    }

    public Company companyOf(Deal deal) {
        CompanyId company = deal.parties().company();
        return Required.found(companies.findById(company), "company", company);
    }

    /** Everyone, indexed. Used to describe a page of deals without a query per deal. */
    public Map<UserId, User> everyone() {
        return index(users.findAll(), User::id);
    }

    public Map<CompanyId, Company> everyCompany() {
        return index(companies.findAll(), Company::id);
    }

    private static <K, V> Map<K, V> index(Collection<V> values, Function<V, K> key) {
        return values.stream().collect(Collectors.toMap(key, Function.identity()));
    }
}
