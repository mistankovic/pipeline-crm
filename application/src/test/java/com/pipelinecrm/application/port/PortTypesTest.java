package com.pipelinecrm.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import com.pipelinecrm.application.port.in.ChangeDealStageUseCase;
import com.pipelinecrm.application.port.in.CreateCompanyUseCase;
import com.pipelinecrm.application.port.in.CreateContactUseCase;
import com.pipelinecrm.application.port.in.CreateDealUseCase;
import com.pipelinecrm.application.port.in.ListDealsUseCase;
import com.pipelinecrm.application.port.in.LoginUseCase;
import com.pipelinecrm.application.port.in.RecordActivityUseCase;
import com.pipelinecrm.application.port.in.UpdateCompanyUseCase;
import com.pipelinecrm.application.port.in.UpdateContactUseCase;
import com.pipelinecrm.application.port.in.UpdateDealUseCase;
import com.pipelinecrm.application.port.in.ViewDealUseCase;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortTypesTest {

    @Test
    void commandRecordsHoldTheSubmittedValues() {
        UserId userId = UserId.generate();
        CompanyId companyId = CompanyId.generate();
        ContactId contactId = ContactId.generate();
        DealId dealId = DealId.generate();
        Money money = Money.of("1.00", "USD");
        Probability probability = Probability.of(10);

        assertThat(new CreateCompanyUseCase.Command("Acme").name()).isEqualTo("Acme");
        assertThat(new UpdateCompanyUseCase.Command(companyId, "Acme Inc").companyId()).isEqualTo(companyId);
        assertThat(new CreateContactUseCase.Command(companyId, "Pat", "p@a.com").email()).isEqualTo("p@a.com");
        assertThat(new UpdateContactUseCase.Command(contactId, "Pat", null).contactId()).isEqualTo(contactId);
        assertThat(new CreateDealUseCase.Command(userId, companyId, userId, "D", money, probability).title())
                .isEqualTo("D");
        assertThat(new UpdateDealUseCase.Command(userId, dealId, "D", money, probability).dealId()).isEqualTo(dealId);
        assertThat(new ChangeDealStageUseCase.Command(userId, dealId, DealStage.QUALIFIED).target())
                .isEqualTo(DealStage.QUALIFIED);
        assertThat(new RecordActivityUseCase.Command(userId, ActivityType.NOTE, "hi", dealId, null).body())
                .isEqualTo("hi");
        assertThat(new ViewDealUseCase.Result(null, List.of()).activities()).isEmpty();
        assertThat(new ListDealsUseCase.Filter(DealStage.LEAD, userId).stage()).isEqualTo(DealStage.LEAD);
        assertThat(new LoginUseCase.Command("a@b.com", "secret").email()).isEqualTo("a@b.com");
        assertThat(new LoginUseCase.Result(userId, "token").token()).isEqualTo("token");
    }
}
