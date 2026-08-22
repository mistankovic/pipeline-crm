package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guard;

/** Who a deal is with and who is responsible for it. */
public record DealParties(CompanyId company, UserId owner) {

    public DealParties {
        Guard.present(company, "company of a deal");
        Guard.present(owner, "owner of a deal");
    }

    public boolean ownedBy(UserId candidate) {
        return owner.equals(candidate);
    }
}
