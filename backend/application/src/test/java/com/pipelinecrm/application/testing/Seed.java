package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.in.CreateCompany;
import com.pipelinecrm.application.port.in.CreateDeal;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;
import com.pipelinecrm.domain.user.UserRole;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/** Puts the world into a starting state, so each test shows only what it cares about. */
public final class Seed {

    private final UseCases application;

    public Seed(UseCases application) {
        this.application = application;
    }

    public User salesperson(String name) {
        return person(name, UserRole.SALES);
    }

    public User manager(String name) {
        return person(name, UserRole.MANAGER);
    }

    public UUID company(String name) {
        return application.createCompany.handle(new CreateCompany.NewCompany(name)).id();
    }

    public DealView deal(String title, UUID company, UUID owner, String amount) {
        return application.createDeal.handle(new CreateDeal.NewDeal(
                title, company, owner, owner, new BigDecimal(amount), "EUR", 50));
    }

    private User person(String name, UserRole role) {
        return application.users.add(new User(UserId.of(UUID.randomUUID()),
                EmailAddress.of(name.toLowerCase(Locale.ROOT) + "@example.com"), name, role));
    }
}
