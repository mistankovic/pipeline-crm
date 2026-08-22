package com.pipelinecrm.domain.company;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.shared.Guard;

/** An organisation deals and contacts belong to. */
public record Company(CompanyId id, String name) {

    public Company {
        Guard.present(id, "company id");
        name = Guard.filled(name, "company name");
    }

    /**
     * The same company under a new name. Renaming is not creating: the identity is kept, so
     * every deal and contact already pointing here keeps pointing here.
     */
    public Company renamedTo(String newName) {
        return new Company(id, newName);
    }
}
