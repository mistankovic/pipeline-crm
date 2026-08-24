package com.pipelinecrm.domain.company;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.shared.Guards;

public final class Company {

    private final CompanyId id;
    private CompanyName name;

    private Company(CompanyId id, CompanyName name) {
        this.id = Guards.notNull(id, "id");
        this.name = Guards.notNull(name, "name");
    }

    public static Company create(CompanyId id, CompanyName name) {
        return new Company(id, name);
    }

    public CompanyId id() {
        return id;
    }

    public CompanyName name() {
        return name;
    }

    public void rename(CompanyName newName) {
        this.name = Guards.notNull(newName, "newName");
    }
}
